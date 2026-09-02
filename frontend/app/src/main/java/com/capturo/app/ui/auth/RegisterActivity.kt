package com.capturo.app.ui.auth

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.capturo.app.databinding.ActivityRegisterBinding
import com.capturo.app.data.model.request.RegisterRequest
import com.capturo.app.utils.Resource
import com.capturo.app.utils.ValidationUtils
import com.capturo.app.data.preferences.SessionManager
import javax.inject.Inject
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterActivity : AppCompatActivity() {

    @Inject
    lateinit var sessionManager: SessionManager

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: AuthViewModel by viewModels()
    private var role: String = "attendee"

    private val requestPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            fetchAndSaveLocation()
        }
    }

    private fun fetchAndSaveLocation() {
        com.capturo.app.utils.LocationUtils.getCurrentLocation(this) { lat, lon ->
            sessionManager.saveTempLocation(lat, lon)
        }
    }

    private fun checkLocationPermission() {
        if (com.capturo.app.utils.LocationUtils.hasLocationPermission(this)) {
            fetchAndSaveLocation()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set edge-to-edge styling and transparent status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.parseColor("#12002A")

        // Retrieve and configure based on selected role
        role = intent.getStringExtra("EXTRA_ROLE") ?: "attendee"
        setupRoleUI()

        binding.btnRegister.setOnClickListener {
            performRegistration()
        }

        binding.txtLoginLink.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }

        observeRegisterState()
    }

    private fun setupRoleUI() {
        if (role == "creator") {
            binding.textSubtitle.text = "Sign up as Creator"
            binding.layoutSpecializations.visibility = View.VISIBLE
            checkLocationPermission()
        } else {
            binding.textSubtitle.text = "Sign up as Attendee"
            binding.layoutSpecializations.visibility = View.GONE
        }
    }

    private fun performRegistration() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // Clear error indicators
        binding.tilName.error = null
        binding.tilEmail.error = null
        binding.tilPhone.error = null
        binding.tilPassword.error = null

        var isValid = true

        if (name.isEmpty()) {
            binding.tilName.error = "Name cannot be empty"
            isValid = false
        } else if (name.length < 2) {
            binding.tilName.error = "Name must be at least 2 characters"
            isValid = false
        }

        if (email.isEmpty()) {
            binding.tilEmail.error = "Email address cannot be empty"
            isValid = false
        } else if (!ValidationUtils.isValidEmail(email)) {
            binding.tilEmail.error = "Please enter a valid email address"
            isValid = false
        }

        if (phone.isNotEmpty() && !ValidationUtils.isValidPhone(phone)) {
            binding.tilPhone.error = "Please enter a valid 10-digit phone number"
            isValid = false
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = "Password cannot be empty"
            isValid = false
        } else if (!ValidationUtils.isValidPassword(password)) {
            binding.tilPassword.error = "Password must be at least 8 characters"
            isValid = false
        }

        // Additional optional check for creators: at least one specialization chip selected
        if (role == "creator" && binding.cgSpecializations.checkedChipIds.isEmpty()) {
            Toast.makeText(this, "Please select at least one specialization", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        if (isValid) {
            if (role == "creator") {
                val specs = mutableSetOf<String>()
                if (binding.chipWedding.isChecked) specs.add("Wedding")
                if (binding.chipPortrait.isChecked) specs.add("Portrait")
                if (binding.chipCorporate.isChecked) specs.add("Corporate")
                if (binding.chipSports.isChecked) specs.add("Sports")
                if (binding.chipEvents.isChecked) specs.add("Events")
                sessionManager.saveTempSpecializations(specs)
            }
            val phoneVal = if (phone.isEmpty()) null else phone
            viewModel.register(
                RegisterRequest(
                    fullName = name,
                    email = email,
                    password = password,
                    role = role,
                    phone = phoneVal
                )
            )
        }
    }

    private fun observeRegisterState() {
        viewModel.registerState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.loadingIndicator.visibility = View.VISIBLE
                    binding.btnRegister.isEnabled = false
                }
                is Resource.Success -> {
                    binding.loadingIndicator.visibility = View.GONE
                    binding.btnRegister.isEnabled = true
                    Toast.makeText(this, "Success! Please log in.", Toast.LENGTH_SHORT).show()
                    
                    val intent = Intent(this, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(intent)
                    finish()
                }
                is Resource.Error -> {
                    binding.loadingIndicator.visibility = View.GONE
                    binding.btnRegister.isEnabled = true
                    
                    Snackbar.make(binding.root, resource.message, Snackbar.LENGTH_LONG)
                        .setBackgroundTint(Color.parseColor("#2D1060"))
                        .setTextColor(Color.WHITE)
                        .setActionTextColor(Color.parseColor("#E040FB"))
                        .setAction("Retry") {
                            performRegistration()
                        }
                        .show()
                }
            }
        }
    }
}
