package com.tosspayments.android.utilsample

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import com.tosspayments.android.auth.interfaces.ConnectPayAuthWebManager
import com.tosspayments.android.ocr.interfaces.ConnectPayOcrWebManager

class WebActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private var url: String = ""

    private val connectPayAuthWebManager = ConnectPayAuthWebManager(this)
    private val connectPayOcrWebManager = ConnectPayOcrWebManager(this)

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
        }

        connectPayAuthWebManager.bind(webView)
        connectPayOcrWebManager.bind(webView)

        webView.loadUrl(url.takeIf { it.isNotBlank() } ?: "https://demo-dev.tosspayments.com/connectpay/test/webview")
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

        connectPayOcrWebManager.handleActivityResult(requestCode, resultCode, data)
    }
}