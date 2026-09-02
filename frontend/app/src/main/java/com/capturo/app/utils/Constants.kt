package com.capturo.app.utils

object Constants {
    // Current active server IP address (PC's Wi-Fi LAN IP; phone must be on the same Wi-Fi)
    const val BASE_URL = "http://192.168.1.15:8000/api/v1/"
    
    // Razorpay Integration Key
    const val RAZORPAY_KEY_ID = "rzp_test_capturo_key"
    
    // Offline Cache Room Database Configuration
    const val DATABASE_NAME = "capturo_offline_cache_db"
    
    // Network Connection Constraints
    const val TIMEOUT_SECONDS = 30L
}
