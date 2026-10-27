package com.courtbook.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.courtbook.data.model.SlotDto
import com.courtbook.ui.theme.*

@Composable
fun SlotChip(
    slot: SlotDto,
    isSelected: Boolean,
    onSelectToggle: (SlotDto) -> Unit,
    modifier: Modifier = Modifier
) {
  val isClickable = slot.isAvailable

  val backgroundColor = when {
    isSelected -> CourtGreen
    slot.isAvailable -> DarkSurfaceElevated
    slot.isHeld -> StatusHeld.copy(alpha = 0.15f)
    slot.isBooked -> StatusBooked.copy(alpha = 0.15f)
    slot.isBlocked -> StatusBlocked.copy(alpha = 0.15f)
    else -> DarkSurfaceElevated
  }

  val border = when {
    isSelected -> BorderStroke(1.5.dp, CourtGreen)
    slot.isAvailable -> BorderStroke(1.dp, DarkBorder)
    slot.isHeld -> BorderStroke(1.dp, StatusHeld.copy(alpha = 0.5f))
    slot.isBooked -> BorderStroke(1.dp, StatusBooked.copy(alpha = 0.3f))
    else -> BorderStroke(1.dp, DarkBorder.copy(alpha = 0.3f))
  }

  val textColor = when {
    isSelected -> Color.Black
    slot.isAvailable -> DarkTextPrimary
    slot.isHeld -> StatusHeld
    slot.isBooked -> StatusBooked.copy(alpha = 0.8f)
    else -> DarkTextSecondary
  }

  Box(
      modifier = modifier
          .clip(RoundedCornerShape(12.dp))
          .background(backgroundColor)
          .then(if (border.width > 0.dp) Modifier.padding(1.dp) else Modifier)
          .clickable(enabled = isClickable) { onSelectToggle(slot) }
          .padding(horizontal = 12.dp, vertical = 10.dp)
  ) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
            text = slot.timeRange,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
        )
        Text(
            text = when {
              slot.isAvailable -> "₹${slot.price.toInt()} • Available"
              slot.isHeld -> "Temporarily Held"
              slot.isBooked -> "Booked"
              slot.isBlocked -> slot.blockReason ?: "Blocked"
              else -> slot.status
            },
            color = if (isSelected) Color.Black.copy(alpha = 0.8f) else textColor.copy(alpha = 0.7f),
            fontSize = 11.sp
        )
      }

      if (isSelected) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Selected",
            tint = Color.Black,
            modifier = Modifier.size(18.dp)
        )
      }
    }
  }
}
