package com.courtbook.ui.admin

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
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
            title = {
              Text(
                  text = "MongoDB Aggregation Analytics",
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
    if (uiState.isLoading && uiState.revenueAnalytics.isEmpty()) {
      StateLoader("Executing MongoDB aggregation pipelines...", modifier = Modifier.padding(paddingValues))
    } else {
      LazyColumn(
          modifier = Modifier
              .fillMaxSize()
              .padding(paddingValues)
              .padding(horizontal = 20.dp),
          verticalArrangement = Arrangement.spacedBy(18.dp),
          contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
      ) {
        // Section 1: Revenue by Venue
        item {
          Column {
            Text(
                text = "Pipeline 1: Revenue by Venue",
                color = TextDeep,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = SoftGreenSurface,
                shape = RoundedCornerShape(6.dp)
            ) {
              Text(
                  text = "\$match(CONFIRMED) ➔ \$group(venueId) ➔ \$lookup(venues) ➔ \$project",
                  color = ForestGreen,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Medium,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
              )
            }
          }
        }

        if (uiState.revenueAnalytics.isEmpty()) {
          item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
              Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                Text("No venue revenue data available yet", color = TextMuted, fontSize = 13.sp)
              }
            }
          }
        } else {
          items(uiState.revenueAnalytics) { item ->
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
                  Text(item.venueName, color = TextDeep, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                  Text("₹${item.totalRevenue.toInt()}", color = ForestGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text("${item.city} • ${item.totalBookings} booking(s)", color = TextMuted, fontSize = 12.sp)
                  Text("Avg: ₹${item.averageBookingValue.toInt()}", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
                color = TextDeep,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = SoftGreenSurface,
                shape = RoundedCornerShape(6.dp)
            ) {
              Text(
                  text = "\$match ➔ \$lookup(courts) ➔ \$unwind ➔ \$group(sport) ➔ \$sort",
                  color = ForestGreen,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Medium,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
              )
            }
          }
        }

        if (uiState.popularSports.isEmpty()) {
          item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
              Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                Text("No sports data available yet", color = TextMuted, fontSize = 13.sp)
              }
            }
          }
        } else {
          items(uiState.popularSports) { sportItem ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, BorderSubtle),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
              Row(
                  modifier = Modifier.padding(18.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text(sportItem.sport, color = TextDeep, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                  Spacer(modifier = Modifier.height(2.dp))
                  Text("${sportItem.bookingCount} bookings placed", color = TextMuted, fontSize = 12.sp)
                }
                Text("₹${sportItem.totalRevenue.toInt()}", color = ForestGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
              }
            }
          }
        }
      }
    }
  }
}
