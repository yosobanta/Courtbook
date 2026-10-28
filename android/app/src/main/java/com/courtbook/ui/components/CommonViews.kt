package com.courtbook.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.courtbook.ui.theme.*

@Composable
fun StateLoader(message: String = "Loading...", modifier: Modifier = Modifier) {
  Box(
      modifier = modifier
          .fillMaxWidth()
          .padding(36.dp),
      contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Box(
          modifier = Modifier
              .size(56.dp)
              .clip(CircleShape)
              .background(SoftGreenSurface),
          contentAlignment = Alignment.Center
      ) {
        CircularProgressIndicator(
            color = ForestGreen,
            strokeWidth = 3.5.dp,
            modifier = Modifier.size(32.dp)
        )
      }
      Spacer(modifier = Modifier.height(16.dp))
      Text(
          text = message,
          color = TextDeep,
          fontSize = 14.sp,
          fontWeight = FontWeight.SemiBold
      )
    }
  }
}

@Composable
fun ErrorBanner(
    errorMessage: String,
    onRetry: (() -> Unit)? = null,
    isConflict: Boolean = false,
    modifier: Modifier = Modifier
) {
  val bgColor = if (isConflict) Color(0xFFFFFBEB) else Color(0xFFFEF2F2)
  val borderColor = if (isConflict) Color(0xFFFDE68A) else Color(0xFFFECACA)
  val iconColor = if (isConflict) StatusWarning else StatusDestructive

  Card(
      modifier = modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = bgColor),
      border = BorderStroke(1.dp, borderColor)
  ) {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
          modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(if (isConflict) Color(0xFFFEF3C7) else Color(0xFFFEE2E2)),
          contentAlignment = Alignment.Center
      ) {
        Icon(
            imageVector = if (isConflict) Icons.Default.Warning else Icons.Default.Info,
            contentDescription = "Alert",
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
      }
      Spacer(modifier = Modifier.width(14.dp))
      Column(modifier = Modifier.weight(1f)) {
        if (isConflict) {
          Text(
              text = "Slot Unavailable",
              color = StatusWarning,
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp
          )
        }
        Text(
            text = errorMessage,
            color = TextDeep,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
      }
      if (onRetry != null) {
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
        ) {
          Text("Retry", color = ElectricLime, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
      }
    }
  }
}

@Composable
fun EmptyStateView(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
  Box(
      modifier = modifier
          .fillMaxWidth()
          .padding(32.dp),
      contentAlignment = Alignment.Center
  ) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(16.dp)
    ) {
      Box(
          modifier = Modifier
              .size(72.dp)
              .clip(CircleShape)
              .background(SoftGreenSurface),
          contentAlignment = Alignment.Center
      ) {
        Icon(
            imageVector = Icons.Default.SportsTennis,
            contentDescription = null,
            tint = ForestGreen,
            modifier = Modifier.size(36.dp)
        )
      }
      Spacer(modifier = Modifier.height(16.dp))
      Text(
          text = title,
          color = TextDeep,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
          text = description,
          color = TextMuted,
          fontSize = 13.sp,
          lineHeight = 18.sp,
          textAlign = TextAlign.Center
      )
    }
  }
}

@Composable
fun SportCategoryChip(
    sport: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
  val bg = if (isSelected) ForestGreen else CardSurface
  val textColor = if (isSelected) ElectricLime else TextDeep
  val border = if (isSelected) null else BorderStroke(1.dp, BorderSubtle)

  Surface(
      modifier = modifier
          .clip(RoundedCornerShape(24.dp))
          .clickable(onClick = onSelect),
      shape = RoundedCornerShape(24.dp),
      color = bg,
      border = border,
      shadowElevation = if (isSelected) 2.dp else 0.dp
  ) {
    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
      Text(
          text = sport,
          color = textColor,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
          fontSize = 13.sp
      )
    }
  }
}

@Composable
fun SectionHeader(
    title: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
  Row(
      modifier = modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
        text = title,
        color = TextDeep,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.3).sp
    )
    if (actionText != null && onActionClick != null) {
      Text(
          text = actionText,
          color = ForestGreen,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .clickable(onClick = onActionClick)
              .padding(4.dp)
      )
    }
  }
}
