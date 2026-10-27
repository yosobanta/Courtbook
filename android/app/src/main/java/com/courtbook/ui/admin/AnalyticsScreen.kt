package com.courtbook.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.courtbook.ui.components.StateLoader
import com.courtbook.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AdminViewModel,
    onNavigateBack: () -> Unit
) {
  LaunchedEffect(Unit) {
    viewModel.loadAnalytics()
  }

  val uiState by viewModel.uiState.collectAsState()

  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text("MongoDB Aggregation Analytics") },
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
    if (uiState.isLoading && uiState.revenueAnalytics.isEmpty()) {
      StateLoader("Executing MongoDB aggregation pipelines...", modifier = Modifier.padding(paddingValues))
    } else {
      LazyColumn(
          modifier = Modifier
              .fillMaxSize()
              .padding(paddingValues)
              .padding(horizontal = 16.dp),
          verticalArrangement = Arrangement.spacedBy(18.dp),
          contentPadding = PaddingValues(bottom = 32.dp)
      ) {
        // Section 1: Revenue by Venue
        item {
          Column {
            Text(
                text = "Pipeline 1: Revenue by Venue",
                style = MaterialTheme.typography.titleMedium,
                color = DarkTextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "\$match(CONFIRMED) ➔ \$group(venueId) ➔ \$lookup(venues) ➔ \$project",
                color = CourtGreen,
                fontSize = 11.sp
            )
          }
        }

        if (uiState.revenueAnalytics.isEmpty()) {
          item { Text("No venue revenue data available yet", color = DarkTextSecondary, fontSize = 13.sp) }
        } else {
          items(uiState.revenueAnalytics) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(item.venueName, color = DarkTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                  Text("₹${item.totalRevenue.toInt()}", color = CourtGreen, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text("${item.city} • ${item.totalBookings} booking(s)", color = DarkTextSecondary, fontSize = 12.sp)
                  Text("Avg: ₹${item.averageBookingValue.toInt()}", color = DarkTextSecondary, fontSize = 12.sp)
                }
              }
            }
          }
        }

        // Section 2: Most Popular Sports
        item {
          Column {
            Text(
                text = "Pipeline 2: Most Popular Sports",
                style = MaterialTheme.typography.titleMedium,
                color = DarkTextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "\$match ➔ \$lookup(courts) ➔ \$unwind ➔ \$group(sport) ➔ \$sort",
                color = AccentAmber,
                fontSize = 11.sp
            )
          }
        }

        if (uiState.popularSports.isEmpty()) {
          item { Text("No sports data available yet", color = DarkTextSecondary, fontSize = 13.sp) }
        } else {
          items(uiState.popularSports) { sportItem ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
              Row(
                  modifier = Modifier.padding(16.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text(sportItem.sport, color = DarkTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                  Text("${sportItem.bookingCount} bookings placed", color = DarkTextSecondary, fontSize = 12.sp)
                }
                Text("₹${sportItem.totalRevenue.toInt()}", color = AccentAmber, fontWeight = FontWeight.Bold, fontSize = 16.sp)
              }
            }
          }
        }
      }
    }
  }
}
