package com.capturo.app.premium

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.capturo.app.databinding.ActivityPremiumInfoBinding

class PremiumInfoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPremiumInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPremiumInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Settings"
        binding.textTitle.text = title
        binding.textBody.text = bodyFor(title)
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun bodyFor(title: String): String = when (title) {
        "Payments" -> "Your saved payment methods and transaction history appear here.\n\nUPI, cards and net-banking are supported. All CAPTURO payments are processed securely."
        "Notifications" -> "Manage push, email and in-app notifications.\n\n• Booking updates\n• New messages\n• Offers & recommendations\n\nAll notifications are enabled by default."
        "Help & Support" -> "Need a hand?\n\n• FAQs\n• Chat with support\n• support@capturo.app\n\nWe typically reply within a few hours."
        "Privacy" -> "Your privacy matters.\n\nCAPTURO only uses your data to power bookings and recommendations. You can request account deletion at any time."
        "Terms" -> "CAPTURO Terms of Service.\n\nBy using CAPTURO you agree to our booking, cancellation and content-usage policies. This is a demo build."
        else -> "This section is part of the CAPTURO experience."
    }

    companion object {
        const val EXTRA_TITLE = "extra_title"
    }
}
