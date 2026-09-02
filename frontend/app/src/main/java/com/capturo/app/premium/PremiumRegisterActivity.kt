package com.capturo.app.premium

import android.Manifest
import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import coil.load
import com.capturo.app.R
import com.capturo.app.databinding.ActivityPremiumRegisterBinding
import com.capturo.app.utils.LocationUtils
import java.util.Locale

/**
 * Lets a customer register themselves as a photographer: name, mobile, email,
 * price/hour, event types, location and sample photos. Hitting "Post & Go Live"
 * publishes the listing so every customer can discover and book them.
 */
class PremiumRegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPremiumRegisterBinding

    private val selectedEvents = linkedSetOf<String>()
    private val sampleImages = mutableListOf<String>()
    private var pickedLat: Double? = null
    private var pickedLon: Double? = null

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private val pickImages = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> -> uris.forEach { addSampleImage(it) } }

    private val requestLocation = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) fetchLocation() else toast("Location permission denied") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPremiumRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Prefill if the user already registered before (edit flow).
        PremiumStore.myPhotographer(this)?.let { prefill(it) }

        buildEventChips()

        binding.btnBack.setOnClickListener { finish() }
        binding.btnAddPhoto.setOnClickListener { pickImages.launch(arrayOf("image/*")) }
        binding.btnUseLocation.setOnClickListener { requestOrFetchLocation() }
        binding.btnPost.setOnClickListener { post() }
    }

    private fun prefill(p: PremiumStore.RegisteredPhotographer) {
        binding.inputName.setText(p.name)
        binding.inputMobile.setText(p.mobile)
        binding.inputEmail.setText(p.email)
        if (p.pricePerHour > 0) binding.inputPrice.setText(p.pricePerHour.toString())
        binding.inputLocation.setText(p.location)
        pickedLat = p.lat
        pickedLon = p.lon
        selectedEvents.addAll(p.eventTypes)
        p.sampleImages.forEach { addSampleImage(Uri.parse(it), persist = false) }
    }

    // ---------- Event type chips (multi-select) ----------
    private fun buildEventChips() {
        binding.eventChips.removeAllViews()
        // Wrap into rows of chips so all event types are reachable.
        var row = newChipRow().also { binding.eventChips.addView(it) }
        DemoData.eventTypes.forEachIndexed { i, event ->
            if (i > 0 && i % 3 == 0) row = newChipRow().also { binding.eventChips.addView(it) }
            row.addView(eventChip(event))
        }
        binding.eventChips.orientation = LinearLayout.VERTICAL
    }

    private fun newChipRow() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(8) }
    }

    private fun eventChip(event: String): TextView {
        val chip = TextView(this).apply {
            text = event
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(9), dp(16), dp(9))
            textSize = 13f
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.marginEnd = dp(8)
            layoutParams = lp
            setOnClickListener {
                if (selectedEvents.contains(event)) selectedEvents.remove(event)
                else selectedEvents.add(event)
                styleChip(this, event)
            }
        }
        styleChip(chip, event)
        return chip
    }

    private fun styleChip(chip: TextView, event: String) {
        val active = selectedEvents.contains(event)
        chip.setBackgroundResource(if (active) R.drawable.bg_chip_gold else R.drawable.bg_chip_dark)
        chip.setTextColor(
            ContextCompat.getColor(this, if (active) R.color.colorTextOnAccent else R.color.colorTextSecondary)
        )
    }

    // ---------- Sample images ----------
    private fun addSampleImage(uri: Uri, persist: Boolean = true) {
        if (persist) {
            runCatching {
                contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
        val s = uri.toString()
        if (sampleImages.contains(s)) return
        sampleImages.add(s)

        val thumb = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(96), dp(96)).apply { marginEnd = dp(10) }
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundResource(R.drawable.bg_card_premium)
            load(uri) { placeholder(R.drawable.bg_image_placeholder); error(R.drawable.bg_image_placeholder) }
            setOnClickListener {
                sampleImages.remove(s)
                binding.imageStrip.removeView(this)
                toast("Photo removed")
            }
        }
        // Insert after the "Add" tile so the add tile stays first.
        binding.imageStrip.addView(thumb, 1)
    }

    // ---------- Location ----------
    private fun requestOrFetchLocation() {
        if (LocationUtils.hasLocationPermission(this)) fetchLocation()
        else requestLocation.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun fetchLocation() {
        toast("Getting your location…")
        LocationUtils.getCurrentLocation(this) { lat, lon ->
            pickedLat = lat
            pickedLon = lon
            val label = reverseGeocode(lat, lon)
            if (binding.inputLocation.text.isNullOrBlank() && label != null) {
                binding.inputLocation.setText(label)
            }
            toast("Location set ✓")
        }
    }

    private fun reverseGeocode(lat: Double, lon: Double): String? = runCatching {
        @Suppress("DEPRECATION")
        val list = Geocoder(this, Locale.getDefault()).getFromLocation(lat, lon, 1)
        val a = list?.firstOrNull() ?: return null
        listOfNotNull(a.subLocality ?: a.locality, a.locality?.takeIf { it != a.subLocality })
            .distinct().joinToString(", ").ifBlank { a.getAddressLine(0) }
    }.getOrNull()

    // ---------- Post ----------
    private fun post() {
        val name = binding.inputName.text?.toString()?.trim().orEmpty()
        val mobile = binding.inputMobile.text?.toString()?.trim().orEmpty()
        val email = binding.inputEmail.text?.toString()?.trim().orEmpty()
        val price = binding.inputPrice.text?.toString()?.trim()?.toIntOrNull() ?: 0
        val location = binding.inputLocation.text?.toString()?.trim().orEmpty()

        when {
            name.isEmpty() -> return toast("Please enter your name")
            mobile.length < 7 -> return toast("Please enter a valid mobile number")
            email.isEmpty() || !email.contains("@") -> return toast("Please enter a valid email")
            price <= 0 -> return toast("Please enter your price per hour")
            selectedEvents.isEmpty() -> return toast("Select at least one event type")
            location.isEmpty() -> return toast("Please enter your location")
            sampleImages.isEmpty() -> return toast("Add at least one sample photo")
        }

        // Stable id so re-posting edits the same listing instead of duplicating.
        val id = PremiumStore.myPhotographer(this)?.id ?: "me-${System.currentTimeMillis()}"
        val reg = PremiumStore.RegisteredPhotographer(
            id = id,
            name = name,
            mobile = mobile,
            email = email,
            pricePerHour = price,
            eventTypes = selectedEvents.toList(),
            location = location,
            lat = pickedLat,
            lon = pickedLon,
            sampleImages = sampleImages.toList()
        )
        PremiumStore.saveMyPhotographer(this, reg)
        with(PremiumStore) { DemoData.mergeLocal(listOf(reg.toPhotographer())) }
        PremiumStore.setPhotographerMode(this, true)

        Toast.makeText(this, "You're live! Customers can now book you 🎉", Toast.LENGTH_LONG).show()
        startActivity(
            Intent(this, PremiumDashboardActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
        finish()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
