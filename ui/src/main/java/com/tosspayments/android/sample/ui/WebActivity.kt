package com.tosspayments.android.sample.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.webkit.JsPromptResult
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebView.setWebContentsDebuggingEnabled
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.tosspayments.android.auth.interfaces.BrandPayAuthWebManager
import com.tosspayments.android.auth.model.BrandpayBiometricAuthException
import com.tosspayments.android.ocr.interfaces.BrandPayOcrWebManager
import kotlinx.coroutines.launch
import org.json.JSONObject

interface A
class WebActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private var url: String = ""
//    private var permissionRequest: PermissionRequest? = null


    private val brandPayAuthWebManager = BrandPayAuthWebManager(
        this
    ).apply {
        callback = object : BrandPayAuthWebManager.Callback {
            override fun onPostScript(script: String) {
                webView.loadUrl(script)
            }

            override fun onErrorOccurred(exception: BrandpayBiometricAuthException) {
                super.onErrorOccurred(exception)
                Log.d("onErrorOccurred", exception.message.toString())
                Log.d("onErrorOccurred", exception.errorCode.toString())
                Log.d("onErrorOccurred", exception.cause?.message.toString())
            }
        }
    }

    private val brandPayOcrWebManager = BrandPayOcrWebManager(this).apply {
        callback = object : BrandPayOcrWebManager.Callback {
            override fun onPostScript(script: String) {
                webView.loadUrl(script)
            }
        }
    }

    companion object {
        private const val EXTRA_WEB_URL = "url"

        fun getIntent(context: Context, webUrl: String): Intent {
            return Intent(context, WebActivity::class.java)
                .putExtra(EXTRA_WEB_URL, webUrl)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web)

        handleIntent(intent)
        initViews()
    }

    @SuppressLint("JavascriptInterface", "SetJavaScriptEnabled")
    private fun initViews() {
        webView = findViewById<WebView>(R.id.web_view).apply {
            settings.run {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = true
//                mediaPlaybackRequiresUserGesture = false
            }

            setWebContentsDebuggingEnabled(true)

//            brandPayOcrWebManager.addJavascriptInterface(this)
//            brandPayAuthWebManager.addJavascriptInterface(this)

//            addJavascriptInterface(object: A {
//                @JavascriptInterface
//                fun bridge(action: String, type: String, value: Boolean) {
//                    runOnUiThread {
//                        Log.d("aaaa", "action: $action, type: $type, value: $value")
//                        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
//                    }
//                }
//            }, "android")
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest?) {
//                this@WebActivity.permissionRequest = request
//                request?.resources?.forEach {
//                    Log.d("aaaa", it.toString())
//                }
//                if (ContextCompat.checkSelfPermission(this@WebActivity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
//                    request?.grant(request.resources) // 권한이 이미 허용된 경우 승인
//                } else {
//                    requestPermissionLauncher.launch(Manifest.permission.CAMERA)
//                }
                request?.resources?.forEach {
                    Log.d("aaaa", it.toString())
                }
//                CoroutineScope(Dispatchers.Main).launch {
//                    delay(8000L)
                    request?.grant(request.resources)
//                }


            }

            override fun onPermissionRequestCanceled(request: PermissionRequest?) {
                Log.d("aaaa", "onPermissionRequestCanceled")
                super.onPermissionRequestCanceled(request)
            }

            override fun onJsPrompt(
                view: WebView?,
                url: String?,
                message: String?,       // prompt(...)로 넘어온 문자열
                defaultValue: String?,
                result: JsPromptResult?
            ): Boolean {
                Log.d("aaaa", ">>>> ${message}")
                message?.let { jsonString ->
                    try {
                        val json = JSONObject(jsonString)
                        val funcName = json.getString("func")
                        val params = json.getJSONArray("param")
                        // Base64 디코딩
                        val arg1 = String(Base64.decode(params.getString(0), Base64.DEFAULT))
                        val arg2 = String(Base64.decode(params.getString(1), Base64.DEFAULT))
                        Log.d("aaaa", "arg1: $arg1  /  arg2: $arg2")

                        if (funcName == "checkPermission") {
                            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                            result?.confirm("")
                            return true
                        } else {
                            result?.confirm("")
                            return super.onJsPrompt(view, url, message, defaultValue, result)
                        }
                    } catch (e: Exception) {
                        // JSON 파싱 에러 등 처리
                        Log.d("aaaa", ">>>> ${e.message}")
                        result?.confirm("")
                    }
                }
                result?.confirm("")
                return super.onJsPrompt(view, url, message, defaultValue, result)
            }
        }

//        webView.loadUrl("https://forhost.kr/tosspayments/sample/brandpay/index.php?method=brandpay&type=live&customerKey=youjunlee")
        webView.loadUrl("https://testbox.tosspayments.com/owners/jh.yang/%EB%B8%8C%EB%9E%9C%EB%93%9C%ED%8E%98%EC%9D%B4%20%EB%B9%84%EB%B0%80%EB%B2%88%ED%98%B8%20%EC%84%A4%EC%A0%95")

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    finish()
                }
            }
        })
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Log.d("aaaa", "requestPermissionLauncher $granted")

            if (granted) {
                // 권한이 허용된 경우, WebView의 요청을 승인
//                permissionRequest?.grant(permissionRequest?.resources)
                webView.evaluateJavascript("console.log('send true');document.getElementsByTagName('iframe')[0].contentWindow.postMessage({ type: 'cameraPermission', permitted: 'true' }, '*');", {})
            } else {
                // 권한이 거부된 경우, 요청을 취소
//                permissionRequest?.deny()
                webView.evaluateJavascript("console.log('send true');document.getElementsByTagName('iframe')[0].contentWindow.postMessage({ type: 'cameraPermission', permitted: 'false' }, '*');", {})
            }
//            permissionRequest = null
        }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
//        url = intent?.getStringExtra(EXTRA_WEB_URL).orEmpty()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        brandPayOcrWebManager.handleActivityResult(requestCode, resultCode, data)

        /**
         * 직접 Handling할 경우
        if (requestCode == BrandPayOcrWebManager.REQUEST_CODE_CARD_SCAN) {
        data?.getStringExtra(BrandPayOcrWebManager.EXTRA_CARD_SCAN_RESULT_SCRIPT)?.let { resultScript ->
        webView.loadUrl(resultScript)
        }
        }
         **/
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Configuration 변경 시 아무 작업도 하지 않음으로써 WebView 인스턴스 유지
        Log.d("WebActivity", "Configuration changed: ${newConfig.orientation}")
    }
}