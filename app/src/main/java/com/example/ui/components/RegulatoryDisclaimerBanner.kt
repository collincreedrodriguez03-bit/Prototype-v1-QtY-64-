package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.QuantAmber
import com.example.ui.theme.QuantAmberDim
import com.example.ui.theme.QuantBorder
import com.example.ui.theme.QuantTextPrimary

@Composable
fun RegulatoryDisclaimerBanner(modifier: Modifier = Modifier) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(6.dp))
      .background(QuantAmberDim)
      .border(1.dp, QuantAmber.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
      .padding(horizontal = 12.dp, vertical = 8.dp)
      .testTag("regulatory_disclaimer_banner"),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Icon(
      imageVector = Icons.Default.Info,
      contentDescription = "Non-Trading Regulatory Disclaimer",
      tint = QuantAmber,
      modifier = Modifier.size(18.dp)
    )
    Text(
      text = "PROTOTYPE SIGNAL ENGINE ONLY • NO AUTOMATED TRADING OR ORDER EXECUTION • NON-BROKERAGE",
      color = QuantTextPrimary,
      fontSize = 11.sp,
      fontWeight = FontWeight.SemiBold,
      letterSpacing = 0.5.sp
    )
  }
}
