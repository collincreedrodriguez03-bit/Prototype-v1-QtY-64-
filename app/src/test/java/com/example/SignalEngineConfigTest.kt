package com.example

import com.example.core.config.SignalEngineConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class SignalEngineConfigTest {

  @Test
  fun `default config has standard 15 minute Kalshi specifications`() {
    val config = SignalEngineConfig()
    assertEquals("BTC/USD", config.symbol)
    assertEquals(15, config.timeframeMinutes)
    assertEquals(60, config.minHistoryCandles)
    assertEquals(30_000L, config.staleDataTimeoutMs)
    assertEquals(60L, config.minExpiryBufferSeconds)
    assertEquals(0.65, config.confidenceThreshold, 0.001)
    assertEquals(50.0, config.maxSpreadUsd, 0.001)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `config rejects invalid confidence threshold outside 0_5 to 1_0`() {
    SignalEngineConfig(confidenceThreshold = 0.4)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `config rejects negative spread`() {
    SignalEngineConfig(maxSpreadUsd = -5.0)
  }
}
