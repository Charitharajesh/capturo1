package com.capturo.app.premium

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.capturo.app.R
import com.capturo.app.databinding.ActivityPremiumAuthBinding
import com.capturo.app.databinding.DialogComingSoonBinding

/**
 * Self-contained email/password auth backed by the on-device local store
 * ([PremiumStore]). Users create an account (name, email, password, confirm)
 * which is saved on the device, then sign in with the same credentials.
 */
class PremiumAuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPremiumAuthBinding

    /** true = create-account form, false = login form. */
    private var signUpMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPremiumAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.heroImage.load(
            "https://images.unsplash.com/photo-1606216794074-735e91aa2c92?auto=format&fit=crop&w=900&q=70"
        ) {
            placeholder(R.drawable.bg_image_placeholder)
            error(R.drawable.bg_image_placeholder)
        }

        // Default to login if the user already has at least one saved account.
        signUpMode = PremiumStore.currentAccount(this) == null && !hasAnyAccount()
        applyMode(animate = false)

        binding.btnToggleMode.setOnClickListener {
            signUpMode = !signUpMode
            applyMode(animate = true)
        }
        binding.btnPrimaryEnter.setOnClickListener { submit() }
        binding.btnGoogle.setOnClickListener { showGoogleComingSoon() }
    }

    private fun hasAnyAccount(): Boolean =
        getSharedPreferences("capturo_premium", MODE_PRIVATE)
            .getString("accounts", "[]")?.length ?: 0 > 2

    private fun applyMode(animate: Boolean) {
        if (signUpMode) {
            binding.textTitle.text = "Create your account"
            binding.textSubtitle.text = "Sign up to book and cherish your moments"
            binding.inputName.visibility = View.VISIBLE
            binding.inputConfirm.visibility = View.VISIBLE
            binding.btnPrimaryEnter.text = "Create Account"
            binding.btnToggleMode.text = "Already have an account? Log in"
        } else {
            binding.textTitle.text = "Welcome back"
            binding.textSubtitle.text = "Log in to continue capturing moments"
            binding.inputName.visibility = View.GONE
            binding.inputConfirm.visibility = View.GONE
            binding.btnPrimaryEnter.text = "Log In"
            binding.btnToggleMode.text = "New here? Create an account"
        }
        if (animate) {
            binding.textTitle.alpha = 0f
            binding.textTitle.animate().alpha(1f).setDuration(260).start()
            val form = binding.inputEmail
            form.alpha = 0f
            form.animate().alpha(1f).setDuration(260).start()
        }
    }

    private fun submit() {
        val email = binding.inputEmail.text.toString()
        val password = binding.inputPassword.text.toString()

        val error = if (signUpMode) {
            val name = binding.inputName.text.toString()
            val confirm = binding.inputConfirm.text.toString()
            when {
                password != confirm -> "Passwords do not match"
                else -> PremiumStore.register(this, name, email, password)
            }
        } else {
            when {
                email.isBlank() || password.isBlank() -> "Please enter your email and password"
                else -> PremiumStore.login(this, email, password)
            }
        }

        if (error != null) {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            return
        }
        val who = PremiumStore.currentAccount(this)?.name ?: "there"
        Toast.makeText(this, "Welcome, $who!", Toast.LENGTH_SHORT).show()
        enterApp()
    }

    /** Attractive "coming soon" popup shown when Google sign-in is tapped. */
    private fun showGoogleComingSoon() {
        val dialog = Dialog(this)
        val db = DialogComingSoonBinding.inflate(layoutInflater)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(db.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.86f).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        db.btnDismiss.setOnClickListener { dialog.dismiss() }

        // Gentle pop-in animation.
        db.root.scaleX = 0.85f
        db.root.scaleY = 0.85f
        db.root.alpha = 0f
        db.root.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(280).start()

        dialog.show()
    }

    private fun enterApp() {
        startActivity(Intent(this, PremiumMainActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}
