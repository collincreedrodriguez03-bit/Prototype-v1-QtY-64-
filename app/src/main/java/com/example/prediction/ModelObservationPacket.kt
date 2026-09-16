package com.example.prediction

import com.example.data.model.BtcMarketObservation
import com.example.data.model.ContractStateObservation
import com.example.quant.QuantitativeFeatureVector

/**
 * Immutable model observation packet passed to prediction models.
 * Enforces strict temporal constraints (`featureTimestamp <= evaluationTimestampMs`)
 * to prevent look-ahead leakage by design.
 */
data class ModelObservationPacket(
  val contractState: ContractStateObservation,
  val marketObservation: BtcMarketObservation,
  val featureVector: QuantitativeFeatureVector,
  val evaluationTimestampMs: Long = System.currentTimeMillis()
) {
  init {
    require(featureVector.timestampMs <= evaluationTimestampMs) {
      "Feature vector timestamp (${featureVector.timestampMs}) is ahead of evaluation timestamp ($evaluationTimestampMs). Look-ahead leak prevention violation."
    }
    require(marketObservation.timestampMs <= evaluationTimestampMs) {
      "Market observation timestamp (${marketObservation.timestampMs}) is ahead of evaluation timestamp ($evaluationTimestampMs). Look-ahead leak prevention violation."
    }
  }

  val isTemporallyValid: Boolean
    get() = featureVector.isValid &&
        featureVector.timestampMs <= evaluationTimestampMs &&
        marketObservation.timestampMs <= evaluationTimestampMs
}
