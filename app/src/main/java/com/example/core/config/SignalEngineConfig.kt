package com.example.core.config

/**
 * Immutable configuration for the QtY 64 BTC quantitative signal engine.
 *
 * @param symbol Target trading pair, e.g. "BTC/USD"
 * @param timeframeMinutes Decision cycle resolution in minutes (Kalshi contract window: 15m)
 * @param minHistoryCandles Minimum complete historical candles required for valid feature extraction
 * @param staleDataTimeoutMs Maximum allowable age of market snapshot before data is declared stale
 * @param minExpiryBufferSeconds Minimum time in seconds remaining before contract expiry to emit trade signals
 * @param confidenceThreshold Minimum model confidence score [0.0 - 1.0] required to emit YES or NO
 * @param maxSpreadUsd Maximum allowable market spread in USD before spread risk filter triggers
 * @param kalshiSeriesTicker Series ticker identifier on Kalshi (e.g. KXBTC15M)
 */
data class SignalEngineConfig(
  val symbol: String = "BTC/USD",
  val timeframeMinutes: Int = 15,
  val minHistoryCandles: Int = 60,
  val staleDataTimeoutMs: Long = 30_000L,
  val minExpiryBufferSeconds: Long = 60L,
  val confidenceThreshold: Double = 0.65,
  val maxSpreadUsd: Double = 50.0,
  val kalshiSeriesTicker: String = "KXBTC15M"
) {
  init {
    require(symbol.isNotBlank()) { "Symbol must not be blank" }
    require(timeframeMinutes > 0) { "Timeframe must be positive" }
    require(minHistoryCandles >= 10) { "Minimum history candles must be >= 10" }
    require(staleDataTimeoutMs > 0) { "Stale data timeout must be positive" }
    require(minExpiryBufferSeconds >= 0) { "Min expiry buffer must be non-negative" }
    require(confidenceThreshold in 0.5..1.0) { "Confidence threshold must be between 0.5 and 1.0" }
    require(maxSpreadUsd > 0) { "Max spread must be positive" }
  }
}
