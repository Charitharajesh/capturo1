package com.capturo.app.premium.ui

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.capturo.app.R
import com.capturo.app.databinding.FragmentPremiumTryBinding
import com.capturo.app.premium.DemoData
import com.capturo.app.premium.Photographer
import com.capturo.app.premium.PremiumProfileActivity
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

class TryFragment : Fragment() {

    private var _binding: FragmentPremiumTryBinding? = null
    private val binding get() = _binding!!

    private val nearby: List<Photographer> by lazy {
        DemoData.photographers.sortedBy { DemoData.distanceKm(it) }
    }

    // Bangalore city centre — where the seeded demo creators live. Used as the
    // map's home camera and as the anchor for any creator missing coordinates.
    private val centerLat = 12.9716
    private val centerLon = 77.5946

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // osmdroid needs a user agent set before the MapView inflates its tiles.
        Configuration.getInstance().userAgentValue = requireContext().packageName
        _binding = FragmentPremiumTryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerNearby.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerNearby.adapter = NearbyAdapter(nearby) { p -> openProfile(p.id) }

        setupMap()
    }

    private fun setupMap() {
        val map = binding.map
        map.setTileSource(TileSourceFactory.MAPNIK)   // real OpenStreetMap tiles
        map.setMultiTouchControls(true)
        map.controller.setZoom(12.0)
        map.controller.setCenter(GeoPoint(centerLat, centerLon))

        // "You are here" marker at the map centre.
        Marker(map).apply {
            position = GeoPoint(centerLat, centerLon)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            title = "You are here"
            icon = ContextCompat.getDrawable(requireContext(), R.drawable.bg_dot_active)
            map.overlays.add(this)
        }

        val pinIcon: Drawable? = ContextCompat.getDrawable(requireContext(), R.drawable.ic_location_pin)
            ?.mutate()?.also {
                it.setTint(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
            }

        nearby.forEach { p ->
            val point = geoPointFor(p)
            Marker(map).apply {
                position = point
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = p.name
                subDescription = p.specialties
                icon = pinIcon
                setOnMarkerClickListener { _, _ ->
                    openProfile(p.id)
                    true
                }
                map.overlays.add(this)
            }
        }
        map.invalidate()
    }

    /**
     * Real coordinates from the backend when the creator has them; otherwise a
     * stable offset around the city centre derived from the id so pins don't
     * jump between redraws.
     */
    private fun geoPointFor(p: Photographer): GeoPoint {
        val lat = p.lat; val lon = p.lon
        if (lat != null && lon != null) return GeoPoint(lat, lon)
        val seed = p.id.hashCode()
        val latOffset = ((seed % 1000) / 1000.0 - 0.5) * 0.08
        val lonOffset = (((seed / 1000) % 1000) / 1000.0 - 0.5) * 0.08
        return GeoPoint(centerLat + latOffset, centerLon + lonOffset)
    }

    private fun openProfile(id: String) {
        startActivity(
            Intent(requireContext(), PremiumProfileActivity::class.java)
                .putExtra(PremiumProfileActivity.EXTRA_ID, id)
        )
    }

    override fun onResume() {
        super.onResume()
        _binding?.map?.onResume()
    }

    override fun onPause() {
        super.onPause()
        _binding?.map?.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
