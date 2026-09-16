package com.example.data.repository

import com.example.data.model.Candle
import com.example.data.model.MarketDataFeedState
import com.example.data.model.MarketSnapshot
import com.example.data.model.OrderBookDepth
import kotlinx.coroutines.flow.StateFlow

/**
 * Clean repository boundary for spot market data.
 * Pure data layer; contains zero business logic, zero prediction code, and zero synthetic generators.
 */
interface MarketDataRepository {
  val feedState: StateFlow<MarketDataFeedState>
  val latestSnapshot: StateFlow<MarketSnapshot?>
  val candles: StateFlow<List<Candle>>
  val orderBook: StateFlow<OrderBookDepth?>

  fun setFeedState(state: MarketDataFeedState)
  fun ingestSnapshot(snapshot: MarketSnapshot)
  fun ingestCandle(candle: Candle)
  fun ingestCandles(candles: List<Candle>)
  fun ingestOrderBook(depth: OrderBookDepth)
  fun clear()
}
