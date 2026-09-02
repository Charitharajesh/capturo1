package com.capturo.app.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Modern utility object wrapping FusedLocationProviderClient for retrieving GPS coordinates
 * asynchronously as a suspend function. Provides client-side distance metrics utilizing the
 * Haversine formula.
 */
object LocationUtils {

    // Default Fallback Coordinates (e.g., Chennai / Bangalore Central)
    private const val DEFAULT_LAT = 13.0827
    private const val DEFAULT_LON = 80.2707

    /**
     * Retrieves the device's last known location as a suspend function using FusedLocationProviderClient.
     * Returns null if permissions are not granted, or fallback values in case of service failures.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): LatLng? {
        if (!hasLocationPermission(context)) {
            Timber.w("Location permission not granted.")
            return null
        }

        return try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            val location = fusedLocationClient.lastLocation.await()
            if (location != null) {
                Timber.d("Successfully retrieved last known location: (${location.latitude}, ${location.longitude})")
                LatLng(location.latitude, location.longitude)
            } else {
                Timber.w("FusedLocationProviderClient returned null location. Defaulting to fallback coordinates.")
                LatLng(DEFAULT_LAT, DEFAULT_LON)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error obtaining location from FusedLocationProviderClient. Defaulting to fallback coordinates.")
            LatLng(DEFAULT_LAT, DEFAULT_LON)
        }
    }

    /**
     * Backward-compatible callback version of getCurrentLocation.
     * Delegates internally to the suspend function using the Main dispatchers scope.
     */
    fun getCurrentLocation(context: Context, callback: (lat: Double, lon: Double) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            val location = getCurrentLocation(context)
            if (location != null) {
                callback(location.latitude, location.longitude)
            } else {
                // Fallback to default coordinates if permissions are denied
                callback(DEFAULT_LAT, DEFAULT_LON)
            }
        }
    }

    /**
     * Checks whether location permissions are granted.
     */
    fun hasLocationPermission(context: Context): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocation || coarseLocation
    }

    /**
     * Calculates the client-side Haversine distance in kilometers between two sets of GPS coordinates.
     */
    fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val originLatRad = Math.toRadians(lat1)
        val targetLatRad = Math.toRadians(lat2)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                sin(dLon / 2) * sin(dLon / 2) * cos(originLatRad) * cos(targetLatRad)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadiusKm * c
    }

    /**
     * Formats distance values nicely for UI display on creator cards (e.g. '0.5 km' or '1.2 km').
     */
    fun formatDistance(km: Double): String {
        return String.format(Locale.US, "%.1f km", km)
    }
}

/**
 * Decoupled LatLng class representation replacing Maps SDK version.
 */
data class LatLng(val latitude: Double, val longitude: Double)

