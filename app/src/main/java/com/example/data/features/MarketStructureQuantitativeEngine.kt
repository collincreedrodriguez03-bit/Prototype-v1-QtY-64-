package com.example.data.features

import com.example.data.model.BtcMarketObservation

/**
 * Structural regime for Market Structure quantitative engine.
 */
enum class MarketStructureRegime {
  BREAKOUT_HIGH,
  BREAKOUT_LOW,
  RANGE_BOUND,
  UPWARD_TREND,
  DOWNWARD_TREND,
  NEUTRAL
}

/**
 * Structured evidence output from the Market Structure quantitative model.
 */
data class MarketStructureEvidence(
  val regime: MarketStructureRegime,
  val rawScore: Double, // Dimensionless score (-1.0 to +1.0)
  val componentMeasurements: Map<String, Double>,
  val dataSufficiency: Boolean,
  val evaluationTimestampMs: Long,
  val diagnosticMetadata: Map<String, String>
) {
  val isUsable: Boolean get() = dataSufficiency && !rawScore.isNaN() && !rawScore.isInfinite()
}

/**
 * Independent Market Structure Quantitative Engine for QtY Phase 3.
 * 
 * ## Mathematical Specification & Formulas:
 * 1. **Recent Range (High_window, Low_window)**:
 *    - Max and min prices observed within the lookback window.
 * 
 * 2. **Range Position (Relative Location)**:
 *    - Formula: `(Price_latest - Low_window) / (High_window - Low_window)`
 *    - Units: Ratio [0.0, 1.0]. 0.0 means at the low, 1.0 means at the high.
 * 
 * 3. **Breakout / Rejection Metric**:
 *    - Compares latest price against rolling mean or breakout thresholds.
 */
object MarketStructureQuantitativeEngine {

  fun evaluateMarketStructure(
    observations: List<BtcMarketObservation>,
    evaluationTimestampMs: Long,
    minRequiredObservations: Int = 5
  ): MarketStructureEvidence {
    val validObservations = observations.filter {
      it.timestampMs <= evaluationTimestampMs && it.qualityStatus.isUsable
    }.sortedBy { it.timestampMs }

    val sufficiency = validObservations.size >= minRequiredObservations
    if (!sufficiency || validObservations.isEmpty()) {
      return MarketStructureEvidence(
        regime = MarketStructureRegime.NEUTRAL,
        rawScore = 0.0,
        componentMeasurements = emptyMap(),
        dataSufficiency = false,
        evaluationTimestampMs = evaluationTimestampMs,
        diagnosticMetadata = mapOf("reason" to "Insufficient observations for market structure: count=${validObservations.size}, required=$minRequiredObservations")
      )
    }

    val prices = validObservations.map { it.price }
    val latestPrice = prices.last()
    val highWindow = prices.maxOrNull() ?: latestPrice
    val lowWindow = prices.minOrNull() ?: latestPrice
    val rangeSpan = highWindow - lowWindow

    val rangePosition = if (rangeSpan > 0.0) {
      (latestPrice - lowWindow) / rangeSpan
    } else {
      0.5
    }

    // Directional score based on range position and local trend
    val rawScore = ((rangePosition - 0.5) * 2.0).coerceIn(-1.0, 1.0)

    val regime = when {
      rangePosition >= 0.9 && rangeSpan > 0.0 -> MarketStructureRegime.BREAKOUT_HIGH
      rangePosition <= 0.1 && rangeSpan > 0.0 -> MarketStructureRegime.BREAKOUT_LOW
      rawScore > 0.3 -> MarketStructureRegime.UPWARD_TREND
      rawScore < -0.3 -> MarketStructureRegime.DOWNWARD_TREND
      else -> MarketStructureRegime.RANGE_BOUND
    }

    val components = mapOf(
      "latest_price" to latestPrice,
      "high_window" to highWindow,
      "low_window" to lowWindow,
      "range_position" to rangePosition,
      "range_span" to rangeSpan
    )

    val diagnostics = mapOf(
      "engine" to "MarketStructureQuantitativeEngine",
      "sample_count" to validObservations.size.toString(),
      "evaluation_timestamp" to evaluationTimestampMs.toString()
    )

    return MarketStructureEvidence(
      regime = regime,
      rawScore = rawScore,
      componentMeasurements = components,
      dataSufficiency = true,
      evaluationTimestampMs = evaluationTimestampMs,
      diagnosticMetadata = diagnostics
    )
  }
}
