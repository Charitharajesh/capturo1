package com.capturo.app.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import timber.log.Timber

/**
 * Utility class to dynamically verify network and internet connection status.
 * Helps direct repository layers between loading from local Room database cache
 * and fetching from the online API.
 */
object NetworkUtils {

    enum class NetworkType {
        WIFI,
        CELLULAR,
        OTHER,
        NONE
    }

    /**
     * Checks if there is any active internet connection (Wi-Fi, Cellular, or Ethernet).
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connectivityManager == null) {
            Timber.e("ConnectivityManager is null")
            return false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            return activeNetworkInfo != null && activeNetworkInfo.isConnected
        }
    }

    /**
     * Retrieves the specific type of the active internet connection.
     */
    fun getNetworkType(context: Context): NetworkType {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return NetworkType.NONE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val activeNetwork = connectivityManager.activeNetwork ?: return NetworkType.NONE
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return NetworkType.NONE

            return when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.OTHER
                else -> NetworkType.NONE
            }
        } else {
            @Suppress("DEPRECATION")
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            if (activeNetworkInfo == null || !activeNetworkInfo.isConnected) {
                return NetworkType.NONE
            }
            @Suppress("DEPRECATION")
            return when (activeNetworkInfo.type) {
                ConnectivityManager.TYPE_WIFI -> NetworkType.WIFI
                ConnectivityManager.TYPE_MOBILE -> NetworkType.CELLULAR
                ConnectivityManager.TYPE_ETHERNET -> NetworkType.OTHER
                else -> NetworkType.NONE
            }
        }
    }

    /**
     * Helper to verify if the active network is Wi-Fi.
     */
    fun isWifiConnected(context: Context): Boolean {
        return getNetworkType(context) == NetworkType.WIFI
    }

    /**
     * Helper to verify if the active network is Cellular.
     */
    fun isCellularConnected(context: Context): Boolean {
        return getNetworkType(context) == NetworkType.CELLULAR
    }
}
