package com.example.data.ingestion

import com.example.data.model.BtcMarketObservation
import com.example.data.model.DataQualityGrade
import com.example.data.model.DataQualityReport

/**
 * Metadata wrapper for historical BTC datasets retrieved for backtesting or window evaluation.
 * Retains strict provenance, temporal range, retrieval timestamp, and deterministic dataset version.
 */
data class BtcHistoricalDatasetMetadata(
  val source: String,
  val symbol: String,
  val startTimeMs: Long,
  val endTimeMs: Long,
  val retrievalTimestampMs: Long,
  val observationCount: Int,
  val datasetVersionId: String,
  val qualityReport: DataQualityReport
) {
  init {
    require(source.isNotBlank()) { "Dataset source must not be blank" }
    require(symbol.isNotBlank()) { "Symbol must not be blank" }
    require(startTimeMs > 0) { "Start time must be positive" }
    require(endTimeMs > startTimeMs) { "End time must be after start time" }
    require(retrievalTimestampMs > 0) { "Retrieval timestamp must be positive" }
    require(observationCount >= 0) { "Observation count cannot be negative" }
    require(datasetVersionId.isNotBlank()) { "Dataset version ID must not be blank" }
  }

  val isUsable: Boolean get() = qualityReport.grade.isUsable
}

/**
 * Validates historical BTC datasets against rigorous quality and temporal integrity rules.
 * Fails closed if any anomaly (duplicates, out-of-order, non-finite prices, abnormal gaps, insufficient depth)
 * is detected. Never repairs datasets with synthetic values.
 */
object BtcHistoricalDatasetValidator {

  fun validateDataset(
    source: String,
    symbol: String,
    startTimeMs: Long,
    endTimeMs: Long,
    observations: List<BtcMarketObservation>,
    minRequiredObservations: Int = 10,
    maxAllowedTimestampGapMs: Long = 300000L // 5 minutes max gap between ticks
  ): BtcHistoricalDatasetMetadata {
    val retrievalTime = System.currentTimeMillis()
    val versionId = "qty-hist-${source}-${symbol}-${startTimeMs}-${endTimeMs}-${observations.size}"

    // 1. Check provider disconnect / empty result / insufficient depth
    if (observations.isEmpty() || observations.size < minRequiredObservations) {
      val report = DataQualityReport(
        grade = DataQualityGrade.MISSING,
        reason = "Insufficient historical depth: count (${observations.size}) < minimum required ($minRequiredObservations).",
        observationTimestampMs = startTimeMs,
        evaluatedTimestampMs = retrievalTime
      )
      return BtcHistoricalDatasetMetadata(source, symbol, startTimeMs, endTimeMs, retrievalTime, observations.size, versionId, report)
    }

    var lastTimestamp = -1L
    for (i in observations.indices) {
      val obs = observations[i]

      // 2. Invalid / non-finite / non-positive prices
      if (obs.price.isNaN() || obs.price.isInfinite() || obs.price <= 0.0) {
        val report = DataQualityReport(
          grade = DataQualityGrade.MALFORMED,
          reason = "Invalid or non-finite price encountered at index $i: ${obs.price}",
          observationTimestampMs = obs.timestampMs,
          evaluatedTimestampMs = retrievalTime
        )
        return BtcHistoricalDatasetMetadata(source, symbol, startTimeMs, endTimeMs, retrievalTime, observations.size, versionId, report)
      }

      // 3. Out-of-order / duplicate timestamps
      if (lastTimestamp != -1L) {
        if (obs.timestampMs == lastTimestamp) {
          val report = DataQualityReport(
            grade = DataQualityGrade.DUPLICATED,
            reason = "Duplicate timestamp detected at index $i: ${obs.timestampMs}",
            observationTimestampMs = obs.timestampMs,
            evaluatedTimestampMs = retrievalTime
          )
          return BtcHistoricalDatasetMetadata(source, symbol, startTimeMs, endTimeMs, retrievalTime, observations.size, versionId, report)
        }
        if (obs.timestampMs < lastTimestamp) {
          val report = DataQualityReport(
            grade = DataQualityGrade.OUT_OF_ORDER,
            reason = "Out-of-order timestamp detected at index $i: ${obs.timestampMs} < previous $lastTimestamp",
            observationTimestampMs = obs.timestampMs,
            evaluatedTimestampMs = retrievalTime
          )
          return BtcHistoricalDatasetMetadata(source, symbol, startTimeMs, endTimeMs, retrievalTime, observations.size, versionId, report)
        }

        // 4. Abnormal timestamp gaps
        val gap = obs.timestampMs - lastTimestamp
        if (gap > maxAllowedTimestampGapMs) {
          val report = DataQualityReport(
            grade = DataQualityGrade.STALE,
            reason = "Abnormal timestamp gap detected between index ${i-1} and $i: gap (${gap}ms) > max allowed (${maxAllowedTimestampGapMs}ms)",
            observationTimestampMs = obs.timestampMs,
            evaluatedTimestampMs = retrievalTime
          )
          return BtcHistoricalDatasetMetadata(source, symbol, startTimeMs, endTimeMs, retrievalTime, observations.size, versionId, report)
        }
      }

      lastTimestamp = obs.timestampMs
    }

    val successReport = DataQualityReport(
      grade = DataQualityGrade.VALID,
      reason = "Historical dataset passed all quality and temporal integrity checks.",
      observationTimestampMs = endTimeMs,
      evaluatedTimestampMs = retrievalTime
    )

    return BtcHistoricalDatasetMetadata(
      source = source,
      symbol = symbol,
      startTimeMs = startTimeMs,
      endTimeMs = endTimeMs,
      retrievalTimestampMs = retrievalTime,
      observationCount = observations.size,
      datasetVersionId = versionId,
      qualityReport = successReport
    )
  }
}
