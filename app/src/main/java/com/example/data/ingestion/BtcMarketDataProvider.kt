package com.example.data.ingestion

import com.example.data.model.BtcMarketObservation
import kotlinx.coroutines.flow.Flow

/**
 * Interface for live streaming BTC market-data providers (e.g., Coinbase Pro, Binance, Kraken).
 * Replaceable implementation allows QtY to switch or aggregate exchange feeds without coupling.
 */
interface BtcLiveMarketDataProvider {
  val providerId: String
  val isConnected: Boolean

  /**
   * Emits a real-time flow of normalized BTC market observations.
   * Fails closed or emits disconnected state if feed fails; never injects fake/synthetic ticks.
   */
  fun streamObservations(): Flow<BtcMarketObservation>
}

/**
 * Interface for historical BTC market-data providers.
 */
interface BtcHistoricalMarketDataProvider {
  val providerId: String

  suspend fun fetchHistoricalObservations(
    startTimeMs: Long,
    endTimeMs: Long
  ): Result<List<BtcMarketObservation>>
}

/**
 * Normalizes raw exchange tick/trade payloads into standardized [BtcMarketObservation].
 */
interface BtcDataNormalizer {
  fun normalize(
    rawSource: String,
    exchangeTimestampMs: Long,
    receiptTimestampMs: Long,
    price: Double,
    volume: Double?,
    bidPrice: Double?,
    askPrice: Double?
  ): BtcMarketObservation
}
