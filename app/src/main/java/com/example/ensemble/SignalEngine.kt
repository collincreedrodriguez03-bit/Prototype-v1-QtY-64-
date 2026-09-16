package com.example.ensemble

import com.example.core.config.SignalEngineConfig
import com.example.core.logging.QuantLogger
import com.example.core.logging.SignalEngineLogEvent
import com.example.data.model.Candle
import com.example.data.model.KalshiContract
import com.example.data.model.MarketDataFeedState
import com.example.data.model.MarketSnapshot
import com.example.prediction.ModelPrediction
import com.example.prediction.PredictionModel
import com.example.prediction.PredictionStatus
import com.example.quant.FeatureExtractor
import com.example.quant.QuantitativeFeatureVector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class SignalEngineState {
  object Idle : SignalEngineState()

  data class AwaitingData(
    val reason: String
  ) : SignalEngineState()

  data class Evaluating(
    val contractTicker: String,
    val startedAtMs: Long
  ) : SignalEngineState()

  data class Evaluated(
    val contractTicker: String,
    val decision: SignalDecision,
    val confidence: Double,
    val completedAtMs: Long
  ) : SignalEngineState()

  data class Error(
    val message: String
  ) : SignalEngineState()
}

/**
 * Foundational Quantitative Signal Engine interface.
 * Operates independently from UI. NO automated trading or order execution.
 */
interface SignalEngine {
  val engineState: StateFlow<SignalEngineState>
  val latestSignal: StateFlow<SignalOutput?>
  val signalHistory: StateFlow<List<SignalOutput>>

  suspend fun evaluateCycle(
    contract: KalshiContract,
    candles: List<Candle>,
    snapshot: MarketSnapshot?,
    feedState: MarketDataFeedState
  ): SignalOutput
}

class DefaultSignalEngine(
  private val config: SignalEngineConfig,
  private val featureExtractor: FeatureExtractor,
  private val models: List<PredictionModel>,
  private val riskFilters: List<RiskFilter>,
  private val logger: QuantLogger,
  private val maxHistory: Int = 100
) : SignalEngine {

  private val _engineState = MutableStateFlow<SignalEngineState>(SignalEngineState.Idle)
  override val engineState: StateFlow<SignalEngineState> = _engineState.asStateFlow()

  private val _latestSignal = MutableStateFlow<SignalOutput?>(null)
  override val latestSignal: StateFlow<SignalOutput?> = _latestSignal.asStateFlow()

  private val _signalHistory = MutableStateFlow<List<SignalOutput>>(emptyList())
  override val signalHistory: StateFlow<List<SignalOutput>> = _signalHistory.asStateFlow()

  override suspend fun evaluateCycle(
    contract: KalshiContract,
    candles: List<Candle>,
    snapshot: MarketSnapshot?,
    feedState: MarketDataFeedState
  ): SignalOutput {
    val cycleStartNanos = System.nanoTime()
    val nowMs = System.currentTimeMillis()

    _engineState.value = SignalEngineState.Evaluating(contract.ticker, nowMs)
    logger.logEvent(
      SignalEngineLogEvent.EngineStateChanged(
        timestampMs = nowMs,
        previousState = "IDLE",
        newState = "EVALUATING",
        detail = "Contract: ${contract.ticker}"
      )
    )

    val triggeredFlags = mutableSetOf<RiskFlag>()

    // Check feed state first
    if (feedState !is MarketDataFeedState.Connected) {
      triggeredFlags.add(RiskFlag.FEED_DISCONNECTED)
    }

    // Step 1: Feature Extraction
    val featStart = System.nanoTime()
    val features = featureExtractor.extract(candles, snapshot, config.minHistoryCandles)
    val featDuration = System.nanoTime() - featStart

    logger.logEvent(
      SignalEngineLogEvent.FeatureExtractionCompleted(
        timestampMs = nowMs,
        featureCount = features.features.size,
        computeDurationNanos = featDuration,
        isValid = features.isValid,
        validationError = features.validationError
      )
    )

    if (!features.isValid) {
      triggeredFlags.add(RiskFlag.INSUFFICIENT_HISTORY)
    }

    // Step 2: Model Evaluations (Interfaces prepared; models are currently baseline prototypes)
    val predictions = mutableListOf<ModelPrediction>()
    for (model in models) {
      val pred = model.evaluate(contract, features)
      predictions.add(pred)
      logger.logEvent(
        SignalEngineLogEvent.ModelEvaluated(
          timestampMs = nowMs,
          modelId = pred.modelId,
          probabilityYes = pred.probabilityYes,
          probabilityNo = pred.probabilityNo,
          confidence = pred.confidence
        )
      )
      if (pred.status == PredictionStatus.PENDING_MATHEMATICS) {
        triggeredFlags.add(RiskFlag.MODEL_PREDICTION_PENDING)
      }
    }

    // Step 3: Consensus aggregation (Equal weight baseline)
    val consensusYes = if (predictions.isNotEmpty()) {
      predictions.map { it.probabilityYes }.average()
    } else 0.5

    val consensusNo = if (predictions.isNotEmpty()) {
      predictions.map { it.probabilityNo }.average()
    } else 0.5

    val consensusConfidence = if (predictions.isNotEmpty()) {
      predictions.map { it.confidence }.average()
    } else 0.0

    // Step 4: Decision Context & Risk Gates
    val context = DecisionContext(
      config = config,
      contract = contract,
      feedState = feedState,
      snapshot = snapshot,
      features = features,
      consensusYes = consensusYes,
      consensusNo = consensusNo,
      consensusConfidence = consensusConfidence,
      evaluationTimeMs = nowMs
    )

    for (filter in riskFilters) {
      val filterResult = filter.evaluate(context)
      logger.logEvent(
        SignalEngineLogEvent.RiskGateEvaluated(
          timestampMs = nowMs,
          gateName = filter.name,
          passed = filterResult.passed,
          reason = filterResult.message
        )
      )
      if (!filterResult.passed && filterResult.riskFlag != null) {
        triggeredFlags.add(filterResult.riskFlag)
      }
    }

    // Step 5: Final Decision Determination (Fail-closed principle)
    val (decision, reason) = when {
      triggeredFlags.isNotEmpty() -> {
        SignalDecision.NO_TRADE to "Risk filters triggered: ${triggeredFlags.joinToString { it.name }}"
      }
      consensusYes >= config.confidenceThreshold && consensusYes > consensusNo -> {
        SignalDecision.YES to "Model consensus YES meets confidence threshold (${consensusYes.format2()})"
      }
      consensusNo >= config.confidenceThreshold && consensusNo > consensusYes -> {
        SignalDecision.NO to "Model consensus NO meets confidence threshold (${consensusNo.format2()})"
      }
      else -> {
        triggeredFlags.add(RiskFlag.LOW_CONFIDENCE)
        SignalDecision.NO_TRADE to "Consensus confidence (${consensusConfidence.format2()}) below threshold (${config.confidenceThreshold})"
      }
    }

    val latencyMs = (System.nanoTime() - cycleStartNanos) / 1_000_000L

    val signal = SignalOutput(
      decision = decision,
      contractTicker = contract.ticker,
      strikePrice = contract.targetStrike,
      targetExpirationMs = contract.expirationTimestampMs,
      confidence = consensusConfidence,
      consensusYesScore = consensusYes,
      consensusNoScore = consensusNo,
      riskFlags = triggeredFlags,
      decisionReason = reason,
      timestampMs = nowMs,
      executionLatencyMs = latencyMs
    )

    _latestSignal.value = signal
    val history = _signalHistory.value.toMutableList()
    history.add(0, signal)
    if (history.size > maxHistory) {
      history.removeAt(history.lastIndex)
    }
    _signalHistory.value = history

    _engineState.value = SignalEngineState.Evaluated(
      contractTicker = contract.ticker,
      decision = decision,
      confidence = consensusConfidence,
      completedAtMs = nowMs
    )

    logger.logEvent(
      SignalEngineLogEvent.SignalEmitted(
        timestampMs = nowMs,
        contractTicker = contract.ticker,
        decision = decision.name,
        confidence = consensusConfidence,
        riskFlags = triggeredFlags.map { it.name },
        reason = reason
      )
    )

    return signal
  }

  private fun Double.format2(): String = String.format(java.util.Locale.US, "%.2f", this)
}
