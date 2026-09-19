package com.example.data.features

import com.example.data.model.BtcMarketObservation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VolatilityQuantitativeEngineTest {

  @Test
  fun `volatility engine identifies zero volatility on constant prices`() {
    val evalTime = 1700000000000L
    val observations = (0..10).map { i ->
      BtcMarketObservation(
        timestampMs = evalTime - ((10 - i) * 1000L),
        price = 95000.0
      )
    }

    val evidence = VolatilityQuantitativeEngine.evaluateVolatility(
      observations = observations,
      evaluationTimestampMs = evalTime,
      minRequiredObservations = 5
    )

    assertTrue(evidence.isUsable)
    assertEquals(0.0, evidence.realizedVolatility, 0.0001)
    assertEquals(VolatilityRegime.LOW, evidence.regime)
  }

  @Test
  fun `volatility engine detects volatility expansion`() {
    val evalTime = 1700000000000L
    // Stable prices followed by wild swings
    val prices = listOf(
      95000.0, 95010.0, 95005.0, 95012.0, 95008.0,
      95200.0, 94800.0, 95500.0, 94300.0, 96000.0
    )
    val observations = prices.mapIndexed { index, price ->
      BtcMarketObservation(
        timestampMs = evalTime - ((prices.size - index) * 1000L),
        price = price
      )
    }

    val evidence = VolatilityQuantitativeEngine.evaluateVolatility(
      observations = observations,
      evaluationTimestampMs = evalTime,
      minRequiredObservations = 5
    )

    assertTrue(evidence.isUsable)
    assertTrue(evidence.realizedVolatility > 0.0)
    assertTrue(evidence.volatilityRatio > 1.0)
  }

  @Test
  fun `volatility engine reports insufficient observations correctly`() {
    val evalTime = 1700000000000L
    val observations = listOf(
      BtcMarketObservation(timestampMs = evalTime - 1000L, price = 95000.0)
    )

    val evidence = VolatilityQuantitativeEngine.evaluateVolatility(
      observations = observations,
      evaluationTimestampMs = evalTime,
      minRequiredObservations = 5
    )

    assertFalse(evidence.dataSufficiency)
    assertFalse(evidence.isUsable)
    assertEquals(VolatilityRegime.UNKNOWN, evidence.regime)
  }

  @Test
  fun `volatility engine enforces future timestamp leakage rejection`() {
    val evalTime = 1700000000000L
    val observations = listOf(
      BtcMarketObservation(timestampMs = evalTime - 2000L, price = 95000.0),
      BtcMarketObservation(timestampMs = evalTime - 1000L, price = 95100.0),
      BtcMarketObservation(timestampMs = evalTime + 5000L, price = 99999.0) // Future leak
    )

    val evidence = VolatilityQuantitativeEngine.evaluateVolatility(
      observations = observations,
      evaluationTimestampMs = evalTime,
      minRequiredObservations = 3
    )

    // Future observation excluded -> 2 valid observations < 3 minRequired -> insufficient data
    assertFalse(evidence.dataSufficiency)
    assertFalse(evidence.isUsable)
  }
}
