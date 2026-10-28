package com.courtbook.ui.admin

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.courtbook.data.model.*
import com.courtbook.data.remote.NetworkModule
import com.courtbook.data.remote.TokenManager
import com.courtbook.ui.components.EmptyStateView
import com.courtbook.ui.components.ErrorBanner
import com.courtbook.ui.components.SportCategoryChip
import com.courtbook.ui.components.StateLoader
import com.courtbook.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: AdminViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPlayerView: () -> Unit,
    onNavigateToAIInsights: () -> Unit = {},
    onLogout: () -> Unit
) {
  // Always auto-refresh data whenever entering Admin Dashboard
  LaunchedEffect(Unit) {
    viewModel.loadAllAdminData()
  }

  val uiState by viewModel.uiState.collectAsState()
  val context = LocalContext.current

  var selectedTab by remember { mutableStateOf(0) }
  val tabTitles = listOf("Overview", "Turfs & Spots", "Bookings", "Slots")

  // Dialog states
  var showAddTurfDialog by remember { mutableStateOf(false) }
  var showAddCourtDialog by remember { mutableStateOf(false) }
  var courtToEditRates by remember { mutableStateOf<CourtDto?>(null) }
  var courtToGenerateSlots by remember { mutableStateOf<CourtDto?>(null) }
  var bookingToCancel by remember { mutableStateOf<BookingDto?>(null) }

  // Toast feedback
  LaunchedEffect(uiState.successMessage) {
    uiState.successMessage?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      viewModel.clearMessages()
    }
  }

  Scaffold(
      topBar = {
        TopAppBar(
            title = {
              Column {
                Text("Admin Console", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDeep)
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Surface(
                      color = ElectricLime,
                      shape = RoundedCornerShape(4.dp)
                  ) {
                    Text(
                        text = "LIVE",
                        color = ForestGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                  }
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                      text = "Facility & Operations",
                      fontSize = 11.sp,
                      color = ForestGreen,
                      fontWeight = FontWeight.Medium,
                      maxLines = 1,
                      overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                  )
                }
              }
            },
            actions = {
              // AI Insights Button
              IconButton(onClick = onNavigateToAIInsights) {
                Icon(
                    imageVector = Icons.Default.AutoGraph,
                    contentDescription = "CourtBook AI Insights",
                    tint = ForestGreen
                )
              }
              // Switch to Player View Button
              IconButton(onClick = onNavigateToPlayerView) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "Preview Player App",
                    tint = TextMuted
                )
              }
              // Refresh
              IconButton(onClick = { viewModel.loadAllAdminData() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextDeep)
              }
              // Sign Out
              IconButton(onClick = onLogout) {
                Icon(Icons.Default.ExitToApp, contentDescription = "Sign Out", tint = StatusDestructive)
              }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = CardSurface, titleContentColor = TextDeep)
        )
      },
      bottomBar = {
        Surface(
            color = CardSurface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
          NavigationBar(
              containerColor = CardSurface,
              tonalElevation = 0.dp
          ) {
            tabTitles.forEachIndexed { index, title ->
              val isSelected = selectedTab == index
              NavigationBarItem(
                  selected = isSelected,
                  onClick = { selectedTab = index },
                  icon = {
                    when (index) {
                      0 -> Icon(Icons.Default.Dashboard, contentDescription = title)
                      1 -> Icon(Icons.Default.SportsTennis, contentDescription = title)
                      2 -> Icon(Icons.Default.ConfirmationNumber, contentDescription = title)
                      3 -> Icon(Icons.Default.DateRange, contentDescription = title)
                      4 -> Icon(Icons.Default.Dns, contentDescription = title)
                      else -> Icon(Icons.Default.Settings, contentDescription = title)
                    }
                  },
                  label = {
                    Text(
                        title,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                  },
                  colors = NavigationBarItemDefaults.colors(
                      selectedIconColor = ForestGreen,
                      selectedTextColor = ForestGreen,
                      indicatorColor = ElectricLime,
                      unselectedIconColor = TextMuted,
                      unselectedTextColor = TextMuted
                  )
              )
            }
          }
        }
      },
      containerColor = WarmBg
  ) { paddingValues ->
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
      if (uiState.isLoading && uiState.summary == null && uiState.courts.isEmpty()) {
        StateLoader("Loading Admin Workspace...")
      } else {
        when (selectedTab) {
          0 -> OverviewTab(
              uiState = uiState,
              onSwitchTab = { selectedTab = it },
              onRefresh = { viewModel.loadAllAdminData() },
              onLogout = onLogout,
              onSwitchToPlayerView = onNavigateToPlayerView
          )
          1 -> TurfsAndSpotsTab(
              uiState = uiState,
              onAddTurfClick = { showAddTurfDialog = true },
              onAddSpotClick = { showAddCourtDialog = true },
              onEditRatesClick = { court -> courtToEditRates = court },
              onGenerateSlotsClick = { court -> courtToGenerateSlots = court }
          )
          2 -> BookingsTab(
              uiState = uiState,
              onFilterSelect = { status -> viewModel.setBookingStatusFilter(status) },
              onCancelBookingClick = { booking -> bookingToCancel = booking }
          )
          3 -> SlotsTab(
              uiState = uiState,
              viewModel = viewModel
          )
        }
      }

      // Dialogs
      if (showAddTurfDialog) {
        AddTurfDialog(
            viewModel = viewModel,
            isSubmitting = uiState.isActionInProgress,
            onDismiss = { showAddTurfDialog = false },
            onSubmit = { name, address, city, sport, basePrice, imageUrl ->
              viewModel.createVenue(
                  name = name,
                  address = address,
                  city = city,
                  sport = sport,
                  basePrice = basePrice,
                  images = if (imageUrl.isNotBlank()) listOf(imageUrl) else emptyList()
              ) {
                showAddTurfDialog = false
              }
            }
        )
      }

      if (showAddCourtDialog) {
        AddCourtDialog(
            venues = uiState.venues,
            isSubmitting = uiState.isActionInProgress,
            onDismiss = { showAddCourtDialog = false },
            onSubmit = { venueId, name, sport, isIndoor, surface, capacity, basePrice, weekday, weekend, peak ->
              viewModel.createCourt(venueId, name, sport, isIndoor, surface, capacity, basePrice, weekday, weekend, peak) {
                showAddCourtDialog = false
              }
            }
        )
      }

      courtToEditRates?.let { court ->
        EditRatesDialog(
            court = court,
            isSubmitting = uiState.isActionInProgress,
            onDismiss = { courtToEditRates = null },
            onSubmit = { basePrice, weekday, weekend, peak, isActive ->
              viewModel.updateCourtRates(court.id, basePrice, weekday, weekend, peak, isActive) {
                courtToEditRates = null
              }
            }
        )
      }

      courtToGenerateSlots?.let { court ->
        GenerateSlotsDialog(
            court = court,
            isSubmitting = uiState.isActionInProgress,
            onDismiss = { courtToGenerateSlots = null },
            onSubmit = { startDate, endDate, startHour, endHour, price ->
              viewModel.generateSlots(court.id, startDate, endDate, startHour, endHour, price) {
                courtToGenerateSlots = null
              }
            }
        )
      }

      bookingToCancel?.let { booking ->
        AlertDialog(
            onDismissRequest = { bookingToCancel = null },
            containerColor = DarkSurfaceElevated,
            title = { Text("Cancel Booking #${booking.bookingNumber}?", color = DarkTextPrimary) },
            text = {
              Text(
                  "Are you sure you want to cancel this booking? All associated slots will be released back to AVAILABLE status.",
                  color = DarkTextSecondary
              )
            },
            confirmButton = {
              Button(
                  onClick = {
                    viewModel.cancelBooking(booking.id)
                    bookingToCancel = null
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = StatusBooked)
              ) {
                Text("Cancel Booking", color = Color.White)
              }
            },
            dismissButton = {
              TextButton(onClick = { bookingToCancel = null }) {
                Text("Keep", color = DarkTextSecondary)
              }
            }
        )
      }
    }
  }
}

// ------------------------------------------------------------------------------------------------
// TAB 0: OVERVIEW & ANALYTICS
// ------------------------------------------------------------------------------------------------
@Composable
private fun OverviewTab(
    uiState: AdminUiState,
    onSwitchTab: (Int) -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    onSwitchToPlayerView: () -> Unit
) {
  val kpi = uiState.summary?.kpi

  LazyColumn(
      modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
  ) {
    if (uiState.errorMessage != null) {
      item {
        val isAuthError = uiState.errorMessage.contains("401") ||
            uiState.errorMessage.contains("Authentication token") ||
            uiState.errorMessage.contains("token missing")
        if (isAuthError) {
          Card(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
              border = BorderStroke(1.dp, AccentAmber)
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Text("Session Expired", fontWeight = FontWeight.Bold, color = DarkTextPrimary, fontSize = 16.sp)
              Spacer(modifier = Modifier.height(4.dp))
              Text("Your admin login session has expired. Please sign in again with your admin credentials to load live platform data.", color = DarkTextSecondary, fontSize = 12.sp)
              Spacer(modifier = Modifier.height(12.dp))
              Button(
                  onClick = {
                    TokenManager.clear()
                    onLogout()
                  },
                  modifier = Modifier.fillMaxWidth().height(44.dp),
                  shape = RoundedCornerShape(8.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = CourtGreen)
              ) {
                Text("Sign In as Admin", color = Color.Black, fontWeight = FontWeight.Bold)
              }
            }
          }
        } else {
          ErrorBanner(errorMessage = uiState.errorMessage, onRetry = onRefresh)
        }
      }
    }

    // 1. KPI Cards
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Platform Metrics", style = MaterialTheme.typography.titleMedium, color = DarkTextPrimary, fontWeight = FontWeight.Bold)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          StatCard("Total Users", "${kpi?.totalUsers ?: 0}", CourtGreen, modifier = Modifier.weight(1f))
          StatCard("Active Courts", "${kpi?.totalCourts ?: uiState.courts.size}", AccentBlue, modifier = Modifier.weight(1f))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          StatCard("Today's Bookings", "${kpi?.todayBookings ?: uiState.allBookings.size}", AccentAmber, modifier = Modifier.weight(1f))
          StatCard("Today's Revenue", "₹${kpi?.todayRevenue?.toInt() ?: 0}", CourtGreen, modifier = Modifier.weight(1f))
        }
      }
    }

    // 2. Quick Operations
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Quick Operations", style = MaterialTheme.typography.titleMedium, color = DarkTextPrimary, fontWeight = FontWeight.Bold)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          QuickActionCard(
              title = "Turfs & Rates",
              subtitle = "Add turfs & change rates",
              icon = Icons.Default.SportsTennis,
              accent = AccentAmber,
              modifier = Modifier.weight(1f),
              onClick = { onSwitchTab(1) }
          )
          QuickActionCard(
              title = "All Bookings",
              subtitle = "Inspect platform bookings",
              icon = Icons.Default.ConfirmationNumber,
              accent = CourtGreen,
              modifier = Modifier.weight(1f),
              onClick = { onSwitchTab(2) }
          )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          QuickActionCard(
              title = "Slot Manager",
              subtitle = "Block / unblock slots",
              icon = Icons.Default.EventAvailable,
              accent = AccentBlue,
              modifier = Modifier.weight(1f),
              onClick = { onSwitchTab(3) }
          )
          QuickActionCard(
              title = "Customer View",
              subtitle = "Preview customer app",
              icon = Icons.Default.Visibility,
              accent = ForestGreen,
              modifier = Modifier.weight(1f),
              onClick = onSwitchToPlayerView
          )
        }
      }
    }

    // 3. Live Recent Bookings Feed right on Overview
    item {
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Recent Platform Bookings", style = MaterialTheme.typography.titleMedium, color = DarkTextPrimary, fontWeight = FontWeight.Bold)
        TextButton(onClick = { onSwitchTab(2) }) {
          Text("View All (${uiState.allBookings.size})", color = CourtGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    if (uiState.allBookings.isEmpty()) {
      item {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
          Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
            Text("No bookings placed yet", color = DarkTextSecondary, fontSize = 13.sp)
          }
        }
      }
    } else {
      items(uiState.allBookings.take(5)) { booking ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
          Row(
              modifier = Modifier.padding(14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text("#${booking.bookingNumber}", color = DarkTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    color = if (booking.isConfirmed) CourtGreen.copy(alpha = 0.2f) else StatusBooked.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                  Text(
                      text = booking.status,
                      color = if (booking.isConfirmed) CourtGreen else StatusBooked,
                      fontSize = 9.sp,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.height(3.dp))
              Text("${booking.userName} • ${booking.courtName}", color = DarkTextSecondary, fontSize = 12.sp)
              Text("${booking.date} • ${booking.startTime}-${booking.endTime}", color = DarkTextSecondary.copy(alpha = 0.8f), fontSize = 11.sp)
            }
            Text("₹${booking.finalAmount.toInt()}", color = CourtGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)
          }
        }
      }
    }

    // 4. Revenue by Venue Breakdown
    item {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Revenue by Venue (MongoDB Pipeline)", style = MaterialTheme.typography.titleMedium, color = DarkTextPrimary, fontWeight = FontWeight.Bold)

        if (uiState.revenueAnalytics.isEmpty()) {
          Text("No revenue recorded yet", color = DarkTextSecondary, fontSize = 12.sp)
        } else {
          uiState.revenueAnalytics.forEach { rev ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
              Row(
                  modifier = Modifier
                      .fillMaxWidth()
                      .padding(14.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text(rev.venueName, color = DarkTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                  Text("${rev.totalBookings} total bookings", color = DarkTextSecondary, fontSize = 12.sp)
                }
                Text("₹${rev.totalRevenue.toInt()}", color = CourtGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
              }
            }
          }
        }
      }
    }
  }
}

// ------------------------------------------------------------------------------------------------
// TAB 1: TURFS & SPOTS MANAGEMENT (ADD TURF, ADD SPOT, CHANGE RATES, GENERATE SLOTS)
// ------------------------------------------------------------------------------------------------
@Composable
private fun TurfsAndSpotsTab(
    uiState: AdminUiState,
    onAddTurfClick: () -> Unit,
    onAddSpotClick: () -> Unit,
    onEditRatesClick: (CourtDto) -> Unit,
    onGenerateSlotsClick: (CourtDto) -> Unit
) {
  LazyColumn(
      modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp)
  ) {
    // Action Bar: Add Turf & Add Spot
    item {
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedButton(
            onClick = onAddTurfClick,
            modifier = Modifier.weight(1f).height(46.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.5.dp, ForestGreen),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ForestGreen)
        ) {
          Icon(Icons.Default.AddBusiness, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("+ Add Turf", color = ForestGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        Button(
            onClick = onAddSpotClick,
            modifier = Modifier.weight(1f).height(46.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
        ) {
          Icon(Icons.Default.AddCircle, contentDescription = null, tint = ElectricLime, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("+ Add Spot", color = ElectricLime, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
      }
    }

    // Turfs / Venues Summary
    item {
      Text(
          text = "Registered Turfs (${uiState.venues.size})",
          style = MaterialTheme.typography.titleMedium,
          color = TextDeep,
          fontWeight = FontWeight.Bold
      )
    }

    if (uiState.venues.isEmpty()) {
      item {
        EmptyStateView(title = "No Turfs Registered", description = "Tap '+ Add Turf' above to register your first sports arena.")
      }
    } else {
      items(uiState.venues) { venue ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                  text = venue.name,
                  fontWeight = FontWeight.Bold,
                  color = TextDeep,
                  fontSize = 16.sp,
                  modifier = Modifier.weight(1f),
                  maxLines = 1,
                  overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
              )
              Spacer(modifier = Modifier.width(8.dp))
              Surface(
                  color = SoftGreenSurface,
                  shape = RoundedCornerShape(6.dp)
              ) {
                Text(
                    text = "★ ${venue.rating?.average ?: 4.5}",
                    color = ForestGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
              }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${venue.address} • ${venue.city.uppercase()}",
                color = TextMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(10.dp))
            
            // Amenity Pills (clean labels without underscores, horizontally scrollable)
            if (venue.amenities.isNotEmpty()) {
              Row(
                  modifier = Modifier
                      .fillMaxWidth()
                      .horizontalScroll(rememberScrollState()),
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                venue.amenities.forEach { amenity ->
                  val cleanLabel = amenity.replace("_", " ")
                      .split(" ")
                      .joinToString(" ") { it.lowercase().replaceFirstChar(Char::uppercase) }
                  Surface(
                      color = SoftGreenSurface,
                      shape = RoundedCornerShape(12.dp)
                  ) {
                    Text(
                        text = cleanLabel,
                        color = ForestGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    // Courts / Spots with Rate Controls
    item {
      Spacer(modifier = Modifier.height(8.dp))
      Text(
          text = "Spots & Hourly Rates (${uiState.courts.size})",
          style = MaterialTheme.typography.titleMedium,
          color = TextDeep,
          fontWeight = FontWeight.Bold
      )
    }

    if (uiState.courts.isEmpty()) {
      item {
        EmptyStateView(title = "No Spots Registered", description = "Tap '+ Add Spot' above to create courts and configure their hourly rates.")
      }
    } else {
      items(uiState.courts) { court ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = court.name,
                    fontWeight = FontWeight.Bold,
                    color = TextDeep,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text("${court.venueName} • ${court.sport}", color = TextMuted, fontSize = 12.sp, maxLines = 1)
              }

              Spacer(modifier = Modifier.width(8.dp))

              Surface(
                  color = if (court.isActive) SoftGreenSurface else StatusBooked.copy(alpha = 0.12f),
                  shape = RoundedCornerShape(6.dp)
              ) {
                Text(
                    text = if (court.isActive) "ACTIVE" else "INACTIVE",
                    color = if (court.isActive) ForestGreen else StatusBooked,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pricing Grid (25% balanced columns, centered numbers)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SoftGreenSurface, RoundedCornerShape(10.dp))
                    .padding(horizontal = 6.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Base Rate", color = TextMuted, fontSize = 10.sp)
                Text("₹${court.basePrice.toInt()}/hr", color = TextDeep, fontWeight = FontWeight.Bold, fontSize = 12.sp)
              }
              Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Weekday", color = TextMuted, fontSize = 10.sp)
                Text("₹${court.pricing?.weekday?.toInt() ?: court.basePrice.toInt()}", color = ForestGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
              }
              Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Weekend", color = TextMuted, fontSize = 10.sp)
                Text("₹${court.pricing?.weekend?.toInt() ?: (court.basePrice + 100).toInt()}", color = ForestGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
              }
              Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Peak Hour", color = TextMuted, fontSize = 10.sp)
                Text("₹${court.pricing?.peak?.toInt() ?: (court.basePrice + 150).toInt()}", color = StatusHeld, fontWeight = FontWeight.Bold, fontSize = 12.sp)
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Change Rates & Generate Slots (Single line with custom contentPadding)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedButton(
                  onClick = { onEditRatesClick(court) },
                  modifier = Modifier.weight(1f).height(42.dp),
                  shape = RoundedCornerShape(10.dp),
                  contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                  border = BorderStroke(1.dp, BorderSubtle),
                  colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDeep)
              ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = ForestGreen)
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "Change Rates",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDeep,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
              }

              Button(
                  onClick = { onGenerateSlotsClick(court) },
                  modifier = Modifier.weight(1f).height(42.dp),
                  shape = RoundedCornerShape(10.dp),
                  contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
              ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = ElectricLime, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "Generate Slots",
                    color = Color.White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
              }
            }
          }
        }
      }
    }
  }
}

// ------------------------------------------------------------------------------------------------
// TAB 2: PLATFORM BOOKINGS
// ------------------------------------------------------------------------------------------------
@Composable
private fun BookingsTab(
    uiState: AdminUiState,
    onFilterSelect: (String) -> Unit,
    onCancelBookingClick: (BookingDto) -> Unit
) {
  val filters = listOf("ALL", "CONFIRMED", "CANCELLED")

  Column(
      modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp)
  ) {
    Spacer(modifier = Modifier.height(12.dp))

    // Filter Chips
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      filters.forEach { filter ->
        SportCategoryChip(
            sport = filter,
            isSelected = uiState.bookingStatusFilter == filter,
            onSelect = { onFilterSelect(filter) }
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    Text(
        text = "Total Bookings: ${uiState.allBookings.size}",
        color = DarkTextSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold
    )

    Spacer(modifier = Modifier.height(8.dp))

    if (uiState.allBookings.isEmpty()) {
      EmptyStateView(
          title = "No Bookings Found",
          description = "No customer bookings match '${uiState.bookingStatusFilter}' filter."
      )
    } else {
      LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(10.dp),
          contentPadding = PaddingValues(bottom = 32.dp)
      ) {
        items(uiState.allBookings) { booking ->
          Card(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                    text = "#${booking.bookingNumber}",
                    fontWeight = FontWeight.Bold,
                    color = DarkTextPrimary,
                    fontSize = 15.sp
                )

                Surface(
                    color = if (booking.isConfirmed) CourtGreen.copy(alpha = 0.2f) else StatusBooked.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                  Text(
                      text = booking.status.uppercase(),
                      color = if (booking.isConfirmed) CourtGreen else StatusBooked,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.height(8.dp))

              Text(
                  text = "${booking.courtName} • ${booking.venueName}",
                  color = DarkTextPrimary,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 13.sp
              )
              Text(
                  text = "Date: ${booking.date} • ${booking.startTime} - ${booking.endTime}",
                  color = DarkTextSecondary,
                  fontSize = 12.sp
              )

              Spacer(modifier = Modifier.height(6.dp))

              // Customer Details
              Row(
                  modifier = Modifier
                      .fillMaxWidth()
                      .background(DarkSurface, RoundedCornerShape(8.dp))
                      .padding(8.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text("Customer: ${booking.userName}", color = DarkTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                  if (booking.userPhone.isNotEmpty()) {
                    Text("Phone: ${booking.userPhone}", color = DarkTextSecondary, fontSize = 11.sp)
                  }
                }

                Text("₹${booking.finalAmount.toInt()}", color = CourtGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)
              }

              if (booking.isConfirmed) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { onCancelBookingClick(booking) },
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusBooked)
                ) {
                  Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Cancel & Release Slots", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }
    }
  }
}

// ------------------------------------------------------------------------------------------------
// TAB 3: SLOTS & AVAILABILITY CALENDAR
// ------------------------------------------------------------------------------------------------
@Composable
private fun SlotsTab(
    uiState: AdminUiState,
    viewModel: AdminViewModel
) {
  Column(
      modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp)
  ) {
    Spacer(modifier = Modifier.height(12.dp))

    // Court Selector Strip
    Text("Select Court / Spot", color = DarkTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(6.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      uiState.courts.forEach { court ->
        val isSelected = uiState.selectedCourtId == court.id
        SportCategoryChip(
            sport = "${court.name} (${court.sport})",
            isSelected = isSelected,
            onSelect = { viewModel.selectCourt(court.id) }
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
          text = "Date: ${uiState.selectedDate}",
          style = MaterialTheme.typography.titleMedium,
          color = DarkTextPrimary,
          fontWeight = FontWeight.Bold
      )

      TextButton(onClick = { viewModel.loadCourtSlotsForAdmin() }) {
        Icon(Icons.Default.Refresh, contentDescription = null, tint = CourtGreen, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Refresh Slots", color = CourtGreen, fontSize = 12.sp)
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    if (uiState.slots.isEmpty()) {
      EmptyStateView(
          title = "No Slots for this Date",
          description = "Use 'Turfs & Spots -> Generate Slots' to populate slots for this court."
      )
    } else {
      LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          contentPadding = PaddingValues(bottom = 32.dp)
      ) {
        items(uiState.slots) { slot ->
          Card(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(10.dp),
              colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
          ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(slot.timeRange, color = DarkTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    text = "Status: ${slot.status} ${if (slot.isBlocked) "(${slot.blockReason ?: "Maintenance"})" else ""}",
                    color = when {
                      slot.isAvailable -> StatusAvailable
                      slot.isBlocked -> StatusBlocked
                      slot.isBooked -> StatusBooked
                      else -> StatusHeld
                    },
                    fontSize = 12.sp
                )
              }

              if (slot.isAvailable) {
                Button(
                    onClick = { viewModel.blockSlot(slot.id, "Maintenance") },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusBlocked),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                  Text("Block", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
              } else if (slot.isBlocked) {
                Button(
                    onClick = { viewModel.unblockSlot(slot.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = CourtGreen),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                  Text("Unblock", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
              } else if (slot.isBooked) {
                Text("Booked by Player", color = DarkTextSecondary, fontSize = 11.sp)
              }
            }
          }
        }
      }
    }
  }
}



// ------------------------------------------------------------------------------------------------
// DIALOGS: ADD TURF, ADD SPOT, EDIT RATES, GENERATE SLOTS
// ------------------------------------------------------------------------------------------------

@Composable
private fun AddTurfDialog(
    viewModel: AdminViewModel,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (name: String, address: String, city: String, sport: String, basePrice: Double, imageUrl: String) -> Unit
) {
  val context = LocalContext.current
  var name by remember { mutableStateOf("") }
  var address by remember { mutableStateOf("") }
  var city by remember { mutableStateOf("Bangalore") }
  var selectedSport by remember { mutableStateOf("FOOTBALL") }
  val sportsList = listOf("FOOTBALL", "BADMINTON", "CRICKET", "TENNIS", "BASKETBALL", "TABLE_TENNIS")
  var basePriceStr by remember { mutableStateOf("600") }
  var photoUrl by remember { mutableStateOf("") }
  var isUploadingPhoto by remember { mutableStateOf(false) }
  var isResolvingUrl by remember { mutableStateOf(false) }

  val photoPickerLauncher = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    if (uri != null) {
      isUploadingPhoto = true
      viewModel.uploadImageFromUri(context, uri) { uploadedUrl ->
        isUploadingPhoto = false
        if (!uploadedUrl.isNullOrBlank()) {
          photoUrl = uploadedUrl
        }
      }
    }
  }

  Dialog(onDismissRequest = onDismiss) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 660.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
      Column(
          modifier = Modifier
              .padding(20.dp)
              .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text("Add New Turf / Arena", fontWeight = FontWeight.Bold, color = TextDeep, fontSize = 18.sp)
        Text("Will automatically set up the venue, main playing spot, and generate 7-day bookable slots.", color = ForestGreen, fontSize = 11.sp)

        // Photo Upload & URL Section
        Text("Turf Cover Photo:", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

        // Option 1: Choose from Gallery Button
        Button(
            onClick = { photoPickerLauncher.launch("image/*") },
            enabled = !isUploadingPhoto,
            colors = ButtonDefaults.buttonColors(containerColor = SoftGreenSurface),
            border = BorderStroke(1.dp, BorderSubtle),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        ) {
          if (isUploadingPhoto) {
            CircularProgressIndicator(color = ForestGreen, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Uploading Photo...", color = ForestGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
          } else {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Choose from Gallery / Photos", color = ForestGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
          }
        }

        // Option 2: Enter URL or Google Photos share link (ALWAYS VISIBLE)
        OutlinedTextField(
            value = photoUrl,
            onValueChange = { input ->
              photoUrl = input
              if (input.contains("photos.app.goo.gl") || input.contains("photos.google.com") || input.contains("drive.google.com")) {
                isResolvingUrl = true
                viewModel.resolveImageUrl(input) { resolved ->
                  isResolvingUrl = false
                  if (resolved.isNotBlank()) {
                    photoUrl = resolved
                  }
                }
              }
            },
            label = { Text(if (isResolvingUrl) "Resolving Google Photos Link..." else "Photo URL or Google Photos Link") },
            placeholder = { Text("https://...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
              if (isResolvingUrl) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = ForestGreen, strokeWidth = 2.dp)
              } else if (photoUrl.isNotBlank()) {
                IconButton(onClick = { photoUrl = "" }) {
                  Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(18.dp))
                }
              }
            }
        )

        // Preset photo chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          val presets = listOf(
              "Football" to "https://images.unsplash.com/photo-1575361204480-aadea25e6e68?auto=format&fit=crop&w=800&q=80",
              "Badminton" to "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?auto=format&fit=crop&w=800&q=80",
              "Cricket" to "https://images.unsplash.com/photo-1531415074968-036ba1b575da?auto=format&fit=crop&w=800&q=80",
              "Tennis" to "https://images.unsplash.com/photo-1595435934249-5df7ed86e1c0?auto=format&fit=crop&w=800&q=80"
          )
          presets.forEach { (label, url) ->
            Surface(
                modifier = Modifier.clickable { photoUrl = url },
                shape = RoundedCornerShape(12.dp),
                color = SoftGreenSurface,
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
              Text(
                  text = "+ $label Preset",
                  color = ForestGreen,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Medium,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
        }

        // Live Photo Preview Card
        if (photoUrl.isNotBlank()) {
          Box(
              modifier = Modifier
                  .fillMaxWidth()
                  .height(120.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .border(1.5.dp, ForestGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
          ) {
            AsyncImage(
                model = photoUrl,
                contentDescription = "Turf Photo Preview",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(bottomStart = 8.dp),
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
              Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = ElectricLime, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Photo Ready", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Turf Name (e.g. Royal Turf Arena)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Address / Location") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = city,
            onValueChange = { city = it },
            label = { Text("City") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Text("Primary Sport:", color = TextMuted, fontSize = 12.sp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          sportsList.forEach { sport ->
            SportCategoryChip(
                sport = sport,
                isSelected = selectedSport == sport,
                onSelect = { selectedSport = sport }
            )
          }
        }

        OutlinedTextField(
            value = basePriceStr,
            onValueChange = { basePriceStr = it },
            label = { Text("Hourly Rate (₹/hour)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
          TextButton(onClick = onDismiss) {
            Text("Cancel", color = TextMuted)
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
              onClick = {
                if (name.isNotBlank() && address.isNotBlank() && city.isNotBlank()) {
                  val price = basePriceStr.toDoubleOrNull() ?: 600.0
                  onSubmit(name.trim(), address.trim(), city.trim(), selectedSport, price, photoUrl.trim())
                }
              },
              enabled = !isSubmitting && name.isNotBlank(),
              colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
              shape = RoundedCornerShape(10.dp)
          ) {
            Text(if (isSubmitting) "Creating..." else "Save Turf", color = ElectricLime, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
private fun AddCourtDialog(
    venues: List<VenueDto>,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (
        venueId: String,
        name: String,
        sport: String,
        isIndoor: Boolean,
        surface: String,
        capacity: Int,
        basePrice: Double,
        weekday: Double,
        weekend: Double,
        peak: Double
    ) -> Unit
) {
  var selectedVenueId by remember { mutableStateOf(venues.firstOrNull()?.id ?: "") }
  var name by remember { mutableStateOf("") }
  var selectedSport by remember { mutableStateOf("BADMINTON") }
  val sportsList = listOf("BADMINTON", "TENNIS", "FOOTBALL", "CRICKET", "BASKETBALL", "TABLE_TENNIS")
  var isIndoor by remember { mutableStateOf(true) }
  var basePriceStr by remember { mutableStateOf("450") }

  Dialog(onDismissRequest = onDismiss) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 560.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
      Column(
          modifier = Modifier
              .padding(20.dp)
              .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text("Add Spot / Court", fontWeight = FontWeight.Bold, color = DarkTextPrimary, fontSize = 18.sp)

        Text("Select Turf:", color = DarkTextSecondary, fontSize = 12.sp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          venues.forEach { venue ->
            SportCategoryChip(
                sport = venue.name,
                isSelected = selectedVenueId == venue.id,
                onSelect = { selectedVenueId = venue.id }
            )
          }
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Spot Name (e.g. Court 3)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Text("Sport Category:", color = DarkTextSecondary, fontSize = 12.sp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          sportsList.forEach { sport ->
            SportCategoryChip(
                sport = sport,
                isSelected = selectedSport == sport,
                onSelect = { selectedSport = sport }
            )
          }
        }

        OutlinedTextField(
            value = basePriceStr,
            onValueChange = { basePriceStr = it },
            label = { Text("Base Price (₹/hour)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Indoor Facility?", color = DarkTextPrimary, fontSize = 14.sp)
          Switch(
              checked = isIndoor,
              onCheckedChange = { isIndoor = it },
              colors = SwitchDefaults.colors(checkedThumbColor = CourtGreen, checkedTrackColor = CourtGreen.copy(alpha = 0.3f))
          )
        }

        val basePrice = basePriceStr.toDoubleOrNull() ?: 450.0
        Text(
            "Auto-Calculated Rates:\n• Weekday: ₹${basePrice.toInt()} • Weekend: ₹${(basePrice + 100).toInt()} • Peak: ₹${(basePrice + 150).toInt()}",
            color = CourtGreen,
            fontSize = 11.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
          TextButton(onClick = onDismiss) { Text("Cancel", color = DarkTextSecondary) }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
              onClick = {
                if (selectedVenueId.isNotBlank() && name.isNotBlank()) {
                  val base = basePriceStr.toDoubleOrNull() ?: 450.0
                  onSubmit(selectedVenueId, name.trim(), selectedSport, isIndoor, "SYNTHETIC", 4, base, base, base + 100, base + 150)
                }
              },
              enabled = !isSubmitting && name.isNotBlank() && selectedVenueId.isNotBlank(),
              colors = ButtonDefaults.buttonColors(containerColor = CourtGreen)
          ) {
            Text(if (isSubmitting) "Creating..." else "Create Spot", color = Color.Black, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
private fun EditRatesDialog(
    court: CourtDto,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (basePrice: Double, weekday: Double, weekend: Double, peak: Double, isActive: Boolean) -> Unit
) {
  var basePriceStr by remember { mutableStateOf("${court.basePrice.toInt()}") }
  var weekdayStr by remember { mutableStateOf("${court.pricing?.weekday?.toInt() ?: court.basePrice.toInt()}") }
  var weekendStr by remember { mutableStateOf("${court.pricing?.weekend?.toInt() ?: (court.basePrice + 100).toInt()}") }
  var peakStr by remember { mutableStateOf("${court.pricing?.peak?.toInt() ?: (court.basePrice + 150).toInt()}") }
  var isActive by remember { mutableStateOf(court.isActive) }

  Dialog(onDismissRequest = onDismiss) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
      Column(
          modifier = Modifier.padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text("Change Rates: ${court.name}", fontWeight = FontWeight.Bold, color = DarkTextPrimary, fontSize = 17.sp)
        Text("${court.venueName} • ${court.sport}", color = DarkTextSecondary, fontSize = 12.sp)

        OutlinedTextField(
            value = basePriceStr,
            onValueChange = {
              basePriceStr = it
              val bp = it.toDoubleOrNull()
              if (bp != null) {
                weekdayStr = "${bp.toInt()}"
                weekendStr = "${(bp + 100).toInt()}"
                peakStr = "${(bp + 150).toInt()}"
              }
            },
            label = { Text("Base Hourly Price (₹)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
              value = weekdayStr,
              onValueChange = { weekdayStr = it },
              label = { Text("Weekday (₹)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.weight(1f),
              singleLine = true
          )

          OutlinedTextField(
              value = weekendStr,
              onValueChange = { weekendStr = it },
              label = { Text("Weekend (₹)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.weight(1f),
              singleLine = true
          )
        }

        OutlinedTextField(
            value = peakStr,
            onValueChange = { peakStr = it },
            label = { Text("Peak Hours Rate (₹)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Court Active for Bookings", color = DarkTextPrimary, fontSize = 14.sp)
          Switch(
              checked = isActive,
              onCheckedChange = { isActive = it },
              colors = SwitchDefaults.colors(checkedThumbColor = CourtGreen, checkedTrackColor = CourtGreen.copy(alpha = 0.3f))
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
          TextButton(onClick = onDismiss) { Text("Cancel", color = DarkTextSecondary) }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
              onClick = {
                val bp = basePriceStr.toDoubleOrNull() ?: court.basePrice
                val wd = weekdayStr.toDoubleOrNull() ?: bp
                val we = weekendStr.toDoubleOrNull() ?: (bp + 100)
                val pk = peakStr.toDoubleOrNull() ?: (bp + 150)
                onSubmit(bp, wd, we, pk, isActive)
              },
              enabled = !isSubmitting,
              colors = ButtonDefaults.buttonColors(containerColor = AccentAmber)
          ) {
            Text(if (isSubmitting) "Updating..." else "Save Rates", color = Color.Black, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
private fun GenerateSlotsDialog(
    court: CourtDto,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (startDate: String, endDate: String, startHour: Int, endHour: Int, price: Double?) -> Unit
) {
  val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
  val cal = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, 7) }
  val nextWeek = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(cal.time)

  var startDate by remember { mutableStateOf(today) }
  var endDate by remember { mutableStateOf(nextWeek) }

  Dialog(onDismissRequest = onDismiss) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
      Column(
          modifier = Modifier.padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text("Generate Bookable Slots", fontWeight = FontWeight.Bold, color = DarkTextPrimary, fontSize = 18.sp)
        Text("For: ${court.name} (${court.sport})", color = CourtGreen, fontSize = 13.sp)

        OutlinedTextField(
            value = startDate,
            onValueChange = { startDate = it },
            label = { Text("Start Date (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = endDate,
            onValueChange = { endDate = it },
            label = { Text("End Date (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Text("Slots will be generated 06:00 to 23:00 (1 hour intervals) using the court's hourly pricing schedule.", color = DarkTextSecondary, fontSize = 11.sp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
          TextButton(onClick = onDismiss) { Text("Cancel", color = DarkTextSecondary) }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
              onClick = {
                if (startDate.isNotBlank() && endDate.isNotBlank()) {
                  onSubmit(startDate.trim(), endDate.trim(), 6, 23, null)
                }
              },
              enabled = !isSubmitting,
              colors = ButtonDefaults.buttonColors(containerColor = CourtGreen)
          ) {
            Text(if (isSubmitting) "Generating..." else "Generate Slots", color = Color.Black, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

// ------------------------------------------------------------------------------------------------
// HELPER COMPONENTS
// ------------------------------------------------------------------------------------------------
@Composable
private fun StatCard(title: String, value: String, accentColor: Color, modifier: Modifier = Modifier) {
  Card(
      modifier = modifier,
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = CardSurface),
      border = BorderStroke(1.dp, BorderSubtle),
      elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(title, color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
      Spacer(modifier = Modifier.height(6.dp))
      Text(value, color = accentColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
  }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
  Card(
      modifier = modifier
          .clip(RoundedCornerShape(14.dp))
          .clickable(onClick = onClick),
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = CardSurface),
      border = BorderStroke(1.dp, BorderSubtle),
      elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Box(
          modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(accent.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center
      ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
      }
      Spacer(modifier = Modifier.height(12.dp))
      Text(title, color = TextDeep, fontWeight = FontWeight.Bold, fontSize = 14.sp)
      Spacer(modifier = Modifier.height(3.dp))
      Text(subtitle, color = TextMuted, fontSize = 11.sp, lineHeight = 15.sp)
    }
  }
}
