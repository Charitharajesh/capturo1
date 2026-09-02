package com.capturo.app.ui.creator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import coil.load
import coil.transform.CircleCropTransformation
import com.capturo.app.R
import com.capturo.app.adapter.GalleryAdapter
import com.capturo.app.data.model.response.CreatorPublicResponse
import com.capturo.app.data.model.response.GalleryResponse
import com.capturo.app.databinding.FragmentCreatorProfileBinding
import com.capturo.app.utils.Resource
import com.capturo.app.utils.toFullUrl
import com.capturo.app.utils.showSnackbar
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.Locale

@AndroidEntryPoint
class CreatorProfileFragment : Fragment() {

    private var _binding: FragmentCreatorProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CreatorViewModel by viewModels()
    private lateinit var galleryAdapter: GalleryAdapter

    private var creatorId: String = ""
    private var creatorUserId: String = ""
    private var hourlyRate: Double = 0.0
    private var creatorName: String = ""
    private var creatorAvatarUrl: String? = null
    private var creatorPhone: String? = null

    private var fullPortfolioList: List<GalleryResponse> = emptyList()
    private var selectedCategory: String = "All"
    private var isFollowing = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreatorProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieve creator ID from navigation safe arguments or bundle fallback
        creatorId = arguments?.getString("creator_id") ?: ""
        if (creatorId.isEmpty()) {
            showSnackbar("Error: Invalid Creator Profile", Snackbar.LENGTH_LONG)
            findNavController().navigateUp()
            return
        }

        val hideCtas = arguments?.getBoolean("hide_ctas", false) ?: false
        if (hideCtas) {
            binding.layoutBottomCta.visibility = View.GONE
        }

        // Setup Toolbar back button
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        // Setup Toolbar Title on Scroll
        var isShow = true
        var scrollRange = -1
        binding.appBarLayout.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            if (scrollRange == -1) {
                scrollRange = appBarLayout.totalScrollRange
            }
            if (scrollRange + verticalOffset == 0) {
                binding.toolbar.title = creatorName
                isShow = true
            } else if (isShow) {
                binding.toolbar.title = " "
                isShow = false
            }
        }

        // Setup Recycler with GalleryAdapter
        binding.recyclerPortfolio.layoutManager = GridLayoutManager(requireContext(), 3)
        galleryAdapter = GalleryAdapter { item ->
            Timber.d("Portfolio item clicked: ${item.fileUrl}")
        }
        binding.recyclerPortfolio.adapter = galleryAdapter

        // Setup Category chips
        setupCategoryChips()

        // Setup Follow Button logic
        binding.buttonFollow.setOnClickListener {
            if (isFollowing) {
                viewModel.unfollowCreator(creatorId)
            } else {
                viewModel.followCreator(creatorId)
            }
        }

        // Chat is booking-scoped; users must book first.
        binding.buttonChat.setOnClickListener {
            showSnackbar("You must book this creator first to send messages.", Snackbar.LENGTH_LONG)
        }

        binding.buttonCall.setOnClickListener {
            creatorPhone?.let { phone ->
                val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:$phone"))
                startActivity(intent)
            } ?: run {
                showSnackbar("Phone number not available", Snackbar.LENGTH_SHORT)
            }
        }

        // Setup Book Now action trigger with safe navigation
        binding.buttonBook.setOnClickListener {
            try {
                val bundle = Bundle().apply {
                    putString("creator_id", creatorUserId)
                    putFloat("hourly_rate", hourlyRate.toFloat())
                    putString("creator_name", creatorName)
                    putString("creator_avatar_url", creatorAvatarUrl)
                }
                findNavController().navigate(R.id.action_creatorProfileFragment_to_createBookingFragment, bundle)
            } catch (e: IllegalArgumentException) {
                Timber.e(e, "Navigation to CreateBookingFragment failed")
            }
        }

        // Fetch creator detail
        viewModel.loadCreator(creatorId)

        // Observe ViewModel streams
        observeViewModel()
    }

    private fun setupCategoryChips() {
        val categories = listOf("All", "Weddings", "Engagements", "Events", "Portraits")
        binding.chipGroupPortfolioCategories.removeAllViews()
        categories.forEach { category ->
            val chip = Chip(requireContext()).apply {
                text = category
                isClickable = true
                isCheckable = true
                isChecked = (category == selectedCategory)
                
                // Design aesthetics matching mockup
                setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTextPrimary))
                setChipBackgroundColorResource(if (isChecked) R.color.colorPrimaryContainer else R.color.colorSurface)
                chipStrokeColor = ContextCompat.getColorStateList(requireContext(), if (isChecked) R.color.colorAccent else R.color.colorBorder)
                chipStrokeWidth = resources.displayMetrics.density * 1.2f
            }
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedCategory = category
                    // Refresh style of all chips
                    setupCategoryChips()
                    filterAndSubmitPortfolio()
                }
            }
            binding.chipGroupPortfolioCategories.addView(chip)
        }
    }

    private fun filterAndSubmitPortfolio() {
        if (selectedCategory == "All") {
            galleryAdapter.submitList(fullPortfolioList)
        } else {
            val filtered = fullPortfolioList.filter { item ->
                val textToSearch = "${item.title.orEmpty()} ${item.description.orEmpty()}".lowercase(Locale.US)
                textToSearch.contains(selectedCategory.lowercase(Locale.US))
            }
            if (filtered.isEmpty()) {
                // Return a beautiful dynamic subset so screen is never blank
                galleryAdapter.submitList(fullPortfolioList.take(3))
            } else {
                galleryAdapter.submitList(filtered)
            }
        }
    }

    private fun updateFollowButtonState() {
        if (isFollowing) {
            binding.buttonFollow.text = "Following"
            binding.buttonFollow.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorSurfaceVariant))
            binding.buttonFollow.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTextSecondary))
            binding.buttonFollow.strokeColor = ContextCompat.getColorStateList(requireContext(), R.color.colorBorder)
            showSnackbar("Following ${creatorName}", Snackbar.LENGTH_SHORT)
        } else {
            binding.buttonFollow.text = "Follow"
            binding.buttonFollow.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
            binding.buttonFollow.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTextPrimary))
            binding.buttonFollow.strokeColor = null
        }
    }

    private fun observeViewModel() {
        // Observe Profile
        viewModel.creatorProfile.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.textName.text = "Loading profile..."
                }
                is Resource.Success -> {
                    bindCreatorDetails(resource.data)
                }
                is Resource.Error -> {
                    showSnackbar(resource.message, Snackbar.LENGTH_LONG)
                }
            }
        }

        // Observe Portfolio
        viewModel.portfolio.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {}
                is Resource.Success -> {
                    val list = resource.data
                    fullPortfolioList = list
                    filterAndSubmitPortfolio()
                }
                is Resource.Error -> {}
            }
        }

        // Observe AI Summary
        viewModel.aiSummary.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.cardAiSummary.visibility = View.GONE
                }
                is Resource.Success -> {
                    val summaryData = resource.data
                    binding.cardAiSummary.visibility = View.VISIBLE
                    binding.textAiSummary.text = summaryData.summary
                    
                    // Populate highlights chips dynamically
                    binding.chipGroupAiHighlights.removeAllViews()
                    summaryData.highlights.forEach { highlight ->
                        val chip = Chip(requireContext()).apply {
                            text = highlight
                            isClickable = false
                            isCheckable = false
                            setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                            setChipBackgroundColorResource(R.color.colorOnlineStatus)
                            chipStrokeWidth = 0f
                        }
                        binding.chipGroupAiHighlights.addView(chip)
                    }

                    // Watch out section
                    if (!summaryData.watchOut.isNullOrBlank()) {
                        binding.layoutAiWatchOut.visibility = View.VISIBLE
                        binding.textAiWatchOut.text = summaryData.watchOut
                    } else {
                        binding.layoutAiWatchOut.visibility = View.GONE
                    }
                }
                is Resource.Error -> {
                    binding.cardAiSummary.visibility = View.GONE
                }
            }
        }
    }

    private fun bindCreatorDetails(creator: CreatorPublicResponse) {
        hourlyRate = creator.hourlyRate
        creatorUserId = creator.userId
        creatorName = creator.user?.fullName ?: ""
        creatorAvatarUrl = creator.user?.profilePicUrl
        creatorPhone = creator.user?.phone

        // Fetch portfolio using the real creator User ID!
        viewModel.loadPortfolio(creator.userId)

        // Fetch AI Review Summary
        viewModel.fetchAiSummary(creator.id, creatorName)

        isFollowing = creator.isFollowing
        updateFollowButtonState()

        binding.textName.text = creatorName
        
        // Format followers count dynamically (e.g. 2.5K followers or 12 followers)
        val followersCountText = if (creator.followersCount >= 1000) {
            String.format(Locale.US, "%.1fK followers", creator.followersCount / 1000.0)
        } else {
            "${creator.followersCount} followers"
        }

        binding.textRatingSummary.text = String.format(
            Locale.US,
            "★ %.1f (%d)  •  %s",
            creator.avgRating,
            creator.totalReviews,
            followersCountText
        )
        
        binding.textReviewsCountStat.text = creator.totalBookings.toString()
        binding.textOnTimeRateStat.text = String.format(Locale.US, "★ %.1f", creator.avgRating)
        binding.textWorksCount.text = com.capturo.app.utils.FormattingUtils.formatCount(creator.followersCount)
        binding.textPrice.text = "₹${creator.hourlyRate.toInt()}/hr"

        if (!creator.bio.isNullOrBlank()) {
            binding.textBio.text = creator.bio
        }

        // Availability status formatting
        val isAvailable = creator.availabilityStatus.lowercase(Locale.US) == "available"
        binding.textAvailability.text = if (isAvailable) "● Online" else "○ Offline"
        binding.textAvailability.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (isAvailable) R.color.colorOnlineStatus else R.color.colorTextSecondary
            )
        )

        // Bind image cover and avatar
        binding.imageAvatar.load(creatorAvatarUrl.toFullUrl()) {
            crossfade(true)
            transformations(CircleCropTransformation())
            placeholder(R.drawable.ic_profile)
            error(R.drawable.ic_profile)
        }

        binding.imageCover.load(creatorAvatarUrl.toFullUrl()) {
            crossfade(true)
            placeholder(R.drawable.bg_grid_pattern)
            error(R.drawable.bg_grid_pattern)
        }

        // Populate chips categories dynamically (specialties)
        binding.chipGroupSpecialties.removeAllViews()
        val specialties = creator.specializations
        if (specialties.isNotEmpty()) {
            specialties.forEach { spec ->
                val chip = Chip(requireContext()).apply {
                    text = spec
                    isClickable = false
                    isCheckable = false
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTextSecondary))
                    setChipBackgroundColorResource(R.color.colorSurface)
                }
                binding.chipGroupSpecialties.addView(chip)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
