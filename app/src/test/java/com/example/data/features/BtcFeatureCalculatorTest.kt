package com.example.data.features

import com.example.data.model.BtcMarketObservation
import com.example.data.model.DataQualityGrade
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BtcFeatureCalculatorTest {

  @Test
  fun `calculator correctly computes statistical primitives and returns from window observations`() {
    val evalTime = 1700000000000L
    val observations = listOf(
      BtcMarketObservation(timestampMs = evalTime - 50000L, price = 95000.0),
      BtcMarketObservation(timestampMs = evalTime - 40000L, price = 95100.0),
      BtcMarketObservation(timestampMs = evalTime - 30000L, price = 95200.0),
      BtcMarketObservation(timestampMs = evalTime - 20000L, price = 95150.0),
      BtcMarketObservation(timestampMs = evalTime - 10000L, price = 95300.0)
    )

    val featureVector = BtcFeatureCalculator.computeFeatures(
      observations = observations,
      evaluationTimestampMs = evalTime,
      sourceWindowMs = 60000L,
      minRequiredObservations = 5
    )

    assertTrue(featureVector.isUsable)
    assertTrue(featureVector.dataSufficiency)
    assertEquals(DataQualityGrade.VALID, featureVector.qualityStatus)

    val values = featureVector.values
    assertEquals(95150.0, values[BtcFeatureCalculator.FEATURE_PRICE_MEAN]!!, 0.001)
    assertEquals(300.0, values[BtcFeatureCalculator.FEATURE_PRICE_CHANGE]!!, 0.001)
    assertEquals(95000.0, values[BtcFeatureCalculator.FEATURE_PRICE_MIN]!!, 0.001)
    assertEquals(95300.0, values[BtcFeatureCalculator.FEATURE_PRICE_MAX]!!, 0.001)
    assertEquals(5.0, values[BtcFeatureCalculator.FEATURE_OBSERVATION_COUNT]!!, 0.001)
  }

  @Test
  fun `calculator strictly rejects future observations preventing look-ahead leakage`() {
    val evalTime = 1700000000000L
    val observations = listOf(
      BtcMarketObservation(timestampMs = evalTime - 10000L, price = 95000.0),
      BtcMarketObservation(timestampMs = evalTime + 5000L, price = 98000.0) // Future leak observation
    )

    val featureVector = BtcFeatureCalculator.computeFeatures(
      observations = observations,
      evaluationTimestampMs = evalTime,
      sourceWindowMs = 60000L,
      minRequiredObservations = 2
    )

    // Only 1 valid past observation remains, which is < minRequiredObservations (2) -> insufficient data
    assertFalse(featureVector.isUsable)
    assertFalse(featureVector.dataSufficiency)
    assertEquals(DataQualityGrade.MISSING, featureVector.qualityStatus)
  }

  @Test
  fun `calculator reports insufficient data when observation count is below minimum`() {
    val evalTime = 1700000000000L
    val observations = listOf(
      BtcMarketObservation(timestampMs = evalTime - 10000L, price = 95000.0)
    )

    val featureVector = BtcFeatureCalculator.computeFeatures(
      observations = observations,
      evaluationTimestampMs = evalTime,
      sourceWindowMs = 60000L,
      minRequiredObservations = 3
    )

    assertFalse(featureVector.isUsable)
    assertFalse(featureVector.dataSufficiency)
  }
}
