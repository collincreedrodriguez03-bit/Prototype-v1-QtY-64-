package com.example

import com.example.core.config.SignalEngineConfig
import com.example.data.model.KalshiContract
import com.example.data.model.MarketDataFeedState
import com.example.data.model.MarketSnapshot
import com.example.ensemble.ConfidenceThresholdFilter
import com.example.ensemble.DecisionContext
import com.example.ensemble.ExpiryBufferRiskFilter
import com.example.ensemble.FreshnessRiskFilter
import com.example.ensemble.RiskFlag
import com.example.ensemble.SpreadRiskFilter
import com.example.quant.QuantitativeFeatureVector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RiskFilterTest {

  private lateinit var config: SignalEngineConfig
  private lateinit var validContract: KalshiContract
  private lateinit var features: QuantitativeFeatureVector

  @Before
  fun setup() {
    config = SignalEngineConfig(
      staleDataTimeoutMs = 10_000L,
      maxSpreadUsd = 20.0,
      minExpiryBufferSeconds = 60L,
      confidenceThreshold = 0.70
    )
    val now = System.currentTimeMillis()
    validContract = KalshiContract(
      ticker = "KXBTC-15M-TEST",
      targetStrike = 95000.0,
      openTimestampMs = now - 60_000L,
      expirationTimestampMs = now + 300_000L // 5 mins left
    )
    features = QuantitativeFeatureVector(
      timestampMs = now,
      sampleSize = 60,
      features = mapOf("price" to 95000.0),
      isValid = true
    )
  }

  @Test
  fun `FreshnessRiskFilter blocks stale snapshot`() {
    val now = System.currentTimeMillis()
    val filter = FreshnessRiskFilter()
    val staleSnapshot = MarketSnapshot(
      timestampMs = now - 20_000L, // 20s old, limit is 10s
      symbol = "BTC/USD",
      lastPrice = 95000.0,
      bidPrice = 94995.0,
      askPrice = 95005.0,
      volume24h = 100.0,
      high24h = 96000.0,
      low24h = 94000.0
    )
    val context = DecisionContext(
      config = config,
      contract = validContract,
      feedState = MarketDataFeedState.Connected("Feed", now, 10),
      snapshot = staleSnapshot,
      features = features,
      consensusYes = 0.8,
      consensusNo = 0.2,
      consensusConfidence = 0.8,
      evaluationTimeMs = now
    )

    val result = filter.evaluate(context)
    assertFalse(result.passed)
    assertEquals(RiskFlag.STALE_MARKET_DATA, result.riskFlag)
  }

  @Test
  fun `SpreadRiskFilter blocks wide bid-ask spread`() {
    val now = System.currentTimeMillis()
    val filter = SpreadRiskFilter()
    val wideSpreadSnapshot = MarketSnapshot(
      timestampMs = now,
      symbol = "BTC/USD",
      lastPrice = 95000.0,
      bidPrice = 94980.0,
      askPrice = 95010.0, // spread = 30.0, max is 20.0
      volume24h = 100.0,
      high24h = 96000.0,
      low24h = 94000.0
    )
    val context = DecisionContext(
      config = config,
      contract = validContract,
      feedState = MarketDataFeedState.Connected("Feed", now, 10),
      snapshot = wideSpreadSnapshot,
      features = features,
      consensusYes = 0.8,
      consensusNo = 0.2,
      consensusConfidence = 0.8,
      evaluationTimeMs = now
    )

    val result = filter.evaluate(context)
    assertFalse(result.passed)
    assertEquals(RiskFlag.SPREAD_EXCEEDS_THRESHOLD, result.riskFlag)
  }

  @Test
  fun `ExpiryBufferRiskFilter blocks when close to expiration`() {
    val now = System.currentTimeMillis()
    val filter = ExpiryBufferRiskFilter()
    val expiringContract = validContract.copy(
      expirationTimestampMs = now + 30_000L // only 30s left, min is 60s
    )
    val context = DecisionContext(
      config = config,
      contract = expiringContract,
      feedState = MarketDataFeedState.Connected("Feed", now, 10),
      snapshot = null,
      features = features,
      consensusYes = 0.8,
      consensusNo = 0.2,
      consensusConfidence = 0.8,
      evaluationTimeMs = now
    )

    val result = filter.evaluate(context)
    assertFalse(result.passed)
    assertEquals(RiskFlag.EXPIRY_WINDOW_CLOSED, result.riskFlag)
  }

  @Test
  fun `ConfidenceThresholdFilter passes when confidence meets threshold`() {
    val now = System.currentTimeMillis()
    val filter = ConfidenceThresholdFilter()
    val context = DecisionContext(
      config = config,
      contract = validContract,
      feedState = MarketDataFeedState.Connected("Feed", now, 10),
      snapshot = null,
      features = features,
      consensusYes = 0.85,
      consensusNo = 0.15,
      consensusConfidence = 0.85, // > 0.70 threshold
      evaluationTimeMs = now
    )

    val result = filter.evaluate(context)
    assertTrue(result.passed)
  }
}
