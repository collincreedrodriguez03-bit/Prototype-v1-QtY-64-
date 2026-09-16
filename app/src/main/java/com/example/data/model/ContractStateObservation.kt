package com.example.data.model

/**
 * Immutable contract state observation for a 15-minute Kalshi event contract.
 * Explicitly captures open/reference price, current spot price, and remaining time
 * with fail-closed data quality validation.
 */
data class ContractStateObservation(
  val ticker: String,
  val targetStrike: Double,
  val openReferencePrice: Double,
  val currentSpotPrice: Double,
  val openTimestampMs: Long,
  val expirationTimestampMs: Long,
  val evaluationTimestampMs: Long = System.currentTimeMillis(),
  val qualityStatus: DataQualityGrade = DataQualityGrade.VALID
) {
  init {
    require(ticker.isNotBlank()) { "Contract ticker must not be blank" }
    require(targetStrike > 0.0) { "Target strike must be positive" }
    require(openReferencePrice > 0.0) { "Open reference price must be positive" }
    require(currentSpotPrice > 0.0) { "Current spot price must be positive" }
    require(expirationTimestampMs > openTimestampMs) { "Expiration must be strictly after contract open" }
  }

  val timeRemainingSeconds: Long
    get() {
      val diff = expirationTimestampMs - evaluationTimestampMs
      return if (diff > 0) diff / 1000 else 0
    }

  val isExpired: Boolean
    get() = evaluationTimestampMs >= expirationTimestampMs

  val priceDeltaFromReference: Double
    get() = currentSpotPrice - openReferencePrice

  val priceDeltaPercentage: Double
    get() = if (openReferencePrice > 0.0) (priceDeltaFromReference / openReferencePrice) * 100.0 else 0.0

  fun validateContractState(): DataQualityReport {
    if (!qualityStatus.isUsable) {
      return DataQualityReport(
        grade = qualityStatus,
        reason = "Contract state marked with non-usable quality grade: $qualityStatus",
        observationTimestampMs = openTimestampMs,
        evaluatedTimestampMs = evaluationTimestampMs
      )
    }

    if (isExpired) {
      return DataQualityReport(
        grade = DataQualityGrade.OUT_OF_ORDER,
        reason = "Contract has already expired at evaluation time.",
        observationTimestampMs = expirationTimestampMs,
        evaluatedTimestampMs = evaluationTimestampMs
      )
    }

    return DataQualityReport(
      grade = DataQualityGrade.VALID,
      reason = "Contract state observation is valid and active.",
      observationTimestampMs = openTimestampMs,
      evaluatedTimestampMs = evaluationTimestampMs
    )
  }
}
