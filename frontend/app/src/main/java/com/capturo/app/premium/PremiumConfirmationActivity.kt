package com.capturo.app.premium

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.capturo.app.databinding.ActivityPremiumConfirmationBinding

class PremiumConfirmationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPremiumConfirmationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPremiumConfirmationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setRow(binding.rowBookingId, "Booking ID", intent.getStringExtra(EXTRA_BOOKING_ID))
        setRow(binding.rowPhotographer, "Photographer", intent.getStringExtra(EXTRA_PHOTOGRAPHER))
        setRow(binding.rowDate, "Date", intent.getStringExtra(EXTRA_DATE))
        setRow(binding.rowTime, "Time", intent.getStringExtra(EXTRA_TIME))
        setRow(binding.rowLocation, "Location", intent.getStringExtra(EXTRA_LOCATION))

        animateCheck()

        binding.btnViewBooking.setOnClickListener { goMain("bookings") }
        binding.btnHome.setOnClickListener { goMain("home") }
    }

    private fun animateCheck() {
        binding.iconCheck.apply {
            scaleX = 0f; scaleY = 0f; alpha = 0f
            animate().scaleX(1f).scaleY(1f).alpha(1f)
                .setInterpolator(OvershootInterpolator(1.6f))
                .setDuration(560).start()
        }
    }

    private fun setRow(
        row: com.capturo.app.databinding.ItemPremiumSummaryRowBinding, label: String, value: String?
    ) {
        row.textLabel.text = label
        row.textValue.text = value ?: "—"
    }

    private fun goMain(tab: String) {
        startActivity(
            Intent(this, PremiumMainActivity::class.java)
                .putExtra(PremiumMainActivity.EXTRA_OPEN_TAB, tab)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }

    companion object {
        const val EXTRA_BOOKING_ID = "extra_booking_id"
        const val EXTRA_PHOTOGRAPHER = "extra_photographer"
        const val EXTRA_DATE = "extra_date"
        const val EXTRA_TIME = "extra_time"
        const val EXTRA_LOCATION = "extra_location"
    }
}
