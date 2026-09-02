package com.capturo.app.ui.creatorDashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.capturo.app.R
import com.capturo.app.adapter.BookingRequestAdapter
import com.capturo.app.data.model.request.CancelRequest
import com.capturo.app.data.model.request.PaymentVerifyRequest
import com.capturo.app.data.model.response.BookingResponse
import com.capturo.app.data.repository.BookingRepository
import com.capturo.app.databinding.FragmentCreatorPendingRequestsBinding
import com.capturo.app.utils.Resource
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CreatorPendingRequestsFragment : Fragment() {

    @Inject
    lateinit var bookingRepository: BookingRepository

    private var _binding: FragmentCreatorPendingRequestsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: BookingRequestAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreatorPendingRequestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSwipeToRefresh()
        
        loadPendingBookings()
    }

    private fun setupRecyclerView() {
        adapter = BookingRequestAdapter(
            onAcceptClick = { booking -> showAcceptConfirmation(booking) },
            onDeclineClick = { booking -> showDeclineBottomSheet(booking) }
        )

        binding.recyclerRequests.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@CreatorPendingRequestsFragment.adapter
        }
    }

    private fun setupSwipeToRefresh() {
        binding.swipeRefresh.setColorSchemeColors(resources.getColor(R.color.colorAccent, null))
        binding.swipeRefresh.setProgressBackgroundColorSchemeColor(resources.getColor(R.color.colorSurface, null))
        binding.swipeRefresh.setOnRefreshListener {
            loadPendingBookings()
        }
    }

    private fun loadPendingBookings() {
        lifecycleScope.launch {
            bookingRepository.getMyBookings("pending").collectLatest { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        if (!binding.swipeRefresh.isRefreshing) {
                            binding.progressBar.visibility = View.VISIBLE
                        }
                        binding.layoutEmpty.visibility = View.GONE
                    }
                    is Resource.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.swipeRefresh.isRefreshing = false
                        val list = resource.data.items
                        adapter.submitList(list)

                        if (list.isEmpty()) {
                            binding.layoutEmpty.visibility = View.VISIBLE
                            binding.recyclerRequests.visibility = View.GONE
                        } else {
                            binding.layoutEmpty.visibility = View.GONE
                            binding.recyclerRequests.visibility = View.VISIBLE
                        }
                    }
                    is Resource.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.swipeRefresh.isRefreshing = false
                        showSnackbar(resource.message)
                    }
                }
            }
        }
    }

    private fun showAcceptConfirmation(booking: BookingResponse) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirm Booking Request")
            .setMessage("Are you sure you want to accept this reservation request?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Accept") { _, _ ->
                acceptBooking(booking)
            }
            .show()
    }

    private fun acceptBooking(booking: BookingResponse) {
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            // Confirm booking request with mock signature values for instant creator-side captured confirmation
            val verifyRequest = PaymentVerifyRequest("pay_mock_creator", "sig_mock_creator")
            bookingRepository.confirmBooking(booking.id, verifyRequest).collectLatest { resource ->
                when (resource) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        binding.progressBar.visibility = View.GONE
                        showSnackbar("Booking successfully accepted!")
                        loadPendingBookings() // refresh lists
                    }
                    is Resource.Error -> {
                        binding.progressBar.visibility = View.GONE
                        showSnackbar(resource.message)
                    }
                }
            }
        }
    }

    private fun showDeclineBottomSheet(booking: BookingResponse) {
        val bottomSheet = BottomSheetDialog(requireContext())
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_decline_reason, null)
        bottomSheet.setContentView(dialogView)

        val rgReasons = dialogView.findViewById<RadioGroup>(R.id.rgReasons)
        val etOtherReason = dialogView.findViewById<EditText>(R.id.etOtherReason)
        val btnDeclineConfirm = dialogView.findViewById<View>(R.id.btnDeclineConfirm)

        rgReasons.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbOther) {
                etOtherReason.visibility = View.VISIBLE
            } else {
                etOtherReason.visibility = View.GONE
            }
        }

        btnDeclineConfirm.setOnClickListener {
            val selectedId = rgReasons.checkedRadioButtonId
            if (selectedId == -1) {
                showSnackbar("Please choose a reason to decline.")
                return@setOnClickListener
            }

            val reasonText = when (selectedId) {
                R.id.rbConflict -> "Schedule conflict"
                R.id.rbRadius -> "Outside service area"
                R.id.rbUnavailable -> "Not available"
                else -> etOtherReason.text.toString().ifEmpty { "creator_declined" }
            }

            bottomSheet.dismiss()
            declineBooking(booking, reasonText)
        }

        bottomSheet.show()
    }

    private fun declineBooking(booking: BookingResponse, reason: String) {
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            val cancelRequest = CancelRequest(reason)
            bookingRepository.cancelBooking(booking.id, cancelRequest).collectLatest { resource ->
                when (resource) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        binding.progressBar.visibility = View.GONE
                        showSnackbar("Booking request declined.")
                        loadPendingBookings() // refresh lists
                    }
                    is Resource.Error -> {
                        binding.progressBar.visibility = View.GONE
                        showSnackbar(resource.message)
                    }
                }
            }
        }
    }

    private fun showSnackbar(message: String?) {
        Snackbar.make(binding.root, message ?: "Error occurred", Snackbar.LENGTH_LONG).apply {
            setBackgroundTint(resources.getColor(R.color.colorSurfaceElevated, null))
            setTextColor(resources.getColor(R.color.colorTextPrimary, null))
            show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
