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
import com.capturo.app.databinding.FragmentPremiumHomeBinding
import com.capturo.app.premium.DemoData
import com.capturo.app.premium.PremiumMainActivity
import com.capturo.app.premium.PremiumProfileActivity
import com.capturo.app.premium.PremiumSearchActivity

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
