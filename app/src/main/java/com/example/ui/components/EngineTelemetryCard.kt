package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.logging.SignalEngineLogEvent
import com.example.core.util.ContractTimeUtils
import com.example.ensemble.SignalEngineState
import com.example.ui.theme.QuantAmber
import com.example.ui.theme.QuantBackground
import com.example.ui.theme.QuantBlue
import com.example.ui.theme.QuantBorder
import com.example.ui.theme.QuantGreen
import com.example.ui.theme.QuantRed
import com.example.ui.theme.QuantSurface
import com.example.ui.theme.QuantSurfaceVariant
import com.example.ui.theme.QuantTextPrimary
import com.example.ui.theme.QuantTextSecondary

@Composable
fun EngineTelemetryCard(
  engineState: SignalEngineState,
  logEvents: List<SignalEngineLogEvent>,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .background(QuantSurface)
      .border(1.dp, QuantBorder, RoundedCornerShape(8.dp))
      .padding(16.dp)
      .testTag("engine_telemetry_card"),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "PIPELINE ARCHITECTURE & AUDIT TRAIL",
        color = QuantTextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
      )

      val stateLabel = when (engineState) {
        is SignalEngineState.Idle -> "IDLE"
        is SignalEngineState.AwaitingData -> "AWAITING DATA"
        is SignalEngineState.Evaluating -> "EVALUATING"
        is SignalEngineState.Evaluated -> "EVALUATED"
        is SignalEngineState.Error -> "ERROR"
      }

      val stateColor = when (engineState) {
        is SignalEngineState.Evaluated -> QuantGreen
        is SignalEngineState.Evaluating -> QuantBlue
        is SignalEngineState.Error -> QuantRed
        else -> QuantAmber
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Box(
          modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(stateColor)
        )
        Text(
          text = stateLabel,
          color = stateColor,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
      }
    }

    // Pipeline Separation Stages
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(6.dp))
        .background(QuantSurfaceVariant)
        .padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      PipelineStageRow(stageNumber = "1", stageName = "Data Layer", status = "Repository Boundaries Active", isOperational = true)
      PipelineStageRow(stageNumber = "2", stageName = "Quant Features", status = "Extractor & Validation Active", isOperational = true)
      PipelineStageRow(stageNumber = "3", stageName = "Prediction Models", status = "Baseline Interface (Math Pending)", isOperational = true)
      PipelineStageRow(stageNumber = "4", stageName = "Risk Gates", status = "Fail-Closed Architecture", isOperational = true)
      PipelineStageRow(stageNumber = "5", stageName = "Decision Engine", status = "Independent from UI (Non-Trading)", isOperational = true)
    }

    // Recent Audit Logs
    Text(
      text = "RECENT QUANT ENGINE EVENTS (${logEvents.size})",
      color = QuantTextSecondary,
      fontSize = 10.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 0.5.sp
    )

    if (logEvents.isEmpty()) {
      Text(
        text = "No engine events recorded yet. Engine will log structured events during decision runs.",
        color = QuantTextSecondary,
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace
      )
    } else {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(4.dp))
          .background(QuantBackground)
          .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        logEvents.takeLast(6).reversed().forEach { event ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = ContractTimeUtils.formatUtcTime(event.timestampMs),
              color = QuantTextSecondary,
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace
            )
            Text(
              text = formatLogEvent(event),
              color = QuantTextPrimary,
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace,
              maxLines = 1
            )
          }
        }
      }
    }
  }
}

@Composable
fun PipelineStageRow(
  stageNumber: String,
  stageName: String,
  status: String,
  isOperational: Boolean
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Text(
        text = "[$stageNumber]",
        color = QuantBlue,
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = stageName,
        color = QuantTextPrimary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold
      )
    }

    Text(
      text = status,
      color = if (isOperational) QuantGreen else QuantAmber,
      fontSize = 10.sp,
      fontFamily = FontFamily.Monospace
    )
  }
}

private fun formatLogEvent(event: SignalEngineLogEvent): String {
  return when (event) {
    is SignalEngineLogEvent.SignalEmitted -> "SIGNAL -> ${event.decision} (${event.contractTicker}) conf=${event.confidence}"
    is SignalEngineLogEvent.RiskGateEvaluated -> "RISK GATE: ${event.gateName} (Passed=${event.passed})"
    is SignalEngineLogEvent.ModelEvaluated -> "MODEL: ${event.modelId} P(YES)=${event.probabilityYes}"
    is SignalEngineLogEvent.FeatureExtractionCompleted -> "FEATURES: ${event.featureCount} extracted"
    is SignalEngineLogEvent.MarketDataReceived -> "DATA: ${event.symbol} @ $${event.lastPrice}"
    is SignalEngineLogEvent.EngineStateChanged -> "STATE: ${event.previousState} -> ${event.newState}"
    is SignalEngineLogEvent.SystemMessage -> "[${event.tag}] ${event.message}"
  }
}
