package com.capturo.app.ui.auth

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.capturo.app.R
import com.capturo.app.databinding.ActivityRoleSelectionBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RoleSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRoleSelectionBinding
    private var selectedRole: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityRoleSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set edge-to-edge styling and transparent status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.parseColor("#12002A")

        binding.cardAttendee.setOnClickListener {
            selectRole("attendee")
        }

        binding.cardCreator.setOnClickListener {
            selectRole("creator")
        }

        binding.btnContinue.setOnClickListener {
            selectedRole?.let { role ->
                val nextScreen = intent.getStringExtra("EXTRA_NEXT_SCREEN") ?: "login"
                val targetClass = if (nextScreen == "register") {
                    RegisterActivity::class.java
                } else {
                    LoginActivity::class.java
                }
                val intent = Intent(this, targetClass).apply {
                    putExtra("EXTRA_ROLE", role)
                }
                startActivity(intent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }
        }
    }

    private fun selectRole(role: String) {
        selectedRole = role

        val activeStrokeColor = ContextCompat.getColor(this, R.color.colorBorderActive)
        val inactiveStrokeColor = ContextCompat.getColor(this, R.color.colorBorder)
        val activeBgColor = ContextCompat.getColor(this, R.color.colorSurfaceVariant)
        val inactiveBgColor = ContextCompat.getColor(this, R.color.colorSurface)
        val activeChevronColor = ContextCompat.getColor(this, R.color.colorAccent)
        val inactiveChevronColor = ContextCompat.getColor(this, R.color.colorTextDisabled)

        if (role == "attendee") {
            // Highlight Attendee Card
            binding.cardAttendee.setStrokeColor(activeStrokeColor)
            binding.cardAttendee.strokeWidth = resources.getDimensionPixelSize(R.dimen.spacing2) // 2dp
            binding.cardAttendee.setCardBackgroundColor(activeBgColor)
            binding.stripAttendee.visibility = View.VISIBLE
            binding.chevronAttendee.setColorFilter(activeChevronColor)

            // Reset Creator Card
            binding.cardCreator.setStrokeColor(inactiveStrokeColor)
            binding.cardCreator.strokeWidth = resources.getDimensionPixelSize(R.dimen.spacing4) / 4 // 1dp
            binding.cardCreator.setCardBackgroundColor(inactiveBgColor)
            binding.stripCreator.visibility = View.GONE
            binding.chevronCreator.setColorFilter(inactiveChevronColor)
        } else {
            // Highlight Creator Card
            binding.cardCreator.setStrokeColor(activeStrokeColor)
            binding.cardCreator.strokeWidth = resources.getDimensionPixelSize(R.dimen.spacing2) // 2dp
            binding.cardCreator.setCardBackgroundColor(activeBgColor)
            binding.stripCreator.visibility = View.VISIBLE
            binding.chevronCreator.setColorFilter(activeChevronColor)

            // Reset Attendee Card
            binding.cardAttendee.setStrokeColor(inactiveStrokeColor)
            binding.cardAttendee.strokeWidth = resources.getDimensionPixelSize(R.dimen.spacing4) / 4 // 1dp
            binding.cardAttendee.setCardBackgroundColor(inactiveBgColor)
            binding.stripAttendee.visibility = View.GONE
            binding.chevronAttendee.setColorFilter(inactiveChevronColor)
        }

        // Enable Continue Button
        binding.btnContinue.isEnabled = true
        binding.btnContinue.alpha = 1.0f
    }
}
