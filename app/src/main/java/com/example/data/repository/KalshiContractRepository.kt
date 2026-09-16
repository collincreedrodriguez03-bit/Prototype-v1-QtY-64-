package com.example.data.repository

import com.example.data.model.KalshiContract
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface managing Kalshi event contracts.
 * Maintains boundary for Kalshi REST/WebSocket market subscriptions.
 */
interface KalshiContractRepository {
  val activeContract: StateFlow<KalshiContract?>
  val recentContracts: StateFlow<List<KalshiContract>>

  fun setActiveContract(contract: KalshiContract)
  fun updateContractQuote(
    ticker: String,
    yesBid: Double?,
    yesAsk: Double?,
    noBid: Double?,
    noAsk: Double?
  )
  fun archiveContract(contract: KalshiContract)
}
