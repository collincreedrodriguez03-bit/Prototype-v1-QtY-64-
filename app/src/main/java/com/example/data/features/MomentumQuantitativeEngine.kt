package com.example.data.features

import com.example.data.model.BtcMarketObservation
import com.example.data.model.DataQualityGrade
import kotlin.math.abs

/**
 * Directional enum for QtY Momentum quantitative engine.
 * Does not represent probability or trading decisions.
 */
enum class MomentumDirection {
  UP,
  DOWN,
  NEUTRAL
}

/**
 * Structured evidence output from the Momentum quantitative model.
 * All formulas, lookbacks, units, and component measurements are explicitly documented.
 */
data class MomentumEvidence(
  val direction: MomentumDirection,
  val rawScore: Double, // Dimensionless momentum score (-1.0 to +1.0)
  val componentMeasurements: Map<String, Double>,
  val dataSufficiency: Boolean,
  val evaluationTimestampMs: Long,
  val diagnosticMetadata: Map<String, String>
) {
  val isUsable: Boolean get() = dataSufficiency && !rawScore.isNaN() && !rawScore.isInfinite()
}

/**
 * Independent Momentum Quantitative Engine for QtY Phase 3.
 * 
 * ## Mathematical Specification & Formulas:
 * 1. **Short-Term Velocity (V_short)**:
 *    - Formula: `(Price_latest - Price_t_minus_short) / Delta_t_short`
 *    - Units: Price change per millisecond.
 *    - Lookback Period: Short window (e.g., 15 seconds).
 *    - Required Sample Count: Minimum 3 observations.
 * 
 * 2. **Medium-Term Velocity (V_medium)**:
 *    - Formula: `(Price_latest - Price_t_minus_medium) / Delta_t_medium`
 *    - Units: Price change per millisecond.
 *    - Lookback Period: Medium window (e.g., 120 seconds).
 *    - Required Sample Count: Minimum 5 observations.
 * 
 * 3. **Momentum Acceleration (A_mom)**:
 *    - Formula: `V_short - V_medium` (rate of velocity change).
 *    - Units: Velocity change per millisecond.
 * 
 * 4. **Log Return / Magnitude**:
 *    - Formula: `ln(Price_latest / Price_earliest)`
 *    - Units: Dimensionless log return.
 * 
 * 5. **Momentum Consistency (Ratio of positive return steps)**:
 *    - Formula: Count(Return_i > 0) / Total_Steps
 *    - Units: Ratio [0.0, 1.0].
 */
object MomentumQuantitativeEngine {

  fun evaluateMomentum(
    observations: List<BtcMarketObservation>,
    evaluationTimestampMs: Long,
    shortWindowMs: Long = 15000L,  // 15 seconds
    mediumWindowMs: Long = 120000L, // 2 minutes
    minRequiredObservations: Int = 5
  ): MomentumEvidence {
    // 1. Enforce strict temporal separation: exclude future observations
    val validObservations = observations.filter {
      it.timestampMs <= evaluationTimestampMs && it.qualityStatus.isUsable
    }.sortedBy { it.timestampMs }

    val windowStartTime = evaluationTimestampMs - mediumWindowMs
    val windowObservations = validObservations.filter { it.timestampMs >= windowStartTime }

    val sufficiency = windowObservations.size >= minRequiredObservations
    if (!sufficiency || windowObservations.isEmpty()) {
      return MomentumEvidence(
        direction = MomentumDirection.NEUTRAL,
        rawScore = 0.0,
        componentMeasurements = emptyMap(),
        dataSufficiency = false,
        evaluationTimestampMs = evaluationTimestampMs,
        diagnosticMetadata = mapOf("reason" to "Insufficient observations for momentum evaluation: count=${windowObservations.size}, required=$minRequiredObservations")
      )
    }

    val prices = windowObservations.map { it.price }
    val times = windowObservations.map { it.timestampMs }
    
    val latestPrice = prices.last()
    val latestTime = times.last()

    // Find price approximately at short window start
    val shortTargetTime = evaluationTimestampMs - shortWindowMs
    val shortBaseObs = windowObservations.lastOrNull { it.timestampMs <= shortTargetTime } ?: windowObservations.first()
    val shortDeltaTime = (latestTime - shortBaseObs.timestampMs).toDouble().coerceAtLeast(1.0)
    val shortVelocity = (latestPrice - shortBaseObs.price) / shortDeltaTime

    // Medium window start (earliest in window)
    val earliestPrice = prices.first()
    val earliestTime = times.first()
    val mediumDeltaTime = (latestTime - earliestTime).toDouble().coerceAtLeast(1.0)
    val mediumVelocity = (latestPrice - earliestPrice) / mediumDeltaTime

    // Acceleration (velocity change)
    val acceleration = shortVelocity - mediumVelocity

    // Log return magnitude
    val logReturn = if (earliestPrice > 0.0 && latestPrice > 0.0) {
      kotlin.math.ln(latestPrice / earliestPrice)
    } else {
      0.0
    }

    // Momentum consistency
    var positiveSteps = 0.0
    var totalSteps = 0
    for (i in 1 until prices.size) {
      totalSteps++
      val stepDiff = prices[i] - prices[i - 1]
      if (stepDiff > 0.0) {
        positiveSteps += 1.0
      } else if (stepDiff == 0.0) {
        positiveSteps += 0.5
      }
    }
    val consistency = if (totalSteps > 0) positiveSteps / totalSteps.toDouble() else 0.5

    // Composite raw momentum score calculation normalized to [-1.0, +1.0]
    // Combines log return, short velocity direction, and acceleration
    val rawScore = ((logReturn * 100.0 * 0.4) + (shortVelocity * 1000.0 * 0.4) + ((consistency - 0.5) * 0.2)).coerceIn(-1.0, 1.0)

    val direction = when {
      rawScore > 0.04 -> MomentumDirection.UP
      rawScore < -0.04 -> MomentumDirection.DOWN
      else -> MomentumDirection.NEUTRAL
    }

    val components = mapOf(
      "short_velocity" to shortVelocity,
      "medium_velocity" to mediumVelocity,
      "acceleration" to acceleration,
      "log_return" to logReturn,
      "momentum_consistency" to consistency,
      "latest_price" to latestPrice
    )

    val diagnostics = mapOf(
      "engine" to "MomentumQuantitativeEngine",
      "sample_count" to windowObservations.size.toString(),
      "evaluation_timestamp" to evaluationTimestampMs.toString()
    )

    return MomentumEvidence(
      direction = direction,
      rawScore = rawScore,
      componentMeasurements = components,
      dataSufficiency = true,
      evaluationTimestampMs = evaluationTimestampMs,
      diagnosticMetadata = diagnostics
    )
  }
}
