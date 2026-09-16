package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ensemble.RiskFlag
import com.example.ensemble.SignalDecision
import com.example.ensemble.SignalOutput
import com.example.ui.theme.QuantAmber
import com.example.ui.theme.QuantAmberDim
import com.example.ui.theme.QuantBorder
import com.example.ui.theme.QuantGreen
import com.example.ui.theme.QuantGreenDim
import com.example.ui.theme.QuantRed
import com.example.ui.theme.QuantRedDim
import com.example.ui.theme.QuantSurface
import com.example.ui.theme.QuantSurfaceVariant
import com.example.ui.theme.QuantTextPrimary
import com.example.ui.theme.QuantTextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SignalBadge(
  signal: SignalOutput?,
  modifier: Modifier = Modifier
) {
  val decision = signal?.decision ?: SignalDecision.NO_TRADE

  val (accentColor, bgColor, decisionText) = when (decision) {
    SignalDecision.YES -> Triple(QuantGreen, QuantGreenDim, "SIGNAL: YES")
    SignalDecision.NO -> Triple(QuantRed, QuantRedDim, "SIGNAL: NO")
    SignalDecision.NO_TRADE -> Triple(QuantAmber, QuantAmberDim, "SIGNAL: NO_TRADE")
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .background(QuantSurface)
      .border(1.dp, QuantBorder, RoundedCornerShape(8.dp))
      .padding(16.dp)
      .testTag("signal_badge_container"),
    verticalArrangement = Arrangement.spacedBy(12.dp)
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
        Box(
          modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(accentColor)
        )
        Text(
          text = "DECISION ENGINE OUTPUT",
          color = QuantTextSecondary,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp
        )
      }

      // Signal Pill
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(4.dp))
          .background(bgColor)
          .border(1.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
          .padding(horizontal = 10.dp, vertical = 4.dp)
          .testTag("signal_decision_pill")
      ) {
        Text(
          text = decisionText,
          color = accentColor,
          fontSize = 13.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace
        )
      }
    }

    // Details & Confidence
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "Target Contract: ${signal?.contractTicker ?: "None Active"}",
          color = QuantTextPrimary,
          fontSize = 12.sp,
          fontFamily = FontFamily.Monospace
        )
        Text(
          text = "Reason: ${signal?.decisionReason ?: "Engine awaiting market cycle"}",
          color = QuantTextSecondary,
          fontSize = 11.sp
        )
      }

      Column(horizontalAlignment = Alignment.End) {
        val confidencePercent = ((signal?.confidence ?: 0.0) * 100).toInt()
        Text(
          text = "Confidence",
          color = QuantTextSecondary,
          fontSize = 10.sp
        )
        Text(
          text = "$confidencePercent%",
          color = accentColor,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
      }
    }

    // Confidence Progress Bar
    LinearProgressIndicator(
      progress = { (signal?.confidence ?: 0.0).toFloat().coerceIn(0f, 1f) },
      modifier = Modifier
        .fillMaxWidth()
        .height(4.dp)
        .clip(RoundedCornerShape(2.dp)),
      color = accentColor,
      trackColor = QuantSurfaceVariant
    )

    // Triggered Risk Flags
    val flags = signal?.riskFlags.orEmpty()
    if (flags.isNotEmpty()) {
      Text(
        text = "ACTIVE RISK GATES (${flags.size})",
        color = QuantTextSecondary,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp
      )
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        for (flag in flags) {
          RiskFlagChip(flag)
        }
      }
    }
  }
}

@Composable
fun RiskFlagChip(flag: RiskFlag) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(4.dp))
      .background(QuantSurfaceVariant)
      .border(1.dp, QuantBorder, RoundedCornerShape(4.dp))
      .padding(horizontal = 6.dp, vertical = 2.dp)
  ) {
    Text(
      text = flag.label,
      color = QuantAmber,
      fontSize = 10.sp,
      fontFamily = FontFamily.Monospace,
      fontWeight = FontWeight.Medium
    )
  }
}
