package com.capturo.app.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.capturo.app.R
import com.capturo.app.data.preferences.SessionManager
import com.capturo.app.data.preferences.UserRole
import com.capturo.app.data.model.response.BookingResponse
import com.capturo.app.data.repository.CreatorRepository
import com.capturo.app.data.repository.BookingRepository
import com.capturo.app.databinding.FragmentPayoutsBinding
import com.capturo.app.utils.Resource
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import timber.log.Timber

@AndroidEntryPoint
class PayoutsFragment : Fragment() {

    private var _binding: FragmentPayoutsBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var creatorRepository: CreatorRepository

    @Inject
    lateinit var bookingRepository: BookingRepository

    @Inject
    lateinit var sessionManager: SessionManager

    private var allBookings: List<BookingResponse> = emptyList()
    private var selectedCategory: String = "All"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPayoutsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isCreator = sessionManager.getUserRole() == UserRole.CREATOR
        setupToolbar(isCreator)

        if (isCreator) {
            setupCreatorUI()
        } else {
            setupAttendeeUI()
        }

        loadPaymentHistory()
    }

    private fun setupToolbar(isCreator: Boolean) {
        binding.toolbar.title = "Payment History"
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupCreatorUI() {
        binding.tvBalanceLabel.text = "Total Earned"
        binding.layoutStatsRow.visibility = View.VISIBLE
        binding.layoutAttendeeActions.visibility = View.VISIBLE 
        binding.layoutFilterScroll.visibility = View.VISIBLE
    }

    private fun setupAttendeeUI() {
        binding.tvBalanceLabel.text = "Total Spent"
        binding.layoutStatsRow.visibility = View.GONE
        binding.layoutAttendeeActions.visibility = View.VISIBLE
        binding.layoutFilterScroll.visibility = View.VISIBLE
    }

    private fun loadPaymentHistory() {
        lifecycleScope.launch {
            try {
                bookingRepository.getMyBookings(null, 1, 100).collectLatest { resource ->
                    if (_binding == null) return@collectLatest
                    when (resource) {
                        is Resource.Success -> {
                            try {
                                allBookings = resource.data.items
                                calculateBalanceAndSpend()
                                setupActionListeners()
                                setupFilterChips()
                                filterAndPopulateTransactions()
                            } catch (e: Exception) {
                                Timber.e(e, "Error processing booking data")
                                Toast.makeText(context, "Error loading payments: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                        is Resource.Error -> {
                            Timber.e("Error loading bookings: ${resource.message}")
                            Toast.makeText(context, resource.message, Toast.LENGTH_LONG).show()
                        }
                        is Resource.Loading -> { /* loading */ }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Fatal error in loadPaymentHistory")
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun calculateBalanceAndSpend() {
        val isCreator = sessionManager.getUserRole() == UserRole.CREATOR
        if (isCreator) {
            val completedBookings = allBookings.filter {
                val statusStr = it.status as? String
                statusStr == "confirmed" || statusStr == "paid" || statusStr == "completed"
            }
            val totalEarnings = completedBookings.sumOf { (it.totalAmount as? Double) ?: 0.0 }
            
            binding.tvAvailableBalance.text = "₹${totalEarnings.toInt()}"
            binding.tvLifetimeEarnings.text = "₹${totalEarnings.toInt()}"
            binding.tvPendingClearance.text = "₹${(allBookings.filter { (it.status as? String) == "pending" }.sumOf { (it.totalAmount as? Double) ?: 0.0 }).toInt()}"
        } else {
            val paidBookings = allBookings.filter {
                val statusStr = it.status as? String
                statusStr == "confirmed" || statusStr == "paid" || statusStr == "completed"
            }
            val totalSpent = paidBookings.sumOf { (it.totalAmount as? Double) ?: 0.0 }
            binding.tvAvailableBalance.text = "₹${totalSpent.toInt()}"
        }
    }

    private fun setupActionListeners() {
        binding.btnAiSummarize.setOnClickListener {
            loadAiSummary()
        }

        binding.btnDownloadStatement.setOnClickListener {
            downloadStatement()
        }
    }

    private fun setupFilterChips() {
        binding.chipAll.setOnClickListener { selectFilter("All") }
        binding.chipReceived.setOnClickListener { selectFilter("Received") }
        binding.chipPaid.setOnClickListener { selectFilter("Paid") }
        binding.chipPending.setOnClickListener { selectFilter("Pending") }
    }

    private fun selectFilter(category: String) {
        selectedCategory = category
        binding.chipAll.isChecked = category == "All"
        binding.chipReceived.isChecked = category == "Received"
        binding.chipPaid.isChecked = category == "Paid"
        binding.chipPending.isChecked = category == "Pending"
        filterAndPopulateTransactions()
    }

    private fun filterAndPopulateTransactions() {
        if (_binding == null) return
        val isCreator = sessionManager.getUserRole() == UserRole.CREATOR
        
        val container = binding.layoutTransactionsContainer
        container.removeAllViews()

        // Filter bookings based on status
        val filteredBookings = allBookings.filter { booking ->
            val statusStr = booking.status as? String
            val isConfirmedOrPaid = statusStr == "confirmed" || statusStr == "paid" || statusStr == "completed"
            val isPending = statusStr == "pending"

            when (selectedCategory) {
                "All" -> true
                "Received" -> isCreator && isConfirmedOrPaid
                "Paid" -> !isCreator && isConfirmedOrPaid
                "Pending" -> isPending
                else -> true
            }
        }

        if (filteredBookings.isEmpty()) {
            val emptyText = TextView(requireContext()).apply {
                text = "No transactions found."
                typeface = ResourcesCompat.getFont(requireContext(), R.font.poppins_medium)
                setTextColor(ContextCompat.getColor(context, R.color.colorTextSecondary))
                gravity = Gravity.CENTER
                setPadding(16, 64, 16, 64)
            }
            container.addView(emptyText)
            return
        }

        // Sort bookings by date descending
        val sortedBookings = filteredBookings.sortedByDescending { (it.eventDate as? String) ?: "2026-01-01" }

        // Group by Month-Year (e.g. "June 2026")
        val formatterInput = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH)
        val formatterOutput = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
        
        val groupedBookings = sortedBookings.groupBy { booking ->
            try {
                val dateStr = (booking.eventDate as? String) ?: "2026-01-01"
                val localDate = LocalDate.parse(dateStr, formatterInput)
                localDate.format(formatterOutput)
            } catch (e: Exception) {
                "Other Transactions"
            }
        }

        for ((month, bookingsInMonth) in groupedBookings) {
            // Add Month Header
            val monthHeader = TextView(requireContext()).apply {
                text = month
                typeface = ResourcesCompat.getFont(requireContext(), R.font.poppins_bold)
                setTextColor(ContextCompat.getColor(context, R.color.colorTextSecondary))
                textSize = 12f
                setPadding(16, 24, 16, 8)
            }
            container.addView(monthHeader)

            // Add Transactions inside Month
            for (booking in bookingsInMonth) {
                val rowLayout = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(16, 16, 16, 16)
                    gravity = Gravity.CENTER_VERTICAL
                    isClickable = true
                    val outValue = android.util.TypedValue()
                    context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                    setBackgroundResource(outValue.resourceId)
                    setOnClickListener {
                        showTransactionDetailsDialog(booking)
                    }
                }

                // Avatar / Circle Icon (Paytm Style)
                val avatarSize = (40 * resources.displayMetrics.density).toInt()
                val avatarLayout = android.widget.FrameLayout(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(avatarSize, avatarSize).apply {
                        setMargins(0, 0, 16, 0)
                    }
                }

                val statusStr = booking.status as? String
                val isPending = statusStr == "pending"
                val isCredit = isCreator && !isPending
                
                val shape = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(ContextCompat.getColor(requireContext(), 
                        when {
                            isPending -> R.color.colorSurfaceVariant
                            isCredit -> R.color.colorSuccessBg
                            else -> R.color.colorPrimaryLight
                        }
                    ))
                }
                avatarLayout.background = shape

                // Initials selection
                val nameForInitials = if (isCreator) {
                    booking.attendee?.fullName as? String ?: "Client"
                } else {
                    booking.creator?.fullName as? String ?: "Creator"
                }
                val initialText = nameForInitials.firstOrNull()?.toString()?.uppercase(Locale.ENGLISH) ?: "C"

                val avatarText = TextView(requireContext()).apply {
                    text = initialText
                    typeface = ResourcesCompat.getFont(requireContext(), R.font.poppins_bold)
                    setTextColor(ContextCompat.getColor(requireContext(), 
                        when {
                            isPending -> R.color.colorTextSecondary
                            isCredit -> R.color.colorSuccess
                            else -> R.color.colorTextOnAccent
                        }
                    ))
                    textSize = 14f
                    gravity = Gravity.CENTER
                }
                avatarLayout.addView(avatarText)
                rowLayout.addView(avatarLayout)

                // Text details (Title and Subtitle)
                val textLayout = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                }

                val titleText = TextView(requireContext()).apply {
                    text = if (isCreator) {
                        "Received from ${(booking.attendee?.fullName as? String ?: "Client")}"
                    } else {
                        "Paid to ${(booking.creator?.fullName as? String ?: "Creator Partner")}"
                    }
                    typeface = ResourcesCompat.getFont(requireContext(), R.font.poppins_medium)
                    setTextColor(ContextCompat.getColor(context, R.color.colorTextPrimary))
                    textSize = 14f
                }

                val subtitleText = TextView(requireContext()).apply {
                    val category = (booking.eventType as? String ?: "Photography").replaceFirstChar { it.uppercase() }
                    val dateVal = (booking.eventDate as? String) ?: "N/A"
                    text = "$category Photography • $dateVal"
                    typeface = ResourcesCompat.getFont(requireContext(), R.font.poppins_regular)
                    setTextColor(ContextCompat.getColor(context, R.color.colorTextSecondary))
                    textSize = 12f
                    setPadding(0, 2, 0, 0)
                }

                textLayout.addView(titleText)
                textLayout.addView(subtitleText)
                rowLayout.addView(textLayout)

                // Amount Text
                val amountText = TextView(requireContext()).apply {
                    val amountVal = (booking.totalAmount as? Double) ?: 0.0
                    text = when {
                        isPending -> "₹${amountVal.toInt()}"
                        isCredit -> "+ ₹${amountVal.toInt()}"
                        else -> "- ₹${amountVal.toInt()}"
                    }
                    typeface = ResourcesCompat.getFont(requireContext(), R.font.poppins_bold)
                    setTextColor(ContextCompat.getColor(context, 
                        when {
                            isPending -> R.color.colorTextSecondary
                            isCredit -> R.color.colorSuccess
                            else -> R.color.colorError
                        }
                    ))
                    textSize = 14f
                    gravity = Gravity.END or Gravity.CENTER_VERTICAL
                }
                rowLayout.addView(amountText)

                container.addView(rowLayout)

                // Divider line
                val divider = View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        (0.5f * resources.displayMetrics.density).toInt()
                    ).apply {
                        setMargins(16, 0, 16, 0)
                    }
                    setBackgroundColor(ContextCompat.getColor(context, R.color.colorDivider))
                }
                container.addView(divider)
            }
        }
    }

    private fun showTransactionDetailsDialog(booking: BookingResponse) {
        try {
            val context = requireContext()
            val isCreator = sessionManager.getUserRole() == UserRole.CREATOR
            
            val dialogView = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(48, 48, 48, 48)
                background = ContextCompat.getDrawable(context, R.drawable.bg_grid_pattern)
            }

            // Title Header
            val headerText = TextView(context).apply {
                text = "Transaction Details"
                typeface = ResourcesCompat.getFont(requireContext(), R.font.poppins_bold)
                setTextColor(ContextCompat.getColor(context, R.color.colorTextPrimary))
                textSize = 20f
                gravity = Gravity.CENTER_HORIZONTAL
            }
            dialogView.addView(headerText)

            val divider1 = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2).apply {
                    setMargins(0, 16, 0, 24)
                }
                setBackgroundColor(ContextCompat.getColor(context, R.color.colorDivider))
            }
            dialogView.addView(divider1)

            // Amount Block
            val amountLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, 24)
                }
            }
            val amountVal = TextView(context).apply {
                val amount = (booking.totalAmount as? Double) ?: 0.0
                text = "₹${amount.toInt()}"
                typeface = ResourcesCompat.getFont(requireContext(), R.font.poppins_bold)
                setTextColor(ContextCompat.getColor(context, R.color.colorTextPrimary))
                textSize = 32f
            }
            
            val statusStr = booking.status as? String
            val isPending = statusStr == "pending"

            val statusVal = TextView(context).apply {
                text = if (isPending) "Payment Pending" else "Payment Successful"
                typeface = ResourcesCompat.getFont(requireContext(), R.font.poppins_medium)
                setTextColor(ContextCompat.getColor(context, if (isPending) R.color.colorTextSecondary else R.color.colorSuccess))
                textSize = 14f
                setPadding(0, 4, 0, 0)
            }
            amountLayout.addView(amountVal)
            amountLayout.addView(statusVal)
            dialogView.addView(amountLayout)

            // Flow Diagram
            val flowCard = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(24, 20, 24, 20)
                val cardShape = android.graphics.drawable.GradientDrawable().apply {
                    setColor(ContextCompat.getColor(context, R.color.colorSurface))
                    cornerRadius = 12f * resources.displayMetrics.density
                    setStroke(1, ContextCompat.getColor(context, R.color.colorBorder))
                }
                background = cardShape
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, 24)
                }
            }

            val senderName = if (isCreator) {
                booking.attendee?.fullName as? String ?: "Client"
            } else {
                "Me"
            }
            val receiverName = if (isCreator) {
                "Me (Creator)"
            } else {
                booking.creator?.fullName as? String ?: "Creator Partner"
            }

            val senderTv = TextView(context).apply {
                text = senderName
                typeface = ResourcesCompat.getFont(requireContext(), R.font.poppins_semibold)
                setTextColor(ContextCompat.getColor(context, R.color.colorTextPrimary))
                textSize = 12f
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            
            val arrowTv = TextView(context).apply {
                text = " ➔ "
                typeface = ResourcesCompat.getFont(requireContext(), R.font.poppins_bold)
                setTextColor(ContextCompat.getColor(context, R.color.colorAccent))
                textSize = 14f
                gravity = Gravity.CENTER
            }

            val receiverTv = TextView(context).apply {
                text = receiverName
                typeface = ResourcesCompat.getFont(requireContext(), R.font.poppins_semibold)
                setTextColor(ContextCompat.getColor(context, R.color.colorTextPrimary))
                textSize = 12f
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            flowCard.addView(senderTv)
            flowCard.addView(arrowTv)
            flowCard.addView(receiverTv)
            dialogView.addView(flowCard)

            // Detail rows
            fun addDetailRow(label: String, value: String) {
                val row = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, 10, 0, 10)
                }
                val labelTv = TextView(context).apply {
                    text = label
                    typeface = ResourcesCompat.getFont(requireContext(), R.font.poppins_regular)
                    setTextColor(ContextCompat.getColor(context, R.color.colorTextSecondary))
                    textSize = 13f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                val valueTv = TextView(context).apply {
                    text = value
                    typeface = ResourcesCompat.getFont(requireContext(), R.font.poppins_medium)
                    setTextColor(ContextCompat.getColor(context, R.color.colorTextPrimary))
                    textSize = 13f
                    gravity = Gravity.END
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.5f)
                }
                row.addView(labelTv)
                row.addView(valueTv)
                dialogView.addView(row)
            }

            addDetailRow("Paid From", senderName)
            addDetailRow("Paid To", receiverName)
            addDetailRow("Category", (booking.eventType as? String ?: "Photography").replaceFirstChar { it.uppercase() })
            addDetailRow("Location", booking.location as? String ?: "N/A")
            addDetailRow("Event Date", booking.eventDate as? String ?: "N/A")
            addDetailRow("Transaction ID", (booking.id as? String ?: "").uppercase())

            val builder = MaterialAlertDialogBuilder(context).setView(dialogView)
            
            if (!isPending) {
                builder.setPositiveButton("Download Invoice") { _, _ ->
                    downloadInvoice(booking.id)
                }
            }
            
            builder.setNegativeButton("Close", null).show()

        } catch (e: Exception) {
            Timber.e(e, "Error showing transaction details")
            Toast.makeText(context, "Error displaying details: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadAiSummary() {
        lifecycleScope.launch {
            bookingRepository.getStatementSummary().collectLatest { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        Toast.makeText(requireContext(), "Generating AI Summary...", Toast.LENGTH_SHORT).show()
                    }
                    is Resource.Success -> {
                        val summary = resource.data["summary"] ?: "No summary available."
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle("AI Payment Summary")
                            .setMessage(summary)
                            .setPositiveButton("Awesome", null)
                            .show()
                    }
                    is Resource.Error -> {
                        Toast.makeText(requireContext(), "Error: ${resource.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun downloadStatement() {
        lifecycleScope.launch {
            bookingRepository.downloadStatement().collectLatest { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        Toast.makeText(requireContext(), "Downloading statement...", Toast.LENGTH_SHORT).show()
                    }
                    is Resource.Success -> {
                        val filename = "Statement_Capturo_${System.currentTimeMillis() / 1000}.pdf"
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
                                startActivity(Intent.createChooser(intent, "Open Statement"))
                            } catch (e: Exception) {
                                Toast.makeText(requireContext(), "No PDF viewer installed", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(requireContext(), "Failed to save statement PDF", Toast.LENGTH_SHORT).show()
                        }
                    }
                    is Resource.Error -> {
                        Toast.makeText(requireContext(), "Error: ${resource.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun downloadInvoice(bookingId: String) {
        lifecycleScope.launch {
            bookingRepository.downloadInvoice(bookingId).collectLatest { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        Toast.makeText(requireContext(), "Downloading Invoice...", Toast.LENGTH_SHORT).show()
                    }
                    is Resource.Success -> {
                        val filename = "Invoice_${bookingId.take(8).uppercase()}.pdf"
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
                                Toast.makeText(requireContext(), "No PDF viewer installed", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(requireContext(), "Failed to save Invoice PDF", Toast.LENGTH_SHORT).show()
                        }
                    }
                    is Resource.Error -> {
                        Toast.makeText(requireContext(), "Error: ${resource.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
