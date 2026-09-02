package com.capturo.app.ui.search

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capturo.app.R
import com.capturo.app.adapter.CreatorListAdapter
import com.capturo.app.databinding.FragmentNearbyCreatorsBinding
import com.capturo.app.utils.LocationUtils
import com.capturo.app.utils.Resource
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class NearbyCreatorsFragment : Fragment() {

    private var _binding: FragmentNearbyCreatorsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SearchViewModel by viewModels()
    private lateinit var adapter: CreatorListAdapter

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineGranted || coarseGranted) {
            Timber.d("Location permission granted")
            fetchLocationAndCreators()
        } else {
            Timber.e("Location permission denied")
            binding.textCount.text = "Permission denied. Defaulting to fallback location."
            fetchLocationAndCreators()
            Snackbar.make(
                binding.root,
                "Location permission is required to find closest creators.",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNearbyCreatorsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup Toolbar
        binding.toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        // Setup Adapter
        adapter = CreatorListAdapter { creatorDistance ->
            Timber.d("Creator clicked: ${creatorDistance.creator.fullName}")
            try {
                val bundle = Bundle().apply {
                    putString("creator_id", creatorDistance.creator.id)
                }
                findNavController().navigate(R.id.creatorProfileFragment, bundle)
            } catch (e: IllegalArgumentException) {
                Timber.e(e, "Navigation to creator profile failed")
            }
        }

        val layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerCreators.layoutManager = layoutManager
        binding.recyclerCreators.adapter = adapter

        // Setup Scroll Listener for Pagination
        binding.recyclerCreators.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                val total = layoutManager.itemCount
                if (dy > 0 && lastVisible >= total - 3) {
                    viewModel.loadNextPage()
                }
            }
        })

        // Setup FAB for filter bottom sheet
        binding.fabFilter.setOnClickListener {
            val dialog = FilterBottomSheetDialog(viewModel.activeFilters) { params ->
                viewModel.applyFilters(params)
                updateChipsSelection(params.specialization)
            }
            dialog.show(childFragmentManager, "FilterBottomSheetDialog")
        }

        // Setup Chip Group click listeners for quick filters
        binding.chipGroupSpecialties.setOnCheckedStateChangeListener { _, checkedIds ->
            val specialization = when (checkedIds.firstOrNull()) {
                binding.chipWedding.id -> "Wedding"
                binding.chipPortrait.id -> "Portrait"
                binding.chipCorporate.id -> "Corporate"
                binding.chipSports.id -> "Sports"
                binding.chipEvents.id -> "Events"
                else -> null
            }
            viewModel.applyFilters(
                viewModel.activeFilters.copy(specialization = specialization)
            )
        }

        // Observe ViewModel
        observeViewModel()

        // Apply initial chip styles
        updateChipsSelection(viewModel.activeFilters.specialization)

        // Check Permissions
        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        val fineLoc = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLoc = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineLoc || coarseLoc) {
            fetchLocationAndCreators()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun fetchLocationAndCreators() {
        binding.textCount.text = "Acquiring accurate location..."
        LocationUtils.getCurrentLocation(requireContext()) { lat, lon ->
            viewModel.loadNearbyCreators(lat, lon)
        }
    }

    private fun observeViewModel() {
        viewModel.nearbyCreators.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.shimmerLayout.visibility = View.VISIBLE
                    binding.shimmerLayout.startShimmer()
                    binding.recyclerCreators.visibility = View.GONE
                    binding.layoutEmpty.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.shimmerLayout.stopShimmer()
                    binding.shimmerLayout.visibility = View.GONE
                    binding.recyclerCreators.visibility = View.VISIBLE

                    val creators = resource.data
                    if (creators.isEmpty()) {
                        binding.textCount.text = "No creators found near you"
                        binding.layoutEmpty.visibility = View.VISIBLE
                    } else {
                        binding.textCount.text = "${creators.size} creators found near you"
                        binding.layoutEmpty.visibility = View.GONE
                    }
                }
                is Resource.Error -> {
                    binding.shimmerLayout.stopShimmer()
                    binding.shimmerLayout.visibility = View.GONE
                    binding.recyclerCreators.visibility = View.VISIBLE
                    binding.layoutEmpty.visibility = View.VISIBLE

                    binding.textCount.text = "Failed to load nearby creators"
                    Snackbar.make(binding.root, resource.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }

        viewModel.paginatedCreators.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
        }
    }

    private fun updateChipsSelection(specialization: String?) {
        val targetChipId = when (specialization?.lowercase()) {
            "wedding" -> binding.chipWedding.id
            "portrait" -> binding.chipPortrait.id
            "corporate" -> binding.chipCorporate.id
            "sports" -> binding.chipSports.id
            "events" -> binding.chipEvents.id
            else -> binding.chipAll.id
        }
        binding.chipGroupSpecialties.check(targetChipId)
        
        for (i in 0 until binding.chipGroupSpecialties.childCount) {
            val chip = binding.chipGroupSpecialties.getChildAt(i) as com.google.android.material.chip.Chip
            val isChecked = chip.id == targetChipId
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTextPrimary))
            chip.setChipBackgroundColorResource(if (isChecked) R.color.colorPrimaryContainer else R.color.colorSurface)
            chip.chipStrokeColor = ContextCompat.getColorStateList(requireContext(), if (isChecked) R.color.colorAccent else R.color.colorBorder)
            chip.chipStrokeWidth = resources.displayMetrics.density * 1.2f
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
