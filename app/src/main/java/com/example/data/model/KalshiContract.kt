package com.example.data.model

enum class KalshiContractStatus {
  ACTIVE,
  CLOSED,
  SETTLED
}

/**
 * Representation of a Kalshi 15-Minute Bitcoin event contract.
 * Event rule: "Will BTC price be above [targetStrike] at [expirationTimestampMs]?"
 */
data class KalshiContract(
  val ticker: String,
  val seriesTicker: String = "KXBTC15M",
  val targetStrike: Double,
  val openTimestampMs: Long,
  val expirationTimestampMs: Long,
  val latestYesBid: Double? = null,
  val latestYesAsk: Double? = null,
  val latestNoBid: Double? = null,
  val latestNoAsk: Double? = null,
  val openInterest: Long = 0,
  val volume: Long = 0,
  val status: KalshiContractStatus = KalshiContractStatus.ACTIVE
) {
  init {
    require(ticker.isNotBlank()) { "Ticker must not be blank" }
    require(targetStrike > 0) { "Target strike must be positive" }
    require(expirationTimestampMs > openTimestampMs) { "Expiration must be after open" }
  }

  fun isExpired(nowMs: Long = System.currentTimeMillis()): Boolean = nowMs >= expirationTimestampMs

  fun remainingSeconds(nowMs: Long = System.currentTimeMillis()): Long {
    val diff = expirationTimestampMs - nowMs
    return if (diff > 0) diff / 1000 else 0
  }
}
