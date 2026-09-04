package com.capturo.app.premium

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
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

        postBookingConfirmedNotification(
            photographer = intent.getStringExtra(EXTRA_PHOTOGRAPHER),
            date = intent.getStringExtra(EXTRA_DATE),
            time = intent.getStringExtra(EXTRA_TIME)
        )

        animateCheck()

        binding.btnViewBooking.setOnClickListener { goMain("bookings") }
        binding.btnHome.setOnClickListener { goMain("home") }
    }

    /** Fires a real "Booking confirmed" system notification once payment succeeds. */
    private fun postBookingConfirmedNotification(photographer: String?, date: String?, time: String?) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "booking_updates"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Booking Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }

        val whenText = listOfNotNull(date, time).joinToString(" · ").ifEmpty { "your session" }
        val withWho = photographer?.takeIf { it.isNotBlank() }?.let { " with $it" } ?: ""

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Booking confirmed")
            .setContentText("Payment received — your shoot$withWho is booked for $whenText.")
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
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
        private const val NOTIFICATION_ID = 4711

        const val EXTRA_BOOKING_ID = "extra_booking_id"
        const val EXTRA_PHOTOGRAPHER = "extra_photographer"
        const val EXTRA_DATE = "extra_date"
        const val EXTRA_TIME = "extra_time"
        const val EXTRA_LOCATION = "extra_location"
    }
}
