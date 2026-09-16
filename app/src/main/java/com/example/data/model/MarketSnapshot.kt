package com.example.data.model

/**
 * Point-in-time spot market snapshot for BTC/USD.
 */
data class MarketSnapshot(
  val timestampMs: Long,
  val symbol: String,
  val lastPrice: Double,
  val bidPrice: Double,
  val askPrice: Double,
  val volume24h: Double,
  val high24h: Double,
  val low24h: Double
) {
  init {
    require(timestampMs > 0) { "Timestamp must be positive" }
    require(symbol.isNotBlank()) { "Symbol must not be blank" }
    require(lastPrice >= 0 && bidPrice >= 0 && askPrice >= 0) { "Prices must be non-negative" }
  }

  val spread: Double get() = askPrice - bidPrice
  val midPrice: Double get() = (bidPrice + askPrice) / 2.0
}
