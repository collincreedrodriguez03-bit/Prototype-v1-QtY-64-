package com.example.data.features

import com.example.data.model.BtcMarketObservation
import com.example.data.model.DataQualityGrade
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketRegimeDataQualityEngineTest {

  @Test
  fun `regime quality engine validates high quality observations`() {
    val evalTime = 1700000000000L
    val observations = (0..10).map { i ->
      BtcMarketObservation(
        timestampMs = evalTime - ((10 - i) * 1000L),
        price = 95000.0 + i,
        qualityStatus = DataQualityGrade.VALID
      )
    }

    val evidence = MarketRegimeDataQualityEngine.evaluateRegimeAndQuality(
      observations = observations,
      evaluationTimestampMs = evalTime,
      minRequiredObservations = 5
    )

    assertTrue(evidence.isUsable)
    assertEquals(DataReliabilityGrade.HIGH, evidence.reliabilityGrade)
    assertFalse(evidence.isStale)
    assertTrue(evidence.dataQualityScore > 0.8)
  }

  @Test
  fun `regime quality engine flags stale data as invalid`() {
    val evalTime = 1700000000000L
    val observations = listOf(
      BtcMarketObservation(timestampMs = evalTime - 120000L, price = 95000.0) // 2 mins old (> 60s threshold)
    )

    val evidence = MarketRegimeDataQualityEngine.evaluateRegimeAndQuality(
      observations = observations,
      evaluationTimestampMs = evalTime,
      minRequiredObservations = 1
    )

    assertFalse(evidence.isUsable)
    assertEquals(DataReliabilityGrade.INVALID, evidence.reliabilityGrade)
    assertTrue(evidence.isStale)
  }
}
