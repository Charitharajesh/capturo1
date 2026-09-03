package com.capturo.app.premium

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import coil.load
import com.capturo.app.R
import com.capturo.app.databinding.ActivityPremiumPaymentBinding
import kotlin.random.Random

class PremiumPaymentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPremiumPaymentBinding

    private val methods = listOf(
        "UPI", "Google Pay", "PhonePe", "Paytm", "Credit / Debit Card", "Net Banking"
    )
    private var selectedMethod = "UPI"
    private var total = 0

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPremiumPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val photographer = intent.getStringExtra(EXTRA_PHOTOGRAPHER) ?: "Photographer"
        val avatar = intent.getStringExtra(EXTRA_AVATAR)
        val event = intent.getStringExtra(EXTRA_EVENT) ?: ""
        val date = intent.getStringExtra(EXTRA_DATE) ?: ""
        total = intent.getIntExtra(EXTRA_TOTAL, 0)

        binding.textPhotographer.text = photographer
        binding.textMeta.text = "$event • $date"
        avatar?.let { binding.imageAvatar.load(it) { placeholder(R.drawable.bg_image_placeholder) } }
        val amount = "₹${"%,d".format(total)}"
        binding.textAmount.text = amount
        binding.btnPay.text = "Pay $amount"

        buildMethods()

        binding.btnBack.setOnClickListener { finish() }
        binding.btnPay.setOnClickListener { pay() }
    }

    private fun buildMethods() {
        binding.methodsContainer.removeAllViews()
        methods.forEach { method ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(16), dp(16), dp(16))
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.topMargin = dp(10)
                layoutParams = lp
                tag = method
                setOnClickListener {
                    selectedMethod = method
                    styleMethods()
                }
            }
            val icon = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(22), dp(22))
                setImageResource(R.drawable.ic_payments)
                setColorFilter(ContextCompat.getColor(this@PremiumPaymentActivity, R.color.colorPrimary))
            }
            val label = TextView(this).apply {
                text = method
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setPadding(dp(12), 0, 0, 0)
                setTextColor(ContextCompat.getColor(this@PremiumPaymentActivity, R.color.colorTextPrimary))
            }
            val check = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(20), dp(20))
                setImageResource(R.drawable.ic_check_circle)
                setColorFilter(ContextCompat.getColor(this@PremiumPaymentActivity, R.color.colorSuccess))
                tag = "check"
            }
            row.addView(icon)
            row.addView(label)
            row.addView(check)
            binding.methodsContainer.addView(row)
        }
        styleMethods()
    }

    private fun styleMethods() {
        for (i in 0 until binding.methodsContainer.childCount) {
            val row = binding.methodsContainer.getChildAt(i) as LinearLayout
            val active = row.tag == selectedMethod
            row.setBackgroundResource(if (active) R.drawable.bg_card_active else R.drawable.bg_card_premium)
            row.findViewWithTag<ImageView>("check")?.visibility =
                if (active) android.view.View.VISIBLE else android.view.View.INVISIBLE
        }
    }

    private fun pay() {
        binding.btnPay.isEnabled = false
        binding.btnPay.text = "Processing…"
        binding.btnPay.postDelayed({
            val bookingId = "CAP-${Random.nextInt(1000, 9999)}"
            recordPayment(bookingId)
            sendReceiptToChat(bookingId)
            startActivity(
                Intent(this, PremiumConfirmationActivity::class.java)
                    .putExtra(PremiumConfirmationActivity.EXTRA_BOOKING_ID, bookingId)
                    .putExtra(PremiumConfirmationActivity.EXTRA_PHOTOGRAPHER, intent.getStringExtra(EXTRA_PHOTOGRAPHER))
                    .putExtra(PremiumConfirmationActivity.EXTRA_DATE, intent.getStringExtra(EXTRA_DATE))
                    .putExtra(PremiumConfirmationActivity.EXTRA_TIME, intent.getStringExtra(EXTRA_TIME))
                    .putExtra(PremiumConfirmationActivity.EXTRA_LOCATION, intent.getStringExtra(EXTRA_LOCATION))
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 1400)
    }

    /** Persists the completed payment to the on-device payment history. */
    private fun recordPayment(bookingId: String) {
        val photographer = intent.getStringExtra(EXTRA_PHOTOGRAPHER) ?: "Photographer"
        val event = intent.getStringExtra(EXTRA_EVENT) ?: ""
        val date = intent.getStringExtra(EXTRA_DATE) ?: ""
        PremiumStore.addPayment(
            this,
            PremiumStore.PaymentRecord(
                id = bookingId,
                photographer = photographer,
                event = event,
                date = date,
                method = selectedMethod,
                amount = total,
                ts = System.currentTimeMillis()
            )
        )
    }

    /**
     * After a successful payment, the photographer sends the customer a thank-you
     * message plus the booking receipt as a PDF in the chat thread.
     */
    private fun sendReceiptToChat(bookingId: String) {
        val photographerId = intent.getStringExtra(EXTRA_PHOTOGRAPHER_ID) ?: return
        val photographerName = intent.getStringExtra(EXTRA_PHOTOGRAPHER) ?: "Photographer"
        val event = intent.getStringExtra(EXTRA_EVENT) ?: ""
        val date = intent.getStringExtra(EXTRA_DATE) ?: ""
        val time = intent.getStringExtra(EXTRA_TIME) ?: ""
        val location = intent.getStringExtra(EXTRA_LOCATION) ?: ""
        val pkg = intent.getStringExtra(EXTRA_PACKAGE) ?: ""

        val details = PremiumReceipt.Details(
            bookingId = bookingId,
            photographer = photographerName,
            customer = "You",
            event = event,
            date = date,
            time = time,
            location = location,
            pkg = pkg,
            paymentMethod = selectedMethod,
            total = total
        )

        val now = System.currentTimeMillis()
        val thankYou = PremiumStore.ChatMessage(
            fromMe = false,
            text = "Hi! 🎉 Thanks for booking $photographerName for your $event on $date " +
                "($time) at $location. Your booking $bookingId is confirmed. " +
                "I've attached your receipt below — see you at the shoot! 📸",
            ts = now
        )
        PremiumStore.addMessage(this, photographerId, thankYou)

        runCatching {
            val uri = PremiumReceipt.generate(this, details)
            val receiptMsg = PremiumStore.ChatMessage(
                fromMe = false,
                text = "",
                ts = now + 1,
                attachmentUri = uri.toString(),
                attachmentName = PremiumReceipt.fileNameFor(details)
            )
            PremiumStore.addMessage(this, photographerId, receiptMsg)
        }
    }

    companion object {
        const val EXTRA_PHOTOGRAPHER_ID = "extra_photographer_id"
        const val EXTRA_PHOTOGRAPHER = "extra_photographer"
        const val EXTRA_AVATAR = "extra_avatar"
        const val EXTRA_EVENT = "extra_event"
        const val EXTRA_DATE = "extra_date"
        const val EXTRA_TIME = "extra_time"
        const val EXTRA_LOCATION = "extra_location"
        const val EXTRA_PACKAGE = "extra_package"
        const val EXTRA_TOTAL = "extra_total"
    }
}
