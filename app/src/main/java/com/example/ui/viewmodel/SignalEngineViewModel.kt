package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.config.KalshiMarketSpecs
import com.example.core.config.SignalEngineConfig
import com.example.core.logging.AndroidQuantLogger
import com.example.core.logging.QuantLogger
import com.example.core.logging.SignalEngineLogEvent
import com.example.core.util.ContractTimeUtils
import com.example.data.model.Candle
import com.example.data.model.KalshiContract
import com.example.data.model.KalshiContractStatus
import com.example.data.model.MarketDataFeedState
import com.example.data.model.MarketSnapshot
import com.example.data.repository.DefaultKalshiContractRepository
import com.example.data.repository.DefaultMarketDataRepository
import com.example.data.repository.KalshiContractRepository
import com.example.data.repository.MarketDataRepository
import com.example.ensemble.ConfidenceThresholdFilter
import com.example.ensemble.DefaultSignalEngine
import com.example.ensemble.ExpiryBufferRiskFilter
import com.example.ensemble.FreshnessRiskFilter
import com.example.ensemble.SignalEngine
import com.example.ensemble.SignalEngineState
import com.example.ensemble.SignalOutput
import com.example.ensemble.SpreadRiskFilter
import com.example.prediction.BaselinePredictionModel
import com.example.quant.DefaultFeatureExtractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SignalEngineUiState(
  val feedState: MarketDataFeedState = MarketDataFeedState.Uninitialized,
  val latestSnapshot: MarketSnapshot? = null,
  val candles: List<Candle> = emptyList(),
  val activeContract: KalshiContract? = null,
  val engineState: SignalEngineState = SignalEngineState.Idle,
  val latestSignal: SignalOutput? = null,
  val recentLogs: List<SignalEngineLogEvent> = emptyList(),
  val config: SignalEngineConfig = SignalEngineConfig()
)

class SignalEngineViewModel(
  private val config: SignalEngineConfig = SignalEngineConfig(),
  private val marketDataRepository: MarketDataRepository = DefaultMarketDataRepository(),
  private val kalshiContractRepository: KalshiContractRepository = DefaultKalshiContractRepository(),
  private val logger: QuantLogger = AndroidQuantLogger(),
  val signalEngine: SignalEngine = DefaultSignalEngine(
    config = config,
    featureExtractor = DefaultFeatureExtractor(),
    models = listOf(BaselinePredictionModel()),
    riskFilters = listOf(
      FreshnessRiskFilter(),
      SpreadRiskFilter(),
      ExpiryBufferRiskFilter(),
      ConfidenceThresholdFilter()
    ),
    logger = logger
  )
) : ViewModel() {

  init {
    logger.info("SignalEngineViewModel", "Initializing QtY 64 Engine Foundation")
    setupInitialContract()
  }

  private data class MarketDataBundle(
    val feedState: MarketDataFeedState,
    val snapshot: MarketSnapshot?,
    val candles: List<Candle>,
    val contract: KalshiContract?
  )

  private val marketDataBundle = combine(
    marketDataRepository.feedState,
    marketDataRepository.latestSnapshot,
    marketDataRepository.candles,
    kalshiContractRepository.activeContract
  ) { feed, snap, cands, contract ->
    MarketDataBundle(feed, snap, cands, contract)
  }

  val uiState: StateFlow<SignalEngineUiState> = combine(
    marketDataBundle,
    signalEngine.engineState,
    signalEngine.latestSignal,
    logger.recentEvents
  ) { data, engState, signal, logs ->
    SignalEngineUiState(
      feedState = data.feedState,
      latestSnapshot = data.snapshot,
      candles = data.candles,
      activeContract = data.contract,
      engineState = engState,
      latestSignal = signal,
      recentLogs = logs,
      config = config
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = SignalEngineUiState(
      feedState = marketDataRepository.feedState.value,
      activeContract = kalshiContractRepository.activeContract.value,
      config = config
    )
  )

  private fun setupInitialContract() {
    val now = System.currentTimeMillis()
    val expiry = ContractTimeUtils.getCurrentWindowExpirationMs(now)
    val open = ContractTimeUtils.getCurrentWindowOpenMs(now)
    val defaultContract = KalshiContract(
      ticker = KalshiMarketSpecs.formatContractTicker(expiry, 95000.0),
      seriesTicker = KalshiMarketSpecs.CONTRACT_SERIES_PREFIX,
      targetStrike = 95000.0,
      openTimestampMs = open,
      expirationTimestampMs = expiry,
      status = KalshiContractStatus.ACTIVE
    )
    kalshiContractRepository.setActiveContract(defaultContract)
  }

  /**
   * Triggers an engine decision cycle.
   * Evaluates market data, features, prediction models, and risk gates.
   */
  fun evaluateCurrentCycle() {
    viewModelScope.launch {
      val contract = kalshiContractRepository.activeContract.value ?: return@launch
      val candles = marketDataRepository.candles.value
      val snapshot = marketDataRepository.latestSnapshot.value
      val feed = marketDataRepository.feedState.value

      signalEngine.evaluateCycle(
        contract = contract,
        candles = candles,
        snapshot = snapshot,
        feedState = feed
      )
    }
  }
}
