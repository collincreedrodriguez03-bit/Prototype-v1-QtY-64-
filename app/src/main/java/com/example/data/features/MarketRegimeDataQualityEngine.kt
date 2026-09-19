package com.example.data.features

import com.example.data.model.BtcMarketObservation
import com.example.data.model.DataQualityGrade

/**
 * Data quality and reliability grade for Market Regime engine.
 */
enum class DataReliabilityGrade {
  HIGH,
  MODERATE,
  DEGRADED,
  INVALID
}

/**
 * Structured evidence output from the Market Regime & Data Quality engine.
 */
data class MarketRegimeDataQualityEvidence(
  val reliabilityGrade: DataReliabilityGrade,
  val dataQualityScore: Double, // [0.0, 1.0]
  val observationContinuityRatio: Double, // Ratio of usable to total observations
  val isStale: Boolean,
  val dataSufficiency: Boolean,
  val evaluationTimestampMs: Long,
  val diagnosticMetadata: Map<String, String>
) {
  val isUsable: Boolean get() = dataSufficiency && reliabilityGrade != DataReliabilityGrade.INVALID && !dataQualityScore.isNaN()
}

/**
 * Independent Market Regime & Data Quality Quantitative Engine for QtY Phase 3.
 * 
 * ## Mathematical Specification & Formulas:
 * 1. **Observation Continuity Ratio**:
 *    - Formula: `Count(Usable_Observations) / Total_Observations`
 *    - Units: Ratio [0.0, 1.0].
 * 
 * 2. **Staleness Check**:
 *    - Formula: `EvaluationTimestampMs - LatestObservationTimestampMs > StalenessThresholdMs`
 * 
 * 3. **Data Quality Score**:
 *    - Composite score combining continuity ratio, quality grade weights, and staleness penalty.
 */
object MarketRegimeDataQualityEngine {

  fun evaluateRegimeAndQuality(
    observations: List<BtcMarketObservation>,
    evaluationTimestampMs: Long,
    stalenessThresholdMs: Long = 60000L, // 60 seconds
    minRequiredObservations: Int = 5
  ): MarketRegimeDataQualityEvidence {
    // 1. Enforce strict temporal separation: exclude future observations
    val rawValidObservations = observations.filter { it.timestampMs <= evaluationTimestampMs }
    val futureLeaksCount = observations.size - rawValidObservations.size

    if (rawValidObservations.isEmpty()) {
      return MarketRegimeDataQualityEvidence(
        reliabilityGrade = DataReliabilityGrade.INVALID,
        dataQualityScore = 0.0,
        observationContinuityRatio = 0.0,
        isStale = true,
        dataSufficiency = false,
        evaluationTimestampMs = evaluationTimestampMs,
        diagnosticMetadata = mapOf("reason" to "No observations found at or prior to evaluation timestamp")
      )
    }

    val latestTimestamp = rawValidObservations.maxOf { it.timestampMs }
    val isStale = (evaluationTimestampMs - latestTimestamp) > stalenessThresholdMs

    val usableObservations = rawValidObservations.filter { it.qualityStatus.isUsable }
    val continuityRatio = usableObservations.size.toDouble() / rawValidObservations.size.toDouble()

    val sufficiency = usableObservations.size >= minRequiredObservations && !isStale

    val grade = when {
      isStale || usableObservations.size < minRequiredObservations -> DataReliabilityGrade.INVALID
      continuityRatio < 0.7 -> DataReliabilityGrade.DEGRADED
      continuityRatio < 0.9 -> DataReliabilityGrade.MODERATE
      else -> DataReliabilityGrade.HIGH
    }

    val qualityScore = if (sufficiency) {
      (continuityRatio * 0.7) + (if (isStale) 0.0 else 0.3)
    } else {
      0.0
    }

    val diagnostics = mapOf(
      "engine" to "MarketRegimeDataQualityEngine",
      "total_observations" to observations.size.toString(),
      "valid_observations" to rawValidObservations.size.toString(),
      "usable_observations" to usableObservations.size.toString(),
      "future_leaks_detected" to futureLeaksCount.toString(),
      "latest_timestamp" to latestTimestamp.toString(),
      "evaluation_timestamp" to evaluationTimestampMs.toString()
    )

    return MarketRegimeDataQualityEvidence(
      reliabilityGrade = grade,
      dataQualityScore = qualityScore,
      observationContinuityRatio = continuityRatio,
      isStale = isStale,
      dataSufficiency = sufficiency,
      evaluationTimestampMs = evaluationTimestampMs,
      diagnosticMetadata = diagnostics
    )
  }
}
