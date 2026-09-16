package com.example.data.ingestion

import com.example.data.model.BtcMarketObservation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BtcHistoricalDatasetValidationTest {

  @Test
  fun `validator accepts valid historical dataset`() {
    val startTime = 1700000000000L
    val observations = (0..15).map { i ->
      BtcMarketObservation(
        timestampMs = startTime + (i * 10000L),
        price = 95000.0 + i
      )
    }

    val metadata = BtcHistoricalDatasetValidator.validateDataset(
      source = "binance",
      symbol = "BTCUSDT",
      startTimeMs = startTime,
      endTimeMs = startTime + 200000L,
      observations = observations,
      minRequiredObservations = 10
    )

    assertTrue(metadata.isUsable)
    assertEquals(16, metadata.observationCount)
    assertTrue(metadata.datasetVersionId.isNotBlank())
  }

  @Test
  fun `validator rejects dataset with insufficient historical depth`() {
    val startTime = 1700000000000L
    val observations = listOf(
      BtcMarketObservation(timestampMs = startTime + 1000L, price = 95000.0)
    )

    val metadata = BtcHistoricalDatasetValidator.validateDataset(
      source = "binance",
      symbol = "BTCUSDT",
      startTimeMs = startTime,
      endTimeMs = startTime + 100000L,
      observations = observations,
      minRequiredObservations = 5
    )

    assertFalse(metadata.isUsable)
    assertTrue(metadata.qualityReport.isFailClosed)
  }

  @Test
  fun `validator rejects dataset with non-finite or negative prices`() {
    val startTime = 1700000000000L
    val observations = (0..10).map { i ->
      val price = if (i == 5) Double.NaN else 95000.0 + i
      BtcMarketObservation(
        timestampMs = startTime + (i * 1000L),
        price = price,
        qualityStatus = if (i == 5) com.example.data.model.DataQualityGrade.MALFORMED else com.example.data.model.DataQualityGrade.VALID
      )
    }

    val metadata = BtcHistoricalDatasetValidator.validateDataset(
      source = "binance",
      symbol = "BTCUSDT",
      startTimeMs = startTime,
      endTimeMs = startTime + 20000L,
      observations = observations,
      minRequiredObservations = 5
    )

    assertFalse(metadata.isUsable)
    assertTrue(metadata.qualityReport.isFailClosed)
  }

  @Test
  fun `validator rejects dataset with duplicate or out-of-order timestamps`() {
    val startTime = 1700000000000L
    val observations = listOf(
      BtcMarketObservation(timestampMs = startTime + 1000L, price = 95000.0),
      BtcMarketObservation(timestampMs = startTime + 1000L, price = 95010.0), // Duplicate
      BtcMarketObservation(timestampMs = startTime + 500L, price = 95020.0)   // Out of order
    )

    val metadata = BtcHistoricalDatasetValidator.validateDataset(
      source = "binance",
      symbol = "BTCUSDT",
      startTimeMs = startTime,
      endTimeMs = startTime + 10000L,
      observations = observations,
      minRequiredObservations = 2
    )

    assertFalse(metadata.isUsable)
    assertTrue(metadata.qualityReport.isFailClosed)
  }

  @Test
  fun `validator rejects dataset with abnormal timestamp gaps`() {
    val startTime = 1700000000000L
    val observations = (0..10).map { i ->
      val timeOffset = if (i == 5) 600000L else (i * 1000L) // 10 min gap at index 5
      BtcMarketObservation(
        timestampMs = startTime + timeOffset,
        price = 95000.0 + i
      )
    }

    val metadata = BtcHistoricalDatasetValidator.validateDataset(
      source = "binance",
      symbol = "BTCUSDT",
      startTimeMs = startTime,
      endTimeMs = startTime + 700000L,
      observations = observations,
      minRequiredObservations = 5,
      maxAllowedTimestampGapMs = 300000L
    )

    assertFalse(metadata.isUsable)
    assertTrue(metadata.qualityReport.isFailClosed)
  }
}
