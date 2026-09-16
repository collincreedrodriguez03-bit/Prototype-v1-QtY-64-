package com.example.core.logging

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidQuantLogger(
  private val maxBufferSize: Int = 100
) : QuantLogger {

  private val _recentEvents = MutableStateFlow<List<SignalEngineLogEvent>>(emptyList())
  override val recentEvents: StateFlow<List<SignalEngineLogEvent>> = _recentEvents.asStateFlow()

  @Synchronized
  override fun logEvent(event: SignalEngineLogEvent) {
    val current = _recentEvents.value.toMutableList()
    if (current.size >= maxBufferSize) {
      current.removeAt(0)
    }
    current.add(event)
    _recentEvents.value = current

    val tag = "QtY64-Engine"
    try {
      when (event) {
        is SignalEngineLogEvent.SignalEmitted ->
          Log.i(tag, "[SIGNAL] ${event.decision} (${event.contractTicker}) conf=${event.confidence} flags=${event.riskFlags} reason=${event.reason}")
        is SignalEngineLogEvent.RiskGateEvaluated ->
          if (!event.passed) Log.w(tag, "[RISK_FAIL] ${event.gateName}: ${event.reason}")
          else Log.d(tag, "[RISK_PASS] ${event.gateName}")
        is SignalEngineLogEvent.ModelEvaluated ->
          Log.d(tag, "[MODEL] ${event.modelId} P(YES)=${event.probabilityYes} P(NO)=${event.probabilityNo} conf=${event.confidence}")
        is SignalEngineLogEvent.FeatureExtractionCompleted ->
          Log.d(tag, "[FEATURES] count=${event.featureCount} valid=${event.isValid} durationNanos=${event.computeDurationNanos}")
        is SignalEngineLogEvent.MarketDataReceived ->
          Log.d(tag, "[DATA] ${event.symbol} price=${event.lastPrice} spread=${event.spread}")
        is SignalEngineLogEvent.EngineStateChanged ->
          Log.i(tag, "[STATE] ${event.previousState} -> ${event.newState} (${event.detail.orEmpty()})")
        is SignalEngineLogEvent.SystemMessage ->
          when (event.level) {
            "ERROR" -> Log.e(event.tag, event.message)
            "WARN" -> Log.w(event.tag, event.message)
            "INFO" -> Log.i(event.tag, event.message)
            else -> Log.d(event.tag, event.message)
          }
      }
    } catch (_: Throwable) {
      // In local JVM tests where android.util.Log is not mocked, in-memory state is maintained
    }
  }

  override fun debug(tag: String, message: String) {
    logEvent(SignalEngineLogEvent.SystemMessage(System.currentTimeMillis(), "DEBUG", tag, message))
  }

  override fun info(tag: String, message: String) {
    logEvent(SignalEngineLogEvent.SystemMessage(System.currentTimeMillis(), "INFO", tag, message))
  }

  override fun warn(tag: String, message: String) {
    logEvent(SignalEngineLogEvent.SystemMessage(System.currentTimeMillis(), "WARN", tag, message))
  }

  override fun error(tag: String, message: String, throwable: Throwable?) {
    val fullMessage = if (throwable != null) "$message | ${throwable.message}" else message
    logEvent(SignalEngineLogEvent.SystemMessage(System.currentTimeMillis(), "ERROR", tag, fullMessage))
  }
}
