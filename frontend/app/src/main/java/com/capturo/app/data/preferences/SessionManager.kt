package com.capturo.app.data.preferences

import android.content.SharedPreferences
import com.capturo.app.data.model.response.AuthResponse
import javax.inject.Inject
import javax.inject.Singleton

enum class UserRole {
    ATTENDEE, CREATOR, ADMIN, UNKNOWN;

    companion object {
        fun fromString(role: String?): UserRole {
            return when (role?.lowercase()) {
                "attendee" -> ATTENDEE
                "creator" -> CREATOR
                "admin" -> ADMIN
                else -> UNKNOWN
            }
        }
    }
}

@Singleton
class SessionManager @Inject constructor(
    private val prefs: SharedPreferences
) {
    fun saveSession(authResponse: AuthResponse) {
        prefs.edit().apply {
            putString("access_token", authResponse.accessToken)
            putString("refresh_token", authResponse.refreshToken)
            putString("user_id", authResponse.user.id)
            putString("role", authResponse.user.role)
            putString("full_name", authResponse.user.fullName)
            putString("profile_pic_url", authResponse.user.profilePicUrl)
            apply()
        }
    }

    fun getAccessToken(): String? = prefs.getString("access_token", null)
    fun getRefreshToken(): String? = prefs.getString("refresh_token", null)
    fun getUserId(): String? = prefs.getString("user_id", null)
    fun getFullName(): String? = prefs.getString("full_name", null)
    fun getProfilePicUrl(): String? = prefs.getString("profile_pic_url", null)
    
    fun getUserRole(): UserRole {
        val roleStr = prefs.getString("role", null)
        return UserRole.fromString(roleStr)
    }

    /**
     * Overrides the active role locally so the user can switch the whole app
     * between Client (attendee) and Event Creator modes via the home toggle.
     */
    fun setUserRole(role: UserRole) {
        val roleStr = when (role) {
            UserRole.CREATOR -> "creator"
            UserRole.ADMIN -> "admin"
            else -> "attendee"
        }
        prefs.edit().putString("role", roleStr).apply()
    }

    fun saveTempSpecializations(specs: Set<String>) {
        prefs.edit().putStringSet("temp_specs", specs).apply()
    }

    fun getTempSpecializations(): Set<String>? {
        return prefs.getStringSet("temp_specs", null)
    }

    fun clearTempSpecializations() {
        prefs.edit().remove("temp_specs").apply()
    }

    fun saveTempLocation(lat: Double, lon: Double) {
        prefs.edit().apply {
            putFloat("temp_lat", lat.toFloat())
            putFloat("temp_lon", lon.toFloat())
            apply()
        }
    }

    fun getTempLatitude(): Double? {
        val lat = prefs.getFloat("temp_lat", -999f)
        return if (lat == -999f) null else lat.toDouble()
    }

    fun getTempLongitude(): Double? {
        val lon = prefs.getFloat("temp_lon", -999f)
        return if (lon == -999f) null else lon.toDouble()
    }

    fun clearTempLocation() {
        prefs.edit().remove("temp_lat").remove("temp_lon").apply()
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean = getAccessToken() != null
}
