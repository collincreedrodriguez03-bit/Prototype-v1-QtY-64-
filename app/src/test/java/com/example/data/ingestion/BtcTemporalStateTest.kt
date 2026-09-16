package com.example.data.ingestion

import com.example.data.model.BtcMarketObservation
import com.example.data.model.ContractTemporalState
import com.example.data.model.DataQualityGrade
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BtcTemporalStateTest {

  @Test
  fun `temporal buffer prevents future leakage and out-of-order insertion`() {
    val buffer = BtcTemporalObservationBuffer(maxCapacity = 10, maxStaleAgeMs = 60000L)
    val now = 1700000000000L

    val futureObs = BtcMarketObservation(timestampMs = now + 5000L, price = 95200.0)
    val report = buffer.ingest(futureObs, evaluationTimestampMs = now)

    assertEquals(DataQualityGrade.OUT_OF_ORDER, report.grade)
    assertTrue(report.isFailClosed)
    assertEquals(0, buffer.size)
  }

  @Test
  fun `temporal buffer enforces duplicate protection`() {
    val buffer = BtcTemporalObservationBuffer(maxCapacity = 10, maxStaleAgeMs = 60000L)
    val now = 1700000000000L

    val obs = BtcMarketObservation(timestampMs = now - 1000L, price = 95000.0)
    val report1 = buffer.ingest(obs, evaluationTimestampMs = now)
    val reportDup = buffer.ingest(obs, evaluationTimestampMs = now)

    assertTrue(report1.grade.isUsable)
    assertEquals(DataQualityGrade.DUPLICATED, reportDup.grade)
    assertTrue(reportDup.isFailClosed)
    assertEquals(1, buffer.size)
  }

  @Test
  fun `temporal buffer supports historical window queries and latest valid observation`() {
    val buffer = BtcTemporalObservationBuffer(maxCapacity = 10, maxStaleAgeMs = 60000L)
    val now = 1700000000000L

    val obs1 = BtcMarketObservation(timestampMs = now - 10000L, price = 94800.0)
    val obs2 = BtcMarketObservation(timestampMs = now - 5000L, price = 95000.0)
    val obs3 = BtcMarketObservation(timestampMs = now - 1000L, price = 95200.0)

    buffer.ingest(obs1, evaluationTimestampMs = now)
    buffer.ingest(obs2, evaluationTimestampMs = now)
    buffer.ingest(obs3, evaluationTimestampMs = now)

    val latest = buffer.getLatestValidObservation(evaluationTimestampMs = now)
    assertNotNull(latest)
    assertEquals(95200.0, latest!!.price, 0.001)

    val window = buffer.queryWindow(startTimeMs = now - 8000L, endTimeMs = now)
    assertEquals(2, window.size)
    assertEquals(95000.0, window[0].price, 0.001)
    assertEquals(95200.0, window[1].price, 0.001)
  }

  @Test
  fun `contract temporal state correctly evaluates active and expired boundaries`() {
    val open = 1700000000000L
    val expiration = open + 900000L // 15 minutes

    val activeState = ContractTemporalState(
      contractTicker = "KXBTC-15M-TEST",
      openTimestampMs = open,
      expirationTimestampMs = expiration,
      referenceStrikePrice = 95000.0,
      currentBtcSpotPrice = 95100.0,
      evaluationTimestampMs = open + 300000L // 5 mins in
    )

    assertTrue(activeState.isActive)
    assertFalse(activeState.isCompleteOrSettled)
    assertEquals(300L, activeState.elapsedTimeSeconds)
    assertEquals(600L, activeState.remainingTimeSeconds)
    assertTrue(activeState.validateTemporalState().grade.isUsable)

    val expiredState = ContractTemporalState(
      contractTicker = "KXBTC-15M-TEST",
      openTimestampMs = open,
      expirationTimestampMs = expiration,
      referenceStrikePrice = 95000.0,
      currentBtcSpotPrice = 95100.0,
      evaluationTimestampMs = expiration + 1000L // Expired
    )

    assertFalse(expiredState.isActive)
    assertTrue(expiredState.isCompleteOrSettled)
    assertFalse(expiredState.validateTemporalState().grade.isUsable)
  }
}
