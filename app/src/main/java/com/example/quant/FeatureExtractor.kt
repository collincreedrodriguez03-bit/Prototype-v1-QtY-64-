package com.example.quant

import com.example.data.model.Candle
import com.example.data.model.MarketSnapshot

/**
 * Interface for extracting quantitative features from market series.
 * Operates independently from prediction models and UI.
 */
interface FeatureExtractor {
  fun extract(
    candles: List<Candle>,
    snapshot: MarketSnapshot?,
    minLookback: Int
  ): QuantitativeFeatureVector
}
