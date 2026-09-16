package com.example.data.model

/**
 * Explicit data quality grades for all market, contract, and feature observations.
 * The system adheres to a fail-closed principle: any non-VALID status blocks trading/signals.
 */
enum class DataQualityGrade {
  VALID,
  MISSING,
  STALE,
  MALFORMED,
  DUPLICATED,
  OUT_OF_ORDER,
  FEED_DISCONNECTED;

  val isUsable: Boolean get() = this == VALID
}

/**
 * Immutable audit wrapper for data quality evaluation on any observation stream.
 */
data class DataQualityReport(
  val grade: DataQualityGrade,
  val reason: String,
  val observationTimestampMs: Long,
  val evaluatedTimestampMs: Long = System.currentTimeMillis()
) {
  init {
    require(observationTimestampMs > 0) { "Observation timestamp must be positive" }
    require(evaluatedTimestampMs > 0) { "Evaluation timestamp must be positive" }
  }

  val isFailClosed: Boolean get() = !grade.isUsable
}
