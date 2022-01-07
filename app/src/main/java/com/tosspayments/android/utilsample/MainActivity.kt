package com.tosspayments.android.utilsample

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.tosspayments.android.auth.utils.ConnectPayAuthManager
import com.tosspayments.android.ocr.common.ConnectPayOcrManager

class MainActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_CODE_CARD_SCAN = 1001
        private const val REQUEST_CODE_PASSWORD = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
    }

    private fun initViews() {
        val activity = this@MainActivity

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
                    successData = ConnectPayAuthManager.getBiometricAuthMethods(activity).toString()
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
                "LICENSE",
                REQUEST_CODE_CARD_SCAN
            )
        }

        findViewById<Button>(R.id.web_interface).setOnClickListener {
            startActivity(Intent(this@MainActivity, WebActivity::class.java))
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