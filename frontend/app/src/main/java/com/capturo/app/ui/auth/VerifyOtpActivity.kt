package com.capturo.app.ui.auth

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.capturo.app.databinding.ActivityVerifyOtpBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VerifyOtpActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVerifyOtpBinding
    private var email: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyOtpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.parseColor("#12002A")

        email = intent.getStringExtra("EXTRA_EMAIL") ?: ""

        binding.btnVerify.setOnClickListener {
            performVerification()
        }
    }

    private fun performVerification() {
        val otp = binding.etOtp.text.toString().trim()
        binding.tilOtp.error = null

        if (otp.isEmpty() || otp.length < 6) {
            binding.tilOtp.error = "Please enter a valid 6-digit code"
            return
        }

        // Pass direct code validation to ResetPasswordActivity
        val intent = Intent(this, ResetPasswordActivity::class.java).apply {
            putExtra("EXTRA_EMAIL", email)
            putExtra("EXTRA_OTP", otp)
        }
        startActivity(intent)
    }
}
