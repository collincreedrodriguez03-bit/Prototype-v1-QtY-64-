package com.example.data.features

import com.example.data.model.BtcMarketObservation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StrikeTimeQuantitativeEngineTest {

  @Test
  fun `strike time engine evaluates price above strike correctly`() {
    val evalTime = 1700000010000L
    val observations = listOf(
      BtcMarketObservation(timestampMs = evalTime - 5000L, price = 95500.0)
    )

    val evidence = StrikeTimeQuantitativeEngine.evaluateStrikeTime(
      observations = observations,
      evaluationTimestampMs = evalTime,
      strikePrice = 95000.0,
      contractEndTimeMs = evalTime + 60000L,
      contractStartTimeMs = evalTime - 60000L
    )

    assertTrue(evidence.isUsable)
    assertEquals(StrikeRelation.ABOVE_STRIKE, evidence.relation)
    assertTrue(evidence.normalizedDistance > 0.0)
    assertTrue(evidence.rawScore > 0.0)
  }

  @Test
  fun `strike time engine evaluates price below strike correctly`() {
    val evalTime = 1700000010000L
    val observations = listOf(
      BtcMarketObservation(timestampMs = evalTime - 5000L, price = 94500.0)
    )

    val evidence = StrikeTimeQuantitativeEngine.evaluateStrikeTime(
      observations = observations,
      evaluationTimestampMs = evalTime,
      strikePrice = 95000.0,
      contractEndTimeMs = evalTime + 60000L,
      contractStartTimeMs = evalTime - 60000L
    )

    assertTrue(evidence.isUsable)
    assertEquals(StrikeRelation.BELOW_STRIKE, evidence.relation)
    assertTrue(evidence.normalizedDistance < 0.0)
    assertTrue(evidence.rawScore < 0.0)
  }

  @Test
  fun `strike time engine handles exact strike equality`() {
    val evalTime = 1700000010000L
    val observations = listOf(
      BtcMarketObservation(timestampMs = evalTime - 5000L, price = 95000.0)
    )

    val evidence = StrikeTimeQuantitativeEngine.evaluateStrikeTime(
      observations = observations,
      evaluationTimestampMs = evalTime,
      strikePrice = 95000.0,
      contractEndTimeMs = evalTime + 60000L,
      contractStartTimeMs = evalTime - 60000L
    )

    assertTrue(evidence.isUsable)
    assertEquals(StrikeRelation.AT_STRIKE, evidence.relation)
    assertEquals(0.0, evidence.normalizedDistance, 0.0001)
  }

  @Test
  fun `strike time engine handles insufficient history or zero strike`() {
    val evalTime = 1700000010000L
    val observations = emptyList<BtcMarketObservation>()

    val evidence = StrikeTimeQuantitativeEngine.evaluateStrikeTime(
      observations = observations,
      evaluationTimestampMs = evalTime,
      strikePrice = 95000.0,
      contractEndTimeMs = evalTime + 60000L,
      contractStartTimeMs = evalTime - 60000L
    )

    assertFalse(evidence.dataSufficiency)
    assertFalse(evidence.isUsable)
    assertEquals(StrikeRelation.UNKNOWN, evidence.relation)
  }

  @Test
  fun `strike time engine enforces temporal leakage rejection`() {
    val evalTime = 1700000010000L
    val observations = listOf(
      BtcMarketObservation(timestampMs = evalTime + 5000L, price = 99999.0) // Future leak
    )

    val evidence = StrikeTimeQuantitativeEngine.evaluateStrikeTime(
      observations = observations,
      evaluationTimestampMs = evalTime,
      strikePrice = 95000.0,
      contractEndTimeMs = evalTime + 60000L,
      contractStartTimeMs = evalTime - 60000L
    )

    // Future observation excluded -> observations list empty -> data insufficient
    assertFalse(evidence.dataSufficiency)
    assertFalse(evidence.isUsable)
  }
}
