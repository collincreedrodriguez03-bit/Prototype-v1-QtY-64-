package com.example.prediction

import com.example.data.model.KalshiContract
import com.example.quant.QuantitativeFeatureVector

/**
 * Common boundary interface for all prediction models in the QtY 64 engine.
 * Predictions operate strictly on validated quantitative feature vectors.
 */
interface PredictionModel {
  val modelId: String
  val modelVersion: String

  suspend fun evaluate(
    contract: KalshiContract,
    features: QuantitativeFeatureVector
  ): ModelPrediction
}
