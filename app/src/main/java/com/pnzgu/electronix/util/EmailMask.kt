package com.pnzgu.electronix.util

/** e.g. `ab***@example.com` — keeps domain visible, masks local part. */
fun maskEmailForDisplay(email: String): String {
    val trimmed = email.trim()
    val at = trimmed.indexOf('@')
    if (at <= 0) return "***"
    val local = trimmed.substring(0, at)
    val domain = trimmed.substring(at)
    if (local.isEmpty()) return "***$domain"
    val prefix = when {
        local.length >= 2 -> local.take(2)
        else -> local.take(1)
    }
    return "$prefix***$domain"
}
