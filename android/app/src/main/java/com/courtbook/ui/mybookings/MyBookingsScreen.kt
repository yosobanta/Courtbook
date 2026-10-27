package com.courtbook.ui.mybookings

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
  val uiState by viewModel.uiState.collectAsState()
  var bookingToCancel by remember { mutableStateOf<BookingDto?>(null) }

  Column(
      modifier = Modifier
          .fillMaxSize()
          .background(DarkBg)
          .padding(horizontal = 16.dp)
  ) {
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = "My Bookings",
        color = DarkTextPrimary,
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = "Manage your confirmed and past court reservations",
        color = DarkTextSecondary,
        fontSize = 13.sp
    )

    Spacer(modifier = Modifier.height(16.dp))

    if (uiState.errorMessage != null) {
      ErrorBanner(
          errorMessage = uiState.errorMessage!!,
          onRetry = { viewModel.loadMyBookings() },
          modifier = Modifier.padding(bottom = 12.dp)
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
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
        title = { Text("Cancel Booking?") },
        text = {
          Text("Are you sure you want to cancel booking ${bookingToCancel!!.bookingNumber}? This will release the court slot for other players and refund your payment.")
        },
        confirmButton = {
          Button(
              onClick = {
                val bId = bookingToCancel!!.id
                bookingToCancel = null
                viewModel.cancelBooking(bId)
              },
              colors = ButtonDefaults.buttonColors(containerColor = StatusBooked)
          ) {
            Text("Cancel Reservation", color = Color.White)
          }
        },
        dismissButton = {
          TextButton(onClick = { bookingToCancel = null }) {
            Text("Keep Booking", color = DarkTextSecondary)
          }
        },
        containerColor = DarkSurfaceElevated,
        titleContentColor = DarkTextPrimary,
        textContentColor = DarkTextSecondary
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
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
            text = booking.courtName,
            color = DarkTextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )

        // Status badge
        Surface(
            color = if (isConfirmed) StatusAvailable.copy(alpha = 0.2f) else StatusBooked.copy(alpha = 0.2f),
            shape = RoundedCornerShape(6.dp)
        ) {
          Text(
              text = booking.status,
              color = if (isConfirmed) StatusAvailable else StatusBooked,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))
      Text(
          text = "${booking.sport} • ${booking.venueName}",
          color = DarkTextSecondary,
          fontSize = 12.sp
      )

      Spacer(modifier = Modifier.height(10.dp))
      HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))
      Spacer(modifier = Modifier.height(10.dp))

      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column {
          Text("Date & Time", color = DarkTextSecondary, fontSize = 11.sp)
          Text(
              text = "${booking.date} • ${booking.startTime} - ${booking.endTime}",
              color = DarkTextPrimary,
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold
          )
        }

        Column(horizontalAlignment = Alignment.End) {
          Text("Paid Amount", color = DarkTextSecondary, fontSize = 11.sp)
          Text(
              text = "₹${booking.finalAmount.toInt()}",
              color = CourtGreen,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      Text(
          text = "Ref: ${booking.bookingNumber}",
          color = DarkTextSecondary.copy(alpha = 0.7f),
          fontSize = 11.sp
      )

      if (isConfirmed) {
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onCancelClick,
            modifier = Modifier.fillMaxWidth().height(40.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusBooked)
        ) {
          Text("Cancel Booking", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
      }
    }
  }
}
