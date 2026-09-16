package com.example.data.model

data class PriceLevel(
  val price: Double,
  val size: Double
)

data class OrderBookDepth(
  val timestampMs: Long,
  val symbol: String,
  val bids: List<PriceLevel>,
  val asks: List<PriceLevel>
) {
  val topBid: PriceLevel? get() = bids.firstOrNull()
  val topAsk: PriceLevel? get() = asks.firstOrNull()
}
