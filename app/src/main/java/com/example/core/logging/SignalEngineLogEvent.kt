package com.example.core.logging

/**
 * Structured audit events emitted throughout the quant engine pipeline.
 * Maintains full traceability for every quantitative decision cycle.
 */
sealed class SignalEngineLogEvent {
  abstract val timestampMs: Long

  data class MarketDataReceived(
    override val timestampMs: Long,
    val symbol: String,
    val lastPrice: Double,
    val spread: Double
  ) : SignalEngineLogEvent()

  data class FeatureExtractionCompleted(
    override val timestampMs: Long,
    val featureCount: Int,
    val computeDurationNanos: Long,
    val isValid: Boolean,
    val validationError: String? = null
  ) : SignalEngineLogEvent()

  data class ModelEvaluated(
    override val timestampMs: Long,
    val modelId: String,
    val probabilityYes: Double,
    val probabilityNo: Double,
    val confidence: Double
  ) : SignalEngineLogEvent()

  data class RiskGateEvaluated(
    override val timestampMs: Long,
    val gateName: String,
    val passed: Boolean,
    val reason: String
  ) : SignalEngineLogEvent()

  data class SignalEmitted(
    override val timestampMs: Long,
    val contractTicker: String,
    val decision: String,
    val confidence: Double,
    val riskFlags: List<String>,
    val reason: String
  ) : SignalEngineLogEvent()

  data class EngineStateChanged(
    override val timestampMs: Long,
    val previousState: String,
    val newState: String,
    val detail: String? = null
  ) : SignalEngineLogEvent()

  data class SystemMessage(
    override val timestampMs: Long,
    val level: String,
    val tag: String,
    val message: String
  ) : SignalEngineLogEvent()
}
