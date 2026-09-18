package com.example.data.features

import com.example.data.model.BtcMarketObservation
import com.example.data.model.DataQualityGrade
import kotlin.math.sqrt

/**
 * Reusable rolling-window mathematical utility for calculating BTC quantitative features
 * (returns, price change, volatility, mean, min, max) strictly from observations
 * available at or before the evaluation timestamp.
 */
object BtcFeatureCalculator {

  const val FEATURE_PRICE_MEAN = "price_mean"
  const val FEATURE_PRICE_VOLATILITY = "price_volatility"
  const val FEATURE_PRICE_CHANGE = "price_change"
  const val FEATURE_LOG_RETURN = "log_return"
  const val FEATURE_PRICE_MIN = "price_min"
  const val FEATURE_PRICE_MAX = "price_max"
  const val FEATURE_ELAPSED_WINDOW_MS = "elapsed_window_ms"
  const val FEATURE_OBSERVATION_COUNT = "observation_count"

  fun computeFeatures(
    observations: List<BtcMarketObservation>,
    evaluationTimestampMs: Long,
    sourceWindowMs: Long,
    minRequiredObservations: Int = 5
  ): BtcFeatureVector {
    // 1. Enforce strict temporal filtering: exclude any future observations (> evaluationTimestampMs)
    val validObservations = observations.filter { 
      it.timestampMs <= evaluationTimestampMs && it.qualityStatus.isUsable 
    }.sortedBy { it.timestampMs }

    // 2. Filter by source rolling window (e.g. last sourceWindowMs relative to evaluation time)
    val windowStartTime = evaluationTimestampMs - sourceWindowMs
    val windowObservations = validObservations.filter { it.timestampMs >= windowStartTime }

    val sufficiency = windowObservations.size >= minRequiredObservations
    val quality = if (sufficiency && windowObservations.isNotEmpty()) DataQualityGrade.VALID else DataQualityGrade.MISSING

    if (!sufficiency || windowObservations.isEmpty()) {
      return BtcFeatureVector(
        featureTimestampMs = evaluationTimestampMs,
        evaluationTimestampMs = evaluationTimestampMs,
        sourceWindowMs = sourceWindowMs,
        dataSufficiency = false,
        qualityStatus = quality,
        values = emptyMap()
      )
    }

    val prices = windowObservations.map { it.price }
    val count = prices.size.toDouble()
    val mean = prices.average()

    // Variance & Volatility (Standard Deviation)
    val variance = if (prices.size > 1) {
      prices.sumOf { (it - mean) * (it - mean) } / (prices.size - 1)
    } else {
      0.0
    }
    val volatility = sqrt(variance)

    // Price change (latest in window vs earliest in window)
    val earliestPrice = prices.first()
    val latestPrice = prices.last()
    val priceChange = latestPrice - earliestPrice

    // Log return between earliest and latest
    val logReturn = if (earliestPrice > 0.0 && latestPrice > 0.0) {
      kotlin.math.ln(latestPrice / earliestPrice)
    } else {
      0.0
    }

    val minPrice = prices.minOrNull() ?: latestPrice
    val maxPrice = prices.maxOrNull() ?: latestPrice
    val actualWindowMs = (windowObservations.last().timestampMs - windowObservations.first().timestampMs).toDouble()

    val values = mapOf(
      FEATURE_PRICE_MEAN to mean,
      FEATURE_PRICE_VOLATILITY to volatility,
      FEATURE_PRICE_CHANGE to priceChange,
      FEATURE_LOG_RETURN to logReturn,
      FEATURE_PRICE_MIN to minPrice,
      FEATURE_PRICE_MAX to maxPrice,
      FEATURE_ELAPSED_WINDOW_MS to actualWindowMs,
      FEATURE_OBSERVATION_COUNT to count
    )

    return BtcFeatureVector(
      featureTimestampMs = windowObservations.last().timestampMs,
      evaluationTimestampMs = evaluationTimestampMs,
      sourceWindowMs = sourceWindowMs,
      dataSufficiency = true,
      qualityStatus = DataQualityGrade.VALID,
      values = values
    )
  }
}
