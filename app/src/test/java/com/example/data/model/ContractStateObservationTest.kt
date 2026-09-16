package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContractStateObservationTest {

  @Test
  fun `valid contract state calculates remaining time and price delta`() {
    val now = 1700000000000L
    val contract = ContractStateObservation(
      ticker = "KXBTC-15M-TEST",
      targetStrike = 95000.0,
      openReferencePrice = 94800.0,
      currentSpotPrice = 95200.0,
      openTimestampMs = now - 60000L,
      expirationTimestampMs = now + 840000L, // 14 mins left
      evaluationTimestampMs = now,
      qualityStatus = DataQualityGrade.VALID
    )

    assertEquals(840L, contract.timeRemainingSeconds)
    assertFalse(contract.isExpired)
    assertEquals(400.0, contract.priceDeltaFromReference, 0.001)

    val report = contract.validateContractState()
    assertTrue(report.grade.isUsable)
  }

  @Test
  fun `expired contract fails validation`() {
    val now = 1700000000000L
    val expiredContract = ContractStateObservation(
      ticker = "KXBTC-15M-TEST",
      targetStrike = 95000.0,
      openReferencePrice = 94800.0,
      currentSpotPrice = 95200.0,
      openTimestampMs = now - 900000L,
      expirationTimestampMs = now - 1000L, // expired
      evaluationTimestampMs = now,
      qualityStatus = DataQualityGrade.VALID
    )

    assertTrue(expiredContract.isExpired)
    val report = expiredContract.validateContractState()
    assertEquals(DataQualityGrade.OUT_OF_ORDER, report.grade)
    assertTrue(report.isFailClosed)
  }
}
