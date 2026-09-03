package com.capturo.app.premium

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.capturo.app.data.api.CreatorApiService
import com.capturo.app.databinding.ActivityPremiumSplashBinding
import com.capturo.app.premium.DemoData
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@AndroidEntryPoint
class PremiumSplashActivity : AppCompatActivity() {

    @Inject
    lateinit var creatorApi: CreatorApiService

    private lateinit var binding: ActivityPremiumSplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // Swaps Theme.SplashScreen -> Theme.Capturo.Premium (AppCompat) before setContentView.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityPremiumSplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.imageLogo.apply {
            alpha = 0f
            scaleX = 0.82f
            scaleY = 0.82f
            animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(900)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
        binding.textPoweredBy.animate().alpha(1f).setStartDelay(600).setDuration(600).start()

        // Fetch real creators from the backend while the splash plays, then
        // navigate. Falls back to bundled demo data if the call fails/offline.
        lifecycleScope.launch {
            val fetch = async { loadRealCreators() }
            delay(2600)                       // keep the branded splash timing
            withTimeoutOrNull(2500) { fetch.await() }
            goNext()
        }
    }

    private suspend fun loadRealCreators() {
        runCatching {
            val resp = creatorApi.getCreators()
            if (resp.isSuccessful) {
                resp.body()?.items?.let { DemoData.hydrateFromCreators(it) }
            }
        }
        // Re-publish the user's own listing on top so it survives restarts.
        with(PremiumStore) {
            myPhotographer(this@PremiumSplashActivity)?.let {
                DemoData.mergeLocal(listOf(it.toPhotographer()))
            }
        }
    }

    private fun goNext() {
        val prefs = getSharedPreferences("capturo_premium", Context.MODE_PRIVATE)
        val onboarded = prefs.getBoolean("onboarded", false)
        val next = when {
            !onboarded -> PremiumOnboardingActivity::class.java
            !PremiumStore.isLoggedIn(this) -> PremiumAuthActivity::class.java
            else -> PremiumMainActivity::class.java
        }
        startActivity(Intent(this, next))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}
