package com.capturo.app.ui.booking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.content.ContextCompat
import com.capturo.app.R
import com.capturo.app.adapter.BookingListAdapter
import com.capturo.app.databinding.FragmentBookingListPageBinding
import com.capturo.app.utils.Resource
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import com.capturo.app.data.preferences.SessionManager
import com.capturo.app.data.preferences.UserRole
import javax.inject.Inject
import timber.log.Timber

@AndroidEntryPoint
class BookingListPageFragment : Fragment() {

    private var _binding: FragmentBookingListPageBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BookingViewModel by viewModels()
    private lateinit var adapter: BookingListAdapter

    @Inject
    lateinit var sessionManager: SessionManager

    private var status: String = "confirmed"

    companion object {
        private const val ARG_STATUS = "arg_status"

        fun newInstance(status: String): BookingListPageFragment {
            val fragment = BookingListPageFragment()
            val args = Bundle().apply {
                putString(ARG_STATUS, status)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        status = arguments?.getString(ARG_STATUS) ?: "confirmed"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookingListPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup Adapter
        adapter = BookingListAdapter(
            sessionManager = sessionManager,
            onBookingClicked = { booking ->
                // Navigate to details screen
                val bundle = Bundle().apply {
                    putString("booking_id", booking.id)
                }
                findNavController().navigate(R.id.bookingDetailFragment, bundle)
            },
            onChatClicked = { booking ->
                val role = sessionManager.getUserRole()
                val isCreator = role == UserRole.CREATOR
                val receiverName = if (isCreator) booking.attendee?.fullName ?: "Attendee" else booking.creator?.fullName ?: "Photographer"
                val receiverPicUrl = if (isCreator) booking.attendee?.profilePicUrl else booking.creator?.profilePicUrl
                val bundle = Bundle().apply {
                    putString("booking_id", booking.id)
                    putString("receiver_id", if (isCreator) booking.attendeeId else booking.creatorId)
                    putString("receiver_name", receiverName)
                    putString("receiver_pic", receiverPicUrl)
                }
                findNavController().navigate(R.id.chatFragment, bundle)
            },
            onCallClicked = { booking ->
                val role = sessionManager.getUserRole()
                val isCreator = role == UserRole.CREATOR
                val phone = if (isCreator) booking.attendee?.phone else booking.creator?.phone
                phone?.let { p ->
                    val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:$p"))
                    startActivity(intent)
                } ?: run {
                    Snackbar.make(binding.root, "Phone number not available", Snackbar.LENGTH_SHORT).show()
                }
            }
        )

        binding.recyclerBookings.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerBookings.adapter = adapter

        // Setup Pull to Refresh
        binding.swipeRefreshBookings.setColorSchemeColors(
            ContextCompat.getColor(requireContext(), R.color.colorAccent)
        )
        binding.swipeRefreshBookings.setProgressBackgroundColorSchemeColor(
            ContextCompat.getColor(requireContext(), R.color.colorSurfaceVariant)
        )
        binding.swipeRefreshBookings.setOnRefreshListener {
            viewModel.loadBookings(status)
        }

        // Fetch bookings
        viewModel.loadBookings(status)

        // Observe ViewModel streams
        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.bookingsState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.swipeRefreshBookings.isRefreshing = true
                }
                is Resource.Success -> {
                    binding.swipeRefreshBookings.isRefreshing = false
                    val bookings = resource.data.items
                    adapter.submitList(bookings)

                    if (bookings.isEmpty()) {
                        binding.layoutEmpty.visibility = View.VISIBLE
                    } else {
                        binding.layoutEmpty.visibility = View.GONE
                    }
                }
                is Resource.Error -> {
                    binding.swipeRefreshBookings.isRefreshing = false
                    Snackbar.make(binding.root, resource.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
