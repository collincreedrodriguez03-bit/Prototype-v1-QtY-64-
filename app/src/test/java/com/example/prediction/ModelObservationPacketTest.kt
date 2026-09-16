package com.example.prediction

import com.example.data.model.BtcMarketObservation
import com.example.data.model.ContractStateObservation
import com.example.quant.QuantitativeFeatureVector
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelObservationPacketTest {

  @Test(expected = IllegalArgumentException::class)
  fun `packet rejects feature vector from the future to prevent look-ahead leak`() {
    val now = 1700000000000L
    val contract = ContractStateObservation(
      ticker = "KXBTC-15M",
      targetStrike = 95000.0,
      openReferencePrice = 95000.0,
      currentSpotPrice = 95100.0,
      openTimestampMs = now - 60000L,
      expirationTimestampMs = now + 840000L,
      evaluationTimestampMs = now
    )
    val market = BtcMarketObservation(
      timestampMs = now,
      price = 95100.0
    )
    val futureFeature = QuantitativeFeatureVector(
      timestampMs = now + 10000L, // future timestamp
      sampleSize = 60,
      features = mapOf("ret" to 0.01),
      isValid = true
    )

    ModelObservationPacket(
      contractState = contract,
      marketObservation = market,
      featureVector = futureFeature,
      evaluationTimestampMs = now
    )
  }
}
