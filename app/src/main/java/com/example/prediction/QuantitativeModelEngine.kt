package com.example.prediction

import com.example.data.model.KalshiContract

enum class ModelDirection {
  BULLISH_YES,
  BEARISH_NO,
  NEUTRAL_NO_TRADE
}

/**
 * Structured result returned by each independent quantitative model engine.
 * Contains evidence, direction, strength/confidence as raw model values,
 * data sufficiency, and diagnostic metadata.
 */
data class QuantitativeModelResult(
  val modelId: String,
  val modelCategory: String,
  val direction: ModelDirection,
  val strengthScore: Double, // Raw model value [0.0..1.0]
  val confidence: Double,    // [0.0..1.0]
  val dataSufficiency: Boolean,
  val evidenceSummary: String,
  val status: PredictionStatus = PredictionStatus.PENDING_MATHEMATICS,
  val diagnosticMetadata: Map<String, String> = emptyMap(),
  val evaluationTimestampMs: Long = System.currentTimeMillis()
) {
  init {
    require(strengthScore in 0.0..1.0) { "Strength score must be between 0.0 and 1.0" }
    require(confidence in 0.0..1.0) { "Confidence must be between 0.0 and 1.0" }
  }
}

/**
 * Boundary interface for all 7 independent quantitative model engines.
 * Models are strictly isolated: they receive validated observation packets
 * and return structured results without executing trades or modifying other models.
 */
interface QuantitativeModelEngine {
  val modelId: String
  val categoryName: String

  suspend fun evaluate(
    contract: KalshiContract,
    packet: ModelObservationPacket
  ): QuantitativeModelResult
}
