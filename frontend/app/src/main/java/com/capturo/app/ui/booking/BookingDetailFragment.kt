package com.capturo.app.ui.booking

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import coil.load
import coil.transform.CircleCropTransformation
import androidx.core.content.ContextCompat
import com.capturo.app.R
import com.capturo.app.ui.creatorDashboard.UploadGalleryDialogFragment
import com.capturo.app.data.model.response.BookingResponse
import com.capturo.app.databinding.FragmentBookingDetailBinding
import com.capturo.app.utils.Resource
import com.capturo.app.utils.Constants
import com.capturo.app.utils.toFullUrl
import com.capturo.app.utils.showSnackbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.capturo.app.data.preferences.SessionManager
import com.capturo.app.data.preferences.UserRole
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class BookingDetailFragment : Fragment() {

    @Inject
    lateinit var sessionManager: SessionManager

    private var _binding: FragmentBookingDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BookingViewModel by viewModels()

    private var bookingId: String = ""
    private var creatorPhone: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookingDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieve Booking ID
        bookingId = arguments?.getString("booking_id") ?: ""
        if (bookingId.isEmpty()) {
            showSnackbar("Error: Invalid Booking Selection", Snackbar.LENGTH_LONG)
            findNavController().navigateUp()
            return
        }

        // Setup Toolbar
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        // Fetch booking info
        viewModel.loadBooking(bookingId)

        // Observe ViewModel streams
        observeViewModel()
    }

    private fun observeViewModel() {
        // Observe Current Booking Detail
        viewModel.currentBooking.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.textCreatorName.text = "Loading..."
                }
                is Resource.Success -> {
                    bindBookingDetails(resource.data)
                }
                is Resource.Error -> {
                    showSnackbar(resource.message, Snackbar.LENGTH_LONG)
                }
            }
        }

        // Observe Cancel state
        viewModel.cancelState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.buttonCancelBooking.isEnabled = false
                    binding.buttonCancelBooking.text = "Cancelling..."
                }
                is Resource.Success -> {
                    binding.buttonCancelBooking.isEnabled = true
                    binding.buttonCancelBooking.text = "Cancel Booking"
                    showSnackbar("Booking cancelled successfully", Snackbar.LENGTH_LONG)
                    viewModel.loadBooking(bookingId) // Reload status
                }
                is Resource.Error -> {
                    binding.buttonCancelBooking.isEnabled = true
                    binding.buttonCancelBooking.text = "Cancel Booking"
                    showSnackbar(resource.message, Snackbar.LENGTH_LONG)
                }
            }
        }

        // Observe Complete state
        viewModel.completeBookingState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.buttonCompleteBooking.isEnabled = false
                    binding.buttonCompleteBooking.text = "Completing..."
                }
                is Resource.Success -> {
                    binding.buttonCompleteBooking.isEnabled = true
                    binding.buttonCompleteBooking.text = "Mark as Completed"
                    showSnackbar("Booking marked as completed successfully", Snackbar.LENGTH_LONG)
                    viewModel.loadBooking(bookingId) // Reload status
                }
                is Resource.Error -> {
                    binding.buttonCompleteBooking.isEnabled = true
                    binding.buttonCompleteBooking.text = "Mark as Completed"
                    showSnackbar(resource.message, Snackbar.LENGTH_LONG)
                }
            }
        }

        // Observe Submit Review state
        viewModel.submitReviewState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    showSnackbar("Submitting review...", Snackbar.LENGTH_SHORT)
                }
                is Resource.Success -> {
                    showSnackbar("Review submitted successfully!", Snackbar.LENGTH_LONG)
                    binding.buttonLeaveReview.visibility = View.GONE
                }
                is Resource.Error -> {
                    showSnackbar(resource.message, Snackbar.LENGTH_LONG)
                }
            }
        }

        // Observe Invoice Download state
        viewModel.invoiceState.observe(viewLifecycleOwner) { resource ->
            if (resource == null) return@observe
            when (resource) {
                is Resource.Loading -> {
                    showSnackbar("Downloading PDF Invoice...", Snackbar.LENGTH_SHORT)
                }
                is Resource.Success -> {
                    viewModel.resetInvoiceState()
                    val filename = "Invoice_${bookingId.substring(0, kotlin.math.min(8, bookingId.length)).toUpperCase()}.pdf"
                    val uri = com.capturo.app.utils.FileUtils.savePdfResponse(
                        requireContext(),
                        resource.data,
                        filename
                    )
                    if (uri != null) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/pdf")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            startActivity(Intent.createChooser(intent, "Open Invoice"))
                        } catch (e: Exception) {
                            showSnackbar("No PDF viewer installed", Snackbar.LENGTH_SHORT)
                        }
                    } else {
                        showSnackbar("Failed to save PDF Invoice", Snackbar.LENGTH_SHORT)
                    }
                }
                is Resource.Error -> {
                    viewModel.resetInvoiceState()
                    showSnackbar(resource.message, Snackbar.LENGTH_SHORT)
                }
            }
        }
    }

    private fun bindBookingDetails(booking: BookingResponse) {
        val context = requireContext()
        val role = sessionManager.getUserRole()
        val isCreator = role == UserRole.CREATOR

        val receiverName: String
        val receiverPicUrl: String?

        if (isCreator) {
            val attendee = booking.attendee
            receiverName = attendee?.fullName ?: "Attendee"
            receiverPicUrl = attendee?.profilePicUrl
            creatorPhone = attendee?.phone

            binding.textCreatorName.text = receiverName
            binding.textSpecialty.text = "Client"
            binding.imageAvatar.load(receiverPicUrl.toFullUrl()) {
                crossfade(true)
                transformations(CircleCropTransformation())
                placeholder(R.drawable.ic_profile)
                error(R.drawable.ic_profile)
            }
        } else {
            val creator = booking.creator
            receiverName = creator?.fullName ?: "Photographer"
            receiverPicUrl = creator?.profilePicUrl
            creatorPhone = creator?.phone

            binding.textCreatorName.text = receiverName
            binding.textSpecialty.text = creator?.email?.split("@")?.firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Photographer"
            binding.imageAvatar.load(receiverPicUrl.toFullUrl()) {
                crossfade(true)
                transformations(CircleCropTransformation())
                placeholder(R.drawable.ic_profile)
                error(R.drawable.ic_profile)
            }
        }

        // Event Card Details
        binding.textEventType.text = booking.eventType.replaceFirstChar { it.uppercase() } + " Photography"
        binding.textDateTimeDetails.text = "${booking.eventDate} | ${booking.startTime.substring(0, 5)} (${booking.durationHours} hours)"
        binding.textLocation.text = booking.location
        binding.textTotalAmount.text = "₹${booking.totalAmount.toInt()}"

        // Set action triggers
        binding.buttonChat.setOnClickListener {
            val bundle = Bundle().apply {
                putString("booking_id", booking.id)
                putString("receiver_id", if (isCreator) booking.attendeeId else booking.creatorId)
                putString("receiver_name", receiverName)
                putString("receiver_pic", receiverPicUrl)
            }
            findNavController().navigate(R.id.chatFragment, bundle)
        }

        binding.buttonCall.setOnClickListener {
            creatorPhone?.let { phone ->
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                startActivity(intent)
            } ?: run {
                showSnackbar("Phone number not available", Snackbar.LENGTH_SHORT)
            }
        }

        // Status Reactive UI Logic
        val status = booking.status.lowercase(Locale.US)
        val isPending = status == "pending"
        val isConfirmed = status == "confirmed" || status == "paid"
        val isCompleted = status == "completed"
        val isCancelled = status == "cancelled"

        // 1. Banner Color adjustments
        val bannerBgRes = when {
            isConfirmed -> R.color.colorSuccessBg
            isCancelled -> R.color.colorErrorBg
            isCompleted -> R.color.colorBottomNavIndicator
            else -> R.color.colorWarningBg
        }
        val bannerTextRes = when {
            isConfirmed -> R.color.colorOnlineStatus
            isCancelled -> R.color.colorError
            isCompleted -> R.color.colorAccentSoft
            else -> R.color.colorWarning
        }
        binding.layoutStatusBanner.setBackgroundColor(ContextCompat.getColor(context, bannerBgRes))
        binding.textStatusBannerMessage.setTextColor(ContextCompat.getColor(context, bannerTextRes))
        binding.imageStatusIcon.imageTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(context, bannerTextRes))

        binding.textStatusBannerMessage.text = when {
            isConfirmed -> "Booking is Confirmed & Paid"
            isCancelled -> "Booking was Cancelled"
            isCompleted -> "Booking is Completed successfully"
            else -> "Booking is Pending Confirmation"
        }

        // 2. Action buttons visibility updates
        binding.cardInvoice.visibility = if (isCompleted || isConfirmed) View.VISIBLE else View.GONE
        binding.buttonCancelBooking.visibility = if (isPending || isConfirmed) View.VISIBLE else View.GONE
        binding.buttonPrimaryAction.visibility = if (isCompleted) View.VISIBLE else View.GONE
        binding.buttonLeaveReview.visibility = if (isConfirmed && !isCreator) View.VISIBLE else View.GONE
        binding.buttonCompleteBooking.visibility = if ((isConfirmed || isCompleted) && isCreator) View.VISIBLE else View.GONE

        if (isCreator) {
            if (isConfirmed) {
                binding.buttonCompleteBooking.text = "Mark as Completed"
                binding.buttonCompleteBooking.icon = ContextCompat.getDrawable(context, R.drawable.ic_check_circle)
                binding.buttonCompleteBooking.setBackgroundColor(ContextCompat.getColor(context, R.color.colorSuccess))
                binding.buttonCompleteBooking.setOnClickListener {
                    MaterialAlertDialogBuilder(context)
                        .setTitle("Mark Booking as Completed?")
                        .setMessage("Are you sure you want to mark this booking as completed? This will finalize the service and process any pending payouts.")
                        .setNegativeButton("No", null)
                        .setPositiveButton("Yes, Completed") { _, _ ->
                            viewModel.completeBooking(booking.id)
                        }
                        .show()
                }
            } else if (isCompleted) {
                binding.buttonCompleteBooking.text = "View Delivery Gallery"
                binding.buttonCompleteBooking.icon = null
                binding.buttonCompleteBooking.setBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimary))
                binding.buttonCompleteBooking.setOnClickListener {
                    val bundle = Bundle().apply {
                        putString("booking_id", booking.id)
                    }
                    findNavController().navigate(R.id.action_bookingDetailFragment_to_deliveryFragment, bundle)
                }
            }
        }

        if (isCompleted) {
            if (isCreator) {
                binding.buttonPrimaryAction.text = "Upload Delivery Media"
                binding.buttonPrimaryAction.setOnClickListener {
                    val dialog = UploadGalleryDialogFragment.newInstance(booking.id, true)
                    dialog.show(childFragmentManager, "UploadGalleryDialog")
                }
            } else {
                binding.buttonPrimaryAction.text = "View Delivery Gallery"
                binding.buttonPrimaryAction.setOnClickListener {
                    val bundle = Bundle().apply {
                        putString("booking_id", booking.id)
                    }
                    findNavController().navigate(R.id.action_bookingDetailFragment_to_deliveryFragment, bundle)
                }
            }
        }

        // 3. Cancel Booking logic with Confirmation dialogue
        binding.buttonCancelBooking.setOnClickListener {
            MaterialAlertDialogBuilder(context)
                .setTitle("Cancel Booking?")
                .setMessage("Are you sure you want to cancel this booking? Refund policy details will be processed automatically.")
                .setNegativeButton("Keep Booking", null)
                .setPositiveButton("Yes, Cancel") { _, _ ->
                    viewModel.cancelBooking(booking.id, "user_requested")
                }
                .show()
        }

        // 4. Leave Review logic
        binding.buttonLeaveReview.setOnClickListener {
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_submit_review, null)
            val ratingBar = dialogView.findViewById<android.widget.RatingBar>(R.id.ratingBar)
            val editComment = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editComment)
            val btnSubmit = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.buttonSubmitReview)

            val dialog = MaterialAlertDialogBuilder(context)
                .setView(dialogView)
                .setBackground(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                .show()

            btnSubmit.setOnClickListener {
                val rating = ratingBar.rating.toInt()
                val comment = editComment.text.toString().trim()
                viewModel.submitReview(com.capturo.app.data.model.request.ReviewRequest(booking.id, rating, comment))
                dialog.dismiss()
            }
        }

        // 5. Invoice trigger (always generates and downloads on-the-fly)
        binding.buttonDownloadInvoice.setOnClickListener {
            viewModel.downloadInvoice(booking.id)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
