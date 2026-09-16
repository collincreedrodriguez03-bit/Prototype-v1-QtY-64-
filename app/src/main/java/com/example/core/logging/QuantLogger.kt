package com.example.core.logging

import kotlinx.coroutines.flow.StateFlow

enum class QuantLogLevel {
  DEBUG, INFO, WARN, ERROR
}

/**
 * QuantLogger interface defining strict telemetry and audit logging boundaries.
 */
interface QuantLogger {
  val recentEvents: StateFlow<List<SignalEngineLogEvent>>

  fun logEvent(event: SignalEngineLogEvent)
  fun debug(tag: String, message: String)
  fun info(tag: String, message: String)
  fun warn(tag: String, message: String)
  fun error(tag: String, message: String, throwable: Throwable? = null)
}
