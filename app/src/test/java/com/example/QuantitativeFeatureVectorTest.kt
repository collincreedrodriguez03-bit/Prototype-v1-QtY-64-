package com.example

import com.example.quant.QuantitativeFeatureVector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuantitativeFeatureVectorTest {

  @Test
  fun `valid feature vector stores finite numbers successfully`() {
    val vector = QuantitativeFeatureVector(
      timestampMs = 1700000000L,
      sampleSize = 60,
      features = mapOf(
        "return_15m" to 0.0125,
        "volatility" to 0.0042,
        "vwap" to 95230.5
      ),
      isValid = true
    )

    assertTrue(vector.isValid)
    assertEquals(60, vector.sampleSize)
    assertEquals(0.0125, vector.getFeature("return_15m")!!, 0.0001)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `feature vector rejects NaN values when marked valid`() {
    QuantitativeFeatureVector(
      timestampMs = 1700000000L,
      sampleSize = 60,
      features = mapOf("invalid_feature" to Double.NaN),
      isValid = true
    )
  }

  @Test(expected = IllegalArgumentException::class)
  fun `feature vector rejects Infinite values when marked valid`() {
    QuantitativeFeatureVector(
      timestampMs = 1700000000L,
      sampleSize = 60,
      features = mapOf("infinite_feature" to Double.POSITIVE_INFINITY),
      isValid = true
    )
  }

  @Test
  fun `invalid helper produces marked invalid vector with reason`() {
    val vector = QuantitativeFeatureVector.invalid(
      timestampMs = 1700000000L,
      sampleSize = 5,
      reason = "Under min history"
    )

    assertFalse(vector.isValid)
    assertEquals("Under min history", vector.validationError)
  }
}
