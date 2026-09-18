package com.example.data.ingestion

import com.example.data.model.BtcMarketObservation
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.atomic.AtomicBoolean

@JsonClass(generateAdapter = true)
data class BinanceTradePayload(
  @field:Json(name = "e") val eventType: String?,
  @field:Json(name = "E") val eventTimeMs: Long?,
  @field:Json(name = "s") val symbol: String?,
  @field:Json(name = "p") val priceString: String?,
  @field:Json(name = "q") val quantityString: String?,
  @field:Json(name = "T") val tradeTimeMs: Long?
)

/**
 * Authentic live BTC market-data provider connecting to Binance Public Spot WebSocket API.
 * Conforms to [BtcLiveMarketDataProvider] and integrates with QtY normalization and fail-closed buffers.
 */
class BinanceBtcLiveMarketDataProvider(
  private val okHttpClient: OkHttpClient = OkHttpClient(),
  private val wsUrl: String = "wss://stream.binance.com:9443/ws/btcusdt@trade",
  private val normalizer: BtcDataNormalizer = DefaultBtcDataNormalizer()
) : BtcLiveMarketDataProvider {

  override val providerId: String = "binance_spot_ws"

  private val _isConnected = AtomicBoolean(false)
  override val isConnected: Boolean get() = _isConnected.get()

  private val _observations = MutableSharedFlow<BtcMarketObservation>(
    replay = 10,
    extraBufferCapacity = 64,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
  )

  private val moshi = Moshi.Builder().build()
  private val adapter = moshi.adapter(BinanceTradePayload::class.java)

  private var webSocket: WebSocket? = null

  @Synchronized
  fun connect() {
    if (_isConnected.get()) return
    val request = Request.Builder().url(wsUrl).build()
    webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
      override fun onOpen(webSocket: WebSocket, response: Response) {
        _isConnected.set(true)
      }

      override fun onMessage(webSocket: WebSocket, text: String) {
        parseAndEmitMessage(text, System.currentTimeMillis())
      }

      override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        _isConnected.set(false)
      }

      override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        _isConnected.set(false)
      }
    })
  }

  fun parseAndEmitMessage(jsonText: String, receiptTimeMs: Long) {
    try {
      val payload = adapter.fromJson(jsonText)
      if (payload != null && payload.priceString != null && payload.eventTimeMs != null) {
        val price = payload.priceString.toDoubleOrNull() ?: Double.NaN
        val volume = payload.quantityString?.toDoubleOrNull()
        val exchangeTime = payload.eventTimeMs

        val observation = normalizer.normalize(
          rawSource = providerId,
          exchangeTimestampMs = exchangeTime,
          receiptTimestampMs = receiptTimeMs,
          price = price,
          volume = volume,
          bidPrice = null,
          askPrice = null
        )
        _observations.tryEmit(observation)
      }
    } catch (e: Exception) {
      // Malformed messages are dropped safely per fail-closed requirements
    }
  }

  @Synchronized
  fun disconnect() {
    webSocket?.close(1000, "Client disconnect")
    webSocket = null
    _isConnected.set(false)
  }

  override fun streamObservations(): SharedFlow<BtcMarketObservation> = _observations.asSharedFlow()
}
