package com.capturo.app.premium

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import coil.load
import com.capturo.app.R
import com.capturo.app.databinding.ActivityPremiumProfileDetailBinding
import com.capturo.app.premium.ui.PortfolioAdapter

class PremiumProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPremiumProfileDetailBinding
    private lateinit var photographer: Photographer
    private lateinit var portfolioAdapter: PortfolioAdapter
    private var displayedPortfolio: List<String> = emptyList()

    private val tabs = listOf("All", "Weddings", "Portraits", "Events", "Pre-Wedding", "Videos")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPremiumProfileDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT

        val id = intent.getStringExtra(EXTRA_ID) ?: DemoData.photographers.first().id
        photographer = DemoData.byId(id)

        bindHeader()
        setupPortfolio()

        ViewCompat.setOnApplyWindowInsetsListener(binding.btnBack) { v, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            (v.layoutParams as android.view.ViewGroup.MarginLayoutParams).topMargin =
                top + (8 * resources.displayMetrics.density).toInt()
            v.requestLayout()
            insets
        }

        binding.btnBack.setOnClickListener { finish() }
        binding.btnShareTop.setOnClickListener { toast("Sharing ${photographer.name}") }

        updateSaveIcon()
        binding.btnSaveTop.setOnClickListener {
            val saved = PremiumStore.toggleSaved(this, photographer.id)
            updateSaveIcon()
            toast(if (saved) "Saved to favorites ♥" else "Removed from favorites")
        }
        binding.btnMessage.setOnClickListener {
            startActivity(
                Intent(this, PremiumChatActivity::class.java)
                    .putExtra(PremiumChatActivity.EXTRA_ID, photographer.id)
            )
        }
        binding.btnBookNow.setOnClickListener {
            startActivity(
                Intent(this, PremiumBookingActivity::class.java)
                    .putExtra(PremiumBookingActivity.EXTRA_ID, photographer.id)
            )
        }
    }

    private fun bindHeader() {
        val p = photographer
        binding.imageCover.load(p.coverUrl) {
            placeholder(R.drawable.bg_image_placeholder)
            error(R.drawable.bg_image_placeholder)
        }
        binding.textName.text = p.name
        binding.iconVerified.visibility = if (p.verified) android.view.View.VISIBLE else android.view.View.GONE
        binding.textRating.text = "${p.rating} (${p.reviews} reviews)"
        binding.textLocation.text = p.location
        binding.textExperience.text = "${p.experience} yrs"
        binding.textFollowers.text = p.followers
        binding.textBookingsCount.text = p.bookings.toString()
        binding.textAbout.text = p.about
    }

    private fun setupPortfolio() {
        portfolioAdapter = PortfolioAdapter(photographer.portfolio) { index ->
            startActivity(
                Intent(this, PremiumImageViewerActivity::class.java)
                    .putStringArrayListExtra(
                        PremiumImageViewerActivity.EXTRA_IMAGES,
                        ArrayList(displayedPortfolio)
                    )
                    .putExtra(PremiumImageViewerActivity.EXTRA_START, index)
            )
        }
        binding.recyclerPortfolio.adapter = portfolioAdapter
        buildTabs()
        selectTab(0)
    }

    private fun buildTabs() {
        val density = resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        binding.tabsPortfolio.removeAllViews()
        tabs.forEachIndexed { i, label ->
            val chip = TextView(this).apply {
                text = label
                gravity = Gravity.CENTER
                setPadding(dp(16), dp(8), dp(16), dp(8))
                textSize = 12f
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.marginEnd = dp(8)
                layoutParams = lp
                setOnClickListener { selectTab(i) }
            }
            binding.tabsPortfolio.addView(chip)
        }
    }

    private fun selectTab(active: Int) {
        for (i in 0 until binding.tabsPortfolio.childCount) {
            val chip = binding.tabsPortfolio.getChildAt(i) as TextView
            if (i == active) {
                chip.setBackgroundResource(R.drawable.bg_chip_gold)
                chip.setTextColor(ContextCompat.getColor(this, R.color.colorTextOnAccent))
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip_dark)
                chip.setTextColor(ContextCompat.getColor(this, R.color.colorTextSecondary))
            }
        }
        // Demo filtering: each tab surfaces a different slice of the portfolio.
        val base = photographer.portfolio
        val filtered = when (active) {
            0 -> base
            else -> base.drop(active - 1).ifEmpty { base }
        }
        displayedPortfolio = filtered
        portfolioAdapter.submit(filtered)
    }

    private fun updateSaveIcon() {
        val saved = PremiumStore.isSaved(this, photographer.id)
        binding.btnSaveTop.setImageResource(
            if (saved) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
        )
        binding.btnSaveTop.setColorFilter(
            androidx.core.content.ContextCompat.getColor(
                this, if (saved) R.color.colorPrimary else R.color.colorTextPrimary
            )
        )
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    companion object {
        const val EXTRA_ID = "extra_photographer_id"
    }
}
