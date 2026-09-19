package com.example.data.features

import com.example.data.model.BtcMarketObservation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketStructureQuantitativeEngineTest {

  @Test
  fun `market structure engine identifies breakout high`() {
    val evalTime = 1700000000000L
    val observations = listOf(
      BtcMarketObservation(timestampMs = evalTime - 50000L, price = 95000.0),
      BtcMarketObservation(timestampMs = evalTime - 30000L, price = 95200.0),
      BtcMarketObservation(timestampMs = evalTime - 10000L, price = 96000.0)
    )

    val evidence = MarketStructureQuantitativeEngine.evaluateMarketStructure(
      observations = observations,
      evaluationTimestampMs = evalTime,
      minRequiredObservations = 3
    )

    assertTrue(evidence.isUsable)
    assertEquals(MarketStructureRegime.BREAKOUT_HIGH, evidence.regime)
    assertTrue(evidence.rawScore > 0.5)
  }

  @Test
  fun `market structure engine identifies breakout low`() {
    val evalTime = 1700000000000L
    val observations = listOf(
      BtcMarketObservation(timestampMs = evalTime - 50000L, price = 96000.0),
      BtcMarketObservation(timestampMs = evalTime - 30000L, price = 95800.0),
      BtcMarketObservation(timestampMs = evalTime - 10000L, price = 95000.0)
    )

    val evidence = MarketStructureQuantitativeEngine.evaluateMarketStructure(
      observations = observations,
      evaluationTimestampMs = evalTime,
      minRequiredObservations = 3
    )

    assertTrue(evidence.isUsable)
    assertEquals(MarketStructureRegime.BREAKOUT_LOW, evidence.regime)
    assertTrue(evidence.rawScore < -0.5)
  }

  @Test
  fun `market structure handles insufficient history`() {
    val evalTime = 1700000000000L
    val observations = listOf(
      BtcMarketObservation(timestampMs = evalTime - 10000L, price = 95000.0)
    )

    val evidence = MarketStructureQuantitativeEngine.evaluateMarketStructure(
      observations = observations,
      evaluationTimestampMs = evalTime,
      minRequiredObservations = 3
    )

    assertFalse(evidence.dataSufficiency)
    assertFalse(evidence.isUsable)
    assertEquals(MarketStructureRegime.NEUTRAL, evidence.regime)
  }
}
