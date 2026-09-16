package com.example.ensemble

import com.example.prediction.QuantitativeModelResult

data class EnsembleConsensusResult(
  val contractTicker: String,
  val finalSignal: SignalDecision,
  val aggregateConfidence: Double,
  val participatingModelsCount: Int,
  val reasoning: String,
  val timestampMs: Long = System.currentTimeMillis()
) {
  init {
    require(contractTicker.isNotBlank()) { "Contract ticker must not be blank" }
    require(aggregateConfidence in 0.0..1.0) { "Aggregate confidence must be in [0.0, 1.0]" }
  }
}

/**
 * Explicit ensemble and decision boundary that consumes structured results
 * from independent quantitative model engines without coupling to individual implementations.
 */
interface EnsembleBoundary {
  fun evaluateEnsemble(
    contractTicker: String,
    modelResults: List<QuantitativeModelResult>
  ): EnsembleConsensusResult
}

class DefaultEnsembleBoundary : EnsembleBoundary {
  override fun evaluateEnsemble(
    contractTicker: String,
    modelResults: List<QuantitativeModelResult>
  ): EnsembleConsensusResult {
    if (modelResults.isEmpty() || modelResults.any { !it.dataSufficiency }) {
      return EnsembleConsensusResult(
        contractTicker = contractTicker,
        finalSignal = SignalDecision.NO_TRADE,
        aggregateConfidence = 0.0,
        participatingModelsCount = modelResults.size,
        reasoning = "Ensemble defaulted to NO_TRADE due to insufficient data or missing model results (fail-closed)."
      )
    }

    // Default stub behavior until mathematical consensus weights are implemented
    val avgConfidence = modelResults.map { it.confidence }.average()
    return EnsembleConsensusResult(
      contractTicker = contractTicker,
      finalSignal = SignalDecision.NO_TRADE,
      aggregateConfidence = if (avgConfidence.isNaN()) 0.0 else avgConfidence,
      participatingModelsCount = modelResults.size,
      reasoning = "Ensemble consensus boundary operational; mathematical weights pending implementation."
    )
  }
}
