package com.example.data.model

/**
 * Explicit generic 15-minute contract-state abstraction for QtY.
 * Represents contract timing, reference strike, current spot price, elapsed/remaining time,
 * activity status, completion/settlement status, and explicit settlement reference source.
 */
data class ContractTemporalState(
  val contractTicker: String,
  val openTimestampMs: Long,
  val expirationTimestampMs: Long,
  val referenceStrikePrice: Double,
  val currentBtcSpotPrice: Double,
  val settlementReferencePrice: Double? = null, // Explicit, replaceable settlement/reference source
  val evaluationTimestampMs: Long = System.currentTimeMillis(),
  val maxStaleAgeMs: Long = 15000L,
  val qualityStatus: DataQualityGrade = DataQualityGrade.VALID
) {
  init {
    require(contractTicker.isNotBlank()) { "Contract ticker must not be blank" }
    require(openTimestampMs > 0) { "Open timestamp must be positive" }
    require(expirationTimestampMs > openTimestampMs) { "Expiration timestamp must be strictly after open" }
    require(referenceStrikePrice > 0.0) { "Reference strike price must be positive" }
    require(currentBtcSpotPrice > 0.0) { "Current BTC spot price must be positive" }
    if (settlementReferencePrice != null) {
      require(settlementReferencePrice > 0.0) { "Settlement reference price must be positive" }
    }
  }

  val elapsedTimeSeconds: Long
    get() {
      val diff = evaluationTimestampMs - openTimestampMs
      return if (diff > 0) diff / 1000 else 0
    }

  val remainingTimeSeconds: Long
    get() {
      val diff = expirationTimestampMs - evaluationTimestampMs
      return if (diff > 0) diff / 1000 else 0
    }

  val isActive: Boolean
    get() = evaluationTimestampMs in openTimestampMs until expirationTimestampMs && qualityStatus.isUsable

  val isCompleteOrSettled: Boolean
    get() = evaluationTimestampMs >= expirationTimestampMs

  fun validateTemporalState(): DataQualityReport {
    if (!qualityStatus.isUsable) {
      return DataQualityReport(
        grade = qualityStatus,
        reason = "Contract state has non-usable quality grade: $qualityStatus",
        observationTimestampMs = openTimestampMs,
        evaluatedTimestampMs = evaluationTimestampMs
      )
    }

    if (evaluationTimestampMs < openTimestampMs) {
      return DataQualityReport(
        grade = DataQualityGrade.OUT_OF_ORDER,
        reason = "Evaluation time ($evaluationTimestampMs) is before contract open ($openTimestampMs).",
        observationTimestampMs = openTimestampMs,
        evaluatedTimestampMs = evaluationTimestampMs
      )
    }

    if (isCompleteOrSettled) {
      return DataQualityReport(
        grade = DataQualityGrade.STALE,
        reason = "Contract has expired/settled at evaluation time.",
        observationTimestampMs = expirationTimestampMs,
        evaluatedTimestampMs = evaluationTimestampMs
      )
    }

    return DataQualityReport(
      grade = DataQualityGrade.VALID,
      reason = "Contract state is active and temporally valid.",
      observationTimestampMs = openTimestampMs,
      evaluatedTimestampMs = evaluationTimestampMs
    )
  }
}
