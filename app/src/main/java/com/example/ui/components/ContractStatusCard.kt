package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.example.core.config.KalshiMarketSpecs
import com.example.core.util.ContractTimeUtils
import com.example.data.model.KalshiContract
import com.example.ui.theme.QuantBlue
import com.example.ui.theme.QuantBorder
import com.example.ui.theme.QuantSurface
import com.example.ui.theme.QuantTextPrimary
import com.example.ui.theme.QuantTextSecondary

@Composable
fun ContractStatusCard(
  contract: KalshiContract?,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .background(QuantSurface)
      .border(1.dp, QuantBorder, RoundedCornerShape(8.dp))
      .padding(16.dp)
      .testTag("contract_status_card"),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "KALSHI 15M CONTRACT SPECIFICATION",
        color = QuantTextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
      )

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(4.dp))
          .background(QuantBlue.copy(alpha = 0.15f))
          .padding(horizontal = 6.dp, vertical = 2.dp)
      ) {
        Text(
          text = KalshiMarketSpecs.CONTRACT_SERIES_PREFIX,
          color = QuantBlue,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
      }
    }

    if (contract == null) {
      Text(
        text = "No active contract configured. Engine will automatically subscribe when feed connects.",
        color = QuantTextSecondary,
        fontSize = 12.sp
      )
    } else {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column {
          Text(text = "Contract Ticker", color = QuantTextSecondary, fontSize = 10.sp)
          Text(
            text = contract.ticker,
            color = QuantTextPrimary,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold
          )
        }

        Column(horizontalAlignment = Alignment.End) {
          Text(text = "Target Strike", color = QuantTextSecondary, fontSize = 10.sp)
          Text(
            text = "$${String.format(java.util.Locale.US, "%,.2f", contract.targetStrike)}",
            color = QuantTextPrimary,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column {
          Text(text = "Settlement Index", color = QuantTextSecondary, fontSize = 10.sp)
          Text(
            text = "CME CF BRTI",
            color = QuantTextPrimary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
          )
        }

        Column(horizontalAlignment = Alignment.End) {
          val remainingSec = contract.remainingSeconds()
          Text(text = "Time to Expiry", color = QuantTextSecondary, fontSize = 10.sp)
          Text(
            text = ContractTimeUtils.formatCountdown(remainingSec),
            color = if (remainingSec < 60) com.example.ui.theme.QuantRed else QuantTextPrimary,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}
