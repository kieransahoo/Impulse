package com.impulse.utils

import android.content.Context
import android.widget.Toast

fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

/**
 * Extracts the first http/https URL from a string.
 * Useful when YouTube/Instagram shares include surrounding text + URL.
 */
fun String.extractUrl(): String {
    val urlRegex = Regex("""https?://[^\s]+""")
    return urlRegex.find(this)?.value ?: this.trim()
}

/**
 * Returns true if this string looks like a valid HTTP URL.
 */
fun String.isValidUrl(): Boolean =
    startsWith("http://") || startsWith("https://")
