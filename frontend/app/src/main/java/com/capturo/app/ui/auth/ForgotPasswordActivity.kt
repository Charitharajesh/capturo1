package com.capturo.app.ui.auth

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.capturo.app.databinding.ActivityForgotPasswordBinding
import com.capturo.app.utils.Resource
import com.capturo.app.utils.ValidationUtils
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.parseColor("#12002A")

        binding.btnSendCode.setOnClickListener {
            performRequestCode()
        }

        observeForgotPasswordState()
    }

    private fun performRequestCode() {
        val email = binding.etEmail.text.toString().trim()
        binding.tilEmail.error = null

        if (email.isEmpty()) {
            binding.tilEmail.error = "Email address cannot be empty"
            return
        } else if (!ValidationUtils.isValidEmail(email)) {
            binding.tilEmail.error = "Please enter a valid email address"
            return
        }

        viewModel.forgotPassword(email)
    }

    private fun observeForgotPasswordState() {
        viewModel.forgotPasswordState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.loadingIndicator.visibility = View.VISIBLE
                    binding.btnSendCode.isEnabled = false
                }
                is Resource.Success -> {
                    binding.loadingIndicator.visibility = View.GONE
                    binding.btnSendCode.isEnabled = true
                    val email = binding.etEmail.text.toString().trim()
                    Toast.makeText(this, "Verification code sent to your email", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, VerifyOtpActivity::class.java).apply {
                        putExtra("EXTRA_EMAIL", email)
                    }
                    startActivity(intent)
                }
                is Resource.Error -> {
                    binding.loadingIndicator.visibility = View.GONE
                    binding.btnSendCode.isEnabled = true
                    Snackbar.make(binding.root, resource.message, Snackbar.LENGTH_LONG)
                        .setBackgroundTint(Color.parseColor("#2D1060"))
                        .setTextColor(Color.WHITE)
                        .setActionTextColor(Color.parseColor("#E040FB"))
                        .setAction("Retry") {
                            performRequestCode()
                        }
                        .show()
                }
            }
        }
    }
}
