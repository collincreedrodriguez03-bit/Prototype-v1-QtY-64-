package com.example.data.features

import com.example.data.model.BtcMarketObservation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RsiMeanReversionQuantitativeEngineTest {

  @Test
  fun `rsi engine identifies oversold conditions correctly`() {
    val evalTime = 1700000000000L
    // Constructing a declining price series to create an oversold RSI (< 30)
    val prices = listOf(
      100.0, 99.0, 98.5, 98.0, 97.5,
      97.0, 96.5, 96.0, 95.5, 95.0,
      94.5, 94.0, 93.5, 93.0, 92.0
    )
    val observations = prices.mapIndexed { index, price ->
      BtcMarketObservation(
        timestampMs = evalTime - ((prices.size - index) * 10000L),
        price = price
      )
    }

    val evidence = RsiMeanReversionQuantitativeEngine.evaluateMeanReversion(
      observations = observations,
      evaluationTimestampMs = evalTime,
      minRequiredObservations = 10
    )

    assertTrue(evidence.isUsable)
    val rsi = evidence.componentMeasurements["rsi"]!!
    assertTrue(rsi < 30.0)
    assertEquals(MeanReversionDirection.UP, evidence.direction)
    assertTrue(evidence.rawScore > 0.0)
  }

  @Test
  fun `rsi engine identifies overbought conditions correctly`() {
    val evalTime = 1700000000000L
    // Constructing an ascending price series to create an overbought RSI (> 70)
    val prices = listOf(
      100.0, 101.0, 101.5, 102.0, 102.5,
      103.0, 103.5, 104.0, 104.5, 105.0,
      105.5, 106.0, 106.5, 107.0, 108.0
    )
    val observations = prices.mapIndexed { index, price ->
      BtcMarketObservation(
        timestampMs = evalTime - ((prices.size - index) * 10000L),
        price = price
      )
    }

    val evidence = RsiMeanReversionQuantitativeEngine.evaluateMeanReversion(
      observations = observations,
      evaluationTimestampMs = evalTime,
      minRequiredObservations = 10
    )

    assertTrue(evidence.isUsable)
    val rsi = evidence.componentMeasurements["rsi"]!!
    assertTrue(rsi > 70.0)
    assertEquals(MeanReversionDirection.DOWN, evidence.direction)
    assertTrue(evidence.rawScore < 0.0)
  }

  @Test
  fun `rsi engine handles flat prices neutrally`() {
    val evalTime = 1700000000000L
    val observations = (0..15).map { i ->
      BtcMarketObservation(
        timestampMs = evalTime - ((15 - i) * 10000L),
        price = 95000.0
      )
    }

    val evidence = RsiMeanReversionQuantitativeEngine.evaluateMeanReversion(
      observations = observations,
      evaluationTimestampMs = evalTime,
      minRequiredObservations = 10
    )

    assertTrue(evidence.isUsable)
    assertEquals(MeanReversionDirection.NEUTRAL, evidence.direction)
    assertEquals(0.0, evidence.rawScore, 0.05)
  }

  @Test
  fun `rsi engine reports insufficient history correctly`() {
    val evalTime = 1700000000000L
    val observations = listOf(
      BtcMarketObservation(timestampMs = evalTime - 10000L, price = 95000.0)
    )

    val evidence = RsiMeanReversionQuantitativeEngine.evaluateMeanReversion(
      observations = observations,
      evaluationTimestampMs = evalTime,
      minRequiredObservations = 15
    )

    assertFalse(evidence.dataSufficiency)
    assertFalse(evidence.isUsable)
    assertEquals(MeanReversionDirection.NEUTRAL, evidence.direction)
  }

  @Test
  fun `rsi engine enforces timestamp integrity against future leakage`() {
    val evalTime = 1700000000000L
    val observations = (0..10).map { i ->
      BtcMarketObservation(
        timestampMs = evalTime - ((10 - i) * 1000L),
        price = 95000.0 + i
      )
    } + BtcMarketObservation(timestampMs = evalTime + 5000L, price = 99999.0) // Future leak

    val evidence = RsiMeanReversionQuantitativeEngine.evaluateMeanReversion(
      observations = observations,
      evaluationTimestampMs = evalTime,
      minRequiredObservations = 12
    )

    // Future observation excluded -> count 11 < minRequiredObservations (12) -> insufficient data
    assertFalse(evidence.dataSufficiency)
    assertFalse(evidence.isUsable)
  }
}
