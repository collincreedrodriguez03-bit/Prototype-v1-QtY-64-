package com.example.data.repository

import com.example.data.model.Candle
import com.example.data.model.MarketDataFeedState
import com.example.data.model.MarketSnapshot
import com.example.data.model.OrderBookDepth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DefaultMarketDataRepository(
  private val maxCandleHistory: Int = 300
) : MarketDataRepository {

  private val _feedState = MutableStateFlow<MarketDataFeedState>(
    MarketDataFeedState.AwaitingFeed(
      expectedSource = "BTC Spot Feed (Coinbase/Binance/Kraken)"
    )
  )
  override val feedState: StateFlow<MarketDataFeedState> = _feedState.asStateFlow()

  private val _latestSnapshot = MutableStateFlow<MarketSnapshot?>(null)
  override val latestSnapshot: StateFlow<MarketSnapshot?> = _latestSnapshot.asStateFlow()

  private val _candles = MutableStateFlow<List<Candle>>(emptyList())
  override val candles: StateFlow<List<Candle>> = _candles.asStateFlow()

  private val _orderBook = MutableStateFlow<OrderBookDepth?>(null)
  override val orderBook: StateFlow<OrderBookDepth?> = _orderBook.asStateFlow()

  override fun setFeedState(state: MarketDataFeedState) {
    _feedState.value = state
  }

  @Synchronized
  override fun ingestSnapshot(snapshot: MarketSnapshot) {
    _latestSnapshot.value = snapshot
  }

  @Synchronized
  override fun ingestCandle(candle: Candle) {
    val current = _candles.value.toMutableList()
    val existingIndex = current.indexOfFirst { it.timestampMs == candle.timestampMs }
    if (existingIndex >= 0) {
      current[existingIndex] = candle
    } else {
      current.add(candle)
      current.sortBy { it.timestampMs }
      if (current.size > maxCandleHistory) {
        current.removeAt(0)
      }
    }
    _candles.value = current
  }

  @Synchronized
  override fun ingestCandles(candles: List<Candle>) {
    val current = _candles.value.associateBy { it.timestampMs }.toMutableMap()
    for (c in candles) {
      current[c.timestampMs] = c
    }
    val sorted = current.values.sortedBy { it.timestampMs }
    _candles.value = if (sorted.size > maxCandleHistory) {
      sorted.takeLast(maxCandleHistory)
    } else {
      sorted
    }
  }

  override fun ingestOrderBook(depth: OrderBookDepth) {
    _orderBook.value = depth
  }

  override fun clear() {
    _candles.value = emptyList()
    _latestSnapshot.value = null
    _orderBook.value = null
    _feedState.value = MarketDataFeedState.AwaitingFeed(expectedSource = "BTC Spot Feed")
  }
}
