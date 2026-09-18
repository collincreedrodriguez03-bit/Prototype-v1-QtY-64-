package com.example.data.features

import com.example.data.model.BtcMarketObservation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MomentumQuantitativeEngineTest {

  @Test
  fun `momentum engine correctly identifies positive momentum`() {
    val evalTime = 1700000000000L
    val observations = listOf(
      BtcMarketObservation(timestampMs = evalTime - 100000L, price = 95000.0),
      BtcMarketObservation(timestampMs = evalTime - 75000L, price = 95200.0),
      BtcMarketObservation(timestampMs = evalTime - 50000L, price = 95400.0),
      BtcMarketObservation(timestampMs = evalTime - 25000L, price = 95700.0),
      BtcMarketObservation(timestampMs = evalTime - 5000L, price = 96000.0)
    )

    val evidence = MomentumQuantitativeEngine.evaluateMomentum(
      observations = observations,
      evaluationTimestampMs = evalTime,
      minRequiredObservations = 3
    )

    assertTrue(evidence.isUsable)
    assertEquals(MomentumDirection.UP, evidence.direction)
    assertTrue(evidence.rawScore > 0.0)
  }

  @Test
  fun `momentum engine correctly identifies negative momentum`() {
    val evalTime = 1700000000000L
    val observations = listOf(
      BtcMarketObservation(timestampMs = evalTime - 100000L, price = 96000.0),
      BtcMarketObservation(timestampMs = evalTime - 75000L, price = 95800.0),
      BtcMarketObservation(timestampMs = evalTime - 50000L, price = 95600.0),
      BtcMarketObservation(timestampMs = evalTime - 25000L, price = 95300.0),
      BtcMarketObservation(timestampMs = evalTime - 5000L, price = 95000.0)
    )

    val evidence = MomentumQuantitativeEngine.evaluateMomentum(
      observations = observations,
      evaluationTimestampMs = evalTime,
      minRequiredObservations = 3
    )

    assertTrue(evidence.isUsable)
    assertEquals(MomentumDirection.DOWN, evidence.direction)
    assertTrue(evidence.rawScore < 0.0)
  }

  @Test
  fun `momentum engine handles zero or near zero movement neutrally`() {
    val evalTime = 1700000000000L
    val observations = listOf(
      BtcMarketObservation(timestampMs = evalTime - 50000L, price = 95000.0),
      BtcMarketObservation(timestampMs = evalTime - 30000L, price = 95000.0),
      BtcMarketObservation(timestampMs = evalTime - 10000L, price = 95000.0)
    )

    val evidence = MomentumQuantitativeEngine.evaluateMomentum(
      observations = observations,
      evaluationTimestampMs = evalTime,
      minRequiredObservations = 3
    )

    assertTrue(evidence.isUsable)
    assertEquals(MomentumDirection.NEUTRAL, evidence.direction)
    assertEquals(0.0, evidence.rawScore, 0.001)
  }

  @Test
  fun `momentum engine reports insufficient history correctly`() {
    val evalTime = 1700000000000L
    val observations = listOf(
      BtcMarketObservation(timestampMs = evalTime - 10000L, price = 95000.0)
    )

    val evidence = MomentumQuantitativeEngine.evaluateMomentum(
      observations = observations,
      evaluationTimestampMs = evalTime,
      minRequiredObservations = 5
    )

    assertFalse(evidence.dataSufficiency)
    assertFalse(evidence.isUsable)
    assertEquals(MomentumDirection.NEUTRAL, evidence.direction)
  }

  @Test
  fun `momentum engine strictly enforces timestamp integrity against future leakage`() {
    val evalTime = 1700000000000L
    val observations = listOf(
      BtcMarketObservation(timestampMs = evalTime - 20000L, price = 95000.0),
      BtcMarketObservation(timestampMs = evalTime - 10000L, price = 95100.0),
      BtcMarketObservation(timestampMs = evalTime + 5000L, price = 99999.0) // Future leak
    )

    val evidence = MomentumQuantitativeEngine.evaluateMomentum(
      observations = observations,
      evaluationTimestampMs = evalTime,
      minRequiredObservations = 3
    )

    // Future observation excluded -> only 2 valid observations remain -> insufficient history
    assertFalse(evidence.dataSufficiency)
    assertFalse(evidence.isUsable)
  }
}
