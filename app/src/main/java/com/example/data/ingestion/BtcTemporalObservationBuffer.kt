package com.example.data.ingestion

import com.example.data.model.BtcMarketObservation
import com.example.data.model.DataQualityGrade
import com.example.data.model.DataQualityReport

/**
 * Bounded rolling temporal observation buffer for authentic BTC market observations.
 * Maintains chronological ordering, prevents future leakage (observations > evaluationTimestampMs),
 * provides duplicate protection, stale detection, latest valid observation access,
 * and historical window queries.
 */
class BtcTemporalObservationBuffer(
  private val maxCapacity: Int = 1000,
  private val maxStaleAgeMs: Long = 15000L,
  private val retentionWindowMs: Long = 900000L // 15 minutes default retention
) {
  private val observations = mutableListOf<BtcMarketObservation>()

  @Synchronized
  fun ingest(
    observation: BtcMarketObservation,
    evaluationTimestampMs: Long = System.currentTimeMillis()
  ): DataQualityReport {
    // 1. Prevent future leakage: observation timestamp cannot be ahead of evaluation timestamp
    if (observation.timestampMs > evaluationTimestampMs) {
      return DataQualityReport(
        grade = DataQualityGrade.OUT_OF_ORDER,
        reason = "Future observation rejected. Timestamp (${observation.timestampMs}) > evaluation time ($evaluationTimestampMs).",
        observationTimestampMs = observation.timestampMs,
        evaluatedTimestampMs = evaluationTimestampMs
      )
    }

    // 2. Check quality usability
    if (!observation.qualityStatus.isUsable) {
      return DataQualityReport(
        grade = observation.qualityStatus,
        reason = "Observation rejected due to non-usable quality status: ${observation.qualityStatus}",
        observationTimestampMs = observation.timestampMs,
        evaluatedTimestampMs = evaluationTimestampMs
      )
    }

    // 3. Check staleness relative to evaluation timestamp
    val ageMs = evaluationTimestampMs - observation.timestampMs
    if (ageMs > maxStaleAgeMs) {
      return DataQualityReport(
        grade = DataQualityGrade.STALE,
        reason = "Observation rejected as stale. Age ($ageMs ms) > maxStaleAge ($maxStaleAgeMs ms).",
        observationTimestampMs = observation.timestampMs,
        evaluatedTimestampMs = evaluationTimestampMs
      )
    }

    // 4. Duplicate protection
    if (observations.any { it.timestampMs == observation.timestampMs }) {
      return DataQualityReport(
        grade = DataQualityGrade.DUPLICATED,
        reason = "Duplicate observation timestamp detected: ${observation.timestampMs}",
        observationTimestampMs = observation.timestampMs,
        evaluatedTimestampMs = evaluationTimestampMs
      )
    }

    // 5. Insert and maintain chronological ordering
    observations.add(observation)
    observations.sortBy { it.timestampMs }

    // 6. Enforce retention window (prune observations older than retentionWindowMs relative to latest)
    val latestTime = observations.last().timestampMs
    observations.removeAll { (latestTime - it.timestampMs) > retentionWindowMs }

    // 7. Enforce max capacity
    if (observations.size > maxCapacity) {
      val excess = observations.size - maxCapacity
      repeat(excess) {
        observations.removeAt(0)
      }
    }

    return DataQualityReport(
      grade = DataQualityGrade.VALID,
      reason = "Observation successfully ingested into temporal buffer.",
      observationTimestampMs = observation.timestampMs,
      evaluatedTimestampMs = evaluationTimestampMs
    )
  }

  @Synchronized
  fun getLatestValidObservation(evaluationTimestampMs: Long = System.currentTimeMillis()): BtcMarketObservation? {
    return observations.lastOrNull { it.timestampMs <= evaluationTimestampMs && (evaluationTimestampMs - it.timestampMs) <= maxStaleAgeMs }
  }

  @Synchronized
  fun queryWindow(startTimeMs: Long, endTimeMs: Long): List<BtcMarketObservation> {
    return observations.filter { it.timestampMs in startTimeMs..endTimeMs }
  }

  @Synchronized
  fun getSnapshot(): List<BtcMarketObservation> = observations.toList()

  @Synchronized
  fun clear() {
    observations.clear()
  }

  val size: Int
    @Synchronized get() = observations.size
}
