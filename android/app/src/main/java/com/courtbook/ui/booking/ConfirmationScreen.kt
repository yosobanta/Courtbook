package com.courtbook.ui.booking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
fun ConfirmationScreen(
    bookingNumber: String,
    onNavigateToMyBookings: () -> Unit,
    onNavigateToHome: () -> Unit
) {
  Box(
      modifier = Modifier
          .fillMaxSize()
          .background(WarmBg)
          .padding(24.dp)
  ) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
      // Big Green Vibrant Check Circle with Soft Halo
      Box(
          modifier = Modifier
              .size(96.dp)
              .clip(CircleShape)
              .background(SoftGreenSurface),
          contentAlignment = Alignment.Center
      ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(ForestGreen),
            contentAlignment = Alignment.Center
        ) {
          Icon(
              imageVector = Icons.Default.Check,
              contentDescription = "Success",
              tint = ElectricLime,
              modifier = Modifier.size(42.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      Text(
          text = "Booking Confirmed! 🎉",
          color = TextDeep,
          fontSize = 28.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = (-0.5).sp,
          textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
          text = "Your court reservation has been committed to MongoDB Atlas with snapshot transaction isolation.",
          color = TextMuted,
          fontSize = 14.sp,
          lineHeight = 20.sp,
          textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(32.dp))

      // Reference Card
      Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = CardSurface),
          border = BorderStroke(1.dp, BorderSubtle),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
      ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text("Booking Reference", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
          Spacer(modifier = Modifier.height(6.dp))
          Text(
              text = bookingNumber,
              color = ForestGreen,
              fontSize = 22.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp
          )
          Spacer(modifier = Modifier.height(8.dp))
          Surface(
              color = SoftGreenSurface,
              shape = RoundedCornerShape(6.dp)
          ) {
            Text(
                text = "STATUS: CONFIRMED • PAID",
                color = StatusSuccess,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(36.dp))

      // Action Buttons
      Button(
          onClick = onNavigateToMyBookings,
          modifier = Modifier
              .fillMaxWidth()
              .height(52.dp),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
      ) {
        Text("View My Bookings", color = ElectricLime, fontWeight = FontWeight.Bold, fontSize = 15.sp)
      }

      Spacer(modifier = Modifier.height(14.dp))

      OutlinedButton(
          onClick = onNavigateToHome,
          modifier = Modifier
              .fillMaxWidth()
              .height(52.dp),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.outlinedButtonColors(containerColor = CardSurface),
          border = BorderStroke(1.dp, BorderSubtle)
      ) {
        Text("Back to Home", color = TextDeep, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
      }
    }
  }
}
