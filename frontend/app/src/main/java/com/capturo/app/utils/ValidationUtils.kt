package com.capturo.app.utils

object ValidationUtils {
    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= 8
    }

    fun isValidPhone(phone: String): Boolean {
        return phone.length >= 10 && phone.all { it.isDigit() }
    }

    fun isNotEmpty(value: String): Boolean {
        return value.trim().isNotEmpty()
    }

    fun isMinLength(value: String, minLength: Int): Boolean {
        return value.trim().length >= minLength
    }
}
