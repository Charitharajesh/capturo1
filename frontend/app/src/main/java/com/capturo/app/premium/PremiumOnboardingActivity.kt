package com.capturo.app.premium

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.capturo.app.R
import com.capturo.app.databinding.ActivityPremiumOnboardingBinding

private data class Slide(val image: String, val title: String, val subtitle: String)

class PremiumOnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPremiumOnboardingBinding

    private val slides = listOf(
        Slide("https://images.unsplash.com/photo-1519741497674-611481863552?auto=format&fit=crop&w=900&q=70",
            "Find Your Perfect Photographer",
            "Discover talented photographers near you for every special moment."),
        Slide("https://images.unsplash.com/photo-1520854221256-17451cc331bf?auto=format&fit=crop&w=900&q=70",
            "Explore Their Work",
            "Browse real portfolios, styles, reviews, packages and previous work."),
        Slide("https://images.unsplash.com/photo-1511285560929-80b456fea0bc?auto=format&fit=crop&w=900&q=70",
            "Book With Confidence",
            "Choose your date, time and package and reserve your photographer.")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPremiumOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.pager.adapter = SlideAdapter(slides)
        buildDots(slides.size)
        setActiveDot(0)

        binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                setActiveDot(position)
                binding.btnNext.text = if (position == slides.size - 1) "Get Started" else "Next"
            }
        })

        binding.btnSkip.setOnClickListener { finishOnboarding() }
        binding.btnNext.setOnClickListener {
            val next = binding.pager.currentItem + 1
            if (next < slides.size) binding.pager.currentItem = next else finishOnboarding()
        }
    }

    private fun finishOnboarding() {
        getSharedPreferences("capturo_premium", Context.MODE_PRIVATE)
            .edit().putBoolean("onboarded", true).apply()
        startActivity(Intent(this, PremiumAuthActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private fun buildDots(count: Int) {
        binding.dots.removeAllViews()
        repeat(count) {
            val dot = View(this)
            val lp = LinearLayout.LayoutParams(dp(6), dp(6))
            lp.marginStart = dp(4); lp.marginEnd = dp(4)
            dot.layoutParams = lp
            dot.setBackgroundResource(R.drawable.bg_dot_inactive)
            binding.dots.addView(dot)
        }
    }

    private fun setActiveDot(active: Int) {
        for (i in 0 until binding.dots.childCount) {
            val dot = binding.dots.getChildAt(i)
            val lp = dot.layoutParams as LinearLayout.LayoutParams
            if (i == active) {
                lp.width = dp(22); lp.height = dp(6)
                dot.setBackgroundResource(R.drawable.bg_dot_active)
            } else {
                lp.width = dp(6); lp.height = dp(6)
                dot.setBackgroundResource(R.drawable.bg_dot_inactive)
            }
            dot.layoutParams = lp
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private inner class SlideAdapter(private val items: List<Slide>) :
        RecyclerView.Adapter<SlideAdapter.VH>() {

        inner class VH(val root: View) : RecyclerView.ViewHolder(root) {
            val image: ImageView = root.findViewById(R.id.imageSlide)
            val title: TextView = root.findViewById(R.id.textTitle)
            val subtitle: TextView = root.findViewById(R.id.textSubtitle)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_premium_onboarding, parent, false)
            return VH(v)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val s = items[position]
            holder.image.load(s.image) {
                placeholder(R.drawable.bg_image_placeholder)
                error(R.drawable.bg_image_placeholder)
            }
            holder.title.text = s.title
            holder.subtitle.text = s.subtitle
        }
    }
}
