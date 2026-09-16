package com.example.prediction

enum class PredictionStatus {
  READY,
  PENDING_MATHEMATICS,
  FEATURE_INVALID,
  FAILED
}

/**
 * Immutable output of a single prediction model.
 */
data class ModelPrediction(
  val modelId: String,
  val contractTicker: String,
  val probabilityYes: Double,
  val probabilityNo: Double,
  val confidence: Double,
  val latencyNanos: Long,
  val status: PredictionStatus,
  val metadata: Map<String, String> = emptyMap()
) {
  init {
    require(probabilityYes in 0.0..1.0) { "probabilityYes must be in [0.0, 1.0]" }
    require(probabilityNo in 0.0..1.0) { "probabilityNo must be in [0.0, 1.0]" }
    require(confidence in 0.0..1.0) { "confidence must be in [0.0, 1.0]" }
  }
}
