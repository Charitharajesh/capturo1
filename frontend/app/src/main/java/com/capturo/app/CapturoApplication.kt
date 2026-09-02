package com.capturo.app

import android.app.Application
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class CapturoApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()

        // Initialise Timber logging for debug builds
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Initialise Firebase
        try {
            FirebaseApp.initializeApp(this)
            Timber.d("FirebaseApp initialised successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialise FirebaseApp")
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            // Configure memory cache
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // Use 25% of available RAM
                    .build()
            }
            // Configure disk cache
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024L * 1024L) // 50MB disk cache
                    .build()
            }
            // Set dark-themed placeholder and error drawables (Default placeholder #1A0033 as per rules)
            .placeholder(ColorDrawable(Color.parseColor("#1A0033")))
            .error(ColorDrawable(Color.parseColor("#0D0020")))
            // Cache policies
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .crossfade(true)
            .build()
    }
}
