package com.example.data.model

/**
 * Immutable BTC market observation contract preserving strict temporal ordering.
 * Look-ahead leakage is prevented by design via validation that observation timestamps
 * do not exceed current evaluation time.
 */
data class BtcMarketObservation(
  val timestampMs: Long,
  val receiptTimestampMs: Long = timestampMs,
  val price: Double,
  val volume: Double? = null,
  val bidPrice: Double? = null,
  val askPrice: Double? = null,
  val qualityStatus: DataQualityGrade = DataQualityGrade.VALID
) {
  init {
    require(timestampMs > 0) { "Observation timestamp must be positive" }
    require(receiptTimestampMs > 0) { "Receipt timestamp must be positive" }
    if (qualityStatus == DataQualityGrade.VALID) {
      require(price > 0.0 && !price.isNaN() && !price.isInfinite()) { "BTC price must be strictly positive and finite for valid observations" }
    }
    if (volume != null) {
      require(volume >= 0.0) { "Volume cannot be negative (malformed observation)" }
    }
    if (bidPrice != null && askPrice != null && qualityStatus == DataQualityGrade.VALID) {
      require(bidPrice > 0.0 && askPrice > 0.0) { "Bid and ask prices must be positive" }
      require(askPrice >= bidPrice) { "Ask price ($askPrice) cannot be lower than bid price ($bidPrice)" }
    }
  }

  val spread: Double? get() = if (bidPrice != null && askPrice != null) askPrice - bidPrice else null
  val midPrice: Double? get() = if (bidPrice != null && askPrice != null) (bidPrice + askPrice) / 2.0 else null

  /**
   * Validates temporal integrity and freshness against a reference evaluation time.
   * Fails closed if the observation is from the future (look-ahead leak) or older than [maxAgeMs].
   */
  fun validateTemporalIntegrity(referenceTimeMs: Long, maxAgeMs: Long): DataQualityReport {
    if (timestampMs > referenceTimeMs) {
      return DataQualityReport(
        grade = DataQualityGrade.OUT_OF_ORDER,
        reason = "Observation timestamp ($timestampMs) is in the future relative to reference time ($referenceTimeMs). Look-ahead leakage detected.",
        observationTimestampMs = timestampMs,
        evaluatedTimestampMs = referenceTimeMs
      )
    }

    val ageMs = referenceTimeMs - timestampMs
    if (ageMs > maxAgeMs) {
      return DataQualityReport(
        grade = DataQualityGrade.STALE,
        reason = "Observation age (${ageMs}ms) exceeds maximum allowable threshold (${maxAgeMs}ms).",
        observationTimestampMs = timestampMs,
        evaluatedTimestampMs = referenceTimeMs
      )
    }

    if (!qualityStatus.isUsable) {
      return DataQualityReport(
        grade = qualityStatus,
        reason = "Observation marked with non-usable quality grade: $qualityStatus",
        observationTimestampMs = timestampMs,
        evaluatedTimestampMs = referenceTimeMs
      )
    }

    return DataQualityReport(
      grade = DataQualityGrade.VALID,
      reason = "Observation passed temporal and quality checks.",
      observationTimestampMs = timestampMs,
      evaluatedTimestampMs = referenceTimeMs
    )
  }
}
