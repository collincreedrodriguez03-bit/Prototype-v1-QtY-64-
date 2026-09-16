package com.example.ensemble

import com.example.core.config.SignalEngineConfig
import com.example.data.model.KalshiContract
import com.example.data.model.MarketDataFeedState
import com.example.data.model.MarketSnapshot
import com.example.quant.QuantitativeFeatureVector

/**
 * Context payload provided to each RiskFilter gate during the evaluation cycle.
 */
data class DecisionContext(
  val config: SignalEngineConfig,
  val contract: KalshiContract,
  val feedState: MarketDataFeedState,
  val snapshot: MarketSnapshot?,
  val features: QuantitativeFeatureVector,
  val consensusYes: Double,
  val consensusNo: Double,
  val consensusConfidence: Double,
  val evaluationTimeMs: Long = System.currentTimeMillis()
)

data class RiskFilterResult(
  val passed: Boolean,
  val riskFlag: RiskFlag? = null,
  val message: String
) {
  companion object {
    fun pass(message: String = "Passed"): RiskFilterResult = RiskFilterResult(true, null, message)
    fun block(flag: RiskFlag, reason: String): RiskFilterResult = RiskFilterResult(false, flag, reason)
  }
}

/**
 * Interface defining a quant risk gate.
 * Multiple filters compose into a fail-closed risk hierarchy.
 */
interface RiskFilter {
  val name: String
  fun evaluate(context: DecisionContext): RiskFilterResult
}

class FreshnessRiskFilter : RiskFilter {
  override val name: String = "FreshnessRiskFilter"

  override fun evaluate(context: DecisionContext): RiskFilterResult {
    val snapshot = context.snapshot
      ?: return RiskFilterResult.block(RiskFlag.FEED_DISCONNECTED, "No snapshot available")

    val ageMs = context.evaluationTimeMs - snapshot.timestampMs
    return if (ageMs > context.config.staleDataTimeoutMs) {
      RiskFilterResult.block(
        RiskFlag.STALE_MARKET_DATA,
        "Snapshot age (${ageMs}ms) exceeds limit (${context.config.staleDataTimeoutMs}ms)"
      )
    } else {
      RiskFilterResult.pass("Snapshot is fresh (${ageMs}ms old)")
    }
  }
}

class SpreadRiskFilter : RiskFilter {
  override val name: String = "SpreadRiskFilter"

  override fun evaluate(context: DecisionContext): RiskFilterResult {
    val snapshot = context.snapshot
      ?: return RiskFilterResult.block(RiskFlag.FEED_DISCONNECTED, "No snapshot available")

    return if (snapshot.spread > context.config.maxSpreadUsd) {
      RiskFilterResult.block(
        RiskFlag.SPREAD_EXCEEDS_THRESHOLD,
        "Spread ($${snapshot.spread}) exceeds threshold ($${context.config.maxSpreadUsd})"
      )
    } else {
      RiskFilterResult.pass("Spread ($${snapshot.spread}) within limit")
    }
  }
}

class ExpiryBufferRiskFilter : RiskFilter {
  override val name: String = "ExpiryBufferRiskFilter"

  override fun evaluate(context: DecisionContext): RiskFilterResult {
    val remainingSec = context.contract.remainingSeconds(context.evaluationTimeMs)
    return if (remainingSec < context.config.minExpiryBufferSeconds) {
      RiskFilterResult.block(
        RiskFlag.EXPIRY_WINDOW_CLOSED,
        "Remaining contract lifetime (${remainingSec}s) below buffer (${context.config.minExpiryBufferSeconds}s)"
      )
    } else {
      RiskFilterResult.pass("Remaining time (${remainingSec}s) is sufficient")
    }
  }
}

class ConfidenceThresholdFilter : RiskFilter {
  override val name: String = "ConfidenceThresholdFilter"

  override fun evaluate(context: DecisionContext): RiskFilterResult {
    return if (context.consensusConfidence < context.config.confidenceThreshold) {
      RiskFilterResult.block(
        RiskFlag.LOW_CONFIDENCE,
        "Confidence (${context.consensusConfidence}) below required threshold (${context.config.confidenceThreshold})"
      )
    } else {
      RiskFilterResult.pass("Confidence (${context.consensusConfidence}) meets threshold")
    }
  }
}
