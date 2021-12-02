package com.tosspayments.android.utilsample

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import com.kangdroid.android.ocr.interfaces.ConnectPayOcrJavascriptInterface
import com.tosspayments.android.auth.interfaces.ConnectPayAuthJavascriptInterface

class WebActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private var url: String = ""

    private val connectPayAuthInterface =
        ConnectPayAuthJavascriptInterface(this@WebActivity).apply {
            callback = object : ConnectPayAuthJavascriptInterface.Callback {
                override fun onSuccess(data: String) {
                    webView.loadUrl(data)
                }

                override fun onError(message: String) {
                    webView.loadUrl(message)
                }
            }
        }

    private val connectPayOcrInterface =
        ConnectPayOcrJavascriptInterface(this@WebActivity)

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
            }

            addJavascriptInterface(connectPayAuthInterface, "ConnectPayAuth")
            addJavascriptInterface(connectPayOcrInterface, "ConnectPayUtil")
        }

        webView.loadUrl(url)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        url = intent?.data?.getQueryParameter("url").orEmpty()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ConnectPayOcrJavascriptInterface.REQUEST_CODE_OCR_SCAN) {
            if (resultCode == RESULT_OK) {
                data?.getStringExtra(ConnectPayOcrJavascriptInterface.RESULT_DATA_OCR_SCAN_SUCCESS_RESULT)
                    ?.let {
                        webView.loadUrl(it)
                    }
            } else {
                data?.getStringExtra(ConnectPayOcrJavascriptInterface.RESULT_DATA_OCR_SCAN_ERROR_RESULT)
                    ?.let {
                        webView.loadUrl(it)
                    }
            }
        }
    }
}