package com.tosspayments.android.sample.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.tosspayments.android.auth.utils.ConnectPayAuthManager
import com.tosspayments.android.ocr.common.ConnectPayOcrManager

class MainActivity : AppCompatActivity() {
    private lateinit var preference: SharedPreferences

    companion object {
        private const val REQUEST_CODE_CARD_SCAN = 1001
        private const val REQUEST_CODE_PASSWORD = 1002

        private const val KEY_WEB_URL = "keyWebUrl"
    }

    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        preference = getSharedPreferences("keyConnectPaySamplePref", Context.MODE_PRIVATE)

        initViews()
    }

    private fun initViews() {
        val activity = this@MainActivity

        val inputWebUrl = findViewById<EditText>(R.id.web_url).apply {
            val webUrl = preference.getString(
                KEY_WEB_URL,
                null
            ) ?: "https://demo-dev.tosspayments.com/connectpay/test/webview"

            setText(webUrl)
        }

        findViewById<Button>(R.id.get_app_info).setOnClickListener {
            startActivity(
                ResultActivity.getIntent(
                    activity,
                    successData = gson.toJson(ConnectPayAuthManager.getAppInfo(this@MainActivity))
                )
            )
        }

        findViewById<Button>(R.id.verify_biometric_auth).setOnClickListener {
            ConnectPayAuthManager.requestBioMetricAuth(activity,
                "MODULUSMODULUSSE",
                "EXPONENTEXPONENT",
                { password ->
                    startActivity(ResultActivity.getIntent(activity, successData = password))
                },
                { _, message ->
                    startActivity(ResultActivity.getIntent(activity, errorMessage = message))
                })
        }

        findViewById<Button>(R.id.get_biometric_methods).setOnClickListener {
            startActivity(
                ResultActivity.getIntent(
                    activity,
                    successData = ConnectPayAuthManager.getBiometricAuthMethods(activity)
                )
            )
        }

        findViewById<Button>(R.id.register_biometric_auth).setOnClickListener {
            startActivityForResult(
                Intent(activity, InputPasswordActivity::class.java),
                REQUEST_CODE_PASSWORD
            )
        }

        findViewById<Button>(R.id.start_card_scan).setOnClickListener {
            ConnectPayOcrManager.requestCardScan(
                activity,
                "",
                REQUEST_CODE_CARD_SCAN
            )
        }

        findViewById<Button>(R.id.web_interface).setOnClickListener {
            startActivity(WebActivity.getIntent(this@MainActivity, inputWebUrl.text.toString()))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            val activity = this@MainActivity

            when (requestCode) {
                REQUEST_CODE_CARD_SCAN -> {
                    data?.getStringExtra("extraKeyCardScanResult")?.let {
                        startActivity(
                            ResultActivity.getIntent(
                                activity,
                                successData = it
                            )
                        )
                    }
                }
                REQUEST_CODE_PASSWORD -> {
                    val password = data?.getStringExtra("password")

                    ConnectPayAuthManager.registerBiometricAuth(
                        activity,
                        password,
                        {
                            ResultActivity.getIntent(
                                activity,
                                successData = password
                            )
                        }, { _, message ->
                            ResultActivity.getIntent(
                                activity,
                                errorMessage = message
                            )
                        }
                    )
                }
            }
        }
    }
}