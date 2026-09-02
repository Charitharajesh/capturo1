package com.capturo.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.capturo.app.R
import com.capturo.app.adapter.CreatorListAdapter
import com.capturo.app.data.model.response.CreatorDistanceResponse
import com.capturo.app.data.preferences.SessionManager
import com.capturo.app.data.preferences.UserRole
import com.capturo.app.databinding.FragmentHomeBinding
import com.capturo.app.ui.main.MainActivity
import com.capturo.app.utils.Resource
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.Locale

@AndroidEntryPoint
class HomeFragment : Fragment() {

    @Inject
    lateinit var sessionManager: SessionManager

    private val viewModel: HomeViewModel by viewModels()
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var creatorAdapter: CreatorListAdapter
    private lateinit var followedCreatorsAdapter: CreatorListAdapter
    private var countdownJob: kotlinx.coroutines.Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupModeSwitch()
        setupGreeting()
        setupRecyclerView()
        setupSwipeRefresh()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupModeSwitch() {
        // Client mode = unchecked, Event Creator mode = checked.
        binding.switchMode.isChecked = sessionManager.getUserRole() == UserRole.CREATOR
        binding.switchMode.setOnCheckedChangeListener { button, isChecked ->
            if (!button.isPressed) return@setOnCheckedChangeListener
            val newRole = if (isChecked) UserRole.CREATOR else UserRole.ATTENDEE
            (activity as? MainActivity)?.switchRole(newRole)
        }
    }

    private fun setupGreeting() {
        val fullName = sessionManager.getFullName()
        val firstName = if (!fullName.isNullOrBlank()) {
            fullName.trim().split("\\s+".toRegex()).firstOrNull() ?: "User"
        } else {
            "User"
        }
        binding.textGreeting.text = "Hi, $firstName! 👋"
    }

    private fun setupRecyclerView() {
        creatorAdapter = CreatorListAdapter { creator ->
            val bundle = Bundle().apply {
                putString("creator_id", creator.creator.id)
                putBoolean("hide_ctas", false)
            }
            findNavController().navigate(R.id.creatorProfileFragment, bundle)
        }

        binding.rvFeaturedCreators.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = creatorAdapter
            val snapHelper = LinearSnapHelper()
            snapHelper.attachToRecyclerView(this)
        }

        followedCreatorsAdapter = CreatorListAdapter { creator ->
            val bundle = Bundle().apply {
                putString("creator_id", creator.creator.id)
                putBoolean("hide_ctas", true)
            }
            findNavController().navigate(R.id.creatorProfileFragment, bundle)
        }

        binding.rvFollowingCreators.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = followedCreatorsAdapter
            val snapHelper = LinearSnapHelper()
            snapHelper.attachToRecyclerView(this)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.colorAccent)
        binding.swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.colorSurface)

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }
    }

    private fun setupClickListeners() {
        binding.layoutSearch.setOnClickListener {
            safeNavigate(R.id.nearbyCreatorsFragment)
        }

        binding.btnMap.setOnClickListener {
            safeNavigate(R.id.mapViewFragment)
        }

        binding.btnNotifications.setOnClickListener {
            safeNavigate(R.id.notificationsFragment)
        }

        binding.btnMapView.setOnClickListener {
            safeNavigate(R.id.mapViewFragment)
        }

        binding.btnQuickBook.setOnClickListener {
            safeNavigate(R.id.nearbyCreatorsFragment)
        }

        binding.btnSeeAll.setOnClickListener {
            safeNavigate(R.id.nearbyCreatorsFragment)
        }
    }

    private fun safeNavigate(destinationId: Int, args: Bundle? = null) {
        try {
            findNavController().navigate(destinationId, args)
        } catch (e: IllegalArgumentException) {
            timber.log.Timber.e(e, "Navigation failed for destination: $destinationId")
        }
    }

    private fun observeViewModel() {
        viewModel.featuredCreators.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    if (!binding.swipeRefresh.isRefreshing) {
                        binding.progressLoading.visibility = View.VISIBLE
                    }
                }
                is Resource.Success -> {
                    binding.progressLoading.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    val distanceCreators = resource.data.map { CreatorDistanceResponse(creator = it, distanceKm = 0.0) }
                    creatorAdapter.submitList(distanceCreators)
                }
                is Resource.Error -> {
                    binding.progressLoading.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    Snackbar.make(binding.root, resource.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }

        viewModel.followedCreators.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    val list = resource.data
                    if (list.isNotEmpty()) {
                        binding.textFollowingTitle.visibility = View.VISIBLE
                        binding.rvFollowingCreators.visibility = View.VISIBLE
                        val distanceCreators = list.map { CreatorDistanceResponse(creator = it, distanceKm = 0.0) }
                        followedCreatorsAdapter.submitList(distanceCreators)
                    } else {
                        binding.textFollowingTitle.visibility = View.GONE
                        binding.rvFollowingCreators.visibility = View.GONE
                    }
                }
                else -> {
                    binding.textFollowingTitle.visibility = View.GONE
                    binding.rvFollowingCreators.visibility = View.GONE
                }
            }
        }

        viewModel.upcomingBooking.observe(viewLifecycleOwner) { booking ->
            if (booking != null) {
                startCountdown(booking)
            } else {
                countdownJob?.cancel()
                binding.cardCountdown.visibility = View.GONE
            }
        }

        viewModel.unreadNotificationCount.observe(viewLifecycleOwner) { count ->
            binding.notificationBadge.visibility = if (count > 0) View.VISIBLE else View.GONE
        }
    }

    private fun startCountdown(booking: com.capturo.app.data.model.response.BookingResponse) {
        countdownJob?.cancel()
        val targetDateTime = try {
            val date = java.time.LocalDate.parse(booking.eventDate)
            val time = parseLocalTime(booking.startTime)
            java.time.LocalDateTime.of(date, time).atZone(java.time.ZoneId.systemDefault())
        } catch (e: Exception) {
            null
        }

        if (targetDateTime == null) {
            binding.cardCountdown.visibility = View.GONE
            return
        }

        binding.cardCountdown.visibility = View.VISIBLE
        binding.textCountdownDetails.text = "${booking.eventType.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString() }} • ${booking.eventDate} at ${booking.startTime}"

        countdownJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                val now = java.time.ZonedDateTime.now(java.time.ZoneId.systemDefault())
                val diffSeconds = java.time.temporal.ChronoUnit.SECONDS.between(now, targetDateTime)
                if (diffSeconds <= 0) {
                    binding.textCountdownDays.text = "00"
                    binding.textCountdownHours.text = "00"
                    binding.textCountdownMinutes.text = "00"
                    binding.textCountdownSeconds.text = "00"
                    break
                } else {
                    val days = diffSeconds / (24 * 3600)
                    var temp = diffSeconds % (24 * 3600)
                    val hours = temp / 3600
                    temp %= 3600
                    val minutes = temp / 60
                    val seconds = temp % 60

                    binding.textCountdownDays.text = String.format(Locale.US, "%02d", days)
                    binding.textCountdownHours.text = String.format(Locale.US, "%02d", hours)
                    binding.textCountdownMinutes.text = String.format(Locale.US, "%02d", minutes)
                    binding.textCountdownSeconds.text = String.format(Locale.US, "%02d", seconds)
                }
                delay(1000)
            }
        }
    }

    private fun parseLocalTime(timeStr: String): java.time.LocalTime {
        return try {
            java.time.LocalTime.parse(timeStr)
        } catch (e: Exception) {
            val parts = timeStr.split(":")
            val hh = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val mm = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val ss = parts.getOrNull(2)?.toIntOrNull() ?: 0
            java.time.LocalTime.of(hh, mm, ss)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countdownJob?.cancel()
        _binding = null
    }
}
