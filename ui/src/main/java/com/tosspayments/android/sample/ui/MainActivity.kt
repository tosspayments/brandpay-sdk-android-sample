package com.tosspayments.android.sample.ui

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.gson.Gson
import com.tosspayments.android.auth.utils.BrandPayAuthManager

class MainActivity : AppCompatActivity() {
    private lateinit var preference: SharedPreferences

    companion object {
        private const val KEY_WEB_URL = "keyWebUrl"
        private const val PAYMENT_PAGE_URL = "https://testbox.tosspayments.com/brandpay/test"
    }

    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        preference = getSharedPreferences("keyBrandPaySamplePref", Context.MODE_PRIVATE)

        initViews()
    }

    private fun initViews() {
        val activity = this@MainActivity

        // 브리지를 붙인 WebView 에 다른 주소를 여는 입력칸은 debuggable 앱에서만 보인다.
        // ui 는 library 모듈이라 BuildConfig.DEBUG 대신 앱의 FLAG_DEBUGGABLE 을 본다.
        val isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

        val inputWebUrl = findViewById<EditText>(R.id.web_url).apply {
            isVisible = isDebuggable

            val webUrl = preference.getString(
                KEY_WEB_URL,
                null
            ) ?: PAYMENT_PAGE_URL

            setText(webUrl)
        }

        findViewById<Button>(R.id.get_app_info).setOnClickListener {
            startActivity(
                ResultActivity.getIntent(
                    activity,
                    successData = gson.toJson(BrandPayAuthManager.getAppInfo(this@MainActivity))
                )
            )
        }

        findViewById<Button>(R.id.web_interface).setOnClickListener {
            val webUrl = if (isDebuggable) inputWebUrl.text.toString() else PAYMENT_PAGE_URL
            startActivity(WebActivity.getIntent(this@MainActivity, webUrl))
        }
    }
}