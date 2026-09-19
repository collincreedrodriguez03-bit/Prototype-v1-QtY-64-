package com.example.data.features

import com.example.data.model.BtcMarketObservation
import com.example.data.model.DataQualityGrade
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelIsolationAuditTest {

  @Test
  fun `audit all seven engines for complete isolation, determinism, and temporal safety`() {
    val evalTime = 1700000000000L
    val observations = (0..20).map { i ->
      BtcMarketObservation(
        timestampMs = evalTime - ((20 - i) * 1000L),
        receiptTimestampMs = evalTime - ((20 - i) * 1000L) + 10L,
        price = 95000.0 + (i * 10.0),
        qualityStatus = DataQualityGrade.VALID
      )
    }

    // 1. Trend Engine Audit
    val trend1 = TrendQuantitativeEngine.evaluateTrend(observations, evalTime, minRequiredObservations = 5)
    val trend2 = TrendQuantitativeEngine.evaluateTrend(observations, evalTime, minRequiredObservations = 5)
    assertTrue(trend1.isUsable)
    assertEquals(trend1.rawScore, trend2.rawScore, 0.00001) // Deterministic
    assertTrue(trend1.rawScore in -1.0..1.0)
    assertNotNull(trend1.componentMeasurements["short_term_return"])

    // 2. Momentum Engine Audit
    val mom1 = MomentumQuantitativeEngine.evaluateMomentum(observations, evalTime, minRequiredObservations = 5)
    val mom2 = MomentumQuantitativeEngine.evaluateMomentum(observations, evalTime, minRequiredObservations = 5)
    assertTrue(mom1.isUsable)
    assertEquals(mom1.rawScore, mom2.rawScore, 0.00001)
    assertTrue(mom1.rawScore in -1.0..1.0)
    assertNotNull(mom1.componentMeasurements["short_velocity"])

    // 3. RSI / Mean Reversion Engine Audit
    val rsi1 = RsiMeanReversionQuantitativeEngine.evaluateMeanReversion(observations, evalTime, minRequiredObservations = 10)
    val rsi2 = RsiMeanReversionQuantitativeEngine.evaluateMeanReversion(observations, evalTime, minRequiredObservations = 10)
    assertTrue(rsi1.isUsable)
    assertEquals(rsi1.rawScore, rsi2.rawScore, 0.00001)
    assertTrue(rsi1.rawScore in -1.0..1.0)
    assertNotNull(rsi1.componentMeasurements["rsi"])

    // 4. Volatility Engine Audit
    val vol1 = VolatilityQuantitativeEngine.evaluateVolatility(observations, evalTime, minRequiredObservations = 5)
    val vol2 = VolatilityQuantitativeEngine.evaluateVolatility(observations, evalTime, minRequiredObservations = 5)
    assertTrue(vol1.isUsable)
    assertEquals(vol1.realizedVolatility, vol2.realizedVolatility, 0.00001)
    assertTrue(vol1.realizedVolatility >= 0.0)
    assertNotNull(vol1.componentMeasurements["long_term_volatility"])

    // 5. Market Structure Engine Audit
    val struct1 = MarketStructureQuantitativeEngine.evaluateMarketStructure(observations, evalTime, minRequiredObservations = 5)
    val struct2 = MarketStructureQuantitativeEngine.evaluateMarketStructure(observations, evalTime, minRequiredObservations = 5)
    assertTrue(struct1.isUsable)
    assertEquals(struct1.rawScore, struct2.rawScore, 0.00001)
    assertTrue(struct1.rawScore in -1.0..1.0)
    assertNotNull(struct1.componentMeasurements["range_position"])

    // 6. Strike / Time Engine Audit
    val strike1 = StrikeTimeQuantitativeEngine.evaluateStrikeTime(observations, evalTime, strikePrice = 95000.0, contractEndTimeMs = evalTime + 60000L, contractStartTimeMs = evalTime - 60000L)
    val strike2 = StrikeTimeQuantitativeEngine.evaluateStrikeTime(observations, evalTime, strikePrice = 95000.0, contractEndTimeMs = evalTime + 60000L, contractStartTimeMs = evalTime - 60000L)
    assertTrue(strike1.isUsable)
    assertEquals(strike1.rawScore, strike2.rawScore, 0.00001)
    assertTrue(strike1.rawScore in -1.0..1.0)
    assertNotNull(strike1.diagnosticMetadata["strike_price"])

    // 7. Market Regime / Data Quality Engine Audit
    val regime1 = MarketRegimeDataQualityEngine.evaluateRegimeAndQuality(observations, evalTime, minRequiredObservations = 5)
    val regime2 = MarketRegimeDataQualityEngine.evaluateRegimeAndQuality(observations, evalTime, minRequiredObservations = 5)
    assertTrue(regime1.isUsable)
    assertEquals(regime1.dataQualityScore, regime2.dataQualityScore, 0.00001)
    assertTrue(regime1.dataQualityScore in 0.0..1.0)
    assertNotNull(regime1.diagnosticMetadata["total_observations"])
  }

  @Test
  fun `future observations cannot affect historical evaluation (Temporal Leakage Test)`() {
    val evalTime = 1700000000000L
    val pastObservations = listOf(
      BtcMarketObservation(timestampMs = evalTime - 2000L, price = 95000.0),
      BtcMarketObservation(timestampMs = evalTime - 1000L, price = 95100.0)
    )
    val observationsWithFutureLeak = pastObservations + listOf(
      BtcMarketObservation(timestampMs = evalTime + 5000L, price = 99999.0)
    )

    val evidenceWithoutLeak = TrendQuantitativeEngine.evaluateTrend(pastObservations, evalTime, minRequiredObservations = 2)
    val evidenceWithLeak = TrendQuantitativeEngine.evaluateTrend(observationsWithFutureLeak, evalTime, minRequiredObservations = 2)

    assertEquals(evidenceWithoutLeak.rawScore, evidenceWithLeak.rawScore, 0.00001)
    assertEquals(evidenceWithoutLeak.dataSufficiency, evidenceWithLeak.dataSufficiency)
  }

  @Test
  fun `insufficient data produces unusable and neutral evidence across all engines`() {
    val evalTime = 1700000000000L
    val sparseObservations = listOf(
      BtcMarketObservation(timestampMs = evalTime - 1000L, price = 95000.0)
    )

    val trend = TrendQuantitativeEngine.evaluateTrend(sparseObservations, evalTime, minRequiredObservations = 5)
    assertFalse(trend.dataSufficiency)
    assertFalse(trend.isUsable)
    assertEquals(0.0, trend.rawScore, 0.0001)

    val mom = MomentumQuantitativeEngine.evaluateMomentum(sparseObservations, evalTime, minRequiredObservations = 5)
    assertFalse(mom.dataSufficiency)
    assertFalse(mom.isUsable)

    val rsi = RsiMeanReversionQuantitativeEngine.evaluateMeanReversion(sparseObservations, evalTime, minRequiredObservations = 10)
    assertFalse(rsi.dataSufficiency)
    assertFalse(rsi.isUsable)

    val vol = VolatilityQuantitativeEngine.evaluateVolatility(sparseObservations, evalTime, minRequiredObservations = 5)
    assertFalse(vol.dataSufficiency)
    assertFalse(vol.isUsable)

    val struct = MarketStructureQuantitativeEngine.evaluateMarketStructure(sparseObservations, evalTime, minRequiredObservations = 5)
    assertFalse(struct.dataSufficiency)
    assertFalse(struct.isUsable)

    val strike = StrikeTimeQuantitativeEngine.evaluateStrikeTime(sparseObservations, evalTime, strikePrice = 0.0, contractEndTimeMs = evalTime, contractStartTimeMs = evalTime)
    assertFalse(strike.dataSufficiency)
    assertFalse(strike.isUsable)

    val regime = MarketRegimeDataQualityEngine.evaluateRegimeAndQuality(sparseObservations, evalTime, minRequiredObservations = 5)
    assertFalse(regime.dataSufficiency)
    assertFalse(regime.isUsable)
  }
}
