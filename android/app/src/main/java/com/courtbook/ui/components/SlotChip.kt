package com.courtbook.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.courtbook.analytics.engine.BookingPredictionHelper
import com.courtbook.data.model.SlotDto
import com.courtbook.ui.theme.*

/**
 * Responsive, compact time-slot card designed for a 2-column grid layout.
 * Displays start-end time, actual price, status badge, predictive indicator, and clear selection styling.
 */
@Composable
fun SlotChip(
    slot: SlotDto,
    isSelected: Boolean,
    onSelectToggle: (SlotDto) -> Unit,
    modifier: Modifier = Modifier,
    isHeldByMe: Boolean = false,
    predictionTag: BookingPredictionHelper.SlotTag? = null
) {
  val isClickable = slot.isAvailable || (slot.isHeld && isHeldByMe && !slot.isHoldExpired())

  val backgroundColor = when {
    isSelected -> ForestGreen
    isHeldByMe && !slot.isHoldExpired() -> Color(0xFFF3FEE8)
    slot.isAvailable -> CardSurface
    slot.isHeld -> Color(0xFFFFFBEB)
    slot.isBooked -> Color(0xFFFEF2F2)
    slot.isBlocked -> Color(0xFFF9FAFB)
    else -> CardSurface
  }

  val borderColor = when {
    isSelected -> ForestGreen
    isHeldByMe && !slot.isHoldExpired() -> ForestGreen.copy(alpha = 0.5f)
    slot.isAvailable -> BorderSubtle
    slot.isHeld -> Color(0xFFFDE68A)
    slot.isBooked -> Color(0xFFFECACA)
    else -> Color(0xFFE5E7EB)
  }

  val primaryTextColor = when {
    isSelected -> Color.White
    isHeldByMe && !slot.isHoldExpired() -> ForestGreen
    slot.isAvailable -> TextDeep
    slot.isHeld -> StatusWarning
    slot.isBooked -> StatusDestructive
    else -> TextMuted
  }

  val priceColor = when {
    isSelected -> ElectricLime
    isHeldByMe && !slot.isHoldExpired() -> ForestGreen
    slot.isAvailable -> ForestGreen
    else -> TextMuted
  }

  Surface(
      modifier = modifier
          .fillMaxWidth()
          .defaultMinSize(minHeight = 94.dp)
          .clip(RoundedCornerShape(14.dp))
          .clickable(enabled = isClickable) { onSelectToggle(slot) },
      shape = RoundedCornerShape(14.dp),
      color = backgroundColor,
      border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
      shadowElevation = if (isSelected) 2.dp else 0.dp
  ) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
      // Row 1: Time range & Selection Checkmark or Status Indicator Dot
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
            text = slot.timeRange,
            color = primaryTextColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (isSelected) {
          Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = "Selected",
              tint = ElectricLime,
              modifier = Modifier.size(18.dp)
          )
        } else {
          Box(
              modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(
                      when {
                        isHeldByMe && !slot.isHoldExpired() -> ForestGreen
                        slot.isAvailable -> StatusSuccess
                        slot.isHeld -> StatusWarning
                        slot.isBooked -> StatusDestructive
                        else -> StatusUnavailable
                      }
                  )
          )
        }
      }

      // Row 2: Predictive Advisory Tag (if available)
      if (predictionTag != null) {
        Surface(
            color = if (isSelected) {
              Color.White.copy(alpha = 0.18f)
            } else {
              when (predictionTag.type) {
                BookingPredictionHelper.PredictionTagType.POPULAR -> Color(0xFFFEF3C7)
                BookingPredictionHelper.PredictionTagType.QUIETER -> SoftGreenSurface
                BookingPredictionHelper.PredictionTagType.BEST_VALUE -> ElectricLime.copy(alpha = 0.35f)
                BookingPredictionHelper.PredictionTagType.FEW_LEFT -> Color(0xFFFEE2E2)
                BookingPredictionHelper.PredictionTagType.FILLING_FAST -> Color(0xFFFFEDD5)
              }
            },
            shape = RoundedCornerShape(4.dp)
        ) {
          Text(
              text = predictionTag.label,
              color = if (isSelected) {
                ElectricLime
              } else {
                when (predictionTag.type) {
                  BookingPredictionHelper.PredictionTagType.POPULAR -> StatusWarning
                  BookingPredictionHelper.PredictionTagType.QUIETER -> ForestGreen
                  BookingPredictionHelper.PredictionTagType.BEST_VALUE -> ForestGreen
                  BookingPredictionHelper.PredictionTagType.FEW_LEFT -> StatusDestructive
                  BookingPredictionHelper.PredictionTagType.FILLING_FAST -> Color(0xFFEA580C)
                }
              },
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
          )
        }
      } else {
        Spacer(modifier = Modifier.height(4.dp))
      }

      // Row 3: Price & Status Label
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Bottom
      ) {
        Text(
            text = "₹${slot.price.toInt()}",
            color = priceColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = when {
              isSelected -> "Selected"
              isHeldByMe && !slot.isHoldExpired() -> "Held by You"
              slot.isAvailable -> "Available"
              slot.isHeld -> "Held"
              slot.isBooked -> "Booked"
              slot.isBlocked -> slot.blockReason ?: "Blocked"
              else -> slot.status
            },
            color = when {
              isSelected -> ElectricLime.copy(alpha = 0.9f)
              isHeldByMe && !slot.isHoldExpired() -> ForestGreen
              slot.isAvailable -> StatusSuccess
              slot.isHeld -> StatusWarning
              slot.isBooked -> StatusDestructive
              else -> TextMuted
            },
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
      }
    }
  }
}
