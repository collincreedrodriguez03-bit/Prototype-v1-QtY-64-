package com.example.prediction

import com.example.data.model.BtcMarketObservation
import com.example.data.model.ContractStateObservation
import com.example.data.model.KalshiContract
import com.example.prediction.engines.TrendModelEngine
import com.example.prediction.engines.VolatilityModelEngine
import com.example.ensemble.DefaultEnsembleBoundary
import com.example.ensemble.SignalDecision
import com.example.quant.QuantitativeFeatureVector
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuantitativeModelIsolationTest {

  @Test
  fun `all quantitative models operate in isolation and return structured results`() = runBlocking {
    val now = 1700000000000L
    val contract = KalshiContract(
      ticker = "KXBTC-15M-TEST",
      targetStrike = 95000.0,
      openTimestampMs = now - 60000L,
      expirationTimestampMs = now + 840000L
    )
    val contractState = ContractStateObservation(
      ticker = contract.ticker,
      targetStrike = contract.targetStrike,
      openReferencePrice = 94800.0,
      currentSpotPrice = 95200.0,
      openTimestampMs = contract.openTimestampMs,
      expirationTimestampMs = contract.expirationTimestampMs,
      evaluationTimestampMs = now
    )
    val marketObs = BtcMarketObservation(
      timestampMs = now,
      price = 95200.0,
      bidPrice = 95195.0,
      askPrice = 95205.0
    )
    val featureVector = QuantitativeFeatureVector(
      timestampMs = now,
      sampleSize = 20,
      features = mapOf("trend" to 0.5),
      isValid = true
    )
    val packet = ModelObservationPacket(
      contractState = contractState,
      marketObservation = marketObs,
      featureVector = featureVector,
      evaluationTimestampMs = now
    )

    val trendEngine = TrendModelEngine()
    val volEngine = VolatilityModelEngine()

    val trendResult = trendEngine.evaluate(contract, packet)
    val volResult = volEngine.evaluate(contract, packet)

    assertEquals("MODEL_TREND_01", trendResult.modelId)
    assertEquals("Trend", trendResult.modelCategory)
    assertEquals(ModelDirection.NEUTRAL_NO_TRADE, trendResult.direction)
    assertTrue(trendResult.dataSufficiency)

    assertEquals("MODEL_VOLATILITY_01", volResult.modelId)
    assertEquals("Volatility", volResult.modelCategory)
    assertTrue(volResult.dataSufficiency)
  }

  @Test
  fun `ensemble boundary correctly defaults to NO_TRADE when models are insufficient`() {
    val boundary = DefaultEnsembleBoundary()
    val insufficientResult = QuantitativeModelResult(
      modelId = "TEST_MODEL",
      modelCategory = "Test",
      direction = ModelDirection.BULLISH_YES,
      strengthScore = 0.8,
      confidence = 0.9,
      dataSufficiency = false, // insufficient data
      evidenceSummary = "Insufficient data"
    )

    val consensus = boundary.evaluateEnsemble("KXBTC-15M-TEST", listOf(insufficientResult))
    assertEquals(SignalDecision.NO_TRADE, consensus.finalSignal)
    assertTrue(consensus.aggregateConfidence == 0.0)
  }
}
