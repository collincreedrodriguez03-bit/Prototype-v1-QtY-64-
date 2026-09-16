package com.example.quant

/**
 * Immutable container for quantitative features extracted from market data series.
 * Enforces data hygiene: validates that features are finite (no NaN, no Inf) and
 * records timestamp/sample size metadata for reproducible quant audits.
 */
data class QuantitativeFeatureVector(
  val timestampMs: Long,
  val sampleSize: Int,
  val features: Map<String, Double>,
  val isValid: Boolean,
  val validationError: String? = null
) {
  init {
    if (isValid) {
      for ((key, value) in features) {
        require(!value.isNaN() && !value.isInfinite()) {
          "Feature $key has non-finite value ($value)"
        }
      }
    }
  }

  fun getFeature(name: String): Double? = features[name]

  companion object {
    fun invalid(timestampMs: Long, sampleSize: Int, reason: String): QuantitativeFeatureVector {
      return QuantitativeFeatureVector(
        timestampMs = timestampMs,
        sampleSize = sampleSize,
        features = emptyMap(),
        isValid = false,
        validationError = reason
      )
    }
  }
}
