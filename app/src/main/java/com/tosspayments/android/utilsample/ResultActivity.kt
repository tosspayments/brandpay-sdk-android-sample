package com.tosspayments.android.utilsample

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.TextView

class ResultActivity : BaseActivity() {
    companion object {
        private const val EXTRA_SUCCESS_DATA = "extraSuccessData"
        private const val EXTRA_ERROR_MESSAGE = "extraErrorData"

        fun getIntent(
            activity: Activity,
            successData: String? = null,
            errorMessage: String? = null
        ): Intent {
            return Intent(activity, ResultActivity::class.java)
                .putExtra(EXTRA_SUCCESS_DATA, successData.orEmpty())
                .putExtra(EXTRA_ERROR_MESSAGE, errorMessage.orEmpty())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        initViews(intent)
    }

    private fun initViews(intent: Intent?) {
        findViewById<TextView>(R.id.on_success_data).text =
            intent?.getStringExtra(EXTRA_SUCCESS_DATA).orEmpty()

        findViewById<TextView>(R.id.on_error_message).text =
            intent?.getStringExtra(EXTRA_ERROR_MESSAGE).orEmpty()
    }
}