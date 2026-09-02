package com.capturo.app.utils

import android.animation.ValueAnimator
import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.capturo.app.R
import java.text.NumberFormat
import java.util.Locale

object FormattingUtils {

    // Format: Double -> "₹1,999" (drop paise for whole numbers)
    fun formatPrice(amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            "₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(amount.toLong())}"
        } else {
            val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
            formatter.format(amount)
        }
    }

    // Format rate: price in colorAccent, /hr in colorTextSecondary
    fun formatPriceRate(amount: Double, context: Context): SpannableString {
        val priceStr = formatPrice(amount)
        val rateStr = "$priceStr/hr"
        val span = SpannableString(rateStr)
        
        val accentColor = ContextCompat.getColor(context, R.color.colorAccent)
        val secondaryColor = ContextCompat.getColor(context, R.color.colorTextSecondary)
        
        span.setSpan(
            ForegroundColorSpan(accentColor),
            0, priceStr.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        span.setSpan(
            ForegroundColorSpan(secondaryColor),
            priceStr.length, rateStr.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return span
    }

    // Format: Double km -> "1.2 km"
    fun formatDistance(km: Double): String {
        return if (km < 10) String.format(Locale.ENGLISH, "%.1f km", km)
        else "${km.toInt()} km"
    }

    // Format rating: ★ 4.9 (234)
    fun formatRating(rating: Double, count: Int, context: Context): SpannableString {
        val ratingStr = String.format(Locale.ENGLISH, "%.1f", rating)
        val fullStr = "★ $ratingStr ($count)"
        val span = SpannableString(fullStr)
        
        val goldColor = ContextCompat.getColor(context, R.color.colorStar)
        val whiteColor = ContextCompat.getColor(context, R.color.colorTextPrimary)
        val secondaryColor = ContextCompat.getColor(context, R.color.colorTextSecondary)
        
        // "★" -> Gold
        span.setSpan(
            ForegroundColorSpan(goldColor),
            0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        // Rating number -> White
        span.setSpan(
            ForegroundColorSpan(whiteColor),
            2, 2 + ratingStr.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        // "(count)" -> Secondary Light Purple
        span.setSpan(
            ForegroundColorSpan(secondaryColor),
            2 + ratingStr.length + 1, fullStr.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return span
    }

    // Format: Long bytes -> "2.4 GB" / "450 MB"
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1_073_741_824L -> String.format(Locale.ENGLISH, "%.1f GB", bytes / 1_073_741_824.0)
            bytes >= 1_048_576L     -> String.format(Locale.ENGLISH, "%.0f MB", bytes / 1_048_576.0)
            bytes >= 1_024L         -> String.format(Locale.ENGLISH, "%.1f KB", bytes / 1_024.0)
            else                    -> "$bytes B"
        }
    }

    // Format count: 1200 -> "1.2k"
    fun formatCount(count: Int): String {
        return when {
            count >= 1_000_000 -> String.format(Locale.ENGLISH, "%.1fM", count / 1_000_000.0)
            count >= 1_000     -> String.format(Locale.ENGLISH, "%.1fk", count / 1_000.0)
            else               -> count.toString()
        }
    }

    // Animate stats value count-up
    fun animateNumber(textView: TextView, target: Int, prefix: String = "", suffix: String = "") {
        ValueAnimator.ofInt(0, target).apply {
            duration = 800L
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val value = animator.animatedValue as Int
                textView.text = "$prefix${formatCount(value)}$suffix"
            }
            start()
        }
    }

    // Animate earnings money count-up
    fun animateMoney(textView: TextView, target: Double) {
        ValueAnimator.ofFloat(0f, target.toFloat()).apply {
            duration = 800L
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                textView.text = formatPrice((animator.animatedValue as Float).toDouble())
            }
            start()
        }
    }
}
