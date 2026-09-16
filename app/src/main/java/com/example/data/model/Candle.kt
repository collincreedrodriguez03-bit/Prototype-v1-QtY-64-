package com.example.data.model

/**
 * Standard OHLCV Candlestick primitive for quantitative calculations.
 *
 * @param timestampMs Epoch millisecond marking start of the candle period
 * @param open Opening price in USD
 * @param high Highest traded price during the interval
 * @param low Lowest traded price during the interval
 * @param close Closing price in USD
 * @param volume Traded volume (base asset units, e.g. BTC)
 * @param isComplete True if candle period has closed; false if still forming
 */
data class Candle(
  val timestampMs: Long,
  val open: Double,
  val high: Double,
  val low: Double,
  val close: Double,
  val volume: Double,
  val isComplete: Boolean = true
) {
  init {
    require(timestampMs > 0) { "Timestamp must be positive" }
    require(open >= 0 && high >= 0 && low >= 0 && close >= 0) { "Prices must be non-negative" }
    require(high >= low) { "High ($high) cannot be less than Low ($low)" }
    require(volume >= 0) { "Volume must be non-negative" }
  }

  val isBullish: Boolean get() = close >= open
  val range: Double get() = high - low
  val bodySize: Double get() = Math.abs(close - open)
}
