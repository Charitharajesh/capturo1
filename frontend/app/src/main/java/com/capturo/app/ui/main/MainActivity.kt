package com.capturo.app.ui.main

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.capturo.app.R
import com.capturo.app.data.preferences.SessionManager
import com.capturo.app.data.preferences.UserRole
import com.capturo.app.databinding.ActivityMainBinding
import com.capturo.app.ui.payment.PaymentFragment
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), PaymentResultWithDataListener {

    @Inject
    lateinit var sessionManager: SessionManager

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set edge-to-edge styling and transparent status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.parseColor("#12002A")

        // Apply window insets to prevent fragment contents from expanding behind the status bar and toolbar
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime())
            
            val bottomInset = Math.max(systemBars.bottom, ime.bottom)
            findViewById<View>(R.id.nav_host_fragment)?.setPadding(0, systemBars.top, 0, bottomInset)
            binding.bottomNav.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        setupNavigation()
        handleIntent(intent)
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val navInflater = navController.navInflater
        val graph = navInflater.inflate(R.navigation.nav_graph)

        // Configure role-dependent menu and start destination
        val role = sessionManager.getUserRole()
        if (role == UserRole.CREATOR) {
            binding.bottomNav.menu.clear()
            binding.bottomNav.inflateMenu(R.menu.bottom_nav_creator)
            graph.setStartDestination(R.id.creatorDashboardFragment)
        } else {
            binding.bottomNav.menu.clear()
            binding.bottomNav.inflateMenu(R.menu.bottom_nav_attendee)
            graph.setStartDestination(R.id.homeFragment)
        }
        navController.graph = graph

        // Link BottomNavigationView with NavController
        binding.bottomNav.setupWithNavController(navController)

        // Show/hide bottom nav based on current destination
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val visibleDestinations = setOf(
                R.id.homeFragment,
                R.id.activeBookingsFragment,
                R.id.profileFragment,
                R.id.creatorDashboardFragment,
                R.id.bookingRequestsFragment,
                R.id.creatorGalleryFragment,
                R.id.notificationsFragment
            )
            if (destination.id in visibleDestinations) {
                binding.bottomNav.visibility = View.VISIBLE
            } else {
                binding.bottomNav.visibility = View.GONE
            }
        }
    }

    /**
     * Switches the entire app between Client (attendee) and Event Creator modes.
     * Persists the new role and relaunches MainActivity with a fresh task so the
     * bottom-nav menu, start destination and every role-gated screen rebuild cleanly.
     */
    fun switchRole(role: UserRole) {
        if (sessionManager.getUserRole() == role) return
        sessionManager.setUserRole(role)
        val restart = Intent(this, MainActivity::class.java)
        restart.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(restart)
        overridePendingTransition(0, 0)
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        // Extract deep link notification payload
        val bookingId = intent.getStringExtra("booking_id")
        val targetTab = intent.getStringExtra("target_tab")

        if (!bookingId.isNullOrEmpty()) {
            val bundle = Bundle().apply {
                putString("booking_id", bookingId)
            }
            navController.navigate(R.id.bookingDetailFragment, bundle)
        } else if (targetTab == "chat") {
            navController.navigate(R.id.chatFragment)
        }
    }

    // Call this helper method from your ViewModels or Fragments to update message badge indicators
    fun updateUnreadBadge(unreadCount: Int) {
        val menuId = if (sessionManager.getUserRole() == UserRole.CREATOR) {
            R.id.bookingRequestsFragment
        } else {
            R.id.notificationsFragment
        }
        
        val badge = binding.bottomNav.getOrCreateBadge(menuId)
        if (unreadCount > 0) {
            badge.backgroundColor = Color.parseColor("#FF5252")
            badge.number = unreadCount
            badge.isVisible = true
        } else {
            badge.isVisible = false
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        val currentFragment = navHostFragment?.childFragmentManager?.fragments?.firstOrNull { it is PaymentFragment } as? PaymentFragment
        currentFragment?.onPaymentSuccess(razorpayPaymentId, paymentData)
    }

    override fun onPaymentError(code: Int, response: String?, paymentData: PaymentData?) {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        val currentFragment = navHostFragment?.childFragmentManager?.fragments?.firstOrNull { it is PaymentFragment } as? PaymentFragment
        currentFragment?.onPaymentError(code, response, paymentData)
    }
}
