package com.example.data.features

import com.example.data.model.BtcMarketObservation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrendQuantitativeEngineTest {

  @Test
  fun `trend engine correctly identifies upward directional structure`() {
    val evalTime = 1700000000000L
    val observations = listOf(
      BtcMarketObservation(timestampMs = evalTime - 50000L, price = 95000.0),
      BtcMarketObservation(timestampMs = evalTime - 40000L, price = 95100.0),
      BtcMarketObservation(timestampMs = evalTime - 30000L, price = 95200.0),
      BtcMarketObservation(timestampMs = evalTime - 20000L, price = 95400.0),
      BtcMarketObservation(timestampMs = evalTime - 10000L, price = 95600.0)
    )

    val evidence = TrendQuantitativeEngine.evaluateTrend(
      observations = observations,
      evaluationTimestampMs = evalTime,
      referenceStrikePrice = 95000.0,
      minRequiredObservations = 3
    )

    assertTrue(evidence.isUsable)
    assertEquals(TrendDirection.UP, evidence.direction)
    assertTrue(evidence.rawScore > 0.0)
  }

  @Test
  fun `trend engine correctly identifies downward directional structure`() {
    val evalTime = 1700000000000L
    val observations = listOf(
      BtcMarketObservation(timestampMs = evalTime - 50000L, price = 96000.0),
      BtcMarketObservation(timestampMs = evalTime - 40000L, price = 95800.0),
      BtcMarketObservation(timestampMs = evalTime - 30000L, price = 95600.0),
      BtcMarketObservation(timestampMs = evalTime - 20000L, price = 95400.0),
      BtcMarketObservation(timestampMs = evalTime - 10000L, price = 95200.0)
    )

    val evidence = TrendQuantitativeEngine.evaluateTrend(
      observations = observations,
      evaluationTimestampMs = evalTime,
      referenceStrikePrice = 95500.0,
      minRequiredObservations = 3
    )

    assertTrue(evidence.isUsable)
    assertEquals(TrendDirection.DOWN, evidence.direction)
    assertTrue(evidence.rawScore < 0.0)
  }

  @Test
  fun `trend engine handles flat prices neutrally`() {
    val evalTime = 1700000000000L
    val observations = listOf(
      BtcMarketObservation(timestampMs = evalTime - 40000L, price = 95000.0),
      BtcMarketObservation(timestampMs = evalTime - 30000L, price = 95000.0),
      BtcMarketObservation(timestampMs = evalTime - 20000L, price = 95000.0),
      BtcMarketObservation(timestampMs = evalTime - 10000L, price = 95000.0)
    )

    val evidence = TrendQuantitativeEngine.evaluateTrend(
      observations = observations,
      evaluationTimestampMs = evalTime,
      minRequiredObservations = 3
    )

    assertTrue(evidence.isUsable)
    assertEquals(TrendDirection.NEUTRAL, evidence.direction)
    assertEquals(0.0, evidence.rawScore, 0.001)
  }

  @Test
  fun `trend engine reports insufficient history correctly`() {
    val evalTime = 1700000000000L
    val observations = listOf(
      BtcMarketObservation(timestampMs = evalTime - 10000L, price = 95000.0)
    )

    val evidence = TrendQuantitativeEngine.evaluateTrend(
      observations = observations,
      evaluationTimestampMs = evalTime,
      minRequiredObservations = 3
    )

    assertFalse(evidence.dataSufficiency)
    assertFalse(evidence.isUsable)
    assertEquals(TrendDirection.NEUTRAL, evidence.direction)
  }

  @Test
  fun `trend engine strictly prevents temporal leakage from future observations`() {
    val evalTime = 1700000000000L
    val observations = listOf(
      BtcMarketObservation(timestampMs = evalTime - 10000L, price = 95000.0),
      BtcMarketObservation(timestampMs = evalTime + 5000L, price = 99000.0) // Future observation
    )

    val evidence = TrendQuantitativeEngine.evaluateTrend(
      observations = observations,
      evaluationTimestampMs = evalTime,
      minRequiredObservations = 2
    )

    // Future observation excluded -> only 1 valid observation remaining -> insufficient history
    assertFalse(evidence.dataSufficiency)
    assertFalse(evidence.isUsable)
  }
}
