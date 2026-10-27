package com.courtbook.ui.booking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.courtbook.ui.components.ErrorBanner
import com.courtbook.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockPaymentScreen(
    courtId: String,
    slotIds: List<String>,
    viewModel: BookingViewModel,
    onNavigateBack: () -> Unit,
    onPaymentSuccess: (bookingNumber: String) -> Unit,
    onSlotConflict: () -> Unit
) {
  val uiState by viewModel.uiState.collectAsState()
  var selectedMethod by remember { mutableStateOf("UPI") }

  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text("Payment Checkout") },
            navigationIcon = {
              IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DarkTextPrimary)
              }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg, titleContentColor = DarkTextPrimary)
        )
      },
      containerColor = DarkBg
  ) { paddingValues ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Amount Due Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
          Column(
              modifier = Modifier.padding(20.dp),
              horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text("Total Amount to Pay", color = DarkTextSecondary, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "₹${uiState.finalAmount.toInt()}",
                color = CourtGreen,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("Simulated Payment Gateway", color = DarkTextSecondary, fontSize = 11.sp)
          }
        }

        // Error message if transaction aborted or slot taken (409 Conflict)
        if (uiState.errorMessage != null) {
          ErrorBanner(
              errorMessage = uiState.errorMessage!!,
              isConflict = uiState.isConflictError,
              onRetry = if (uiState.isConflictError) onSlotConflict else null
          )
        }

        Text(
            text = "Select Payment Method",
            style = MaterialTheme.typography.titleMedium,
            color = DarkTextPrimary,
            fontWeight = FontWeight.Bold
        )

        PaymentMethodOption(
            title = "UPI (Google Pay, PhonePe, Paytm)",
            icon = Icons.Default.QrCode,
            isSelected = selectedMethod == "UPI",
            onSelect = { selectedMethod = "UPI" }
        )

        PaymentMethodOption(
            title = "Credit or Debit Card",
            icon = Icons.Default.CreditCard,
            isSelected = selectedMethod == "CARD",
            onSelect = { selectedMethod = "CARD" }
        )

        PaymentMethodOption(
            title = "Net Banking",
            icon = Icons.Default.AccountBalance,
            isSelected = selectedMethod == "NET_BANKING",
            onSelect = { selectedMethod = "NET_BANKING" }
        )

        // Security Notice Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
          Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Security, contentDescription = null, tint = CourtGreen, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Backed by MongoDB ACID transactions with snapshot isolation and concurrency safeguards.",
                color = DarkTextSecondary,
                fontSize = 11.sp
            )
          }
        }
      }

      // Complete Payment Button
      Button(
          onClick = {
            viewModel.confirmPayment(
                courtId = courtId,
                slotIds = slotIds,
                paymentMethod = selectedMethod,
                onSuccess = onPaymentSuccess
            )
          },
          enabled = !uiState.isSubmittingBooking,
          modifier = Modifier.fillMaxWidth().height(52.dp),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = CourtGreen)
      ) {
        if (uiState.isSubmittingBooking) {
          CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        } else {
          Text(
              text = "Pay ₹${uiState.finalAmount.toInt()}",
              color = Color.Black,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}

@Composable
private fun PaymentMethodOption(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
  val border = if (isSelected) BorderStroke(1.5.dp, CourtGreen) else BorderStroke(1.dp, DarkBorder)
  val bg = if (isSelected) CourtGreen.copy(alpha = 0.1f) else DarkSurfaceElevated

  Card(
      modifier = Modifier
          .fillMaxWidth()
          .clickable(onClick = onSelect),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = bg),
      border = border
  ) {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(icon, contentDescription = null, tint = if (isSelected) CourtGreen else DarkTextSecondary, modifier = Modifier.size(22.dp))
      Spacer(modifier = Modifier.width(14.dp))
      Text(
          text = title,
          color = DarkTextPrimary,
          fontSize = 14.sp,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
      )
    }
  }
}
