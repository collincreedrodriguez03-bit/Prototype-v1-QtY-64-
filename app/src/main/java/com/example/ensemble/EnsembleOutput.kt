package com.example.ensemble

import com.example.prediction.ModelPrediction

/**
 * Immutable ensemble output aggregating multiple model predictions
 * into a unified consensus score before risk gate filtering.
 */
data class EnsembleOutput(
  val contractTicker: String,
  val predictions: List<ModelPrediction>,
  val consensusProbabilityYes: Double,
  val consensusProbabilityNo: Double,
  val consensusConfidence: Double,
  val timestampMs: Long = System.currentTimeMillis()
) {
  init {
    require(contractTicker.isNotBlank()) { "Contract ticker must not be blank" }
    require(consensusProbabilityYes in 0.0..1.0) { "Consensus probability YES must be in [0.0, 1.0]" }
    require(consensusProbabilityNo in 0.0..1.0) { "Consensus probability NO must be in [0.0, 1.0]" }
    require(consensusConfidence in 0.0..1.0) { "Consensus confidence must be in [0.0, 1.0]" }
  }

  val hasValidPredictions: Boolean
    get() = predictions.isNotEmpty() && predictions.all { it.status == com.example.prediction.PredictionStatus.READY }
}
