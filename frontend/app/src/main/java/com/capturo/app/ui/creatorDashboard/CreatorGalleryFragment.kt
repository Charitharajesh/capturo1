package com.capturo.app.ui.creatorDashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.capturo.app.R
import com.capturo.app.adapter.GalleryAdapter
import com.capturo.app.data.preferences.SessionManager
import com.capturo.app.data.preferences.UserRole
import com.capturo.app.data.repository.CreatorRepository
import com.capturo.app.data.repository.GalleryRepository
import com.capturo.app.databinding.FragmentCreatorGalleryBinding
import com.capturo.app.utils.FormattingUtils
import com.capturo.app.utils.Resource
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CreatorGalleryFragment : Fragment() {

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var galleryRepository: GalleryRepository

    @Inject
    lateinit var creatorRepository: CreatorRepository

    private var _binding: FragmentCreatorGalleryBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: GalleryAdapter
    private var fullPortfolioList: List<com.capturo.app.data.model.response.GalleryResponse> = emptyList()
    private var selectedCategory: String = "All"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreatorGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (sessionManager.getUserRole() != UserRole.CREATOR) {
            return
        }

        setupRecyclerView()
        setupSwipeToRefresh()
        setupButtons()
        setupChips()

        loadProfile()
        loadPortfolio()
    }

    private fun setupRecyclerView() {
        adapter = GalleryAdapter(
            onItemClicked = { item ->
                // Future expansion: preview full screen
            }
        )

        binding.recyclerGallery.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = this@CreatorGalleryFragment.adapter
        }
    }

    private fun setupSwipeToRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            loadPortfolio()
        }
    }

    private fun setupChips() {
        binding.chipGroupFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull()
            selectedCategory = when (checkedId) {
                R.id.chipAll -> "All"
                R.id.chipWeddings -> "Weddings"
                R.id.chipEngagements -> "Engagements"
                R.id.chipEvents -> "Events"
                R.id.chipPortraits -> "Portraits"
                else -> "All"
            }
            filterAndSubmitPortfolio()
        }
    }

    private fun setupButtons() {
        binding.btnUpload.setOnClickListener {
            val dialog = UploadGalleryDialogFragment()
            dialog.show(childFragmentManager, "UploadGalleryDialog")
        }
    }

    private fun loadProfile() {
        val creatorId = sessionManager.getUserId() ?: return
        
        lifecycleScope.launch {
            creatorRepository.getCreatorById(creatorId).collectLatest { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val profile = resource.data
                        val name = profile.user?.fullName ?: "Creator"
                        binding.tvCreatorName.text = name
                        
                        val specializations = profile.specializations
                        binding.tvCreatorTitle.text = if (specializations.isNotEmpty()) specializations.first() else "Creator"
                        
                        binding.tvRating.text = String.format(java.util.Locale.ENGLISH, "%.1f (%d)", profile.avgRating, profile.totalReviews)
                        
                        val followersStr = FormattingUtils.formatCount(profile.followersCount)
                        binding.tvFollowers.text = "$followersStr followers"
                    }
                    else -> {}
                }
            }
        }
    }

    fun loadPortfolio() {
        val creatorId = sessionManager.getUserId() ?: return
        
        lifecycleScope.launch {
            galleryRepository.getCreatorPortfolio(creatorId).collectLatest { resource ->
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
                        val items = resource.data.items
                        fullPortfolioList = items
                        filterAndSubmitPortfolio()
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

    private fun filterAndSubmitPortfolio() {
        if (selectedCategory == "All") {
            adapter.submitList(fullPortfolioList)
            if (fullPortfolioList.isEmpty()) {
                binding.layoutEmpty.visibility = View.VISIBLE
                binding.recyclerGallery.visibility = View.GONE
            } else {
                binding.layoutEmpty.visibility = View.GONE
                binding.recyclerGallery.visibility = View.VISIBLE
            }
        } else {
            val filtered = fullPortfolioList.filter { item ->
                val textToSearch = "${item.title.orEmpty()} ${item.description.orEmpty()}".lowercase(java.util.Locale.US)
                textToSearch.contains(selectedCategory.lowercase(java.util.Locale.US))
            }
            adapter.submitList(filtered)
            if (filtered.isEmpty()) {
                binding.layoutEmpty.visibility = View.VISIBLE
                binding.recyclerGallery.visibility = View.GONE
            } else {
                binding.layoutEmpty.visibility = View.GONE
                binding.recyclerGallery.visibility = View.VISIBLE
            }
        }
    }

    private fun showSnackbar(message: String?) {
        Snackbar.make(binding.root, message ?: "Error occurred", Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
