package com.example.data.model

/**
 * State representing market data feed connectivity.
 * Adheres strictly to non-synthetic data policies: no fake data is generated.
 */
sealed class MarketDataFeedState {
  object Uninitialized : MarketDataFeedState()

  data class AwaitingFeed(
    val expectedSource: String,
    val instructions: String = "Awaiting authentic exchange feed connection (WebSocket/REST). Synthetic data is strictly prohibited."
  ) : MarketDataFeedState()

  data class Connected(
    val sourceName: String,
    val lastHeartbeatMs: Long,
    val latencyMs: Long
  ) : MarketDataFeedState()

  data class Stale(
    val sourceName: String,
    val lastHeartbeatMs: Long,
    val elapsedSinceLastUpdateMs: Long
  ) : MarketDataFeedState()

  data class Error(
    val sourceName: String,
    val message: String,
    val cause: Throwable? = null
  ) : MarketDataFeedState()
}
