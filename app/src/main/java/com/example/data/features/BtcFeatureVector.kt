package com.example.data.features

import com.example.data.model.DataQualityGrade

/**
 * Immutable feature representation derived exclusively from authentic BTC observations
 * available at or before the evaluation timestamp.
 */
data class BtcFeatureVector(
  val featureTimestampMs: Long,
  val evaluationTimestampMs: Long,
  val sourceWindowMs: Long,
  val dataSufficiency: Boolean,
  val qualityStatus: DataQualityGrade,
  val values: Map<String, Double>
) {
  init {
    require(featureTimestampMs > 0) { "Feature timestamp must be positive" }
    require(evaluationTimestampMs >= featureTimestampMs) { "Evaluation timestamp cannot be before feature timestamp" }
    require(sourceWindowMs > 0) { "Source window must be positive" }
  }

  val isUsable: Boolean get() = dataSufficiency && qualityStatus.isUsable && featureTimestampMs <= evaluationTimestampMs
}
