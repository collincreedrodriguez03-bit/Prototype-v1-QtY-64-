package com.example.data.ingestion

import com.example.data.model.BtcMarketObservation
import com.example.data.model.DataQualityGrade
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BinanceWebSocketParserTest {

  @Test
  fun `parser correctly parses valid Binance trade payload and normalizes timestamps`() = runBlocking {
    val provider = BinanceBtcLiveMarketDataProvider()
    val validJson = """
      {
        "e": "trade",
        "E": 1700000000123,
        "s": "BTCUSDT",
        "t": 123456789,
        "p": "95250.75",
        "q": "0.12500000",
        "T": 1700000000125
      }
    """.trimIndent()

    val receiptTime = 1700000000150L
    provider.parseAndEmitMessage(validJson, receiptTime)

    val observation = provider.streamObservations().first()
    assertEquals(1700000000123L, observation.timestampMs)
    assertEquals(95250.75, observation.price, 0.001)
    assertEquals(0.125, observation.volume!!, 0.0001)
    assertEquals(DataQualityGrade.VALID, observation.qualityStatus)
  }

  @Test
  fun `parser handles malformed or incomplete JSON without crashing`() = runBlocking {
    val provider = BinanceBtcLiveMarketDataProvider()
    val malformedJson = "{ \"invalid\": json }"

    // Should not throw exception
    provider.parseAndEmitMessage(malformedJson, System.currentTimeMillis())
    assertFalse(provider.isConnected)
  }

  @Test
  fun `provider initial state is disconnected`() {
    val provider = BinanceBtcLiveMarketDataProvider()
    assertFalse(provider.isConnected)
    assertEquals("binance_spot_ws", provider.providerId)
  }
}
