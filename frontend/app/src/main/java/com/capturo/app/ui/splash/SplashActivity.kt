package com.capturo.app.ui.splash

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.capturo.app.R
import com.capturo.app.data.preferences.SessionManager
import com.capturo.app.databinding.ActivitySplashBinding
import com.capturo.app.ui.auth.LoginActivity
import com.capturo.app.ui.main.MainActivity
import com.capturo.app.ui.onboarding.OnboardingActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    @Inject
    lateinit var session: SessionManager

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install Android 12+ SplashScreen Compat
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set edge-to-edge styling and transparent status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.parseColor("#12002A")

        // Style the App Name wordmark: "Captur" (white) and "o" (accent magenta)
        val appName = "Capturo"
        val spannable = SpannableString(appName)
        val colorAccent = ContextCompat.getColor(this, R.color.colorAccent)
        spannable.setSpan(
            ForegroundColorSpan(colorAccent),
            appName.length - 1,
            appName.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        binding.textAppName.text = spannable

        // Start the animation sequence
        startSplashAnimations()
    }

    private fun startSplashAnimations() {
        // Phase 1: Glow fades in and scales up
        val glowFadeIn = ObjectAnimator.ofFloat(binding.glowView, View.ALPHA, 0f, 1f).apply {
            duration = 600
            interpolator = DecelerateInterpolator()
        }
        val glowScale = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(binding.glowView, View.SCALE_X, 0.5f, 1f),
                ObjectAnimator.ofFloat(binding.glowView, View.SCALE_Y, 0.5f, 1f)
            )
            duration = 600
            interpolator = DecelerateInterpolator()
        }

        // Phase 2: Lottie camera plays
        val lottieReveal = ObjectAnimator.ofFloat(binding.lottieCamera, View.ALPHA, 0f, 1f).apply {
            duration = 200
        }

        // Phase 3: App name slides up and fades in
        val titleSlideUp = ObjectAnimator.ofFloat(binding.textAppName, View.TRANSLATION_Y, 40f, 0f).apply {
            duration = 500
            interpolator = OvershootInterpolator(1.2f)
        }
        val titleFadeIn = ObjectAnimator.ofFloat(binding.textAppName, View.ALPHA, 0f, 1f).apply {
            duration = 400
            interpolator = DecelerateInterpolator()
        }

        // Phase 4: Tagline fades in
        val taglineSlideUp = ObjectAnimator.ofFloat(binding.textTagline, View.TRANSLATION_Y, 20f, 0f).apply {
            duration = 400
            interpolator = DecelerateInterpolator()
        }
        val taglineFadeIn = ObjectAnimator.ofFloat(binding.textTagline, View.ALPHA, 0f, 1f).apply {
            duration = 400
            interpolator = DecelerateInterpolator()
        }

        // Phase 5: Bottom branding fades in
        val bottomFadeIn = ObjectAnimator.ofFloat(binding.textPoweredBy, View.ALPHA, 0f, 0.7f).apply {
            duration = 500
            interpolator = DecelerateInterpolator()
        }

        // Orchestrate the full sequence
        val fullSequence = AnimatorSet()
        
        // Glow + Lottie appear together at start
        val phase1 = AnimatorSet().apply {
            playTogether(glowFadeIn, glowScale, lottieReveal)
        }
        
        // Title animation
        val phase2 = AnimatorSet().apply {
            playTogether(titleSlideUp, titleFadeIn)
            startDelay = 200
        }
        
        // Tagline
        val phase3 = AnimatorSet().apply {
            playTogether(taglineSlideUp, taglineFadeIn)
        }

        fullSequence.playSequentially(phase1, phase2, phase3, bottomFadeIn)
        
        fullSequence.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                // Start Lottie when phase 1 starts
                binding.lottieCamera.alpha = 1f
                binding.lottieCamera.playAnimation()
            }
        })
        
        fullSequence.start()

        // Navigate after the full animation plays (2.5 seconds total)
        binding.root.postDelayed({
            navigateNext()
        }, 2800)
    }

    private fun navigateNext() {
        val isFirstLaunch = checkFirstLaunch()
        
        val intent = when {
            session.isLoggedIn() -> {
                Intent(this, MainActivity::class.java)
            }
            isFirstLaunch -> {
                Intent(this, OnboardingActivity::class.java)
            }
            else -> {
                Intent(this, LoginActivity::class.java)
            }
        }

        startActivity(intent)
        finish()
        
        // Apply fade transition
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun checkFirstLaunch(): Boolean {
        val prefs = getSharedPreferences("capturo_app_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("onboarding_completed", false).not()
    }
}
