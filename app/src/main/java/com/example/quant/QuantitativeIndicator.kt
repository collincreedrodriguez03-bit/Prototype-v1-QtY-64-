package com.example.quant

/**
 * Pure mathematical transformation interface for quantitative indicators.
 * Implementations must be stateless and deterministic.
 */
interface QuantitativeIndicator<in TInput, out TOutput> {
  val id: String
  val description: String

  fun compute(input: TInput): TOutput
}
