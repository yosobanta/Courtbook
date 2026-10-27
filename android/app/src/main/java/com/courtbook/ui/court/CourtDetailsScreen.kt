package com.courtbook.ui.court

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.courtbook.ui.components.*
import com.courtbook.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourtDetailsScreen(
    courtId: String,
    viewModel: CourtViewModel,
    onNavigateBack: () -> Unit,
    onProceedToSummary: (courtId: String, slotIds: List<String>) -> Unit
) {
  LaunchedEffect(courtId) {
    viewModel.initCourt(courtId)
  }

  val uiState by viewModel.uiState.collectAsState()

  // Generate list of 7 consecutive dates starting from today
  val dateList = remember {
    val cal = Calendar.getInstance()
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val displayFormat = SimpleDateFormat("EEE\ndd MMM", Locale.getDefault())
    (0..6).map {
      val dateStr = sdf.format(cal.time)
      val label = if (it == 0) "Today\n${SimpleDateFormat("dd MMM", Locale.getDefault()).format(cal.time)}" else displayFormat.format(cal.time)
      cal.add(Calendar.DAY_OF_YEAR, 1)
      Pair(dateStr, label)
    }
  }

  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text(uiState.court?.name ?: "Court Details", maxLines = 1) },
            navigationIcon = {
              IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DarkTextPrimary)
              }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = DarkBg,
                titleContentColor = DarkTextPrimary
            )
        )
      },
      bottomBar = {
        // Sticky Bottom Booking Bar
        if (uiState.selectedSlots.isNotEmpty()) {
          Surface(
              color = DarkSurfaceElevated,
              tonalElevation = 8.dp,
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
                Text(
                    text = "${uiState.selectedSlots.size} slot(s) selected",
                    color = DarkTextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    text = "₹${uiState.totalPrice.toInt()}",
                    color = CourtGreen,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
              }

              Button(
                  onClick = { viewModel.proceedToHold(onProceedToSummary) },
                  enabled = uiState.canProceed,
                  colors = ButtonDefaults.buttonColors(containerColor = CourtGreen),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.height(48.dp)
              ) {
                if (uiState.isHoldingSlots) {
                  CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                  Text("Proceed to Book", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
              }
            }
          }
        }
      },
      containerColor = DarkBg
  ) { paddingValues ->
    if (uiState.isLoadingCourt) {
      StateLoader("Loading court details...", modifier = Modifier.padding(paddingValues))
    } else if (uiState.court == null) {
      ErrorBanner(
          errorMessage = uiState.errorMessage ?: "Court details unavailable",
          onRetry = { viewModel.initCourt(courtId) },
          modifier = Modifier.padding(paddingValues).padding(16.dp)
      )
    } else {
      val court = uiState.court!!

      LazyColumn(
          modifier = Modifier
              .fillMaxSize()
              .padding(paddingValues)
              .padding(horizontal = 16.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
          contentPadding = PaddingValues(bottom = 24.dp)
      ) {
        // 1. Hero Image Banner
        item {
          Box(
              modifier = Modifier
                  .fillMaxWidth()
                  .height(180.dp)
                  .clip(RoundedCornerShape(16.dp))
          ) {
            val image = court.images.firstOrNull() ?: "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?auto=format&fit=crop&w=800&q=80"
            AsyncImage(
                model = image,
                contentDescription = court.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.TopStart)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CourtGreen)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text(court.sport, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
          }
        }

        // 2. Court & Venue Meta
        item {
          Column {
            Text(
                text = court.name,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = DarkTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.LocationOn, contentDescription = null, tint = DarkTextSecondary, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(court.venueAddress.ifEmpty { "Bengaluru Arena" }, color = DarkTextSecondary, fontSize = 13.sp)
              Spacer(modifier = Modifier.width(12.dp))
              Icon(Icons.Default.Star, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(2.dp))
              Text("4.7 (56 reviews)", color = DarkTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Surface & Capacity Badges
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              AssistChip(
                  onClick = {},
                  label = { Text(court.surfaceType.replace("_", " ")) },
                  colors = AssistChipDefaults.assistChipColors(labelColor = DarkTextSecondary)
              )
              AssistChip(
                  onClick = {},
                  label = { Text("Capacity: ${court.capacity} players") },
                  colors = AssistChipDefaults.assistChipColors(labelColor = DarkTextSecondary)
              )
            }
          }
        }

        // 3. Error Banner (e.g. 409 Conflict)
        if (uiState.errorMessage != null) {
          item {
            ErrorBanner(
                errorMessage = uiState.errorMessage!!,
                isConflict = uiState.isConflictError,
                onRetry = { viewModel.loadSlotsForCurrentDate(court.id) }
            )
          }
        }

        // 4. Date Selector Strip
        item {
          Column {
            Text(
                text = "Select Date",
                style = MaterialTheme.typography.titleMedium,
                color = DarkTextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              dateList.forEach { (dateStr, label) ->
                val isSelected = uiState.selectedDate == dateStr
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) CourtGreen else DarkSurfaceElevated)
                        .clickable { viewModel.selectDate(dateStr) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                  Text(
                      text = label,
                      color = if (isSelected) Color.Black else DarkTextPrimary,
                      fontSize = 12.sp,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                      textAlign = androidx.compose.ui.text.style.TextAlign.Center
                  )
                }
              }
            }
          }
        }

        // 5. Slot Status Legend
        item {
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            LegendItem(color = StatusAvailable, label = "Available")
            LegendItem(color = StatusHeld, label = "Held (Pending)")
            LegendItem(color = StatusBooked, label = "Booked")
          }
        }

        // 6. Slots List
        item {
          Text(
              text = "Available Time Slots",
              style = MaterialTheme.typography.titleMedium,
              color = DarkTextPrimary,
              fontWeight = FontWeight.Bold
          )
        }

        when {
          uiState.isLoadingSlots -> {
            item { StateLoader("Loading slots for ${uiState.selectedDate}...") }
          }
          uiState.slots.isEmpty() -> {
            item {
              EmptyStateView(
                  title = "No slots available",
                  description = "No slots have been generated for this date or all slots are blocked."
              )
            }
          }
          else -> {
            items(uiState.slots) { slot ->
              val isSelected = uiState.selectedSlots.any { it.id == slot.id }
              SlotChip(
                  slot = slot,
                  isSelected = isSelected,
                  onSelectToggle = { viewModel.toggleSlotSelection(it) }
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun LegendItem(color: Color, label: String) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(color)
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(text = label, color = DarkTextSecondary, fontSize = 11.sp)
  }
}
