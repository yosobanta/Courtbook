package com.courtbook.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
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
          .padding(32.dp),
      contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      CircularProgressIndicator(color = CourtGreen, strokeWidth = 3.dp)
      Spacer(modifier = Modifier.height(16.dp))
      Text(text = message, color = DarkTextSecondary, fontSize = 14.sp)
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
  val bgColor = if (isConflict) StatusHeld.copy(alpha = 0.15f) else StatusBooked.copy(alpha = 0.15f)
  val iconColor = if (isConflict) StatusHeld else StatusBooked

  Card(
      modifier = modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = bgColor)
  ) {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
          imageVector = if (isConflict) Icons.Default.Warning else Icons.Default.Info,
          contentDescription = "Alert",
          tint = iconColor,
          modifier = Modifier.size(24.dp)
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        if (isConflict) {
          Text(
              text = "Slot Unavailable",
              color = StatusHeld,
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp
          )
        }
        Text(
            text = errorMessage,
            color = DarkTextPrimary,
            fontSize = 13.sp
        )
      }
      if (onRetry != null) {
        Spacer(modifier = Modifier.width(8.dp))
        TextButton(onClick = onRetry) {
          Text("Refresh", color = CourtGreen, fontWeight = FontWeight.Bold)
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
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
          text = "🏟️",
          fontSize = 48.sp
      )
      Spacer(modifier = Modifier.height(16.dp))
      Text(
          text = title,
          color = DarkTextPrimary,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
          text = description,
          color = DarkTextSecondary,
          fontSize = 13.sp,
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
  val bg = if (isSelected) CourtGreen else DarkSurfaceElevated
  val textColor = if (isSelected) Color.Black else DarkTextSecondary

  Box(
      modifier = modifier
          .clip(RoundedCornerShape(20.dp))
          .background(bg)
          .clickable(onClick = onSelect)
          .padding(horizontal = 16.dp, vertical = 8.dp)
  ) {
    Text(
        text = sport,
        color = textColor,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        fontSize = 13.sp
    )
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
        style = MaterialTheme.typography.titleLarge,
        color = DarkTextPrimary,
        fontWeight = FontWeight.Bold
    )
    if (actionText != null && onActionClick != null) {
      Text(
          text = actionText,
          color = CourtGreen,
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.clickable(onClick = onActionClick)
      )
    }
  }
}
