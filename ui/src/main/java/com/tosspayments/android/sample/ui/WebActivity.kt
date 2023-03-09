package com.tosspayments.android.sample.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebView.setWebContentsDebuggingEnabled
import androidx.appcompat.app.AppCompatActivity
import com.tosspayments.android.auth.interfaces.BrandPayAuthWebManager
import com.tosspayments.android.ocr.interfaces.BrandPayOcrWebManager

class WebActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private var url: String = ""

    private val brandPayAuthWebManager = BrandPayAuthWebManager(
        this
    ).apply {
        callback = object : BrandPayAuthWebManager.Callback {
            override fun onPostScript(script: String) {
                webView.loadUrl(script)
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
            }

            setWebContentsDebuggingEnabled(true)

            brandPayOcrWebManager.addJavascriptInterface(this)
            brandPayAuthWebManager.addJavascriptInterface(this)
        }

        webView.loadUrl(url.takeIf { it.isNotBlank() }
            ?: "https://demo-dev.tosspayments.com/connectpay/test/webview")
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        url = intent?.getStringExtra(EXTRA_WEB_URL).orEmpty()
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
}