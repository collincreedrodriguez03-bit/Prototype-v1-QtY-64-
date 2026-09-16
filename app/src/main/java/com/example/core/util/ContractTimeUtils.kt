package com.example.core.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Quant temporal utilities for Kalshi 15-minute interval contracts.
 * 15-minute contract cycles align to :00, :15, :30, :45 of every hour.
 */
object ContractTimeUtils {
  const val FIFTEEN_MINUTES_MS = 15 * 60 * 1000L

  /**
   * Calculates the expiration epoch ms of the current 15-minute window.
   */
  fun getCurrentWindowExpirationMs(nowMs: Long = System.currentTimeMillis()): Long {
    val remainder = nowMs % FIFTEEN_MINUTES_MS
    return nowMs - remainder + FIFTEEN_MINUTES_MS
  }

  /**
   * Calculates the open epoch ms of the current 15-minute window.
   */
  fun getCurrentWindowOpenMs(nowMs: Long = System.currentTimeMillis()): Long {
    val remainder = nowMs % FIFTEEN_MINUTES_MS
    return nowMs - remainder
  }

  /**
   * Calculates remaining time in seconds to contract expiration.
   */
  fun getRemainingSeconds(expirationMs: Long, nowMs: Long = System.currentTimeMillis()): Long {
    val diff = expirationMs - nowMs
    return if (diff > 0) diff / 1000 else 0
  }

  /**
   * Formats remaining time as mm:ss
   */
  fun formatCountdown(remainingSeconds: Long): String {
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
  }

  /**
   * Formats timestamp in UTC HH:mm:ss
   */
  fun formatUtcTime(timestampMs: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.US).apply {
      timeZone = TimeZone.getTimeZone("UTC")
    }
    return sdf.format(Date(timestampMs))
  }
}
