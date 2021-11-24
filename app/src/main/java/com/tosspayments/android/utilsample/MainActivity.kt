package com.tosspayments.android.utilsample

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.tosspayments.android.util.utils.ConnectPayUtil

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
            ConnectPayUtil.requestBioMetricAuth(activity,
                { password ->
                    startActivity(ResultActivity.getIntent(activity, successData = password))
                },
                { message ->
                    startActivity(ResultActivity.getIntent(activity, errorMessage = message))
                })
        }

        findViewById<Button>(R.id.get_biometric_methods).setOnClickListener {
            startActivity(
                ResultActivity.getIntent(
                    activity,
                    successData = ConnectPayUtil.getBiometricAuthMethods(activity).toString()
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
            ConnectPayUtil.requestCardScan(
                activity,
                "LICENSE",
                REQUEST_CODE_CARD_SCAN
            )
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

                    ConnectPayUtil.registerBiometricAuth(
                        activity, password
                    ) { isSuccess, message ->
                        if (isSuccess) {
                            ResultActivity.getIntent(
                                activity,
                                successData = password
                            )
                        } else {
                            ResultActivity.getIntent(
                                activity,
                                errorMessage = message
                            )
                        }
                    }
                }
            }
        }
    }
}