package com.capturo.app.premium.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.capturo.app.R
import com.capturo.app.databinding.FragmentPremiumBookingsBinding
import com.capturo.app.premium.Booking
import com.capturo.app.premium.BookingStatus
import com.capturo.app.premium.DemoData

class BookingsFragment : Fragment() {

    private var _binding: FragmentPremiumBookingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: BookingAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPremiumBookingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = BookingAdapter(emptyList())
        binding.recyclerBookings.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerBookings.adapter = adapter

        select(BookingStatus.CONFIRMED)
        binding.tabUpcoming.setOnClickListener { select(BookingStatus.CONFIRMED) }
        binding.tabCompleted.setOnClickListener { select(BookingStatus.COMPLETED) }
        binding.tabCancelled.setOnClickListener { select(BookingStatus.CANCELLED) }
    }

    private fun select(status: BookingStatus) {
        styleTab(binding.tabUpcoming, status == BookingStatus.CONFIRMED)
        styleTab(binding.tabCompleted, status == BookingStatus.COMPLETED)
        styleTab(binding.tabCancelled, status == BookingStatus.CANCELLED)

        val list: List<Booking> = DemoData.bookings.filter { it.status == status }
        adapter.submit(list)
        binding.textEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun styleTab(tab: TextView, active: Boolean) {
        tab.setBackgroundResource(if (active) R.drawable.bg_chip_gold else R.drawable.bg_chip_dark)
        tab.setTextColor(
            resources.getColor(
                if (active) R.color.colorTextOnAccent else R.color.colorTextSecondary, null
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
