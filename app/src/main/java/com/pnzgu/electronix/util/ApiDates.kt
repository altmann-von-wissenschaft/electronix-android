package com.pnzgu.electronix.util

import java.time.Instant
import java.time.ZoneOffset

/** Midnight UTC for the calendar day of [epochMillis] (interpreted in UTC). */
fun millisToUtcDateString(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneOffset.UTC).toLocalDate().toString()

fun parseApiDateTimeToMillis(iso: String): Long =
    try {
        java.time.OffsetDateTime.parse(iso).toInstant().toEpochMilli()
    } catch (_: Exception) {
        try {
            java.time.Instant.parse(iso).toEpochMilli()
        } catch (_: Exception) {
            0L
        }
    }
