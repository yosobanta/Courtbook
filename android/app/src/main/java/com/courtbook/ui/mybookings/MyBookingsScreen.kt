package com.courtbook.ui.mybookings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.courtbook.data.model.BookingDto
import com.courtbook.ui.components.EmptyStateView
import com.courtbook.ui.components.ErrorBanner
import com.courtbook.ui.components.StateLoader
import com.courtbook.ui.theme.*

@Composable
fun MyBookingsScreen(
    viewModel: MyBookingsViewModel,
    onNavigateToHome: () -> Unit
) {
  LaunchedEffect(Unit) {
    viewModel.loadMyBookings()
  }

  val uiState by viewModel.uiState.collectAsState()
  var bookingToCancel by remember { mutableStateOf<BookingDto?>(null) }

  Column(
      modifier = Modifier
          .fillMaxSize()
          .background(WarmBg)
          .padding(horizontal = 20.dp)
  ) {
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = "My Bookings",
        color = TextDeep,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = "Manage your confirmed and past court reservations",
        color = TextMuted,
        fontSize = 14.sp
    )

    Spacer(modifier = Modifier.height(20.dp))

    if (uiState.errorMessage != null) {
      ErrorBanner(
          errorMessage = uiState.errorMessage!!,
          onRetry = { viewModel.loadMyBookings() },
          modifier = Modifier.padding(bottom = 14.dp)
      )
    }

    when {
      uiState.isLoading -> {
        StateLoader("Loading your booking history...")
      }
      uiState.bookings.isEmpty() -> {
        EmptyStateView(
            title = "No Bookings Yet",
            description = "You haven't made any court reservations yet. Find courts and start playing!"
        )
      }
      else -> {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
          items(uiState.bookings) { booking ->
            BookingCard(
                booking = booking,
                onCancelClick = { bookingToCancel = booking }
            )
          }
        }
      }
    }
  }

  // Cancel Confirmation Dialog
  if (bookingToCancel != null) {
    AlertDialog(
        onDismissRequest = { bookingToCancel = null },
        title = {
          Text("Cancel Booking?", color = TextDeep, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
          Text(
              "Are you sure you want to cancel booking ${bookingToCancel!!.bookingNumber}? This will release the court slot for other players and refund your payment.",
              color = TextMuted,
              fontSize = 14.sp,
              lineHeight = 20.sp
          )
        },
        confirmButton = {
          Button(
              onClick = {
                val bId = bookingToCancel!!.id
                bookingToCancel = null
                viewModel.cancelBooking(bId)
              },
              colors = ButtonDefaults.buttonColors(containerColor = StatusDestructive),
              shape = RoundedCornerShape(10.dp)
          ) {
            Text("Cancel Reservation", color = Color.White, fontWeight = FontWeight.Bold)
          }
        },
        dismissButton = {
          TextButton(onClick = { bookingToCancel = null }) {
            Text("Keep Booking", color = TextMuted, fontWeight = FontWeight.SemiBold)
          }
        },
        containerColor = CardSurface
    )
  }
}

@Composable
private fun BookingCard(
    booking: BookingDto,
    onCancelClick: () -> Unit
) {
  val isConfirmed = booking.isConfirmed

  Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = CardSurface),
      border = BorderStroke(1.dp, BorderSubtle),
      elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
            text = booking.courtName,
            color = TextDeep,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )

        // Status badge
        Surface(
            color = if (isConfirmed) SoftGreenSurface else Color(0xFFFEF2F2),
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.dp, if (isConfirmed) Color(0xFFBBF7D0) else Color(0xFFFECACA))
        ) {
          Text(
              text = booking.status.uppercase(),
              color = if (isConfirmed) StatusSuccess else StatusDestructive,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))
      Text(
          text = "${booking.sport} • ${booking.venueName}",
          color = TextMuted,
          fontSize = 13.sp
      )

      Spacer(modifier = Modifier.height(12.dp))
      HorizontalDivider(color = BorderSubtle)
      Spacer(modifier = Modifier.height(12.dp))

      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text("Date & Time", color = TextMuted, fontSize = 11.sp)
          Spacer(modifier = Modifier.height(2.dp))
          Text(
              text = "${booking.date} • ${booking.startTime} - ${booking.endTime}",
              color = TextDeep,
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold
          )
        }

        Column(horizontalAlignment = Alignment.End) {
          Text("Paid Amount", color = TextMuted, fontSize = 11.sp)
          Spacer(modifier = Modifier.height(2.dp))
          Text(
              text = "₹${booking.finalAmount.toInt()}",
              color = ForestGreen,
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
            text = "Ref: ${booking.bookingNumber}",
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
      }

      if (isConfirmed) {
        Spacer(modifier = Modifier.height(14.dp))
        OutlinedButton(
            onClick = onCancelClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = CardSurface),
            border = BorderStroke(1.dp, Color(0xFFFECACA))
        ) {
          Text("Cancel Booking", color = StatusDestructive, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
      }
    }
  }
}
