package com.capturo.app.premium.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import androidx.core.content.ContextCompat
import com.capturo.app.R
import com.capturo.app.databinding.FragmentPremiumHomeBinding
import com.capturo.app.premium.DemoData
import com.capturo.app.premium.PremiumDashboardActivity
import com.capturo.app.premium.PremiumMainActivity
import com.capturo.app.premium.PremiumProfileActivity
import com.capturo.app.premium.PremiumRegisterActivity
import com.capturo.app.premium.PremiumSearchActivity
import com.capturo.app.premium.PremiumStore

class HomeFragment : Fragment() {

    private var _binding: FragmentPremiumHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPremiumHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tryBannerImage.load(DemoData.photographers[2].coverUrl)

        // Personalised greeting from the signed-in account.
        PremiumStore.currentAccount(requireContext())?.let { acc ->
            val first = acc.name.trim().split(" ").firstOrNull().orEmpty()
            if (first.isNotEmpty()) binding.textGreeting.text = "Good morning, $first 👋"
        }

        // Top-right profile icon → open the profile tab.
        binding.imageAvatar.setOnClickListener {
            (activity as? PremiumMainActivity)?.goToProfile()
        }

        setupModeToggle()

        binding.recyclerCategories.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerCategories.adapter = CategoryAdapter(DemoData.categories) { c ->
            startActivity(
                Intent(requireContext(), PremiumSearchActivity::class.java)
                    .putExtra(PremiumSearchActivity.EXTRA_CATEGORY, c.name)
            )
        }

        binding.searchBar.setOnClickListener {
            startActivity(Intent(requireContext(), PremiumSearchActivity::class.java))
        }

        binding.recyclerPhotographers.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerPhotographers.adapter =
            PhotographerAdapter(DemoData.photographers) { p -> openProfile(p.id) }

        val goTry = View.OnClickListener { (activity as? PremiumMainActivity)?.goToTry() }
        binding.tryBanner.setOnClickListener(goTry)
    }

    /** Animated User / Photographer segmented toggle with a sliding gold thumb. */
    private fun setupModeToggle() {
        // The home screen is the user experience, so the toggle always opens on
        // "User"; tapping "Photographer" animates and transitions to that side.
        binding.modeToggle.post {
            binding.toggleThumb.translationX = 0f
            styleTabs(photographer = false)
        }

        binding.tabUser.setOnClickListener { selectMode(photographer = false) }
        binding.tabPhotographer.setOnClickListener { selectMode(photographer = true) }
    }

    private fun styleTabs(photographer: Boolean) {
        val onAccent = ContextCompat.getColor(requireContext(), R.color.colorTextOnAccent)
        val secondary = ContextCompat.getColor(requireContext(), R.color.colorTextSecondary)
        binding.tabUser.setTextColor(if (photographer) secondary else onAccent)
        binding.tabPhotographer.setTextColor(if (photographer) onAccent else secondary)
    }

    private fun selectMode(photographer: Boolean) {
        if (PremiumStore.isPhotographerMode(requireContext()) == photographer && !photographer) return
        PremiumStore.setPhotographerMode(requireContext(), photographer)

        val travel = binding.toggleThumb.width.toFloat()
        binding.toggleThumb.animate()
            .translationX(if (photographer) travel else 0f)
            .setDuration(280)
            .withStartAction { styleTabs(photographer) }
            .withEndAction {
                if (photographer && isAdded) {
                    // Switch into the photographer experience.
                    val target = if (PremiumStore.myPhotographer(requireContext()) == null)
                        PremiumRegisterActivity::class.java else PremiumDashboardActivity::class.java
                    startActivity(Intent(requireContext(), target))
                }
            }
            .start()
    }

    private fun openProfile(id: String) {
        startActivity(
            Intent(requireContext(), PremiumProfileActivity::class.java)
                .putExtra(PremiumProfileActivity.EXTRA_ID, id)
        )
    }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
