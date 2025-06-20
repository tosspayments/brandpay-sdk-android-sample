package com.tosspayments.android.sample.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.tosspayments.android.auth.utils.BrandPayAuthManager

class MainActivity : AppCompatActivity() {
    private lateinit var preference: SharedPreferences

    companion object {
        private const val KEY_WEB_URL = "keyWebUrl"
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

        val inputWebUrl = findViewById<EditText>(R.id.web_url).apply {
            val webUrl = preference.getString(
                KEY_WEB_URL,
                null
            ) ?: "https://testbox.tosspayments.com/brandpay/test"

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
            startActivity(WebActivity.getIntent(this@MainActivity, inputWebUrl.text.toString()))
        }
    }
}