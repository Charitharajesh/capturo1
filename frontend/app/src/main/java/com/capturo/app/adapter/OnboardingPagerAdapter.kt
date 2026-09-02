package com.capturo.app.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.capturo.app.R
import com.capturo.app.ui.onboarding.OnboardingFragment

/**
 * ViewPager2 adapter hosting standard onboarding step slides.
 */
class OnboardingPagerAdapter(
    activity: FragmentActivity
) : FragmentStateAdapter(activity) {

    private val slides = listOf(
        OnboardingSlide(
            "Instant Booking",
            "Book professional content creators instantly for your weddings, parties, or corporate events.",
            R.drawable.img_onboarding_booking
        ),
        OnboardingSlide(
            "Real-time Delivery",
            "Get your high-quality media delivered to you instantly through our fast delivery pipeline.",
            R.drawable.img_onboarding_delivery
        ),
        OnboardingSlide(
            "Nearby Creators",
            "Discover professional creators within your location radius with transparent pricing and reviews.",
            R.drawable.img_onboarding_nearby
        )
    )

    override fun getItemCount(): Int = slides.size

    override fun createFragment(position: Int): Fragment {
        val slide = slides[position]
        return OnboardingFragment.newInstance(slide.title, slide.desc, slide.imageRes)
    }

    private data class OnboardingSlide(
        val title: String,
        val desc: String,
        val imageRes: Int
    )
}
