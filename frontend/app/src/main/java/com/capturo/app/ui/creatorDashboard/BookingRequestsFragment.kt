package com.capturo.app.ui.creatorDashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.capturo.app.databinding.FragmentBookingRequestsBinding
import com.capturo.app.ui.booking.BookingListPageFragment
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BookingRequestsFragment : Fragment() {

    private var _binding: FragmentBookingRequestsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookingRequestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup Toolbar back button navigation
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        // Setup ViewPager2 Adapter
        val statuses = listOf("confirmed", "pending", "completed", "cancelled")
        val tabTitles = listOf("Confirmed", "Pending", "Completed", "Cancelled")

        binding.viewPagerBookings.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = statuses.size

            override fun createFragment(position: Int): Fragment {
                return if (statuses[position] == "pending") {
                    CreatorPendingRequestsFragment()
                } else {
                    BookingListPageFragment.newInstance(statuses[position])
                }
            }
        }

        // Setup TabLayout with ViewPager2 using TabLayoutMediator
        TabLayoutMediator(binding.tabLayoutBookings, binding.viewPagerBookings) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()
        
        // Select Pending by default for Creators to see new requests easily
        binding.viewPagerBookings.setCurrentItem(1, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
