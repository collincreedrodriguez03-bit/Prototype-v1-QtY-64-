package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ContractStatusCard
import com.example.ui.components.EngineTelemetryCard
import com.example.ui.components.RegulatoryDisclaimerBanner
import com.example.ui.components.SignalBadge
import com.example.ui.components.TradingViewCanvas
import com.example.ui.theme.QuantBackground
import com.example.ui.theme.QuantBlue
import com.example.ui.theme.QuantBorder
import com.example.ui.theme.QuantSurface
import com.example.ui.theme.QuantTextPrimary
import com.example.ui.theme.QuantTextSecondary
import com.example.ui.viewmodel.SignalEngineViewModel

@Composable
fun SignalEngineScreen(
  viewModel: SignalEngineViewModel,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .windowInsetsPadding(WindowInsets.safeDrawing),
    containerColor = QuantBackground
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Header
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(QuantSurface)
          .padding(horizontal = 16.dp, vertical = 12.dp)
          .testTag("app_header_bar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "QtY 64",
              color = QuantTextPrimary,
              fontSize = 18.sp,
              fontWeight = FontWeight.Black,
              letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(QuantBlue.copy(alpha = 0.2f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = "v1 PROTOTYPE",
                color = QuantBlue,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
              )
            }
          }
          Text(
            text = "Quantitative BTC Signal Engine • 15M Kalshi Cycle",
            color = QuantTextSecondary,
            fontSize = 11.sp
          )
        }

        Button(
          onClick = { viewModel.evaluateCurrentCycle() },
          colors = ButtonDefaults.buttonColors(containerColor = QuantBlue),
          shape = RoundedCornerShape(6.dp),
          modifier = Modifier.testTag("evaluate_cycle_button")
        ) {
          Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Evaluate Cycle",
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Evaluate",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }

      // Regulatory Non-Trading Notice
      RegulatoryDisclaimerBanner()

      // Primary Output: Decision Badge
      SignalBadge(signal = uiState.latestSignal)

      // Active 15M Contract Specifications
      ContractStatusCard(contract = uiState.activeContract)

      // TradingView-Inspired Quant Canvas
      TradingViewCanvas(
        candles = uiState.candles,
        snapshot = uiState.latestSnapshot,
        targetStrike = uiState.activeContract?.targetStrike,
        feedState = uiState.feedState
      )

      // Quantitative Telemetry and Audit Log
      EngineTelemetryCard(
        engineState = uiState.engineState,
        logEvents = uiState.recentLogs
      )

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}
