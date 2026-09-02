package com.capturo.app.premium

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.capturo.app.R
import com.capturo.app.databinding.ActivityPremiumBookingBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PremiumBookingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPremiumBookingBinding
    private lateinit var photographer: Photographer

    private val platformFee = 500

    private var selectedEvent = DemoData.eventTypes.first()
    private var selectedDate = ""
    private var selectedSlot = ""
    private var selectedPackage: PhotoPackage? = null

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPremiumBookingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val id = intent.getStringExtra(EXTRA_ID) ?: DemoData.photographers.first().id
        photographer = DemoData.byId(id)
        selectedPackage = photographer.packages.getOrNull(1) ?: photographer.packages.first()

        binding.textTitle.text = "Book ${photographer.name}"
        binding.btnBack.setOnClickListener { finish() }

        buildEventChips()
        buildDateChips()
        buildSlots()
        buildPackages()

        binding.inputLocation.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = updateSummary()
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnContinue.setOnClickListener { continueToPayment() }

        updateSummary()
    }

    // ---------- Event ----------
    private fun buildEventChips() {
        binding.eventChips.removeAllViews()
        DemoData.eventTypes.forEach { event ->
            val chip = chip(event) {
                selectedEvent = event
                refreshChipRow(binding.eventChips, event) { (it as TextView).text.toString() }
                updateSummary()
            }
            binding.eventChips.addView(chip)
        }
        refreshChipRow(binding.eventChips, selectedEvent) { (it as TextView).text.toString() }
    }

    // ---------- Date ----------
    private fun buildDateChips() {
        binding.dateChips.removeAllViews()
        val cal = Calendar.getInstance()
        val dowFmt = SimpleDateFormat("EEE", Locale.getDefault())
        val dayFmt = SimpleDateFormat("d", Locale.getDefault())
        val monFmt = SimpleDateFormat("MMM", Locale.getDefault())
        val fullFmt = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

        for (i in 0 until 14) {
            val date = cal.time
            val full = fullFmt.format(date)
            if (i == 0) selectedDate = full

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(14), dp(12), dp(14), dp(12))
                val lp = LinearLayout.LayoutParams(dp(64), LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.marginEnd = dp(10)
                layoutParams = lp
                tag = full
                setOnClickListener {
                    selectedDate = full
                    styleDateChips()
                    updateSummary()
                }
            }
            card.addView(TextView(this).apply {
                text = dowFmt.format(date); textSize = 11f
                setTextColor(ContextCompat.getColor(this@PremiumBookingActivity, R.color.colorTextHint))
            })
            card.addView(TextView(this).apply {
                text = dayFmt.format(date); textSize = 19f
                typeface = androidx.core.content.res.ResourcesCompat.getFont(this@PremiumBookingActivity, R.font.poppins_bold)
                setTextColor(ContextCompat.getColor(this@PremiumBookingActivity, R.color.colorTextPrimary))
            })
            card.addView(TextView(this).apply {
                text = monFmt.format(date); textSize = 11f
                setTextColor(ContextCompat.getColor(this@PremiumBookingActivity, R.color.colorTextHint))
            })
            binding.dateChips.addView(card)
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        styleDateChips()
    }

    private fun styleDateChips() {
        for (i in 0 until binding.dateChips.childCount) {
            val card = binding.dateChips.getChildAt(i)
            card.setBackgroundResource(
                if (card.tag == selectedDate) R.drawable.bg_chip_gold else R.drawable.bg_chip_dark
            )
        }
    }

    // ---------- Time slots ----------
    private fun buildSlots() {
        binding.slotContainer.removeAllViews()
        DemoData.timeSlots.forEach { (label, status) ->
            val enabled = status != "booked"
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(14), dp(16), dp(14))
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.bottomMargin = dp(10)
                layoutParams = lp
                tag = label
                setBackgroundResource(R.drawable.bg_chip_dark)
                isEnabled = enabled
                alpha = if (enabled) 1f else 0.45f
                if (enabled) setOnClickListener {
                    selectedSlot = label
                    styleSlots()
                    updateSummary()
                }
            }
            row.addView(TextView(this).apply {
                text = label
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setTextColor(ContextCompat.getColor(this@PremiumBookingActivity, R.color.colorTextPrimary))
            })
            val (dot, colorRes) = when (status) {
                "available" -> "🟢 Available" to R.color.colorSuccess
                "few" -> "🟠 Few left" to R.color.colorWarning
                else -> "🔴 Booked" to R.color.colorError
            }
            row.addView(TextView(this).apply {
                text = dot; textSize = 11f
                setTextColor(ContextCompat.getColor(this@PremiumBookingActivity, colorRes))
            })
            binding.slotContainer.addView(row)
            if (enabled && selectedSlot.isEmpty()) selectedSlot = label
        }
        styleSlots()
    }

    private fun styleSlots() {
        for (i in 0 until binding.slotContainer.childCount) {
            val row = binding.slotContainer.getChildAt(i)
            if (!row.isEnabled) continue
            row.setBackgroundResource(
                if (row.tag == selectedSlot) R.drawable.bg_card_active else R.drawable.bg_chip_dark
            )
        }
    }

    // ---------- Packages ----------
    private fun buildPackages() {
        binding.packageContainer.removeAllViews()
        photographer.packages.forEach { pkg ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(14), dp(16), dp(14))
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.bottomMargin = dp(10)
                layoutParams = lp
                tag = pkg.name
                setOnClickListener {
                    selectedPackage = pkg
                    stylePackages()
                    updateSummary()
                }
            }
            val header = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            header.addView(TextView(this).apply {
                text = pkg.name
                textSize = 16f
                typeface = androidx.core.content.res.ResourcesCompat.getFont(this@PremiumBookingActivity, R.font.poppins_semibold)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setTextColor(ContextCompat.getColor(this@PremiumBookingActivity, R.color.colorTextPrimary))
            })
            header.addView(TextView(this).apply {
                text = pkg.price
                textSize = 16f
                typeface = androidx.core.content.res.ResourcesCompat.getFont(this@PremiumBookingActivity, R.font.poppins_bold)
                setTextColor(ContextCompat.getColor(this@PremiumBookingActivity, R.color.colorPrimary))
            })
            card.addView(header)
            card.addView(TextView(this).apply {
                text = pkg.features.joinToString(" • ")
                textSize = 12f
                setPadding(0, dp(6), 0, 0)
                setTextColor(ContextCompat.getColor(this@PremiumBookingActivity, R.color.colorTextSecondary))
            })
            binding.packageContainer.addView(card)
        }
        stylePackages()
    }

    private fun stylePackages() {
        for (i in 0 until binding.packageContainer.childCount) {
            val card = binding.packageContainer.getChildAt(i)
            card.setBackgroundResource(
                if (card.tag == selectedPackage?.name) R.drawable.bg_card_active else R.drawable.bg_card_premium
            )
        }
    }

    // ---------- Generic chip helpers ----------
    private fun chip(label: String, onClick: () -> Unit): TextView {
        val t = TextView(this)
        t.text = label
        t.gravity = Gravity.CENTER
        t.setPadding(dp(16), dp(9), dp(16), dp(9))
        t.textSize = 13f
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.marginEnd = dp(8)
        t.layoutParams = lp
        t.setOnClickListener { onClick() }
        return t
    }

    private fun refreshChipRow(row: LinearLayout, selected: String, labelOf: (android.view.View) -> String) {
        for (i in 0 until row.childCount) {
            val v = row.getChildAt(i)
            val active = labelOf(v) == selected
            v.setBackgroundResource(if (active) R.drawable.bg_chip_gold else R.drawable.bg_chip_dark)
            (v as? TextView)?.setTextColor(
                ContextCompat.getColor(this, if (active) R.color.colorTextOnAccent else R.color.colorTextSecondary)
            )
        }
    }

    // ---------- Summary ----------
    private fun priceToInt(s: String): Int = s.filter { it.isDigit() }.toIntOrNull() ?: 0

    private fun total(): Int = (selectedPackage?.let { priceToInt(it.price) } ?: 0) + platformFee

    private fun updateSummary() {
        setRow(binding.rowPhotographer.textLabel, binding.rowPhotographer.textValue, "Photographer", photographer.name)
        setRow(binding.rowEvent.textLabel, binding.rowEvent.textValue, "Event", selectedEvent)
        setRow(binding.rowDate.textLabel, binding.rowDate.textValue, "Date", selectedDate)
        setRow(binding.rowTime.textLabel, binding.rowTime.textValue, "Time", selectedSlot.ifEmpty { "—" })
        val loc = binding.inputLocation.text?.toString()?.ifBlank { "—" } ?: "—"
        setRow(binding.rowLocation.textLabel, binding.rowLocation.textValue, "Location", loc)
        setRow(binding.rowPackage.textLabel, binding.rowPackage.textValue, "Package", selectedPackage?.name ?: "—")
        setRow(binding.rowPrice.textLabel, binding.rowPrice.textValue, "Package Price", selectedPackage?.price ?: "—")
        setRow(binding.rowFee.textLabel, binding.rowFee.textValue, "Platform Fee", "₹$platformFee")
        binding.textTotal.text = "₹${"%,d".format(total())}"
    }

    private fun setRow(label: TextView, value: TextView, l: String, v: String) {
        label.text = l
        value.text = v
    }

    private fun continueToPayment() {
        val location = binding.inputLocation.text?.toString()?.trim().orEmpty()
        if (location.isEmpty()) {
            Toast.makeText(this, "Please enter the event location", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(
            Intent(this, PremiumPaymentActivity::class.java)
                .putExtra(PremiumPaymentActivity.EXTRA_PHOTOGRAPHER_ID, photographer.id)
                .putExtra(PremiumPaymentActivity.EXTRA_PHOTOGRAPHER, photographer.name)
                .putExtra(PremiumPaymentActivity.EXTRA_AVATAR, photographer.avatarUrl)
                .putExtra(PremiumPaymentActivity.EXTRA_EVENT, selectedEvent)
                .putExtra(PremiumPaymentActivity.EXTRA_DATE, selectedDate)
                .putExtra(PremiumPaymentActivity.EXTRA_TIME, selectedSlot)
                .putExtra(PremiumPaymentActivity.EXTRA_LOCATION, location)
                .putExtra(PremiumPaymentActivity.EXTRA_PACKAGE, selectedPackage?.name)
                .putExtra(PremiumPaymentActivity.EXTRA_TOTAL, total())
        )
    }

    companion object {
        const val EXTRA_ID = "extra_photographer_id"
    }
}
