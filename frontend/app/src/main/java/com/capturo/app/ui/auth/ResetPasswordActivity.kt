package com.capturo.app.ui.auth

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.capturo.app.databinding.ActivityResetPasswordBinding
import com.capturo.app.utils.Resource
import com.capturo.app.utils.ValidationUtils
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResetPasswordBinding
    private val viewModel: AuthViewModel by viewModels()
    private var email: String = ""
    private var otp: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.parseColor("#12002A")

        email = intent.getStringExtra("EXTRA_EMAIL") ?: ""
        otp = intent.getStringExtra("EXTRA_OTP") ?: ""

        binding.btnReset.setOnClickListener {
            performReset()
        }

        observeResetPasswordState()
    }

    private fun performReset() {
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null

        var isValid = true

        if (password.isEmpty()) {
            binding.tilPassword.error = "Password cannot be empty"
            isValid = false
        } else if (!ValidationUtils.isValidPassword(password)) {
            binding.tilPassword.error = "Password must be at least 8 characters"
            isValid = false
        }

        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.error = "Confirm Password cannot be empty"
            isValid = false
        } else if (password != confirmPassword) {
            binding.tilConfirmPassword.error = "Passwords do not match"
            isValid = false
        }

        if (isValid) {
            viewModel.resetPassword(otp, password)
        }
    }

    private fun observeResetPasswordState() {
        viewModel.resetPasswordState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.loadingIndicator.visibility = View.VISIBLE
                    binding.btnReset.isEnabled = false
                }
                is Resource.Success -> {
                    binding.loadingIndicator.visibility = View.GONE
                    binding.btnReset.isEnabled = true
                    Toast.makeText(this, "Password updated successfully! Please login.", Toast.LENGTH_LONG).show()
                    val intent = Intent(this, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                }
                is Resource.Error -> {
                    binding.loadingIndicator.visibility = View.GONE
                    binding.btnReset.isEnabled = true
                    Snackbar.make(binding.root, resource.message, Snackbar.LENGTH_LONG)
                        .setBackgroundTint(Color.parseColor("#2D1060"))
                        .setTextColor(Color.WHITE)
                        .setActionTextColor(Color.parseColor("#E040FB"))
                        .setAction("Retry") {
                            performReset()
                        }
                        .show()
                }
            }
        }
    }
}
