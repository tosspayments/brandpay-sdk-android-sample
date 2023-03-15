package com.tosspayments.android.sample.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText

class InputPasswordActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_input_password)
        initViews()
    }

    private fun initViews() {
        val inputPassword = findViewById<EditText>(R.id.input_password).apply {
            requestFocus()
        }

        findViewById<Button>(R.id.confirm_password).setOnClickListener {
            setResult(RESULT_OK, Intent().apply {
                putExtra("password", inputPassword.text.toString())
            })
            finish()
        }

        supportActionBar?.run {
            setHomeButtonEnabled(true)
        }
    }
}