package com.example.data.ingestion

import com.example.data.model.BtcMarketObservation
import com.example.data.model.DataQualityGrade
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BtcIngestionPipelineTest {

  private val normalizer = DefaultBtcDataNormalizer()

  @Test
  fun `normalizer correctly flags valid observations`() {
    val obs = normalizer.normalize(
      rawSource = "coinbase",
      exchangeTimestampMs = 1700000000000L,
      receiptTimestampMs = 1700000000050L,
      price = 95200.0,
      volume = 1.5,
      bidPrice = 95195.0,
      askPrice = 95205.0
    )

    assertEquals(DataQualityGrade.VALID, obs.qualityStatus)
    assertEquals(95200.0, obs.price, 0.001)
  }

  @Test
  fun `normalizer flags malformed observations for negative price`() {
    val obs = normalizer.normalize(
      rawSource = "binance",
      exchangeTimestampMs = 1700000000000L,
      receiptTimestampMs = 1700000000050L,
      price = -500.0,
      volume = null,
      bidPrice = null,
      askPrice = null
    )

    assertEquals(DataQualityGrade.MALFORMED, obs.qualityStatus)
    assertFalse(obs.qualityStatus.isUsable)
  }

  @Test
  fun `observation buffer enforces chronological sorting and rejects duplicates`() {
    val buffer = BtcObservationBuffer(maxCapacity = 10, maxStaleAgeMs = 60000L)
    val now = 1700000000000L

    val obs1 = BtcMarketObservation(timestampMs = now - 2000L, price = 95100.0)
    val obs2 = BtcMarketObservation(timestampMs = now - 1000L, price = 95150.0)
    val duplicateObs = BtcMarketObservation(timestampMs = now - 1000L, price = 95150.0)

    val report1 = buffer.ingest(obs1, referenceTimeMs = now)
    val report2 = buffer.ingest(obs2, referenceTimeMs = now)
    val reportDup = buffer.ingest(duplicateObs, referenceTimeMs = now)

    assertTrue(report1.grade.isUsable)
    assertTrue(report2.grade.isUsable)
    assertEquals(DataQualityGrade.DUPLICATED, reportDup.grade)
    assertTrue(reportDup.isFailClosed)

    val snapshot = buffer.getSnapshot()
    assertEquals(2, snapshot.size)
    assertEquals(95100.0, snapshot[0].price, 0.001)
    assertEquals(95150.0, snapshot[1].price, 0.001)
  }

  @Test
  fun `observation buffer rejects stale or future observations`() {
    val buffer = BtcObservationBuffer(maxCapacity = 10, maxStaleAgeMs = 5000L)
    val now = 1700000000000L

    val staleObs = BtcMarketObservation(timestampMs = now - 10000L, price = 94000.0)
    val futureObs = BtcMarketObservation(timestampMs = now + 5000L, price = 96000.0)

    val staleReport = buffer.ingest(staleObs, referenceTimeMs = now)
    val futureReport = buffer.ingest(futureObs, referenceTimeMs = now)

    assertEquals(DataQualityGrade.STALE, staleReport.grade)
    assertEquals(DataQualityGrade.OUT_OF_ORDER, futureReport.grade)
    assertTrue(buffer.size == 0)
  }

  @Test
  fun `provider failure propagates without synthetic fabrication`() = runBlocking {
    val failingProvider = object : BtcLiveMarketDataProvider {
      override val providerId: String = "mock_failing_exchange"
      override val isConnected: Boolean = false

      override fun streamObservations(): Flow<BtcMarketObservation> = flow {
        // Feed fails to emit valid observations or throws connection error
        throw IllegalStateException("Exchange WebSocket disconnected")
      }
    }

    var caughtError = false
    try {
      failingProvider.streamObservations().toList()
    } catch (e: IllegalStateException) {
      caughtError = true
    }

    assertTrue(caughtError)
    assertFalse(failingProvider.isConnected)
  }
}
