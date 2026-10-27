package com.courtbook.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EventNote
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
fun AdminDashboardScreen(
    viewModel: AdminViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToManageSlots: () -> Unit,
    onNavigateToAnalytics: () -> Unit
) {
  val uiState by viewModel.uiState.collectAsState()

  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text("Admin Dashboard") },
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
    if (uiState.isLoading && uiState.summary == null) {
      StateLoader("Loading platform statistics...", modifier = Modifier.padding(paddingValues))
    } else {
      val kpi = uiState.summary?.kpi

      LazyColumn(
          modifier = Modifier
              .fillMaxSize()
              .padding(paddingValues)
              .padding(horizontal = 16.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
          contentPadding = PaddingValues(bottom = 32.dp)
      ) {
        if (uiState.errorMessage != null) {
          item {
            ErrorBanner(
                errorMessage = uiState.errorMessage!!,
                onRetry = { viewModel.loadDashboard() }
            )
          }
        }

        // 1. KPI Grid (2 rows)
        item {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Platform Overview", style = MaterialTheme.typography.titleMedium, color = DarkTextPrimary, fontWeight = FontWeight.Bold)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              StatCard("Total Users", "${kpi?.totalUsers ?: 0}", CourtGreen, modifier = Modifier.weight(1f))
              StatCard("Active Courts", "${kpi?.totalCourts ?: 0}", AccentBlue, modifier = Modifier.weight(1f))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              StatCard("Today's Bookings", "${kpi?.todayBookings ?: 0}", AccentAmber, modifier = Modifier.weight(1f))
              StatCard("Today's Revenue", "₹${kpi?.todayRevenue?.toInt() ?: 0}", CourtGreen, modifier = Modifier.weight(1f))
            }
          }
        }

        // 2. Quick Management Shortcuts
        item {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Admin Controls", style = MaterialTheme.typography.titleMedium, color = DarkTextPrimary, fontWeight = FontWeight.Bold)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToManageSlots),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
              Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EventNote, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                  Text("Slot Availability & Blocking", color = DarkTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                  Text("Block slots for maintenance, private events, or cleaning", color = DarkTextSecondary, fontSize = 12.sp)
                }
              }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToAnalytics),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
              Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.BarChart, contentDescription = null, tint = CourtGreen, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                  Text("MongoDB Aggregation Analytics", color = DarkTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                  Text("View Revenue by Venue and Popular Sports aggregation pipelines", color = DarkTextSecondary, fontSize = 12.sp)
                }
              }
            }
          }
        }

        // 3. Recent Bookings List
        item {
          Text("Recent Activity", style = MaterialTheme.typography.titleMedium, color = DarkTextPrimary, fontWeight = FontWeight.Bold)
        }

        val recent = uiState.summary?.recentBookings ?: emptyList()
        if (recent.isEmpty()) {
          item {
            Text("No recent bookings to display", color = DarkTextSecondary, fontSize = 13.sp)
          }
        } else {
          items(recent) { booking ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
              Row(
                  modifier = Modifier.padding(12.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text(booking.courtName, color = DarkTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                  Text("${booking.date} • ${booking.startTime}-${booking.endTime}", color = DarkTextSecondary, fontSize = 12.sp)
                }
                Text("₹${booking.finalAmount.toInt()}", color = CourtGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun StatCard(title: String, value: String, accentColor: Color, modifier: Modifier = Modifier) {
  Card(
      modifier = modifier,
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Text(title, color = DarkTextSecondary, fontSize = 11.sp)
      Spacer(modifier = Modifier.height(6.dp))
      Text(value, color = accentColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
  }
}
