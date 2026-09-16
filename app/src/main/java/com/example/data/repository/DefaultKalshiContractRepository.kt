package com.example.data.repository

import com.example.data.model.KalshiContract
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DefaultKalshiContractRepository(
  private val maxHistory: Int = 50
) : KalshiContractRepository {

  private val _activeContract = MutableStateFlow<KalshiContract?>(null)
  override val activeContract: StateFlow<KalshiContract?> = _activeContract.asStateFlow()

  private val _recentContracts = MutableStateFlow<List<KalshiContract>>(emptyList())
  override val recentContracts: StateFlow<List<KalshiContract>> = _recentContracts.asStateFlow()

  override fun setActiveContract(contract: KalshiContract) {
    _activeContract.value = contract
  }

  override fun updateContractQuote(
    ticker: String,
    yesBid: Double?,
    yesAsk: Double?,
    noBid: Double?,
    noAsk: Double?
  ) {
    val current = _activeContract.value
    if (current != null && current.ticker == ticker) {
      _activeContract.value = current.copy(
        latestYesBid = yesBid,
        latestYesAsk = yesAsk,
        latestNoBid = noBid,
        latestNoAsk = noAsk
      )
    }
  }

  override fun archiveContract(contract: KalshiContract) {
    val history = _recentContracts.value.toMutableList()
    history.add(0, contract)
    if (history.size > maxHistory) {
      history.removeAt(history.lastIndex)
    }
    _recentContracts.value = history
    if (_activeContract.value?.ticker == contract.ticker) {
      _activeContract.value = null
    }
  }
}
