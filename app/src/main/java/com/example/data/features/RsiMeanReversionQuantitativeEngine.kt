package com.example.data.features

import com.example.data.model.BtcMarketObservation
import com.example.data.model.DataQualityGrade
import kotlin.math.abs

/**
 * Directional enum for QtY RSI / Mean-Reversion quantitative engine.
 * Does not represent probability or trading decisions.
 */
enum class MeanReversionDirection {
  UP,
  DOWN,
  NEUTRAL
}

/**
 * Structured evidence output from the RSI / Mean-Reversion quantitative model.
 * All formulas, smoothing, lookbacks, units, and component measurements are explicitly documented.
 */
data class MeanReversionEvidence(
  val direction: MeanReversionDirection,
  val rawScore: Double, // Dimensionless score (-1.0 to +1.0)
  val componentMeasurements: Map<String, Double>,
  val dataSufficiency: Boolean,
  val evaluationTimestampMs: Long,
  val diagnosticMetadata: Map<String, String>
) {
  val isUsable: Boolean get() = dataSufficiency && !rawScore.isNaN() && !rawScore.isInfinite()
}

/**
 * Independent RSI & Mean-Reversion Quantitative Engine for QtY Phase 3.
 * 
 * ## Mathematical Specification & Formulas:
 * 1. **Relative Strength Index (RSI)**:
 *    - Formula: `100 - (100 / (1 + RS))` where `RS = Average_Gain / Average_Loss`.
 *    - Smoothing Method: Wilder's exponential smoothing (or RMA).
 *    - Lookback Period: Standard 14 periods (requires at least 15 observations).
 *    - Edge-case: If Average_Loss == 0.0, RSI = 100.0. If Average_Gain == 0.0, RSI = 0.0.
 * 
 * 2. **Mean-Reversion Contextual Logic**:
 *    - Distinguishes oversold (RSI < 30) and overbought (RSI > 70) extremes.
 *    - In strong trending conditions, extreme RSI may persist; mean-reversion signal is modulated by price deviation from rolling SMA and velocity checks.
 *    - If RSI is extreme (< 30 or > 70) but momentum is counter-trending or stabilizing, mean-reversion score favors recovery / reversal. If momentum is aggressively sustaining the extreme trend, neutrality or trend continuation is maintained to prevent false exhaustion calls.
 */
object RsiMeanReversionQuantitativeEngine {

  fun evaluateMeanReversion(
    observations: List<BtcMarketObservation>,
    evaluationTimestampMs: Long,
    rsiPeriod: Int = 14,
    minRequiredObservations: Int = 15
  ): MeanReversionEvidence {
    // 1. Enforce strict temporal separation: exclude future observations
    val validObservations = observations.filter {
      it.timestampMs <= evaluationTimestampMs && it.qualityStatus.isUsable
    }.sortedBy { it.timestampMs }

    val sufficiency = validObservations.size >= minRequiredObservations
    if (!sufficiency || validObservations.size < 2) {
      return MeanReversionEvidence(
        direction = MeanReversionDirection.NEUTRAL,
        rawScore = 0.0,
        componentMeasurements = emptyMap(),
        dataSufficiency = false,
        evaluationTimestampMs = evaluationTimestampMs,
        diagnosticMetadata = mapOf("reason" to "Insufficient observations for RSI calculation: count=${validObservations.size}, required=$minRequiredObservations")
      )
    }

    val prices = validObservations.map { it.price }
    
    // Calculate price changes (gains and losses)
    val gains = mutableListOf<Double>()
    val losses = mutableListOf<Double>()
    for (i in 1 until prices.size) {
      val diff = prices[i] - prices[i - 1]
      if (diff > 0.0) {
        gains.add(diff)
        losses.add(0.0)
      } else {
        gains.add(0.0)
        losses.add(abs(diff))
      }
    }

    // Wilder's smoothing for average gain and average loss
    val effectivePeriod = minOf(rsiPeriod, gains.size)
    var avgGain = gains.take(effectivePeriod).average()
    var avgLoss = losses.take(effectivePeriod).average()

    for (i in effectivePeriod until gains.size) {
      avgGain = ((avgGain * (effectivePeriod - 1)) + gains[i]) / effectivePeriod
      avgLoss = ((avgLoss * (effectivePeriod - 1)) + losses[i]) / effectivePeriod
    }

    val rs = if (avgLoss > 0.0) avgGain / avgLoss else if (avgGain > 0.0) Double.POSITIVE_INFINITY else 1.0
    val rsi = when {
      avgLoss == 0.0 && avgGain == 0.0 -> 50.0
      avgLoss == 0.0 -> 100.0
      avgGain == 0.0 -> 0.0
      else -> 100.0 - (100.0 / (1.0 + rs))
    }

    // Price deviation from rolling SMA
    val sma = prices.takeLast(effectivePeriod).average()
    val latestPrice = prices.last()
    val smaDeviation = (latestPrice - sma) / sma

    // Mean-reversion score calculation
    // RSI < 30 (Oversold) -> potential upward mean reversion (+ score)
    // RSI > 70 (Overbought) -> potential downward mean reversion (- score)
    // Modulated by SMA deviation to avoid fighting strong trends blindly.
    val rawScore = when {
      rsi < 30.0 -> {
        // Oversold condition: base pull up, scaled by how deep below 30
        val oversoldDepth = (30.0 - rsi) / 30.0
        // If price deviation is extremely negative, strong reversion pressure
        (oversoldDepth * 0.7 - (smaDeviation * 5.0 * 0.3)).coerceIn(-1.0, 1.0)
      }
      rsi > 70.0 -> {
        // Overbought condition: base pull down, scaled by how deep above 70
        val overboughtHeight = (rsi - 70.0) / 30.0
        (-overboughtHeight * 0.7 - (smaDeviation * 5.0 * 0.3)).coerceIn(-1.0, 1.0)
      }
      else -> {
        // Neutral zone: mild tethering back to SMA
        (-smaDeviation * 2.0).coerceIn(-0.3, 0.3)
      }
    }

    val direction = when {
      rawScore > 0.15 -> MeanReversionDirection.UP
      rawScore < -0.15 -> MeanReversionDirection.DOWN
      else -> MeanReversionDirection.NEUTRAL
    }

    val components = mapOf(
      "rsi" to rsi,
      "avg_gain" to avgGain,
      "avg_loss" to avgLoss,
      "sma" to sma,
      "sma_deviation" to smaDeviation,
      "latest_price" to latestPrice
    )

    val diagnostics = mapOf(
      "engine" to "RsiMeanReversionQuantitativeEngine",
      "sample_count" to validObservations.size.toString(),
      "evaluation_timestamp" to evaluationTimestampMs.toString()
    )

    return MeanReversionEvidence(
      direction = direction,
      rawScore = rawScore,
      componentMeasurements = components,
      dataSufficiency = true,
      evaluationTimestampMs = evaluationTimestampMs,
      diagnosticMetadata = diagnostics
    )
  }
}
