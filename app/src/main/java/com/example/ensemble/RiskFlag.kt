package com.example.ensemble

/**
 * Quant risk flags that can prevent signal emission or force NO_TRADE.
 * Implements a fail-closed safety-first architecture.
 */
enum class RiskFlag(val label: String, val description: String) {
  FEED_DISCONNECTED("FEED_DISCONNECTED", "Market data feed is not streaming or uninitialized"),
  INSUFFICIENT_HISTORY("INSUFFICIENT_HISTORY", "Candle history length below minimum required lookback"),
  STALE_MARKET_DATA("STALE_MARKET_DATA", "Market snapshot timestamp exceeds staleness threshold"),
  SPREAD_EXCEEDS_THRESHOLD("SPREAD_EXCEEDS_THRESHOLD", "Spot bid-ask spread exceeds maximum risk tolerance"),
  EXPIRY_WINDOW_CLOSED("EXPIRY_WINDOW_CLOSED", "Remaining contract lifetime is below safety buffer"),
  LOW_CONFIDENCE("LOW_CONFIDENCE", "Model prediction confidence is below decision threshold"),
  MODEL_PREDICTION_PENDING("MODEL_PREDICTION_PENDING", "Mathematical models have not yet emitted trained outputs"),
  IRREGULAR_PRICE_DATA("IRREGULAR_PRICE_DATA", "Detected non-finite or negative price data values")
}
