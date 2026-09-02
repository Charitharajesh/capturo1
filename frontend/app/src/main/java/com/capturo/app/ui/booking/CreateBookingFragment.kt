package com.capturo.app.ui.booking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.capturo.app.R
import com.capturo.app.data.model.request.CreateBookingRequest
import com.capturo.app.databinding.FragmentCreateBookingBinding
import com.capturo.app.utils.DateTimeUtils
import com.capturo.app.utils.Resource
import com.capturo.app.utils.ValidationUtils
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import coil.load
import android.graphics.Color
import androidx.core.content.ContextCompat
import com.capturo.app.utils.showSnackbar

@AndroidEntryPoint
class CreateBookingFragment : Fragment() {

    private var _binding: FragmentCreateBookingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BookingViewModel by viewModels()

    private var creatorId: String = ""
    private var hourlyRate: Double = 0.0
    private var creatorName: String = ""
    private var creatorAvatarUrl: String? = null

    private var selectedDateApi: String? = null
    private var selectedTimeApi: String? = null
    private var durationHours: Double = 2.0
    private var selectedDate: LocalDate? = null

    private var currentStep = 1

    private val eventImageUrls = mapOf(
        "wedding" to "https://images.unsplash.com/photo-1519741497674-611481863552?w=600",
        "birthday" to "https://images.unsplash.com/photo-1530103862676-de8c9debad1d?w=600",
        "corporate" to "https://images.unsplash.com/photo-1511578314322-379afb476865?w=600",
        "graduation" to "https://images.unsplash.com/photo-1523050854058-8df90110c9f1?w=600",
        "party" to "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=600",
        "other" to "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=600"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateBookingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieve properties from Safe Args or Bundle fallback
        creatorId = arguments?.getString("creator_id") ?: ""
        hourlyRate = arguments?.getFloat("hourly_rate", 0f)?.toDouble() ?: 0.0
        creatorName = arguments?.getString("creator_name") ?: "Creator"
        creatorAvatarUrl = arguments?.getString("creator_avatar_url")

        if (creatorId.isEmpty()) {
            showSnackbar("Error: Invalid Creator Selection", Snackbar.LENGTH_LONG)
            findNavController().navigateUp()
            return
        }

        // Setup Toolbar
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        // Pre-populate creator information card
        binding.textCreatorName.text = creatorName
        binding.textHourlyRate.text = "₹${hourlyRate.toInt()}/hr"

        // Setup Dropdown for Event Types
        val eventTypes = arrayOf("Wedding", "Birthday", "Corporate", "Graduation", "Party", "Other")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, eventTypes)
        binding.spinnerEventType.setAdapter(adapter)

        // Set default category image
        binding.imageEventCategory.load(eventImageUrls["other"])

        // Listen for Event Type selection to update the visual category illustration dynamically
        binding.spinnerEventType.setOnItemClickListener { _, _, _, _ ->
            val selectedType = binding.spinnerEventType.text.toString().lowercase(Locale.US)
            val imgUrl = eventImageUrls[selectedType] ?: eventImageUrls["other"]
            binding.imageEventCategory.load(imgUrl) {
                crossfade(true)
                placeholder(R.drawable.bg_cover_scrim)
            }
        }

        // Setup Date Picker trigger
        binding.editEventDate.setOnClickListener {
            showDatePicker()
        }

        // Setup Time Picker trigger
        binding.editStartTime.setOnClickListener {
            showTimePicker()
        }

        // Setup Counter buttons logic
        updateCostCalculation()
        binding.buttonMinus.setOnClickListener {
            if (durationHours > 2.0) {
                durationHours -= 0.5
                updateCostCalculation()
            }
        }
        binding.buttonPlus.setOnClickListener {
            if (durationHours < 12.0) {
                durationHours += 0.5
                updateCostCalculation()
            }
        }

        // Location Auto-Detect end icon logic
        binding.layoutLocation.setEndIconOnClickListener {
            binding.layoutLocationDetecting.visibility = View.VISIBLE
            binding.layoutLocation.isEnabled = false
            binding.layoutLocationDetecting.postDelayed({
                binding.layoutLocationDetecting.visibility = View.GONE
                binding.layoutLocation.isEnabled = true
                val mockLocations = listOf(
                    "MG Road, Bengaluru, Karnataka 560001",
                    "Connaught Place, New Delhi 110001",
                    "Bandra West, Mumbai, Maharashtra 400050",
                    "Indiranagar, Bengaluru, Karnataka 560038",
                    "T. Nagar, Chennai, Tamil Nadu 600017",
                    "Salt Lake Sector V, Kolkata, West Bengal 700091"
                )
                val randomLoc = mockLocations.random()
                binding.editLocation.setText(randomLoc)
                showSnackbar("GPS Location auto-detected successfully!", Snackbar.LENGTH_SHORT)
            }, 1200)
        }

        // Location Dynamic suggestions text watcher
        binding.editLocation.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim() ?: ""
                if (query.length >= 3) {
                    binding.layoutSuggestionsContainer.visibility = View.VISIBLE
                    binding.layoutSuggestionsContainer.removeAllViews()
                    val spots = listOf("Studio", "Grand Palace", "Hyatt Regency", "Royal Garden", "Central Park", "Beach Resort", "Town Hall")
                    spots.forEach { spot ->
                        val suggestionText = "$spot, $query"
                        val textView = android.widget.TextView(requireContext()).apply {
                            text = suggestionText
                            setPadding(32, 24, 32, 24)
                            setTextColor(Color.WHITE)
                            setBackgroundResource(android.R.drawable.list_selector_background)
                            isClickable = true
                            setOnClickListener {
                                binding.editLocation.setText(suggestionText)
                                binding.layoutSuggestionsContainer.visibility = View.GONE
                            }
                        }
                        binding.layoutSuggestionsContainer.addView(textView)
                    }
                } else {
                    binding.layoutSuggestionsContainer.visibility = View.GONE
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // Step wise Confirm/Next navigation action triggers
        binding.buttonConfirm.setOnClickListener {
            handleNextStepClick()
        }

        binding.buttonPrevStep.setOnClickListener {
            handlePrevStepClick()
        }

        // Initialize progress view for Step 1
        moveToStep(1)

        // Observe ViewModel streams
        observeViewModel()
    }

    private fun handleNextStepClick() {
        when (currentStep) {
            1 -> {
                val eventType = binding.spinnerEventType.text.toString()
                if (!ValidationUtils.isNotEmpty(eventType)) {
                    binding.layoutEventType.error = "Event type is required"
                    return
                }
                binding.layoutEventType.error = null
                moveToStep(2)
            }
            2 -> {
                if (selectedDateApi == null) {
                    binding.layoutEventDate.error = "Event date is required"
                    return
                }
                binding.layoutEventDate.error = null

                if (selectedTimeApi == null) {
                    binding.layoutStartTime.error = "Start time is required"
                    return
                }
                binding.layoutStartTime.error = null

                moveToStep(3)
            }
            3 -> {
                performBookingSubmission()
            }
        }
    }

    private fun handlePrevStepClick() {
        if (currentStep > 1) {
            moveToStep(currentStep - 1)
        }
    }

    private fun moveToStep(step: Int) {
        currentStep = step
        
        // Toggle layouts
        binding.layoutStep1.visibility = if (step == 1) View.VISIBLE else View.GONE
        binding.layoutStep2.visibility = if (step == 2) View.VISIBLE else View.GONE
        binding.layoutStep3.visibility = if (step == 3) View.VISIBLE else View.GONE

        // Toggle back navigation button
        binding.buttonPrevStep.visibility = if (step == 1) View.GONE else View.VISIBLE

        // Set confirming button text
        binding.buttonConfirm.text = if (step == 3) "CONFIRM BOOKING" else "NEXT STEP"

        // Update step visual indicators color status
        val activeBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_indicator_active)
        val inactiveBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_indicator_inactive)
        val activeTextColor = Color.WHITE
        val inactiveTextColor = ContextCompat.getColor(requireContext(), R.color.colorTextSecondary)
        val activeAccentColor = ContextCompat.getColor(requireContext(), R.color.colorAccent)

        // Reset step 1
        binding.indicatorStep1.background = if (step >= 1) activeBg else inactiveBg
        binding.indicatorStep1.setTextColor(if (step >= 1) activeTextColor else inactiveTextColor)
        binding.textLabelStep1.setTextColor(if (step >= 1) activeAccentColor else inactiveTextColor)

        // Reset step 2
        binding.indicatorStep2.background = if (step >= 2) activeBg else inactiveBg
        binding.indicatorStep2.setTextColor(if (step >= 2) activeTextColor else inactiveTextColor)
        binding.textLabelStep2.setTextColor(if (step >= 2) activeAccentColor else inactiveTextColor)
        binding.connector1.setBackgroundColor(if (step >= 2) activeAccentColor else ContextCompat.getColor(requireContext(), R.color.colorBorder))

        // Reset step 3
        binding.indicatorStep3.background = if (step >= 3) activeBg else inactiveBg
        binding.indicatorStep3.setTextColor(if (step >= 3) activeTextColor else inactiveTextColor)
        binding.textLabelStep3.setTextColor(if (step >= 3) activeAccentColor else inactiveTextColor)
        binding.connector2.setBackgroundColor(if (step >= 3) activeAccentColor else ContextCompat.getColor(requireContext(), R.color.colorBorder))
    }

    private fun showDatePicker() {
        // Bookings must be scheduled at least 3 days in advance
        val minDate = System.currentTimeMillis() + (3L * 24 * 60 * 60 * 1000)
        val constraints = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointForward.from(minDate))
            .build()

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Event Date (min 3 days advance)")
            .setCalendarConstraints(constraints)
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val localDate = Instant.ofEpochMilli(selection)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()

            selectedDate = localDate
            selectedDateApi = DateTimeUtils.toApiDateFormat(localDate)
            binding.editEventDate.setText(localDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH)))
            binding.layoutEventDate.error = null
        }
        datePicker.show(childFragmentManager, "MaterialDatePicker")
    }

    private fun showTimePicker() {
        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(12)
            .setMinute(0)
            .setTitleText("Select Start Time")
            .build()

        timePicker.addOnPositiveButtonClickListener {
            val hour = timePicker.hour
            val minute = timePicker.minute
            selectedTimeApi = DateTimeUtils.toApiTimeFormat(hour, minute)

            val amPm = if (hour >= 12) "PM" else "AM"
            val displayHour = if (hour % 12 == 0) 12 else hour % 12
            binding.editStartTime.setText(String.format(Locale.US, "%02d:%02d %s", displayHour, minute, amPm))
            binding.layoutStartTime.error = null
        }
        timePicker.show(childFragmentManager, "MaterialTimePicker")
    }

    private fun updateCostCalculation() {
        binding.textDurationValue.text = String.format(Locale.US, "%.1f Hours", durationHours)
        val totalCost = hourlyRate * durationHours
        binding.textTotalAmount.text = "₹${totalCost.toInt()}"
        binding.textCalculationDetails.text = String.format(Locale.US, "₹%d/hr × %.1f hrs", hourlyRate.toInt(), durationHours)
        binding.textCalculationDetailsStep1.text = String.format(Locale.US, "₹%d/hr × %.1f hrs", hourlyRate.toInt(), durationHours)
    }

    private fun performBookingSubmission() {
        val eventType = binding.spinnerEventType.text.toString()
        val location = binding.editLocation.text.toString()
        val notes = binding.editSpecialNotes.text.toString()

        var isValid = true

        // Validate Location
        if (!ValidationUtils.isMinLength(location, 5)) {
            binding.layoutLocation.error = "Location must be at least 5 characters"
            isValid = false
        } else {
            binding.layoutLocation.error = null
        }

        if (!isValid) return

        val request = CreateBookingRequest(
            creatorId = creatorId,
            eventType = eventType.lowercase(Locale.US),
            location = location,
            eventDate = selectedDateApi!!,
            startTime = selectedTimeApi!!,
            durationHours = durationHours,
            specialNotes = if (notes.trim().isNotEmpty()) notes else null
        )

        viewModel.createBooking(request)
    }

    private fun observeViewModel() {
        viewModel.createBookingState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.buttonConfirm.isEnabled = false
                    binding.buttonConfirm.text = "Creating Booking..."
                }
                is Resource.Success -> {
                    binding.buttonConfirm.isEnabled = true
                    binding.buttonConfirm.text = "NEXT STEP"

                    val data = resource.data
                    val bundle = Bundle().apply {
                        putString("booking_id", data.bookingId)
                        putString("order_id", data.paymentOrderId)
                        putString("key_id", data.paymentKeyId)
                        putFloat("amount", data.totalAmount.toFloat())
                        putString("creator_name", creatorName)
                    }
                    // Navigate to Payment screen
                    findNavController().navigate(R.id.paymentFragment, bundle)
                }
                is Resource.Error -> {
                    binding.buttonConfirm.isEnabled = true
                    binding.buttonConfirm.text = "CONFIRM BOOKING"
                    showSnackbar(resource.message, Snackbar.LENGTH_LONG)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
