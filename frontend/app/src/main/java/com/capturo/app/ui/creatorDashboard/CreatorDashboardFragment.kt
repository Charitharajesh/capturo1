package com.capturo.app.ui.creatorDashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.capturo.app.R
import com.capturo.app.data.model.response.BookingResponse
import com.capturo.app.data.preferences.SessionManager
import com.capturo.app.data.preferences.UserRole
import com.capturo.app.databinding.FragmentCreatorDashboardBinding
import com.capturo.app.databinding.ItemScheduleBookingBinding
import com.capturo.app.utils.DateTimeUtils
import com.capturo.app.utils.FormattingUtils
import com.capturo.app.utils.Resource
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class CreatorDashboardFragment : Fragment() {

    @Inject
    lateinit var sessionManager: SessionManager

    private var _binding: FragmentCreatorDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CreatorDashboardViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreatorDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // GATE: Creator Role Check
        if (sessionManager.getUserRole() != UserRole.CREATOR) {
            findNavController().navigate(R.id.homeFragment)
            return
        }

        setupModeSwitch()
        setupButtons()
        observeViewModel()
        
        // Load fresh data
        viewModel.loadDashboardData()
    }

    private fun setupModeSwitch() {
        // In creator mode the switch is ON; flipping it OFF returns to Client mode.
        binding.switchMode.isChecked = true
        binding.switchMode.setOnCheckedChangeListener { button, isChecked ->
            if (!button.isPressed) return@setOnCheckedChangeListener
            if (!isChecked) {
                (activity as? com.capturo.app.ui.main.MainActivity)?.switchRole(UserRole.ATTENDEE)
            }
        }
    }

    private fun setupButtons() {
        binding.btnViewRequests.setOnClickListener {
            findNavController().navigate(R.id.bookingRequestsFragment)
        }

        binding.btnUploadContent.setOnClickListener {
            findNavController().navigate(R.id.creatorGalleryFragment)
        }

        binding.btnNotifications.setOnClickListener {
            findNavController().navigate(R.id.notificationsFragment)
        }
    }

    private fun observeViewModel() {
        // Observe Dashboard Stats
        viewModel.dashboardStats.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    // Premium shimmer effect or silent placeholder display
                }
                is Resource.Success -> {
                    val stats = resource.data
                    
                    // Trigger entrance animations and bind data
                    animateEarningsCard()
                    FormattingUtils.animateMoney(binding.tvEarningsAmount, stats.earningsThisMonth)
                    FormattingUtils.animateNumber(binding.tvBookingsCount, stats.totalBookings)
                    FormattingUtils.animateNumber(binding.tvRatingVal, stats.totalReviews)
                    FormattingUtils.animateNumber(binding.tvReachVal, stats.followersCount)
                    
                    // Bind revenue growth percentage
                    val growthPercent = stats.revenueGrowthPercentage
                    val growthText = if (growthPercent >= 0) {
                        binding.tvEarningsTrend.setTextColor(resources.getColor(R.color.colorSuccess, null))
                        String.format(Locale.ENGLISH, "+%.0f%% ↑ vs last month", growthPercent)
                    } else {
                        binding.tvEarningsTrend.setTextColor(resources.getColor(R.color.colorError, null))
                        String.format(Locale.ENGLISH, "%.0f%% ↓ vs last month", growthPercent)
                    }
                    binding.tvEarningsTrend.text = growthText

                    // Bind pending count overlay and text badge
                    if (stats.pendingBookingsCount > 0) {
                        binding.tvPendingBadge.text = "${stats.pendingBookingsCount} pending"
                        binding.tvPendingBadge.visibility = View.VISIBLE
                        binding.tvViewRequestsText.text = "View Requests (${stats.pendingBookingsCount})"
                        binding.tvNotificationBadge.text = stats.pendingBookingsCount.toString()
                        binding.tvNotificationBadge.visibility = View.VISIBLE
                    } else {
                        binding.tvPendingBadge.visibility = View.GONE
                        binding.tvViewRequestsText.text = "View Requests"
                        binding.tvNotificationBadge.visibility = View.GONE
                    }
                }
                is Resource.Error -> {
                    showSnackbar(resource.message ?: "Failed to load dashboard statistics")
                }
            }
        }

        // Observe Today's Schedule
        viewModel.todaySchedule.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.tvEmptySchedule.visibility = View.GONE
                }
                is Resource.Success -> {
                    val bookings = resource.data
                    bindTodaySchedule(bookings)
                }
                is Resource.Error -> {
                    binding.tvEmptySchedule.text = "Error loading schedule"
                    binding.tvEmptySchedule.visibility = View.VISIBLE
                    showSnackbar(resource.message ?: "Failed to load today's schedule")
                }
            }
        }
    }

    private fun bindTodaySchedule(bookings: List<BookingResponse>) {
        binding.layoutScheduleItems.removeAllViews()

        val todayIso = DateTimeUtils.toApiDateFormat(LocalDate.now())
        val isToday = bookings.any { it.eventDate == todayIso }

        if (isToday) {
            binding.tvScheduleTitle.text = "Today's Schedule"
            binding.tvTodayDate.text = LocalDate.now().format(
                DateTimeFormatter.ofPattern("EEE, MMM dd", Locale.ENGLISH)
            )
        } else if (bookings.isNotEmpty()) {
            binding.tvScheduleTitle.text = "Nearest Booking"
            try {
                val parsedDate = LocalDate.parse(bookings[0].eventDate)
                binding.tvTodayDate.text = parsedDate.format(
                    DateTimeFormatter.ofPattern("EEE, MMM dd", Locale.ENGLISH)
                )
            } catch (e: Exception) {
                binding.tvTodayDate.text = bookings[0].eventDate
            }
        } else {
            binding.tvScheduleTitle.text = "Today's Schedule"
            binding.tvTodayDate.text = LocalDate.now().format(
                DateTimeFormatter.ofPattern("EEE, MMM dd", Locale.ENGLISH)
            )
        }

        if (bookings.isEmpty()) {
            binding.tvEmptySchedule.visibility = View.VISIBLE
        } else {
            binding.tvEmptySchedule.visibility = View.GONE
            
            bookings.forEach { booking ->
                val itemBinding = ItemScheduleBookingBinding.inflate(
                    LayoutInflater.from(context),
                    binding.layoutScheduleItems,
                    false
                )
                
                // Parse date-only ISO if start time / event date is absolute
                val bookingDateTimeStr = "${booking.eventDate}T${booking.startTime}Z"
                val timeRangeStr = try {
                    val eventZdt = DateTimeUtils.parseFromBackend(bookingDateTimeStr)
                    DateTimeUtils.formatTimeRange(eventZdt, booking.durationHours)
                } catch (e: Exception) {
                    "${booking.startTime} (${booking.durationHours} hrs)"
                }

                itemBinding.tvBookingTime.text = timeRangeStr
                itemBinding.tvBookingType.text = booking.eventType.replaceFirstChar { it.uppercase() }
                itemBinding.tvBookingClient.text = "Client ID: ${booking.attendeeId?.take(8) ?: "N/A"}"
                
                itemBinding.btnDetails.setOnClickListener {
                    val bundle = Bundle().apply {
                        putString("booking_id", booking.id)
                    }
                    findNavController().navigate(R.id.bookingDetailFragment, bundle)
                }

                binding.layoutScheduleItems.addView(itemBinding.root)
            }
        }
    }

    // Premium entrance micro-animation for Earnings Card
    private fun animateEarningsCard() {
        val animSet = AnimationSet(true).apply {
            addAnimation(AlphaAnimation(0.0f, 1.0f).apply { duration = 200 })
            addAnimation(ScaleAnimation(
                0.95f, 1.0f, 0.95f, 1.0f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f
            ).apply { duration = 200 })
        }
        binding.cardEarnings.startAnimation(animSet)
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).apply {
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
