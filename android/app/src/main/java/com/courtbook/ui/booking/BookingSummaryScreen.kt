package com.courtbook.ui.booking

import androidx.compose.foundation.BorderStroke
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
            title = {
              Text(
                  text = "Booking Summary",
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
      bottomBar = {
        Surface(
            color = CardSurface,
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
          Row(
              modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 20.dp, vertical = 14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text("Total Amount", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
              Text(
                  text = "₹${uiState.finalAmount.toInt()}",
                  color = ForestGreen,
                  fontSize = 22.sp,
                  fontWeight = FontWeight.Bold
              )
            }

            Button(
                onClick = onProceedToPayment,
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
              Text("Proceed to Pay", color = ElectricLime, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
          }
        }
      },
      containerColor = WarmBg
  ) { paddingValues ->
    if (uiState.isLoadingData) {
      StateLoader("Preparing booking summary...", modifier = Modifier.padding(paddingValues))
    } else {
      Column(
          modifier = Modifier
              .fillMaxSize()
              .padding(paddingValues)
              .padding(horizontal = 20.dp, vertical = 16.dp)
              .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // 1. Court Overview Card
        val court = uiState.court
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            border = BorderStroke(1.dp, BorderSubtle),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = court?.name ?: "Sports Court",
                color = TextDeep,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${court?.sport ?: "SPORTS"} • ${if (court?.isIndoor == true) "Indoor Court" else "Outdoor Court"}",
                color = ForestGreen,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = court?.venueAddress ?: "Bengaluru Arena",
                color = TextMuted,
                fontSize = 13.sp
            )
          }
        }

        // 2. Selected Slots Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            border = BorderStroke(1.dp, BorderSubtle),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Selected Slots (${slotIds.size})",
                color = TextDeep,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            slotIds.forEachIndexed { index, slotId ->
              val slot = uiState.slots.find { it.id == slotId }
              Row(
                  modifier = Modifier
                      .fillMaxWidth()
                      .padding(vertical = 4.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                    text = slot?.timeRange ?: "Slot ${index + 1}",
                    color = TextDeep,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "₹${(slot?.price ?: court?.basePrice ?: 400.0).toInt()}",
                    color = ForestGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }

        // 3. Coupon Application Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            border = BorderStroke(1.dp, BorderSubtle),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.LocalOffer, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Have a promo code?", color = TextDeep, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.appliedCoupon != null) {
              // Applied Coupon Badge
              Row(
                  modifier = Modifier
                      .fillMaxWidth()
                      .background(SoftGreenSurface, RoundedCornerShape(10.dp))
                      .padding(horizontal = 14.dp, vertical = 10.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.Check, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                      text = "${uiState.appliedCoupon} applied (Saved ₹${uiState.discountAmount.toInt()})",
                      color = ForestGreen,
                      fontWeight = FontWeight.Bold,
                      fontSize = 13.sp
                  )
                }
                IconButton(onClick = { viewModel.removeCoupon() }, modifier = Modifier.size(24.dp)) {
                  Icon(Icons.Default.Close, contentDescription = "Remove", tint = TextMuted, modifier = Modifier.size(16.dp))
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
                    placeholder = { Text("Enter coupon (e.g. WELCOME50)", fontSize = 13.sp, color = TextMuted) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardSurface,
                        unfocusedContainerColor = CardSurface,
                        focusedBorderColor = ForestGreen,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextDeep,
                        unfocusedTextColor = TextDeep
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { viewModel.applyCoupon(couponInput) },
                    enabled = !uiState.isApplyingCoupon && couponInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                  Text("Apply", color = ElectricLime, fontWeight = FontWeight.Bold)
                }
              }

              // Suggested coupon quick chip
              Spacer(modifier = Modifier.height(10.dp))
              SuggestionChip(
                  onClick = { couponInput = "WELCOME50"; viewModel.applyCoupon("WELCOME50") },
                  label = { Text("Use WELCOME50 for 50% Off", fontSize = 11.sp, color = ForestGreen, fontWeight = FontWeight.SemiBold) },
                  colors = SuggestionChipDefaults.suggestionChipColors(containerColor = SoftGreenSurface),
                  border = BorderStroke(1.dp, BorderSubtle)
              )
            }

            if (uiState.couponError != null) {
              Spacer(modifier = Modifier.height(8.dp))
              Text(text = uiState.couponError!!, color = StatusDestructive, fontSize = 12.sp)
            }
          }
        }

        // 4. Price Breakdown Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            border = BorderStroke(1.dp, BorderSubtle),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
          Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Payment Breakdown", color = TextDeep, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            HorizontalDivider(color = BorderSubtle)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Court Fee", color = TextMuted, fontSize = 14.sp)
              Text("₹${uiState.originalAmount.toInt()}", color = TextDeep, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            if (uiState.discountAmount > 0) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Coupon Discount", color = ForestGreen, fontSize = 14.sp)
                Text("-₹${uiState.discountAmount.toInt()}", color = ForestGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
              }
            }

            HorizontalDivider(color = BorderSubtle)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Total Amount", color = TextDeep, fontSize = 16.sp, fontWeight = FontWeight.Bold)
              Text("₹${uiState.finalAmount.toInt()}", color = ForestGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}
