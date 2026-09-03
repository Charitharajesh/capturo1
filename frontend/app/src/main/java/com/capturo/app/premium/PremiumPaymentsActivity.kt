package com.capturo.app.premium

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.capturo.app.R
import com.capturo.app.databinding.ActivityPremiumPaymentsBinding

/** Shows the on-device payment history, newest first. */
class PremiumPaymentsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPremiumPaymentsBinding

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPremiumPaymentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        val payments = PremiumStore.payments(this)
        binding.emptyState.visibility = if (payments.isEmpty()) View.VISIBLE else View.GONE
        payments.forEach { binding.paymentsContainer.addView(buildCard(it)) }
    }

    private fun buildCard(p: PremiumStore.PaymentRecord): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setBackgroundResource(R.drawable.bg_card_premium)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(12)
            layoutParams = lp
        }

        val left = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        left.addView(text(p.photographer, 15f, R.color.colorTextPrimary, R.font.poppins_semibold))
        left.addView(text("${p.event} • ${p.date}", 12f, R.color.colorTextSecondary, R.font.poppins_regular, dp(3)))
        left.addView(text("${p.method}  •  ${p.id}", 11f, R.color.colorTextHint, R.font.poppins_regular, dp(3)))

        val right = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        right.addView(text("₹${"%,d".format(p.amount)}", 16f, R.color.colorPrimary, R.font.poppins_bold))
        right.addView(text("Paid", 11f, R.color.colorSuccess, R.font.poppins_semibold, dp(2)))

        card.addView(left)
        card.addView(right)
        return card
    }

    private fun text(
        value: String, size: Float, colorRes: Int, fontRes: Int, topMargin: Int = 0
    ): TextView = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(ContextCompat.getColor(this@PremiumPaymentsActivity, colorRes))
        typeface = androidx.core.content.res.ResourcesCompat.getFont(this@PremiumPaymentsActivity, fontRes)
        if (topMargin > 0) {
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = topMargin
            layoutParams = lp
        }
    }
}
