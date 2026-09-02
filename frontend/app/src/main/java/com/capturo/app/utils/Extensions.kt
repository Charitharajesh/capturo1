package com.capturo.app.utils

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import coil.load
import coil.transform.CircleCropTransformation
import com.capturo.app.R
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Reusable Kotlin extension functions used across the Capturo application.
 * Aimed at reducing boilerplate within Fragments, Activities, and ViewModels.
 */

// ─── View Extensions ─────────────────────────────────────────────────────────

fun View.show() {
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.INVISIBLE
}

fun View.gone() {
    visibility = View.GONE
}

// ─── Context & Fragment Extensions ───────────────────────────────────────────

fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun Fragment.hideKeyboard() {
    val view = activity?.currentFocus
    if (view != null) {
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }
}

// ─── Image Extensions ────────────────────────────────────────────────────────

fun String?.toFullUrl(): String? {
    if (this == null) return null
    if (this.startsWith("http://") || this.startsWith("https://")) return this
    val baseUrlHost = Constants.BASE_URL.substringBefore("/api/v1")
    return if (this.startsWith("/")) {
        "$baseUrlHost$this"
    } else {
        "$baseUrlHost/$this"
    }
}

fun ImageView.loadCircular(url: String?) {
    load(url.toFullUrl()) {
        crossfade(true)
        crossfade(200)
        transformations(CircleCropTransformation())
        placeholder(R.drawable.shape_avatar_circle)
        error(R.drawable.shape_avatar_circle)
    }
}

// ─── Formatting & Parsing Extensions ─────────────────────────────────────────

/**
 * Formats standard Epoch millisecond timestamps into relative user-friendly terms.
 */
fun Long.toRelativeTime(): String {
    return try {
        val zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        val diffSeconds = ChronoUnit.SECONDS.between(zdt, now)
        when {
            diffSeconds < 60           -> "Just now"
            diffSeconds < 3600         -> "${diffSeconds / 60} min ago"
            diffSeconds < 86400        -> "${diffSeconds / 3600}h ago"
            diffSeconds < 172800       -> "Yesterday"
            else                       -> zdt.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH))
        }
    } catch (e: Exception) {
        "Just now"
    }
}

/**
 * Parses and converts standard ISO-8601 string-based API timestamps (e.g. Chat/Booking records)
 * into relative UI times (e.g. 'Just now', '2 min ago', 'Yesterday').
 */
fun String.toRelativeTime(): String {
    if (this.isBlank()) return ""
    return try {
        val instant = Instant.parse(this)
        instant.toEpochMilli().toRelativeTime()
    } catch (e: Exception) {
        try {
            val formatter = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
            val ldt = java.time.LocalDateTime.parse(this, formatter)
            val zdt = ldt.atZone(ZoneId.systemDefault())
            zdt.toInstant().toEpochMilli().toRelativeTime()
        } catch (ex: Exception) {
            this
        }
    }
}

/**
 * Formats a Double into Indian Standard currency style (e.g. ₹1,999 or ₹1,999.50).
 */
fun Double.toIndianPrice(): String {
    return try {
        if (this % 1.0 == 0.0) {
            "₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(this.toLong())}"
        } else {
            val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
            formatter.format(this)
        }
    } catch (e: Exception) {
        "₹${this.toLong()}"
    }
}

/**
 * Helper to safely format a numerical price String into Indian Currency format.
 */
fun String.toFormattedPrice(): String {
    val doubleValue = this.toDoubleOrNull() ?: return this
    return doubleValue.toIndianPrice()
}

// ─── Input & Form Validation Extensions ──────────────────────────────────────

fun String.isValidEmail(): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

/**
 * Extension wrapping EditText with debounced callbacks to execute validations during inputs.
 */
fun EditText.afterTextChangedDebounced(coroutineScope: CoroutineScope, delayMs: Long = 400L, action: (String) -> Unit) {
    var job: Job? = null
    addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        
        override fun afterTextChanged(editable: Editable?) {
            job?.cancel()
            job = coroutineScope.launch {
                delay(delayMs)
                action(editable?.toString().orEmpty())
            }
        }
    })
}

/**
 * Alias helper corresponding directly to standard dynamic TextChange debouncers.
 */
fun EditText.onTextChange(coroutineScope: CoroutineScope, delayMs: Long = 400L, action: (String) -> Unit) {
    afterTextChangedDebounced(coroutineScope, delayMs, action)
}

/**
 * Extension to display a styled Snackbar that automatically anchors to R.id.layoutBottomCta if present.
 */
fun Fragment.showSnackbar(message: String, duration: Int = Snackbar.LENGTH_SHORT) {
    view?.let { root ->
        Snackbar.make(root, message, duration).apply {
            val bottomCta = root.findViewById<View>(R.id.layoutBottomCta)
            if (bottomCta != null) {
                anchorView = bottomCta
            }
            show()
        }
    }
}
