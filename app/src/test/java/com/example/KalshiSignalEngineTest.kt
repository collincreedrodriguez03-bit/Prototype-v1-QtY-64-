package com.example

import com.example.core.config.SignalEngineConfig
import com.example.core.logging.AndroidQuantLogger
import com.example.data.model.Candle
import com.example.data.model.KalshiContract
import com.example.data.model.MarketDataFeedState
import com.example.data.model.MarketSnapshot
import com.example.ensemble.ConfidenceThresholdFilter
import com.example.ensemble.DefaultSignalEngine
import com.example.ensemble.ExpiryBufferRiskFilter
import com.example.ensemble.FreshnessRiskFilter
import com.example.ensemble.RiskFlag
import com.example.ensemble.SignalDecision
import com.example.ensemble.SpreadRiskFilter
import com.example.prediction.BaselinePredictionModel
import com.example.prediction.ModelPrediction
import com.example.prediction.PredictionModel
import com.example.prediction.PredictionStatus
import com.example.quant.DefaultFeatureExtractor
import com.example.quant.QuantitativeFeatureVector
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class KalshiSignalEngineTest {

  private lateinit var config: SignalEngineConfig
  private lateinit var logger: AndroidQuantLogger
  private lateinit var contract: KalshiContract

  @Before
  fun setup() {
    config = SignalEngineConfig(
      symbol = "BTC/USD",
      timeframeMinutes = 15,
      minHistoryCandles = 10,
      staleDataTimeoutMs = 15_000L,
      minExpiryBufferSeconds = 60L,
      confidenceThreshold = 0.65,
      maxSpreadUsd = 25.0
    )
    logger = AndroidQuantLogger()

    val now = System.currentTimeMillis()
    contract = KalshiContract(
      ticker = "KXBTC-15M-95000-TEST",
      targetStrike = 95000.0,
      openTimestampMs = now - 5 * 60 * 1000L,
      expirationTimestampMs = now + 10 * 60 * 1000L
    )
  }

  @Test
  fun `engine blocks trade when feed is disconnected`() = runBlocking {
    val engine = DefaultSignalEngine(
      config = config,
      featureExtractor = DefaultFeatureExtractor(),
      models = listOf(BaselinePredictionModel()),
      riskFilters = listOf(FreshnessRiskFilter()),
      logger = logger
    )

    val output = engine.evaluateCycle(
      contract = contract,
      candles = emptyList(),
      snapshot = null,
      feedState = MarketDataFeedState.AwaitingFeed("Exchange")
    )

    assertEquals(SignalDecision.NO_TRADE, output.decision)
    assertTrue(output.riskFlags.contains(RiskFlag.FEED_DISCONNECTED))
    assertFalse(output.isActionableSignal)
  }

  @Test
  fun `engine blocks trade when market snapshot is stale`() = runBlocking {
    val now = System.currentTimeMillis()
    val staleSnapshot = MarketSnapshot(
      timestampMs = now - 30_000L, // 30s old, limit is 15s
      symbol = "BTC/USD",
      lastPrice = 95200.0,
      bidPrice = 95195.0,
      askPrice = 95205.0,
      volume24h = 1000.0,
      high24h = 96000.0,
      low24h = 94000.0
    )

    val engine = DefaultSignalEngine(
      config = config,
      featureExtractor = DefaultFeatureExtractor(),
      models = listOf(BaselinePredictionModel()),
      riskFilters = listOf(FreshnessRiskFilter()),
      logger = logger
    )

    val output = engine.evaluateCycle(
      contract = contract,
      candles = generateValidCandles(15),
      snapshot = staleSnapshot,
      feedState = MarketDataFeedState.Connected("Exchange", now, 5)
    )

    assertEquals(SignalDecision.NO_TRADE, output.decision)
    assertTrue(output.riskFlags.contains(RiskFlag.STALE_MARKET_DATA))
  }

  @Test
  fun `engine emits YES when models reach high confidence consensus and all risk gates pass`() = runBlocking {
    val now = System.currentTimeMillis()
    val freshSnapshot = MarketSnapshot(
      timestampMs = now - 1000L,
      symbol = "BTC/USD",
      lastPrice = 95200.0,
      bidPrice = 95199.0,
      askPrice = 95201.0, // spread = 2.0 (well within max 25.0)
      volume24h = 1000.0,
      high24h = 96000.0,
      low24h = 94000.0
    )

    // A model that outputs high confidence YES
    val bullishModel = object : PredictionModel {
      override val modelId: String = "TEST_BULLISH_MODEL"
      override val modelVersion: String = "1.0"
      override suspend fun evaluate(
        contract: KalshiContract,
        features: QuantitativeFeatureVector
      ): ModelPrediction {
        return ModelPrediction(
          modelId = modelId,
          contractTicker = contract.ticker,
          probabilityYes = 0.85,
          probabilityNo = 0.15,
          confidence = 0.85,
          latencyNanos = 1000L,
          status = PredictionStatus.READY
        )
      }
    }

    val engine = DefaultSignalEngine(
      config = config,
      featureExtractor = DefaultFeatureExtractor(),
      models = listOf(bullishModel),
      riskFilters = listOf(
        FreshnessRiskFilter(),
        SpreadRiskFilter(),
        ExpiryBufferRiskFilter(),
        ConfidenceThresholdFilter()
      ),
      logger = logger
    )

    val output = engine.evaluateCycle(
      contract = contract,
      candles = generateValidCandles(15),
      snapshot = freshSnapshot,
      feedState = MarketDataFeedState.Connected("Exchange", now, 5)
    )

    assertEquals(SignalDecision.YES, output.decision)
    assertEquals(0.85, output.confidence, 0.001)
    assertTrue(output.riskFlags.isEmpty())
    assertTrue(output.isActionableSignal)
  }

  private fun generateValidCandles(count: Int): List<Candle> {
    val now = System.currentTimeMillis()
    return (0 until count).map { i ->
      val t = now - (count - i) * 60_000L
      Candle(
        timestampMs = t,
        open = 95000.0 + i,
        high = 95050.0 + i,
        low = 94950.0 + i,
        close = 95020.0 + i,
        volume = 10.0
      )
    }
  }
}
