package com.courtbook.ui.booking

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
          .background(DarkBg)
          .padding(24.dp)
  ) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
      // Big Green Animated/Vibrant Check Circle
      Box(
          modifier = Modifier
              .size(88.dp)
              .clip(CircleShape)
              .background(CourtGreen),
          contentAlignment = Alignment.Center
      ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Success",
            tint = Color.Black,
            modifier = Modifier.size(48.dp)
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      Text(
          text = "Booking Confirmed! 🎉",
          color = DarkTextPrimary,
          fontSize = 26.sp,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
          text = "Your court reservation has been committed to MongoDB Atlas with snapshot transaction isolation.",
          color = DarkTextSecondary,
          fontSize = 14.sp,
          textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(28.dp))

      // Reference Card
      Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
      ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text("Booking Reference", color = DarkTextSecondary, fontSize = 12.sp)
          Spacer(modifier = Modifier.height(6.dp))
          Text(
              text = bookingNumber,
              color = CourtGreen,
              fontSize = 20.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text("Status: CONFIRMED • PAID", color = StatusAvailable, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
      }

      Spacer(modifier = Modifier.height(36.dp))

      // Action Buttons
      Button(
          onClick = onNavigateToMyBookings,
          modifier = Modifier.fillMaxWidth().height(50.dp),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = CourtGreen)
      ) {
        Text("View My Bookings", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
      }

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedButton(
          onClick = onNavigateToHome,
          modifier = Modifier.fillMaxWidth().height(50.dp),
          shape = RoundedCornerShape(12.dp)
      ) {
        Text("Back to Home", color = DarkTextPrimary, fontSize = 15.sp)
      }
    }
  }
}
