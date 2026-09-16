package com.example.core.config

/**
 * Institutional market specifications for Kalshi 15-Minute Bitcoin Contracts.
 *
 * Kalshi contracts are event contracts structured as:
 * "Will BTC/USD price be above [Strike] at [ExpirationTime]?"
 *
 * Each contract settles strictly to $1.00 (YES) or $0.00 (NO) based on the CF Benchmarks
 * CME CF Bitcoin Real Time Index (BRTI).
 */
object KalshiMarketSpecs {
  const val CONTRACT_SERIES_PREFIX = "KXBTC"
  const val DURATION_MINUTES = 15
  const val CONTRACT_PAYOUT_YES_USD = 1.00
  const val CONTRACT_PAYOUT_NO_USD = 0.00
  const val SETTLEMENT_INDEX = "CME CF Bitcoin Real Time Index (BRTI)"
  const val BASE_ASSET = "BTC"
  const val QUOTE_ASSET = "USD"
  const val TICK_SIZE_CENTS = 1 // $0.01 per contract

  /**
   * Generates standard contract ticker format for a given settlement timestamp.
   * Format example: KXBTC-26SEP16-15M-T1
   */
  fun formatContractTicker(expiryEpochMs: Long, strikeLevel: Double): String {
    return "$CONTRACT_SERIES_PREFIX-15M-${strikeLevel.toInt()}-${expiryEpochMs / 1000}"
  }
}
