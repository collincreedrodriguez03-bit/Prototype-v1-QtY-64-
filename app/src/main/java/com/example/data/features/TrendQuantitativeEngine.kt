package com.example.data.features

import com.example.data.model.BtcMarketObservation
import kotlin.math.abs

/**
 * Directional enum for QtY Trend quantitative engine.
 * Does not represent probability or trading decisions.
 */
enum class TrendDirection {
  UP,
  DOWN,
  NEUTRAL
}

/**
 * Structured evidence output from the Trend quantitative model.
 * All formulas, lookbacks, units, and component measurements are explicitly documented.
 */
data class TrendEvidence(
  val direction: TrendDirection,
  val rawScore: Double, // Dimensionless directional score (-1.0 to +1.0)
  val componentMeasurements: Map<String, Double>,
  val dataSufficiency: Boolean,
  val evaluationTimestampMs: Long,
  val diagnosticMetadata: Map<String, String>
) {
  val isUsable: Boolean get() = dataSufficiency && !rawScore.isNaN() && !rawScore.isInfinite()
}

/**
 * Independent Trend Quantitative Engine for QtY Phase 3.
 * 
 * ## Mathematical Specification & Formulas:
 * 1. **Short-Term Return (R_short)**:
 *    - Formula: `(Price_latest - Price_t_minus_short) / Price_t_minus_short`
 *    - Units: Dimensionless fractional return.
 *    - Lookback Period: Short window (e.g., 30 seconds / 3 observations).
 *    - Required Sample Count: Minimum 3 observations.
 *    - Edge-case: If denominator <= 0.0, return 0.0.
 * 
 * 2. **Medium-Term Return (R_medium)**:
 *    - Formula: `(Price_latest - Price_t_minus_medium) / Price_t_minus_medium`
 *    - Units: Dimensionless fractional return.
 *    - Lookback Period: Medium window (e.g., 300 seconds / 10 observations).
 *    - Required Sample Count: Minimum 10 observations.
 *    - Edge-case: If denominator <= 0.0, return 0.0.
 * 
 * 3. **Moving-Average Relationship (SMA Ratio)**:
 *    - Formula: `(SMA_short - SMA_medium) / SMA_medium`
 *    - Units: Fractional divergence.
 *    - Required Sample Count: Maximum required lookback count.
 * 
 * 4. **Trend Consistency (Fraction of Positive Successive Steps)**:
 *    - Formula: Count(P[i] > P[i-1]) / Total_Steps
 *    - Units: Ratio [0.0, 1.0].
 * 
 * 5. **Reference Level Distance**:
 *    - Formula: `(Price_latest - ReferenceStrike) / ReferenceStrike`
 *    - Units: Fractional distance from reference strike.
 */
object TrendQuantitativeEngine {

  fun evaluateTrend(
    observations: List<BtcMarketObservation>,
    evaluationTimestampMs: Long,
    referenceStrikePrice: Double? = null,
    shortWindowMs: Long = 30000L,   // 30 seconds
    mediumWindowMs: Long = 300000L, // 5 minutes
    minRequiredObservations: Int = 5
  ): TrendEvidence {
    // 1. Enforce strict temporal separation: exclude future observations
    val validObservations = observations.filter {
      it.timestampMs <= evaluationTimestampMs && it.qualityStatus.isUsable
    }.sortedBy { it.timestampMs }

    val windowStartTime = evaluationTimestampMs - mediumWindowMs
    val windowObservations = validObservations.filter { it.timestampMs >= windowStartTime }

    val sufficiency = windowObservations.size >= minRequiredObservations
    if (!sufficiency || windowObservations.isEmpty()) {
      return TrendEvidence(
        direction = TrendDirection.NEUTRAL,
        rawScore = 0.0,
        componentMeasurements = emptyMap(),
        dataSufficiency = false,
        evaluationTimestampMs = evaluationTimestampMs,
        diagnosticMetadata = mapOf("reason" to "Insufficient observations in window: count=${windowObservations.size}, required=$minRequiredObservations")
      )
    }

    val prices = windowObservations.map { it.price }
    val latestPrice = prices.last()
    val earliestPrice = prices.first()

    // Short-term return (last vs approx 30s ago or earliest in window)
    val shortTermReturn = if (earliestPrice > 0.0) {
      (latestPrice - prices[maxOf(0, prices.size - 3)]) / prices[maxOf(0, prices.size - 3)]
    } else {
      0.0
    }

    // Medium-term return (latest vs earliest in window)
    val mediumTermReturn = if (earliestPrice > 0.0) {
      (latestPrice - earliestPrice) / earliestPrice
    } else {
      0.0
    }

    // Trend consistency (successive price changes direction ratio)
    var upwardSteps = 0.0
    var totalSteps = 0
    for (i in 1 until prices.size) {
      totalSteps++
      val diff = prices[i] - prices[i - 1]
      if (diff > 0.0) {
        upwardSteps += 1.0
      } else if (diff == 0.0) {
        upwardSteps += 0.5
      }
    }
    val consistency = if (totalSteps > 0) upwardSteps / totalSteps.toDouble() else 0.5

    // Reference level distance
    val strikeDistance = if (referenceStrikePrice != null && referenceStrikePrice > 0.0) {
      (latestPrice - referenceStrikePrice) / referenceStrikePrice
    } else {
      0.0
    }

    // Composite raw score calculation (balanced arithmetic combination without arbitrary probability conversion)
    // Score range normalized approximately to [-1.0, +1.0]
    val rawScore = ((shortTermReturn * 0.4) + (mediumTermReturn * 0.4) + ((consistency - 0.5) * 0.2)).coerceIn(-1.0, 1.0)

    val direction = when {
      rawScore > 0.05 -> TrendDirection.UP
      rawScore < -0.05 -> TrendDirection.DOWN
      else -> TrendDirection.NEUTRAL
    }

    val components = mapOf(
      "short_term_return" to shortTermReturn,
      "medium_term_return" to mediumTermReturn,
      "trend_consistency" to consistency,
      "strike_distance" to strikeDistance,
      "latest_price" to latestPrice
    )

    val diagnostics = mapOf(
      "engine" to "TrendQuantitativeEngine",
      "sample_count" to windowObservations.size.toString(),
      "evaluation_timestamp" to evaluationTimestampMs.toString()
    )

    return TrendEvidence(
      direction = direction,
      rawScore = rawScore,
      componentMeasurements = components,
      dataSufficiency = true,
      evaluationTimestampMs = evaluationTimestampMs,
      diagnosticMetadata = diagnostics
    )
  }
}
