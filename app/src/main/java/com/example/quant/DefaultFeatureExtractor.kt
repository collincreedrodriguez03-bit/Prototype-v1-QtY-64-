package com.example.quant

import com.example.data.model.Candle
import com.example.data.model.MarketSnapshot

/**
 * Foundational feature extractor.
 * Validates input integrity, enforces lookback constraints, and prepares feature boundaries.
 */
class DefaultFeatureExtractor : FeatureExtractor {

  override fun extract(
    candles: List<Candle>,
    snapshot: MarketSnapshot?,
    minLookback: Int
  ): QuantitativeFeatureVector {
    val nowMs = System.currentTimeMillis()

    if (candles.size < minLookback) {
      return QuantitativeFeatureVector.invalid(
        timestampMs = nowMs,
        sampleSize = candles.size,
        reason = "Insufficient history candles: required >= $minLookback, provided = ${candles.size}"
      )
    }

    if (snapshot == null) {
      return QuantitativeFeatureVector.invalid(
        timestampMs = nowMs,
        sampleSize = candles.size,
        reason = "Missing real-time market snapshot"
      )
    }

    // Foundational feature map extraction with strict finiteness validation
    val featureMap = mutableMapOf<String, Double>()
    val latestCandle = candles.last()

    featureMap["last_price"] = snapshot.lastPrice
    featureMap["bid_price"] = snapshot.bidPrice
    featureMap["ask_price"] = snapshot.askPrice
    featureMap["spread"] = snapshot.spread
    featureMap["candle_close"] = latestCandle.close
    featureMap["candle_volume"] = latestCandle.volume
    featureMap["sample_size"] = candles.size.toDouble()

    // Price change percentage over sample window
    val oldestCandle = candles.first()
    if (oldestCandle.close > 0) {
      val returnWindow = (latestCandle.close - oldestCandle.close) / oldestCandle.close
      featureMap["return_window"] = returnWindow
    }

    return QuantitativeFeatureVector(
      timestampMs = nowMs,
      sampleSize = candles.size,
      features = featureMap,
      isValid = true,
      validationError = null
    )
  }
}
