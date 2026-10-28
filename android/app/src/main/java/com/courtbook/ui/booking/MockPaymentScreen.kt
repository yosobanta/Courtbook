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
            title = {
              Text(
                  text = "Payment Checkout",
                  fontWeight = FontWeight.Bold,
                  fontSize = 18.sp,
                  color = TextDeep
              )
            },
            navigationIcon = {
              IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextDeep)
              }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = CardSurface,
                titleContentColor = TextDeep
            )
        )
      },
      containerColor = WarmBg
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
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            border = BorderStroke(1.dp, BorderSubtle),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
          Column(
              modifier = Modifier.padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text("Total Amount to Pay", color = TextMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "₹${uiState.finalAmount.toInt()}",
                color = ForestGreen,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = SoftGreenSurface,
                shape = RoundedCornerShape(6.dp)
            ) {
              Text(
                  text = "Simulated Payment Gateway",
                  color = ForestGreen,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
              )
            }
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
            color = TextDeep,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp
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
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SoftGreenSurface),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
          Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Security, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Backed by MongoDB ACID transactions with snapshot isolation and concurrency safeguards.",
                color = ForestGreen,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
          }
        }
      }

      Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
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
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ForestGreen,
                disabledContainerColor = ForestGreen.copy(alpha = 0.6f)
            )
        ) {
          if (uiState.isSubmittingBooking) {
            CircularProgressIndicator(color = ElectricLime, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
          } else {
            Text(
                text = "Pay ₹${uiState.finalAmount.toInt()}",
                color = ElectricLime,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
          }
        }

        TextButton(
            onClick = {
              viewModel.cancelAndReleaseHold(slotIds) {
                onNavigateBack()
              }
            },
            enabled = !uiState.isSubmittingBooking
        ) {
          Text(
              text = "Cancel & Release Slot",
              color = TextMuted,
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold
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
  val border = if (isSelected) BorderStroke(2.dp, ForestGreen) else BorderStroke(1.dp, BorderSubtle)
  val bg = if (isSelected) SoftGreenSurface else CardSurface

  Card(
      modifier = Modifier
          .fillMaxWidth()
          .clickable(onClick = onSelect),
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = bg),
      border = border,
      elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.dp)
  ) {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (isSelected) ForestGreen else TextMuted,
          modifier = Modifier.size(22.dp)
      )
      Spacer(modifier = Modifier.width(14.dp))
      Text(
          text = title,
          color = TextDeep,
          fontSize = 14.sp,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
      )
    }
  }
}
