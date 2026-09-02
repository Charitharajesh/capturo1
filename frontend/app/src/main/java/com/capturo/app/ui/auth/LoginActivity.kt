package com.capturo.app.ui.auth

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.capturo.app.databinding.ActivityLoginBinding
import com.capturo.app.ui.main.MainActivity
import com.capturo.app.utils.Resource
import com.capturo.app.utils.ValidationUtils
import com.capturo.app.data.preferences.SessionManager
import com.capturo.app.data.repository.CreatorRepository
import javax.inject.Inject
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var creatorRepository: CreatorRepository

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set edge-to-edge styling and transparent status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.parseColor("#12002A")

        binding.btnLogin.setOnClickListener {
            performLogin()
        }

        binding.txtSignupLink.setOnClickListener {
            val selectedRole = intent.getStringExtra("EXTRA_ROLE")
            if (selectedRole != null) {
                val intent = Intent(this, RegisterActivity::class.java).apply {
                    putExtra("EXTRA_ROLE", selectedRole)
                }
                startActivity(intent)
            } else {
                val intent = Intent(this, RoleSelectionActivity::class.java).apply {
                    putExtra("EXTRA_NEXT_SCREEN", "register")
                }
                startActivity(intent)
            }
        }

        binding.btnGoogle.setOnClickListener {
            Toast.makeText(this, "Google Sign-In coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnApple.setOnClickListener {
            Toast.makeText(this, "Apple Sign-In coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnForgotPassword.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }

        observeLoginState()
    }

    private fun performLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        binding.tilEmail.error = null
        binding.tilPassword.error = null

        var isValid = true

        if (email.isEmpty()) {
            binding.tilEmail.error = "Email address cannot be empty"
            isValid = false
        } else if (!ValidationUtils.isValidEmail(email)) {
            binding.tilEmail.error = "Please enter a valid email address"
            isValid = false
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = "Password cannot be empty"
            isValid = false
        } else if (password.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
            isValid = false
        }

        if (isValid) {
            viewModel.login(email, password)
        }
    }

    private fun observeLoginState() {
        viewModel.loginState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.loadingIndicator.visibility = View.VISIBLE
                    binding.btnLogin.isEnabled = false
                }
                is Resource.Success -> {
                    binding.loadingIndicator.visibility = View.GONE
                    binding.btnLogin.isEnabled = true
                    
                    checkForCachedRegistrationDetails()

                    val intent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                }
                is Resource.Error -> {
                    binding.loadingIndicator.visibility = View.GONE
                    binding.btnLogin.isEnabled = true
                    
                    Snackbar.make(binding.root, resource.message, Snackbar.LENGTH_LONG)
                        .setBackgroundTint(Color.parseColor("#2D1060"))
                        .setTextColor(Color.WHITE)
                        .setActionTextColor(Color.parseColor("#E040FB"))
                        .setAction("Retry") {
                            performLogin()
                        }
                        .show()
                }
            }
        }
    }

    private fun checkForCachedRegistrationDetails() {
        val tempSpecs = sessionManager.getTempSpecializations()
        val tempLat = sessionManager.getTempLatitude()
        val tempLon = sessionManager.getTempLongitude()

        if (sessionManager.getUserRole() == com.capturo.app.data.preferences.UserRole.CREATOR) {
            val hasSpecs = tempSpecs != null
            val hasLocation = tempLat != null && tempLon != null

            if (hasSpecs || hasLocation) {
                val listSpecs = tempSpecs?.toList()
                lifecycleScope.launchWhenResumed {
                    try {
                        creatorRepository.updateOwnCreatorProfile(
                            com.capturo.app.data.model.request.UpdateCreatorProfileRequest(
                                specializations = listSpecs,
                                latitude = tempLat,
                                longitude = tempLon
                            )
                        ).collect { resource ->
                            if (resource is Resource.Success) {
                                sessionManager.clearTempSpecializations()
                                sessionManager.clearTempLocation()
                            }
                        }
                    } catch (e: Exception) {
                        // Ignore background sync errors
                    }
                }
            }
        }
    }
}
