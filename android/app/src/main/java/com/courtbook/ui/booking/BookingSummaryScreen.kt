package com.courtbook.ui.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.courtbook.ui.components.ErrorBanner
import com.courtbook.ui.components.StateLoader
import com.courtbook.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingSummaryScreen(
    courtId: String,
    slotIds: List<String>,
    viewModel: BookingViewModel,
    onNavigateBack: () -> Unit,
    onProceedToPayment: () -> Unit
) {
  LaunchedEffect(courtId, slotIds) {
    viewModel.initBooking(courtId, slotIds)
  }

  val uiState by viewModel.uiState.collectAsState()
  var couponInput by remember { mutableStateOf("") }

  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text("Booking Summary") },
            navigationIcon = {
              IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DarkTextPrimary)
              }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg, titleContentColor = DarkTextPrimary)
        )
      },
      bottomBar = {
        Surface(
            color = DarkSurfaceElevated,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
          Row(
              modifier = Modifier
                  .fillMaxWidth()
                  .padding(20.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text("Total Amount", color = DarkTextSecondary, fontSize = 12.sp)
              Text(
                  text = "₹${uiState.finalAmount.toInt()}",
                  color = CourtGreen,
                  fontSize = 22.sp,
                  fontWeight = FontWeight.Bold
              )
            }

            Button(
                onClick = onProceedToPayment,
                colors = ButtonDefaults.buttonColors(containerColor = CourtGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
              Text("Proceed to Pay", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
          }
        }
      },
      containerColor = DarkBg
  ) { paddingValues ->
    if (uiState.isLoadingData) {
      StateLoader("Preparing booking summary...", modifier = Modifier.padding(paddingValues))
    } else {
      Column(
          modifier = Modifier
              .fillMaxSize()
              .padding(paddingValues)
              .padding(16.dp)
              .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // 1. Court Overview Card
        val court = uiState.court
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = court?.name ?: "Sports Court",
                color = DarkTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${court?.sport ?: "SPORTS"} • ${if (court?.isIndoor == true) "Indoor" else "Outdoor"}",
                color = CourtGreen,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = court?.venueAddress ?: "Bengaluru",
                color = DarkTextSecondary,
                fontSize = 12.sp
            )
          }
        }

        // 2. Selected Slots Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Selected Slots (${slotIds.size})",
                color = DarkTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            slotIds.forEachIndexed { index, slotId ->
              val slot = uiState.slots.find { it.id == slotId }
              Row(
                  modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                  horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                    text = slot?.timeRange ?: "Slot ${index + 1}",
                    color = DarkTextPrimary,
                    fontSize = 14.sp
                )
                Text(
                    text = "₹${(slot?.price ?: court?.basePrice ?: 400.0).toInt()}",
                    color = DarkTextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
              }
            }
          }
        }

        // 3. Coupon Application Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.LocalOffer, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Have a promo code?", color = DarkTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.appliedCoupon != null) {
              // Applied Coupon Badge
              Row(
                  modifier = Modifier
                      .fillMaxWidth()
                      .background(CourtGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                      .padding(horizontal = 12.dp, vertical = 10.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.Check, contentDescription = null, tint = CourtGreen, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                      text = "${uiState.appliedCoupon} applied (Saved ₹${uiState.discountAmount.toInt()})",
                      color = CourtGreen,
                      fontWeight = FontWeight.Bold,
                      fontSize = 13.sp
                  )
                }
                IconButton(onClick = { viewModel.removeCoupon() }, modifier = Modifier.size(24.dp)) {
                  Icon(Icons.Default.Close, contentDescription = "Remove", tint = DarkTextSecondary, modifier = Modifier.size(16.dp))
                }
              }
            } else {
              // Input Field with Apply Button
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically
              ) {
                OutlinedTextField(
                    value = couponInput,
                    onValueChange = { couponInput = it },
                    placeholder = { Text("Enter coupon (e.g. WELCOME50)", fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CourtGreen,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = DarkTextPrimary,
                        unfocusedTextColor = DarkTextPrimary
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { viewModel.applyCoupon(couponInput) },
                    enabled = !uiState.isApplyingCoupon && couponInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = CourtGreen),
                    shape = RoundedCornerShape(10.dp)
                ) {
                  Text("Apply", color = Color.Black, fontWeight = FontWeight.Bold)
                }
              }

              // Suggested coupon quick chip
              Spacer(modifier = Modifier.height(8.dp))
              SuggestionChip(
                  onClick = { couponInput = "WELCOME50"; viewModel.applyCoupon("WELCOME50") },
                  label = { Text("Use WELCOME50 for 50% Off", fontSize = 11.sp, color = AccentAmber) }
              )
            }

            if (uiState.couponError != null) {
              Spacer(modifier = Modifier.height(8.dp))
              Text(text = uiState.couponError!!, color = StatusBooked, fontSize = 12.sp)
            }
          }
        }

        // 4. Price Breakdown Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Payment Breakdown", color = DarkTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            HorizontalDivider(color = DarkBorder)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Court Fee", color = DarkTextSecondary, fontSize = 14.sp)
              Text("₹${uiState.originalAmount.toInt()}", color = DarkTextPrimary, fontSize = 14.sp)
            }

            if (uiState.discountAmount > 0) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Coupon Discount", color = CourtGreen, fontSize = 14.sp)
                Text("-₹${uiState.discountAmount.toInt()}", color = CourtGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
              }
            }

            HorizontalDivider(color = DarkBorder)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Total Amount", color = DarkTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
              Text("₹${uiState.finalAmount.toInt()}", color = CourtGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}
