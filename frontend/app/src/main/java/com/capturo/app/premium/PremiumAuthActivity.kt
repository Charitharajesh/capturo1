package com.capturo.app.premium

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.capturo.app.R
import com.capturo.app.data.api.AuthApiService
import com.capturo.app.data.model.request.LoginRequest
import com.capturo.app.data.preferences.SessionManager
import com.capturo.app.databinding.ActivityPremiumAuthBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PremiumAuthActivity : AppCompatActivity() {

    @Inject lateinit var authApi: AuthApiService
    @Inject lateinit var session: SessionManager

    private lateinit var binding: ActivityPremiumAuthBinding
    private var busy = false

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

        val enter = View.OnClickListener { signInThenEnter() }
        binding.btnGoogle.setOnClickListener(enter)
        binding.btnPhone.setOnClickListener(enter)
        binding.btnEmail.setOnClickListener(enter)
        binding.btnPrimaryEnter.setOnClickListener(enter)
    }

    /**
     * Performs a real backend login (seeded demo account) so the app holds a
     * genuine session token for authenticated calls, then opens the app.
     * If the backend is unreachable it still enters so the demo never blocks.
     */
    private fun signInThenEnter() {
        if (busy) return
        busy = true
        // Already signed in from a previous run — go straight in.
        if (session.isLoggedIn()) { enterApp(); return }

        lifecycleScope.launch {
            runCatching {
                val resp = authApi.login(LoginRequest("client@example.com", "Password123!"))
                resp.body()?.data?.let { session.saveSession(it) }
            }.onFailure {
                Toast.makeText(
                    this@PremiumAuthActivity,
                    "Offline mode — showing demo content",
                    Toast.LENGTH_SHORT
                ).show()
            }
            enterApp()
        }
    }

    private fun enterApp() {
        startActivity(Intent(this, PremiumMainActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}
