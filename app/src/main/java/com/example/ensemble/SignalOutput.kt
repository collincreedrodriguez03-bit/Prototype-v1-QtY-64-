package com.example.ensemble

import java.util.UUID

/**
 * Immutable final output of the QtY 64 Quantitative Signal Engine.
 * Represents an analytical evaluation for a specific 15-minute Kalshi contract.
 */
data class SignalOutput(
  val id: String = UUID.randomUUID().toString(),
  val decision: SignalDecision,
  val contractTicker: String,
  val strikePrice: Double,
  val targetExpirationMs: Long,
  val confidence: Double,
  val consensusYesScore: Double,
  val consensusNoScore: Double,
  val riskFlags: Set<RiskFlag> = emptySet(),
  val decisionReason: String,
  val timestampMs: Long = System.currentTimeMillis(),
  val executionLatencyMs: Long = 0
) {
  val hasRiskFlags: Boolean get() = riskFlags.isNotEmpty()
  val isActionableSignal: Boolean get() = decision != SignalDecision.NO_TRADE
}
