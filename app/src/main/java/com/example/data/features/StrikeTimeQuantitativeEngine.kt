package com.example.data.features

import com.example.data.model.BtcMarketObservation

/**
 * Directional relation relative to the contract reference/strike.
 */
enum class StrikeRelation {
  ABOVE_STRIKE,
  BELOW_STRIKE,
  AT_STRIKE,
  UNKNOWN
}

/**
 * Structured evidence output from the Strike/Time quantitative model.
 */
data class StrikeTimeEvidence(
  val relation: StrikeRelation,
  val normalizedDistance: Double, // (CurrentPrice - Strike) / Strike
  val timeRemainingMs: Long,
  val timeDecayFactor: Double, // Urgency / expiry weight factor [0.0, 1.0]
  val rawScore: Double, // Dimensionless score (-1.0 to +1.0)
  val dataSufficiency: Boolean,
  val evaluationTimestampMs: Long,
  val diagnosticMetadata: Map<String, String>
) {
  val isUsable: Boolean get() = dataSufficiency && !rawScore.isNaN() && !rawScore.isInfinite()
}

/**
 * Independent Strike & Time Quantitative Engine for QtY Phase 3.
 * 
 * ## Mathematical Specification & Formulas:
 * 1. **Normalized Strike Distance**:
 *    - Formula: `(Price_latest - Strike_price) / Strike_price`
 *    - Units: Dimensionless fractional distance from reference strike.
 * 
 * 2. **Time Remaining & Expiry Urgency**:
 *    - Formula: `Contract_EndTimeMs - EvaluationTimestampMs`
 *    - Units: Milliseconds remaining.
 *    - Time Decay Factor: Exponential or linear decay as expiry approaches (`exp(-Lambda * TimeRemaining)` or clamped ratio).
 * 
 * 3. **Raw Score Formulation**:
 *    - Combines normalized distance and time decay without guaranteeing settlement outcome prior to actual expiry.
 */
object StrikeTimeQuantitativeEngine {

  fun evaluateStrikeTime(
    observations: List<BtcMarketObservation>,
    evaluationTimestampMs: Long,
    strikePrice: Double,
    contractEndTimeMs: Long,
    contractStartTimeMs: Long
  ): StrikeTimeEvidence {
    // 1. Enforce strict temporal separation: exclude future observations
    val validObservations = observations.filter {
      it.timestampMs <= evaluationTimestampMs && it.qualityStatus.isUsable
    }.sortedBy { it.timestampMs }

    val sufficiency = validObservations.isNotEmpty() && strikePrice > 0.0
    if (!sufficiency) {
      return StrikeTimeEvidence(
        relation = StrikeRelation.UNKNOWN,
        normalizedDistance = 0.0,
        timeRemainingMs = 0L,
        timeDecayFactor = 0.0,
        rawScore = 0.0,
        dataSufficiency = false,
        evaluationTimestampMs = evaluationTimestampMs,
        diagnosticMetadata = mapOf("reason" to "Insufficient observations or invalid strike price: observations=${validObservations.size}, strike=$strikePrice")
      )
    }

    val latestPrice = validObservations.last().price
    val normalizedDistance = (latestPrice - strikePrice) / strikePrice
    val timeRemainingMs = (contractEndTimeMs - evaluationTimestampMs).coerceAtLeast(0L)
    val totalContractDurationMs = (contractEndTimeMs - contractStartTimeMs).coerceAtLeast(1L)
    
    // Time decay factor: 1.0 at start, approaches 0.0 at expiry
    val timeProgress = (evaluationTimestampMs - contractStartTimeMs).toDouble().coerceIn(0.0, totalContractDurationMs.toDouble()) / totalContractDurationMs
    val timeDecayFactor = (1.0 - timeProgress).coerceIn(0.0, 1.0)

    val relation = when {
      normalizedDistance > 0.0001 -> StrikeRelation.ABOVE_STRIKE
      normalizedDistance < -0.0001 -> StrikeRelation.BELOW_STRIKE
      else -> StrikeRelation.AT_STRIKE
    }

    // Raw score weighted by distance and urgency (clamped to [-1.0, +1.0])
    // Note: Does not predict final settlement with certainty before contract expiry.
    val rawScore = (normalizedDistance * 50.0 * (1.0 + (1.0 - timeDecayFactor))).coerceIn(-1.0, 1.0)

    val diagnostics = mapOf(
      "engine" to "StrikeTimeQuantitativeEngine",
      "strike_price" to strikePrice.toString(),
      "latest_price" to latestPrice.toString(),
      "time_remaining_ms" to timeRemainingMs.toString(),
      "evaluation_timestamp" to evaluationTimestampMs.toString()
    )

    return StrikeTimeEvidence(
      relation = relation,
      normalizedDistance = normalizedDistance,
      timeRemainingMs = timeRemainingMs,
      timeDecayFactor = timeDecayFactor,
      rawScore = rawScore,
      dataSufficiency = true,
      evaluationTimestampMs = evaluationTimestampMs,
      diagnosticMetadata = diagnostics
    )
  }
}
