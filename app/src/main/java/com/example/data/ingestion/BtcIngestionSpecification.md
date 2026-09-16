# QtY 64 BTC Market Data Ingestion Specification

## Required Data Fields
To reliably power 15-minute Kalshi BTC prediction windows, QtY requires raw and normalized market observations containing:
1. **Source Identifier**: Exchange name / feed origin (e.g., Coinbase, Binance, Kraken).
2. **Exchange Timestamp**: Precise epoch millisecond timestamp reported by the matching engine/exchange.
3. **Receipt Timestamp**: Local epoch millisecond timestamp when the client received the observation (for latency and drift auditing).
4. **Spot Price**: Trade execution price or mid-market price ($P > 0.0$).
5. **Volume**: Executed trade volume ($V \ge 0.0$, optional/where available).
6. **Order Book Depth / Bids & Asks**: Level-1 / Level-2 quote prices and sizes for spread calculation ($Ask \ge Bid > 0.0$, optional/where available).
7. **Quality Status**: Data quality grade (`VALID`, `MISSING`, `STALE`, `MALFORMED`, `DUPLICATED`, `OUT_OF_ORDER`, `FEED_DISCONNECTED`).

## Required Properties of Eventual BTC Source
1. **Low Latency & High Throughput**: WebSocket streaming with minimal network jitter (< 100ms round-trip expectation).
2. **Chronological Integrity**: Strict sequencing of trades and order book updates.
3. **NTP Time Synchronization**: Server and client clocks synchronized to within millisecond tolerances to prevent false out-of-order rejections.
4. **Fail-Closed Reliability**: Automatic heartbeat detection and reconnection logic; if the feed drops or data is malformed, failure propagates into `NO_TRADE` without synthesizing mock values.
5. **Multi-Provider Interchangeability**: Conformance to `BtcLiveMarketDataProvider` and `BtcHistoricalMarketDataProvider` interfaces so QtY is never coupled to a single exchange API.
