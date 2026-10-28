package com.courtbook.analytics.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.courtbook.analytics.model.AnalyticsUiState
import com.courtbook.analytics.model.CurrencyFormatter
import com.courtbook.analytics.model.ForecastResult
import com.courtbook.analytics.model.SlotRecommendation
import com.courtbook.analytics.model.TrendDirection
import com.courtbook.ui.components.StateLoader
import com.courtbook.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIInsightsScreen(
    viewModel: AIInsightsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val forecast = uiState.forecast

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "AI Insights",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextDeep,
                            maxLines = 1
                        )
                        Surface(
                            color = ElectricLime,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (uiState.isAdmin) "ADMIN AI" else "PLAYER AI",
                                color = ForestGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ForestGreen
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = ForestGreen
                        )
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
        if (uiState.isLoading || forecast == null) {
            StateLoader(
                message = "Synthesizing booking patterns & forecasting...",
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
            ) {
                // Screen subtitle
                item {
                    Text(
                        text = if (uiState.isAdmin)
                            "Demand forecasting, occupancy analysis & predictive revenue"
                        else
                            "Smart court availability, peak hour alerts & quieter times",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }

                // Filter Controls (Sport & Forecast Horizon)
                item {
                    FilterControlsSection(
                        availableSports = uiState.availableSports,
                        selectedSport = uiState.selectedSport,
                        onSportSelect = { viewModel.selectSport(it) },
                        selectedHorizon = uiState.forecastHorizon,
                        onHorizonSelect = { viewModel.setForecastHorizon(it) }
                    )
                }

                // ── Distinct Views for Admin vs Player ───────────────────
                if (uiState.isAdmin) {
                    // Admin View
                    item {
                        AdminOverviewKPIs(
                            forecast = forecast,
                            horizonDays = uiState.forecastHorizon,
                            selectedSport = uiState.selectedSport
                        )
                    }

                    item {
                        DataSourceTransparencyCard(
                            sourceLabel = forecast.dataSourceLabel,
                            syntheticWeight = forecast.syntheticWeight,
                            realWeight = forecast.realWeight,
                            realBookingCount = uiState.realBookingCount,
                            simulatedCount = uiState.simulatedBookingsCount
                        )
                    }

                    item {
                        AnalyticsCard(
                            title = "Demand Forecast Trajectory",
                            subtitle = "7-day historical baseline vs ${uiState.forecastHorizon}-day AI projection"
                        ) {
                            DemandLineChart(
                                historicalData = forecast.historicalData,
                                predictions = forecast.predictions,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }

                    item {
                        AnalyticsCard(
                            title = "Smart Slot Recommendations",
                            subtitle = "Optimal time slots scoring high on availability and value"
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                forecast.recommendations.forEach { rec ->
                                    SlotRecommendationCard(recommendation = rec)
                                }
                            }
                        }
                    }

                    item {
                        AnalyticsCard(
                            title = "Hourly Utilization vs Capacity",
                            subtitle = "Average projected occupancy across available slots"
                        ) {
                            SlotUtilizationBarChart(
                                predictions = forecast.predictions,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }

                    item {
                        AnalyticsCard(
                            title = "Demand Intensity Heatmap",
                            subtitle = "Cross-sport occupancy distribution by time slot"
                        ) {
                            DemandHeatmapGrid(
                                predictions = forecast.predictions,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }

                    item {
                        AnalyticsCard(
                            title = "Projected Daily Revenue",
                            subtitle = "Estimated revenue over the ${uiState.forecastHorizon}-day horizon: ${CurrencyFormatter.formatInr(forecast.revenueProjection.totalProjectedRevenue)}"
                        ) {
                            val dailyRev = forecast.revenueProjection.dailyProjections.map {
                                "Day +${it.dayOffset}" to it.revenue
                            }
                            RevenueBarChart(
                                dailyRevenue = dailyRev,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Projected Revenue by Sport",
                                color = TextDeep,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                forecast.revenueProjection.bySport.forEach { (sport, amount) ->
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        color = SoftGreenSurface,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = sport.take(4),
                                                color = TextMuted,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = CurrencyFormatter.formatInr(amount, compact = true),
                                                color = ForestGreen,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Admin Simulation Sandbox (Collapsed by default)
                    item {
                        DataTestingSandboxCard(
                            simulatedCount = uiState.simulatedBookingsCount,
                            onSimulate = { viewModel.simulateAdditionalBookings(it) },
                            onReset = { viewModel.resetSimulation() }
                        )
                    }
                } else {
                    // Player View
                    item {
                        PlayerSummaryCard(
                            busiestSlot = forecast.peakDemand.busiestTimeSlot,
                            leastBusySlot = forecast.peakDemand.leastBusyTimeSlot,
                            busiestSport = forecast.peakDemand.busiestSport
                        )
                    }

                    item {
                        AnalyticsCard(
                            title = "Recommended Slots for You",
                            subtitle = "Optimal times based on availability, crowd levels, and court pricing"
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                forecast.recommendations.forEach { rec ->
                                    SlotRecommendationCard(recommendation = rec)
                                }
                            }
                        }
                    }

                    item {
                        AnalyticsCard(
                            title = "Expected Crowd by Time Slot",
                            subtitle = "Typical busy and quiet hours across courts"
                        ) {
                            SlotUtilizationBarChart(
                                predictions = forecast.predictions,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }

                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = SoftGreenSurface,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = ForestGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Estimated demand based on simulated data and available booking history.",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Admin Overview Section ─────────────────────────────────────────────

@Composable
private fun AdminOverviewKPIs(
    forecast: ForecastResult,
    horizonDays: Int,
    selectedSport: String
) {
    val totalProjectedBookings = forecast.predictions.sumOf { it.predictedDemand }.toInt()
    val trend = forecast.peakDemand.demandTrend

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KPICard(
                modifier = Modifier.weight(1f),
                label = "Projected Bookings",
                value = "$totalProjectedBookings",
                sublabel = "$horizonDays-day forecast${if (selectedSport != "ALL") " · $selectedSport" else ""}",
                icon = Icons.Default.ConfirmationNumber,
                tint = ForestGreen
            )

            KPICard(
                modifier = Modifier.weight(1f),
                label = "Projected Revenue",
                value = CurrencyFormatter.formatInr(forecast.revenueProjection.totalProjectedRevenue, compact = true),
                sublabel = "$horizonDays-day estimate",
                icon = Icons.Default.Payments,
                tint = ForestGreen
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KPICard(
                modifier = Modifier.weight(1f),
                label = "Peak Demand Period",
                value = forecast.peakDemand.busiestTimeSlot,
                sublabel = "Highest crowd expected",
                icon = Icons.Default.AccessTime,
                tint = StatusWarning
            )

            KPICard(
                modifier = Modifier.weight(1f),
                label = "Demand Trend",
                value = when (trend) {
                    TrendDirection.INCREASING -> "Rising"
                    TrendDirection.DECREASING -> "Falling"
                    TrendDirection.STABLE -> "Stable"
                },
                sublabel = "Trajectory slope",
                icon = when (trend) {
                    TrendDirection.INCREASING -> Icons.AutoMirrored.Filled.TrendingUp
                    TrendDirection.DECREASING -> Icons.AutoMirrored.Filled.TrendingDown
                    TrendDirection.STABLE -> Icons.AutoMirrored.Filled.TrendingFlat
                },
                tint = when (trend) {
                    TrendDirection.INCREASING -> ForestGreen
                    TrendDirection.DECREASING -> StatusDestructive
                    TrendDirection.STABLE -> AccentBlue
                }
            )
        }

        if (forecast.actualRevenue > 0.0 || forecast.actualBookingCount > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SoftGreenSurface),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Actual Confirmed Bookings", color = TextMuted, fontSize = 11.sp)
                        Text(
                            "${forecast.actualBookingCount} real bookings recorded",
                            color = TextDeep,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Actual Collected Revenue", color = TextMuted, fontSize = 11.sp)
                        Text(
                            CurrencyFormatter.formatInr(forecast.actualRevenue),
                            color = ForestGreen,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ── Player Overview Card ───────────────────────────────────────────────

@Composable
private fun PlayerSummaryCard(
    busiestSlot: String,
    leastBusySlot: String,
    busiestSport: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Booking Insights Summary", color = TextDeep, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    modifier = Modifier.weight(1f),
                    color = SoftGreenSurface,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Quietest Time", color = ForestGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text(leastBusySlot, color = TextDeep, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Low crowds", color = TextMuted, fontSize = 10.sp)
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Peak Hours", color = StatusWarning, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text(busiestSlot, color = TextDeep, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Book early", color = TextMuted, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

// ── Reusable Component Pieces ──────────────────────────────────────────

@Composable
private fun AnalyticsCard(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                color = TextDeep,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(10.dp))
            }
            content()
        }
    }
}

@Composable
private fun DataSourceTransparencyCard(
    sourceLabel: String,
    syntheticWeight: Double,
    realWeight: Double,
    realBookingCount: Int,
    simulatedCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoGraph,
                        contentDescription = null,
                        tint = ForestGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Data Source & Blending",
                        color = TextDeep,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = if (realWeight >= 0.6) SoftGreenSurface else ElectricLime.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (realWeight >= 0.6) "REAL DOMINANT" else if (realWeight > 0.0) "BLENDED FORECAST" else "SIMULATION-LED",
                        color = ForestGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = sourceLabel,
                color = TextMuted,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Weight indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Synthetic: ${(syntheticWeight * 100).toInt()}%",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Real History: ${(realWeight * 100).toInt()}% ($realBookingCount real bookings)",
                    color = ForestGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Blending Progress Bar
            LinearProgressIndicator(
                progress = { realWeight.toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = ForestGreen,
                trackColor = SoftGreenSurface
            )
        }
    }
}

@Composable
private fun FilterControlsSection(
    availableSports: List<String>,
    selectedSport: String,
    onSportSelect: (String) -> Unit,
    selectedHorizon: Int,
    onHorizonSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Horizon selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Forecast Horizon", color = TextDeep, fontSize = 12.sp, fontWeight = FontWeight.Bold)

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(3, 7, 14).forEach { days ->
                    val isSelected = selectedHorizon == days
                    Surface(
                        modifier = Modifier.clickable { onHorizonSelect(days) },
                        color = if (isSelected) ForestGreen else CardSurface,
                        border = if (isSelected) null else BorderStroke(1.dp, BorderSubtle),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "$days Days",
                            color = if (isSelected) ElectricLime else TextDeep,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Sports chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(availableSports) { sport ->
                val isSelected = selectedSport.equals(sport, ignoreCase = true)
                Surface(
                    modifier = Modifier.clickable { onSportSelect(sport) },
                    color = if (isSelected) ForestGreen else CardSurface,
                    shape = RoundedCornerShape(20.dp),
                    border = if (isSelected) null else BorderStroke(1.dp, BorderSubtle)
                ) {
                    Text(
                        text = sport.lowercase().replaceFirstChar { it.uppercase() },
                        color = if (isSelected) ElectricLime else TextDeep,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun KPICard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    sublabel: String,
    icon: ImageVector,
    tint: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, color = TextDeep, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(text = label, color = TextDeep, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(text = sublabel, color = TextMuted, fontSize = 10.sp, maxLines = 1)
        }
    }
}

@Composable
private fun SlotRecommendationCard(
    recommendation: SlotRecommendation
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CardSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${recommendation.sport.lowercase().replaceFirstChar { it.uppercase() }} · ${recommendation.timeSlot}",
                        color = TextDeep,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color = SoftGreenSurface,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${recommendation.score.coerceIn(0.0, 100.0).toInt()}% Match",
                            color = ForestGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        color = ElectricLime.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = recommendation.recommendationBadge,
                            color = ForestGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = recommendation.explanation,
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyFormatter.formatInr(recommendation.estimatedPrice),
                    color = ForestGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${(recommendation.predictedUtilization * 100).toInt()}% busy",
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
        }
    }
}

/**
 * Admin Data Growth Simulation Sandbox (collapsed by default).
 * Isolated testing sandbox that NEVER creates real bookings in MongoDB.
 */
@Composable
private fun DataTestingSandboxCard(
    simulatedCount: Int,
    onSimulate: (Int) -> Unit,
    onReset: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = null,
                        tint = ForestGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Data & Model Testing (Sandbox)",
                            color = TextDeep,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (simulatedCount > 0) "+$simulatedCount simulated observations active" else "Simulate booking data growth (in-memory)",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Surface(
                    color = SoftGreenSurface,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (isExpanded) "Hide" else "Open",
                        color = ForestGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HorizontalDivider(color = BorderSubtle)

                    Text(
                        text = "Simulates additional historical observations to evaluate how forecasts transition from synthetic baseline to real dominance. Runs purely in-memory — never creates real reservations, alters availability, or modifies revenue.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(10, 25, 50, 100).forEach { count ->
                            val isSelected = simulatedCount == count
                            Button(
                                onClick = { onSimulate(count) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) ForestGreen else SoftGreenSurface
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "+$count",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) ElectricLime else ForestGreen
                                )
                            }
                        }
                    }

                    if (simulatedCount > 0) {
                        OutlinedButton(
                            onClick = onReset,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, StatusDestructive.copy(alpha = 0.5f))
                        ) {
                            Text("Reset to Baseline", color = StatusDestructive, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
