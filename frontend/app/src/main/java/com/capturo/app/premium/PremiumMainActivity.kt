package com.capturo.app.premium

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.capturo.app.R
import com.capturo.app.databinding.ActivityPremiumMainBinding
import com.capturo.app.premium.ui.BookingsFragment
import com.capturo.app.premium.ui.DiscoverFragment
import com.capturo.app.premium.ui.HomeFragment
import com.capturo.app.premium.ui.ProfileFragment
import com.capturo.app.premium.ui.TryFragment

class PremiumMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPremiumMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPremiumMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.parseColor("#0E0E10")

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.fragmentContainer.setPadding(0, bars.top, 0, 0)
            binding.bottomNav.setPadding(0, 0, 0, bars.bottom)
            insets
        }

        if (savedInstanceState == null) {
            when (intent.getStringExtra(EXTRA_OPEN_TAB)) {
                "bookings" -> binding.bottomNav.selectedItemId = R.id.nav_bookings
                "home" -> show(HomeFragment())
                else -> show(HomeFragment())
            }
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            val f: Fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_discover -> DiscoverFragment()
                R.id.nav_try -> TryFragment()
                R.id.nav_bookings -> BookingsFragment()
                R.id.nav_profile -> ProfileFragment()
                else -> HomeFragment()
            }
            show(f)
            true
        }
    }

    private fun show(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    fun goToTry() {
        binding.bottomNav.selectedItemId = R.id.nav_try
    }

    fun goToProfile() {
        binding.bottomNav.selectedItemId = R.id.nav_profile
    }

    companion object {
        const val EXTRA_OPEN_TAB = "extra_open_tab"
    }
}
