package com.example.data.ingestion

import com.example.data.model.BtcMarketObservation
import com.example.data.model.DataQualityGrade
import com.example.data.model.DataQualityReport

class BtcObservationBuffer(
  private val maxCapacity: Int = 500,
  private val maxStaleAgeMs: Long = 15000L
) {
  private val observations = mutableListOf<BtcMarketObservation>()

  @Synchronized
  fun ingest(observation: BtcMarketObservation, referenceTimeMs: Long = System.currentTimeMillis()): DataQualityReport {
    if (!observation.qualityStatus.isUsable) {
      return DataQualityReport(
        grade = observation.qualityStatus,
        reason = "Observation rejected due to non-usable quality status: ${observation.qualityStatus}",
        observationTimestampMs = observation.timestampMs,
        evaluatedTimestampMs = referenceTimeMs
      )
    }

    // Validate temporal and quality integrity
    val temporalReport = observation.validateTemporalIntegrity(referenceTimeMs, maxStaleAgeMs)
    if (temporalReport.isFailClosed) {
      return temporalReport
    }

    // Check for duplicates
    if (observations.any { it.timestampMs == observation.timestampMs }) {
      return DataQualityReport(
        grade = DataQualityGrade.DUPLICATED,
        reason = "Duplicate observation timestamp detected: ${observation.timestampMs}",
        observationTimestampMs = observation.timestampMs,
        evaluatedTimestampMs = referenceTimeMs
      )
    }

    // Add and sort chronologically
    observations.add(observation)
    observations.sortBy { it.timestampMs }

    // Enforce capacity limit (drop oldest)
    if (observations.size > maxCapacity) {
      observations.removeAt(0)
    }

    return DataQualityReport(
      grade = DataQualityGrade.VALID,
      reason = "Observation successfully ingested and buffered.",
      observationTimestampMs = observation.timestampMs,
      evaluatedTimestampMs = referenceTimeMs
    )
  }

  @Synchronized
  fun getSnapshot(): List<BtcMarketObservation> = observations.toList()

  @Synchronized
  fun clear() {
    observations.clear()
  }

  val size: Int
    get() = observations.size
}
