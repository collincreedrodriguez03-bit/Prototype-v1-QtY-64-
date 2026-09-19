package com.example.data.features

import com.example.data.model.BtcMarketObservation
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Volatility regime classification for QtY Volatility engine.
 * Does not represent directional bias or trading probability.
 */
enum class VolatilityRegime {
  LOW,
  NORMAL,
  HIGH,
  EXTREME,
  UNKNOWN
}

/**
 * Structured evidence output from the Volatility quantitative model.
 * All formulas, lookbacks, return definitions, and variance metrics are explicitly documented.
 */
data class VolatilityEvidence(
  val regime: VolatilityRegime,
  val realizedVolatility: Double, // Realized volatility (standard deviation of log returns)
  val shortTermVolatility: Double,
  val longTermVolatility: Double,
  val volatilityRatio: Double, // Short / Long volatility ratio (expansion / contraction indicator)
  val componentMeasurements: Map<String, Double>,
  val dataSufficiency: Boolean,
  val evaluationTimestampMs: Long,
  val diagnosticMetadata: Map<String, String>
) {
  val isUsable: Boolean get() = dataSufficiency && !realizedVolatility.isNaN() && !realizedVolatility.isInfinite()
}

/**
 * Independent Volatility Quantitative Engine for QtY Phase 3.
 * 
 * ## Mathematical Specification & Formulas:
 * 1. **Log Returns (r_i)**:
 *    - Formula: `ln(Price_i / Price_{i-1})`
 *    - Units: Dimensionless log return.
 * 
 * 2. **Realized Volatility (sigma)**:
 *    - Formula: Sample standard deviation of log returns: `sqrt(sum((r_i - mean(r))^2) / (N - 1))`
 *    - Required Sample Count: Minimum 5 observations (4 return periods).
 * 
 * 3. **Multi-Window Volatility Comparison**:
 *    - Short-term realized volatility (recent window).
 *    - Long-term realized volatility (extended window).
 *    - Volatility Ratio: `ShortVolatility / LongVolatility` (> 1.0 indicates expansion, < 1.0 indicates contraction).
 * 
 * 4. **Regime Classification**:
 *    - LOW: Realized Volatility < threshold (e.g. 0.001)
 *    - NORMAL: Moderate volatility
 *    - HIGH: Elevated volatility
 *    - EXTREME: Severe volatility expansion
 */
object VolatilityQuantitativeEngine {

  fun evaluateVolatility(
    observations: List<BtcMarketObservation>,
    evaluationTimestampMs: Long,
    shortWindowSampleCount: Int = 5,
    longWindowSampleCount: Int = 15,
    minRequiredObservations: Int = 5
  ): VolatilityEvidence {
    // 1. Enforce strict temporal separation: exclude future observations
    val validObservations = observations.filter {
      it.timestampMs <= evaluationTimestampMs && it.qualityStatus.isUsable
    }.sortedBy { it.timestampMs }

    val sufficiency = validObservations.size >= minRequiredObservations
    if (!sufficiency || validObservations.size < 2) {
      return VolatilityEvidence(
        regime = VolatilityRegime.UNKNOWN,
        realizedVolatility = 0.0,
        shortTermVolatility = 0.0,
        longTermVolatility = 0.0,
        volatilityRatio = 1.0,
        componentMeasurements = emptyMap(),
        dataSufficiency = false,
        evaluationTimestampMs = evaluationTimestampMs,
        diagnosticMetadata = mapOf("reason" to "Insufficient observations for volatility evaluation: count=${validObservations.size}, required=$minRequiredObservations")
      )
    }

    val prices = validObservations.map { it.price }
    val logReturns = mutableListOf<Double>()
    for (i in 1 until prices.size) {
      val pPrev = prices[i - 1]
      val pCurr = prices[i]
      if (pPrev > 0.0 && pCurr > 0.0) {
        logReturns.add(ln(pCurr / pPrev))
      } else {
        logReturns.add(0.0)
      }
    }

    if (logReturns.isEmpty()) {
      return VolatilityEvidence(
        regime = VolatilityRegime.UNKNOWN,
        realizedVolatility = 0.0,
        shortTermVolatility = 0.0,
        longTermVolatility = 0.0,
        volatilityRatio = 1.0,
        componentMeasurements = emptyMap(),
        dataSufficiency = false,
        evaluationTimestampMs = evaluationTimestampMs,
        diagnosticMetadata = mapOf("reason" to "Valid log returns list is empty")
      )
    }

    // Long term realized volatility (all available valid returns)
    val longVol = calculateStdDev(logReturns)

    // Short term realized volatility (recent returns)
    val shortReturns = logReturns.takeLast(minOf(shortWindowSampleCount, logReturns.size))
    val shortVol = calculateStdDev(shortReturns)

    val volRatio = if (longVol > 0.0) shortVol / longVol else 1.0

    val regime = when {
      longVol < 0.0005 -> VolatilityRegime.LOW
      volRatio > 1.8 -> VolatilityRegime.EXTREME
      longVol > 0.005 || volRatio > 1.3 -> VolatilityRegime.HIGH
      else -> VolatilityRegime.NORMAL
    }

    val components = mapOf(
      "long_term_volatility" to longVol,
      "short_term_volatility" to shortVol,
      "volatility_ratio" to volRatio,
      "return_count" to logReturns.size.toDouble()
    )

    val diagnostics = mapOf(
      "engine" to "VolatilityQuantitativeEngine",
      "sample_count" to validObservations.size.toString(),
      "evaluation_timestamp" to evaluationTimestampMs.toString()
    )

    return VolatilityEvidence(
      regime = regime,
      realizedVolatility = longVol,
      shortTermVolatility = shortVol,
      longTermVolatility = longVol,
      volatilityRatio = volRatio,
      componentMeasurements = components,
      dataSufficiency = true,
      evaluationTimestampMs = evaluationTimestampMs,
      diagnosticMetadata = diagnostics
    )
  }

  private fun calculateStdDev(values: List<Double>): Double {
    if (values.size < 2) return 0.0
    val mean = values.average()
    val varianceSum = values.sumOf { (it - mean) * (it - mean) }
    val variance = varianceSum / (values.size - 1)
    return if (variance > 0.0) sqrt(variance) else 0.0
  }
}
