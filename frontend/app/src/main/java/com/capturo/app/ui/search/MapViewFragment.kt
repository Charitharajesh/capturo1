package com.capturo.app.ui.search

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import coil.load
import coil.transform.CircleCropTransformation
import com.capturo.app.R
import com.capturo.app.data.model.response.CreatorDistanceResponse
import com.capturo.app.databinding.FragmentMapViewBinding
import com.capturo.app.utils.LocationUtils
import com.capturo.app.utils.Resource
import com.capturo.app.utils.toIndianPrice
import com.capturo.app.utils.toFullUrl
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import timber.log.Timber
import java.util.Locale

/**
 * Fragment rendering an interactive dark-styled OpenStreetMap Map showing nearby creators
 * as custom markers. Selecting a marker opens a bottom floating preview card.
 */
@AndroidEntryPoint
class MapViewFragment : Fragment() {

    private var _binding: FragmentMapViewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SearchViewModel by viewModels()
    private val markersMap = HashMap<String, Marker>()

    // Device location — defaults to Bangalore city centre (where the demo
    // creators are seeded) until a real GPS fix is acquired
    private var userLat: Double = 12.9716
    private var userLon: Double = 77.5946
    private var userMarker: Marker? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup Toolbar
        binding.toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        // Configure osmdroid user agent
        Configuration.getInstance().userAgentValue = requireContext().packageName

        // Initialize Map safely
        try {
            val map = binding.map
            map.setTileSource(TileSourceFactory.MAPNIK)
            map.setMultiTouchControls(true)
            
            // Set zoom and controller settings
            val mapController = map.controller
            mapController.setZoom(12.0)
            
            // Apply custom dark style via ColorMatrix invert filter
            val inverseMatrix = ColorMatrix(floatArrayOf(
                -1.0f,  0.0f,  0.0f,  0.0f, 255.0f, // Red
                 0.0f, -1.0f,  0.0f,  0.0f, 255.0f, // Green
                 0.0f,  0.0f, -1.0f,  0.0f, 255.0f, // Blue
                 0.0f,  0.0f,  0.0f,  1.0f,   0.0f  // Alpha
            ))
            val filter = ColorMatrixColorFilter(inverseMatrix)
            map.overlayManager.tilesOverlay.setColorFilter(filter)
            
            // Center map initially around default coords (Chennai)
            val startPoint = GeoPoint(13.0827, 80.2707)
            mapController.setCenter(startPoint)

            // Setup map tap listener to hide card
            val mapOverlay = MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                    binding.cardCreatorQuickView.root.visibility = View.GONE
                    return true
                }
                override fun longPressHelper(p: GeoPoint?): Boolean {
                    return false
                }
            })
            map.overlays.add(0, mapOverlay)

            // Apply any already cached data if available
            viewModel.nearbyCreators.value?.let { resource ->
                if (resource is Resource.Success) {
                    displayMarkers(resource.data)
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "Error initializing MapView")
            Snackbar.make(binding.root, "Map initialization failed", Snackbar.LENGTH_LONG).show()
        }

        // Observe nearby creators data
        observeViewModel()

        // Acquire device location and launch search queries
        checkLocationPermissionAndFetch()
    }

    private fun checkLocationPermissionAndFetch() {
        val hasPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            LocationUtils.getCurrentLocation(requireContext()) { lat, lon ->
                userLat = lat
                userLon = lon
                centerOnUser()
                viewModel.loadNearbyCreators(lat, lon)
            }
        } else {
            // Default search from fallback location if permission is missing
            centerOnUser()
            viewModel.loadNearbyCreators(userLat, userLon)
        }
    }

    /** Centre the camera on the user and drop a distinct "you are here" marker. */
    private fun centerOnUser() {
        val map = binding.map
        val userPoint = GeoPoint(userLat, userLon)
        map.controller.animateTo(userPoint)

        userMarker?.let { map.overlays.remove(it) }
        val marker = Marker(map).apply {
            position = userPoint
            title = "You are here"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_online_dot)
        }
        userMarker = marker
        map.overlays.add(marker)
        map.invalidate()
    }

    private fun observeViewModel() {
        viewModel.nearbyCreators.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    // Handled gracefully inside Map loader
                }
                is Resource.Success -> {
                    displayMarkers(resource.data)
                }
                is Resource.Error -> {
                    Snackbar.make(binding.root, resource.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun displayMarkers(creators: List<CreatorDistanceResponse>) {
        val map = binding.map
        // Clear creator markers but preserve the user's own "you are here" marker
        map.overlays.removeAll { it is Marker && it != userMarker }
        markersMap.clear()

        if (creators.isEmpty()) {
            map.invalidate()
            return
        }

        creators.forEach { item ->
            val creator = item.creator
            val geoPoint = geoPointFor(creator.id, creator.latitude, creator.longitude)

            val marker = Marker(map).apply {
                position = geoPoint
                title = creator.fullName
                relatedObject = item

                // Use default marker icon style
                icon = ContextCompat.getDrawable(requireContext(), org.osmdroid.library.R.drawable.marker_default)

                setOnMarkerClickListener { m, _ ->
                    val creatorResponse = m.relatedObject as? CreatorDistanceResponse
                    if (creatorResponse != null) {
                        showQuickViewCard(creatorResponse)
                    }
                    true
                }
            }

            map.overlays.add(marker)
            markersMap[creator.id] = marker
        }

        // Keep the user centred so nearby markers stay in context
        map.controller.animateTo(GeoPoint(userLat, userLon))
        map.invalidate()
    }

    /**
     * Resolve a creator's map position. Prefers the real coordinates returned by
     * the backend; when a creator has none, falls back to a *stable* offset around
     * the user derived from the creator id so the marker doesn't jump on redraws.
     */
    private fun geoPointFor(id: String, lat: Double?, lon: Double?): GeoPoint {
        if (lat != null && lon != null) {
            return GeoPoint(lat, lon)
        }
        val seed = id.hashCode()
        val latOffset = ((seed % 1000) / 1000.0 - 0.5) * 0.05
        val lonOffset = (((seed / 1000) % 1000) / 1000.0 - 0.5) * 0.05
        return GeoPoint(userLat + latOffset, userLon + lonOffset)
    }

    private fun showQuickViewCard(item: CreatorDistanceResponse) {
        val creator = item.creator
        val quickViewBinding = binding.cardCreatorQuickView

        quickViewBinding.textName.text = creator.fullName
        quickViewBinding.textPrice.text = "${creator.hourlyRate.toIndianPrice()}/hr"
        quickViewBinding.textRating.text = String.format(Locale.US, "%.1f", creator.avgRating)
        quickViewBinding.textReviewsCount.text = "(${creator.totalReviews})"
        
        // Format distance using our location utils
        quickViewBinding.textDistance.text = LocationUtils.formatDistance(item.distanceKm)

        // Set availability status indicators
        val isAvailable = creator.availabilityStatus.lowercase(Locale.US) == "available"
        quickViewBinding.viewOnlineDot.visibility = if (isAvailable) View.VISIBLE else View.INVISIBLE
        quickViewBinding.textAvailability.visibility = if (isAvailable) View.VISIBLE else View.INVISIBLE

        // Specialty label
        quickViewBinding.textSpecialty.text = creator.email.split("@").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Photographer"

        // Load avatar profile
        quickViewBinding.imageAvatar.load(creator.profilePicUrl.toFullUrl()) {
            crossfade(true)
            transformations(CircleCropTransformation())
            placeholder(R.drawable.ic_profile)
            error(R.drawable.ic_profile)
        }

        // Show the preview card
        quickViewBinding.root.visibility = View.VISIBLE

        // Click handles to creator profile detail navigation — wrapped for safety
        quickViewBinding.root.setOnClickListener {
            try {
                val bundle = Bundle().apply {
                    putString("creator_id", creator.id)
                }
                findNavController().navigate(R.id.creatorProfileFragment, bundle)
            } catch (e: IllegalArgumentException) {
                Timber.e(e, "Navigation to creator profile failed")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.map.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.map.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
