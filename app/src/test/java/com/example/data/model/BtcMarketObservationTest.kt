package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BtcMarketObservationTest {

  @Test
  fun `valid market observation passes validation and computes metrics`() {
    val obs = BtcMarketObservation(
      timestampMs = 1700000000000L,
      price = 95200.0,
      volume = 15.5,
      bidPrice = 95195.0,
      askPrice = 95205.0,
      qualityStatus = DataQualityGrade.VALID
    )

    assertEquals(95200.0, obs.price, 0.001)
    assertEquals(10.0, obs.spread!!, 0.001)
    assertEquals(95200.0, obs.midPrice!!, 0.001)

    val report = obs.validateTemporalIntegrity(referenceTimeMs = 1700000005000L, maxAgeMs = 10000L)
    assertTrue(report.grade.isUsable)
    assertFalse(report.isFailClosed)
  }

  @Test
  fun `observation from the future fails temporal integrity with out-of-order grade`() {
    val futureObs = BtcMarketObservation(
      timestampMs = 1700000010000L,
      price = 95200.0
    )

    val report = futureObs.validateTemporalIntegrity(referenceTimeMs = 1700000005000L, maxAgeMs = 10000L)
    assertEquals(DataQualityGrade.OUT_OF_ORDER, report.grade)
    assertTrue(report.isFailClosed)
  }

  @Test
  fun `stale observation fails temporal integrity with stale grade`() {
    val oldObs = BtcMarketObservation(
      timestampMs = 1700000000000L,
      price = 95200.0
    )

    val report = oldObs.validateTemporalIntegrity(referenceTimeMs = 1700000020000L, maxAgeMs = 10000L)
    assertEquals(DataQualityGrade.STALE, report.grade)
    assertTrue(report.isFailClosed)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `malformed price throws exception`() {
    BtcMarketObservation(
      timestampMs = 1700000000000L,
      price = -100.0
    )
  }

  @Test(expected = IllegalArgumentException::class)
  fun `invalid bid ask spread throws exception`() {
    BtcMarketObservation(
      timestampMs = 1700000000000L,
      price = 95200.0,
      bidPrice = 95210.0,
      askPrice = 95200.0 // ask less than bid
    )
  }
}
