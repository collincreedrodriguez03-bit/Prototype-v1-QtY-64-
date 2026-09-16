package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Candle
import com.example.data.model.MarketDataFeedState
import com.example.data.model.MarketSnapshot
import com.example.ui.theme.QuantAmber
import com.example.ui.theme.QuantBackground
import com.example.ui.theme.QuantBlue
import com.example.ui.theme.QuantBorder
import com.example.ui.theme.QuantGreen
import com.example.ui.theme.QuantRed
import com.example.ui.theme.QuantSurface
import com.example.ui.theme.QuantTextPrimary
import com.example.ui.theme.QuantTextSecondary
import kotlin.math.max
import kotlin.math.min

@Composable
fun TradingViewCanvas(
  candles: List<Candle>,
  snapshot: MarketSnapshot?,
  targetStrike: Double?,
  feedState: MarketDataFeedState,
  modifier: Modifier = Modifier
) {
  var crosshairPosition by remember { mutableStateOf<Offset?>(null) }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(280.dp)
      .clip(RoundedCornerShape(8.dp))
      .background(QuantBackground)
      .border(1.dp, QuantBorder, RoundedCornerShape(8.dp))
      .testTag("tradingview_canvas_container")
  ) {
    // Header overlay for TradingView style symbol & price summary
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(QuantSurface.copy(alpha = 0.85f))
        .padding(horizontal = 12.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "BTC/USD",
        color = QuantTextPrimary,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        fontFamily = FontFamily.Monospace
      )
      Spacer(modifier = Modifier.size(10.dp))
      Text(
        text = "15M KALSHI CYCLE",
        color = QuantBlue,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = FontFamily.Monospace
      )
      Spacer(modifier = Modifier.weight(1f))
      if (snapshot != null) {
        Text(
          text = "$${String.format(java.util.Locale.US, "%,.2f", snapshot.lastPrice)}",
          color = QuantTextPrimary,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
      }
    }

    if (candles.isEmpty()) {
      // Clean, professional state indicating waiting for authentic exchange feed
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(top = 40.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
      ) {
        Icon(
          imageVector = Icons.Default.Sensors,
          contentDescription = "Feed Status",
          tint = QuantAmber,
          modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = "AWAITING AUTHENTIC MARKET FEED",
          color = QuantTextPrimary,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Per strict quant rules, synthetic or randomized data is not generated.",
          color = QuantTextSecondary,
          fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        val feedStatusText = when (feedState) {
          is MarketDataFeedState.AwaitingFeed -> feedState.instructions
          is MarketDataFeedState.Connected -> "Connected: ${feedState.sourceName}"
          is MarketDataFeedState.Stale -> "Feed stale (${feedState.elapsedSinceLastUpdateMs}ms)"
          is MarketDataFeedState.Error -> "Feed error: ${feedState.message}"
          MarketDataFeedState.Uninitialized -> "Feed uninitialized"
        }
        Text(
          text = feedStatusText,
          color = QuantAmber,
          fontSize = 10.sp,
          fontFamily = FontFamily.Monospace
        )
      }
    } else {
      Canvas(
        modifier = Modifier
          .fillMaxSize()
          .padding(top = 36.dp, bottom = 20.dp, end = 60.dp)
          .pointerInput(Unit) {
            detectTapGestures(
              onTap = { crosshairPosition = it },
              onDoubleTap = { crosshairPosition = null }
            )
          }
          .pointerInput(Unit) {
            detectDragGestures(
              onDragStart = { crosshairPosition = it },
              onDrag = { change, _ -> crosshairPosition = change.position },
              onDragEnd = { /* keep last crosshair or clear */ },
              onDragCancel = { crosshairPosition = null }
            )
          }
      ) {
        val width = size.width
        val height = size.height
        if (width <= 0 || height <= 0) return@Canvas

        // Price bounds
        var minPrice = candles.minOf { it.low }
        var maxPrice = candles.maxOf { it.high }
        if (targetStrike != null) {
          minPrice = min(minPrice, targetStrike * 0.999)
          maxPrice = max(maxPrice, targetStrike * 1.001)
        }
        val priceRange = if (maxPrice > minPrice) maxPrice - minPrice else 1.0

        // Draw horizontal gridlines
        val gridLevels = 4
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
        for (i in 0..gridLevels) {
          val y = (height / gridLevels) * i
          drawLine(
            color = Color(0xFF262B3D),
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 1f
          )
        }

        // Draw vertical time gridlines
        val timeSteps = 5
        for (i in 0..timeSteps) {
          val x = (width / timeSteps) * i
          drawLine(
            color = Color(0xFF262B3D),
            start = Offset(x, 0f),
            end = Offset(x, height),
            strokeWidth = 1f
          )
        }

        // Target strike horizontal line if present
        if (targetStrike != null) {
          val strikeY = height - ((targetStrike - minPrice) / priceRange * height).toFloat()
          if (strikeY in 0f..height) {
            drawLine(
              color = QuantAmber,
              start = Offset(0f, strikeY),
              end = Offset(width, strikeY),
              strokeWidth = 2f,
              pathEffect = dashEffect
            )
          }
        }

        // Render Candles
        val candleCount = candles.size
        val candleWidth = max(2f, (width / candleCount) * 0.7f)
        val candleSpacing = width / candleCount

        for (i in candles.indices) {
          val c = candles[i]
          val x = i * candleSpacing + (candleSpacing / 2f)

          val openY = height - ((c.open - minPrice) / priceRange * height).toFloat()
          val closeY = height - ((c.close - minPrice) / priceRange * height).toFloat()
          val highY = height - ((c.high - minPrice) / priceRange * height).toFloat()
          val lowY = height - ((c.low - minPrice) / priceRange * height).toFloat()

          val candleColor = if (c.isBullish) QuantGreen else QuantRed

          // Draw wick
          drawLine(
            color = candleColor,
            start = Offset(x, highY),
            end = Offset(x, lowY),
            strokeWidth = 1.5f
          )

          // Draw body
          val top = min(openY, closeY)
          val bodyHeight = max(2f, kotlin.math.abs(closeY - openY))
          drawRect(
            color = candleColor,
            topLeft = Offset(x - candleWidth / 2f, top),
            size = Size(candleWidth, bodyHeight)
          )
        }

        // Draw Crosshair if active
        crosshairPosition?.let { pos ->
          if (pos.x in 0f..width && pos.y in 0f..height) {
            drawLine(
              color = QuantTextSecondary.copy(alpha = 0.7f),
              start = Offset(0f, pos.y),
              end = Offset(width, pos.y),
              strokeWidth = 1f,
              pathEffect = dashEffect
            )
            drawLine(
              color = QuantTextSecondary.copy(alpha = 0.7f),
              start = Offset(pos.x, 0f),
              end = Offset(pos.x, height),
              strokeWidth = 1f,
              pathEffect = dashEffect
            )
          }
        }
      }
    }
  }
}
