package com.capturo.app.ui.onboarding

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.viewpager2.widget.ViewPager2
import com.capturo.app.R
import com.capturo.app.adapter.OnboardingPagerAdapter
import com.capturo.app.databinding.ActivityOnboardingBinding
import com.capturo.app.ui.auth.RoleSelectionActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private val viewModel: OnboardingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set edge-to-edge styling and transparent status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.parseColor("#12002A")

        setupViewPager()
        setupIndicators()

        viewModel.currentPage.observe(this) { page ->
            binding.viewPager.currentItem = page
            updateIndicators(page)
            if (page == 2) {
                binding.btnNext.text = "Get Started"
            } else {
                binding.btnNext.text = "Next"
            }
        }

        binding.btnNext.setOnClickListener {
            val currentPage = viewModel.currentPage.value ?: 0
            if (currentPage < 2) {
                viewModel.setCurrentPage(currentPage + 1)
            } else {
                finishOnboarding()
            }
        }

        binding.btnSkip.setOnClickListener {
            viewModel.setCurrentPage(2)
        }
    }

    private fun setupViewPager() {
        val adapter = OnboardingPagerAdapter(this)
        binding.viewPager.adapter = adapter
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                viewModel.setCurrentPage(position)
            }
        })
    }

    private fun setupIndicators() {
        val count = binding.viewPager.adapter?.itemCount ?: 3
        binding.layoutIndicators.removeAllViews()

        for (i in 0 until count) {
            val indicator = ImageView(this)
            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(8, 0, 8, 0)
            }
            indicator.layoutParams = params
            binding.layoutIndicators.addView(indicator)
        }
        updateIndicators(0)
    }

    private fun updateIndicators(position: Int) {
        val childCount = binding.layoutIndicators.childCount
        for (i in 0 until childCount) {
            val imageView = binding.layoutIndicators.getChildAt(i) as ImageView
            val params = imageView.layoutParams as LinearLayout.LayoutParams

            if (i == position) {
                imageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.bg_indicator_active))
                params.width = resources.getDimensionPixelSize(R.dimen.spacing24) // 24dp
            } else {
                imageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.bg_indicator_inactive))
                params.width = resources.getDimensionPixelSize(R.dimen.spacing8)  // 8dp
            }
            params.height = resources.getDimensionPixelSize(R.dimen.spacing8) // 8dp
            imageView.layoutParams = params
        }
    }

    private fun finishOnboarding() {
        val prefs = getSharedPreferences("capturo_app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("onboarding_completed", true).apply()

        startActivity(Intent(this, RoleSelectionActivity::class.java))
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
