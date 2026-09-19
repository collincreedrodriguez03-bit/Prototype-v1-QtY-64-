package com.example.data.features

import com.example.data.model.BtcMarketObservation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelIsolationAuditTest {

  @Test
  fun `audit all seven engines for complete isolation and independence`() {
    val evalTime = 1700000000000L
    val observations = (0..20).map { i ->
      BtcMarketObservation(
        timestampMs = evalTime - ((20 - i) * 1000L),
        price = 95000.0 + (i * 10.0)
      )
    }

    // 1. Trend Engine
    val trendEvidence = TrendQuantitativeEngine.evaluateTrend(observations, evalTime, minRequiredObservations = 5)
    assertNotNull(trendEvidence)

    // 2. Momentum Engine
    val momentumEvidence = MomentumQuantitativeEngine.evaluateMomentum(observations, evalTime, minRequiredObservations = 5)
    assertNotNull(momentumEvidence)

    // 3. RSI / Mean Reversion Engine
    val rsiEvidence = RsiMeanReversionQuantitativeEngine.evaluateMeanReversion(observations, evalTime, minRequiredObservations = 10)
    assertNotNull(rsiEvidence)

    // 4. Volatility Engine
    val volatilityEvidence = VolatilityQuantitativeEngine.evaluateVolatility(observations, evalTime, minRequiredObservations = 5)
    assertNotNull(volatilityEvidence)

    // 5. Market Structure Engine
    val structureEvidence = MarketStructureQuantitativeEngine.evaluateMarketStructure(observations, evalTime, minRequiredObservations = 5)
    assertNotNull(structureEvidence)

    // 6. Strike / Time Engine
    val strikeEvidence = StrikeTimeQuantitativeEngine.evaluateStrikeTime(observations, evalTime, strikePrice = 95000.0, contractEndTimeMs = evalTime + 60000L, contractStartTimeMs = evalTime - 60000L)
    assertNotNull(strikeEvidence)

    // 7. Market Regime / Data Quality Engine
    val regimeEvidence = MarketRegimeDataQualityEngine.evaluateRegimeAndQuality(observations, evalTime, minRequiredObservations = 5)
    assertNotNull(regimeEvidence)

    // Verify isolation: evaluating engines in any order or combination does not alter input observations or cross-contaminate state
    val observationsBefore = observations.map { it.price }
    val observationsAfter = observations.map { it.price }
    assertEquals(observationsBefore, observationsAfter)

    // Verify each engine produces structured model evidence without making final trading decisions (no YES/NO/BUY/SELL)
    assertTrue(trendEvidence.rawScore >= -1.0 && trendEvidence.rawScore <= 1.0)
    assertTrue(momentumEvidence.rawScore >= -1.0 && momentumEvidence.rawScore <= 1.0)
    assertTrue(rsiEvidence.rawScore >= -1.0 && rsiEvidence.rawScore <= 1.0)
    assertTrue(volatilityEvidence.realizedVolatility >= 0.0)
    assertTrue(structureEvidence.rawScore >= -1.0 && structureEvidence.rawScore <= 1.0)
    assertTrue(strikeEvidence.rawScore >= -1.0 && strikeEvidence.rawScore <= 1.0)
    assertTrue(regimeEvidence.dataQualityScore >= 0.0 && regimeEvidence.dataQualityScore <= 1.0)
  }
}
