package com.example.data.ingestion

import com.example.data.model.BtcMarketObservation
import com.example.data.model.DataQualityGrade

class DefaultBtcDataNormalizer : BtcDataNormalizer {
  override fun normalize(
    rawSource: String,
    exchangeTimestampMs: Long,
    receiptTimestampMs: Long,
    price: Double,
    volume: Double?,
    bidPrice: Double?,
    askPrice: Double?
  ): BtcMarketObservation {
    val quality = when {
      exchangeTimestampMs <= 0 || receiptTimestampMs <= 0 -> DataQualityGrade.MALFORMED
      price.isNaN() || price.isInfinite() || price <= 0.0 -> DataQualityGrade.MALFORMED
      exchangeTimestampMs > receiptTimestampMs + 5000L -> DataQualityGrade.OUT_OF_ORDER
      else -> DataQualityGrade.VALID
    }

    return BtcMarketObservation(
      timestampMs = exchangeTimestampMs,
      receiptTimestampMs = receiptTimestampMs,
      price = if (quality.isUsable) price else 1.0, // fallback dummy price for construction if malformed, but qualityStatus is MALFORMED
      volume = volume,
      bidPrice = bidPrice,
      askPrice = askPrice,
      qualityStatus = quality
    )
  }
}
