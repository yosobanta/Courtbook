package com.courtbook.analytics.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.courtbook.analytics.model.DataSource
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
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "CourtBook AI Insights",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = TextDeep
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = ElectricLime,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (uiState.isAdmin) "ADMIN AI" else "PLAYER AI",
                                    color = ForestGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Predictive intelligence & demand analytics",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
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
                contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
            ) {
                // 1. Data Source Transparency Banner
                item {
                    DataSourceTransparencyCard(
                        sourceLabel = forecast.dataSourceLabel,
                        syntheticWeight = forecast.syntheticWeight,
                        realWeight = forecast.realWeight,
                        realBookingCount = uiState.realBookingCount,
                        simulatedCount = uiState.simulatedBookingsCount
                    )
                }

                // 2. Filter Controls (Sport & Forecast Horizon)
                item {
                    FilterControlsSection(
                        availableSports = uiState.availableSports,
                        selectedSport = uiState.selectedSport,
                        onSportSelect = { viewModel.selectSport(it) },
                        selectedHorizon = uiState.forecastHorizon,
                        onHorizonSelect = { viewModel.setForecastHorizon(it) }
                    )
                }

                // 3. Key Performance Indicators
                item {
                    KPIOverviewSection(
                        totalPredictions = forecast.predictions.sumOf { it.predictedDemand }.toInt(),
                        peakSport = forecast.peakDemand.busiestSport,
                        peakSlot = forecast.peakDemand.busiestTimeSlot,
                        trend = forecast.peakDemand.demandTrend,
                        projectedRevenue = forecast.revenueProjection.totalProjectedRevenue
                    )
                }

                // 4. Demand Trajectory Chart
                item {
                    AnalyticsCard(
                        title = "Demand Forecast Trajectory",
                        subtitle = "Historical baseline vs ${uiState.forecastHorizon}-day AI projection"
                    ) {
                        DemandLineChart(
                            historicalData = forecast.historicalData,
                            predictions = forecast.predictions,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                // 5. Smart Slot Recommendations (Player-facing & Managerial)
                item {
                    AnalyticsCard(
                        title = "Smart Slot Recommendations",
                        subtitle = "Optimal time slots scoring high on availability and value"
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            forecast.recommendations.take(4).forEach { rec ->
                                SlotRecommendationCard(recommendation = rec)
                            }
                        }
                    }
                }

                // 6. Slot Capacity & Utilization
                item {
                    AnalyticsCard(
                        title = "Hourly Utilization vs Capacity",
                        subtitle = "Dotted line marks 75% peak pressure threshold"
                    ) {
                        SlotUtilizationBarChart(
                            predictions = forecast.predictions,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                // 7. Admin-Only: Heatmap, Revenue Breakdown & Simulation Sandbox
                if (uiState.isAdmin) {
                    item {
                        AnalyticsCard(
                            title = "Demand Intensity Heatmap",
                            subtitle = "Cross-sport occupancy distribution by time slot"
                        ) {
                            DemandHeatmapGrid(
                                predictions = forecast.predictions,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    item {
                        AnalyticsCard(
                            title = "Projected Daily Revenue",
                            subtitle = "Estimated earnings over the ${uiState.forecastHorizon}-day horizon"
                        ) {
                            val dailyRev = forecast.revenueProjection.dailyProjections.map {
                                "Day +${it.dayOffset}" to it.revenue
                            }
                            RevenueBarChart(
                                dailyRevenue = dailyRev,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Revenue by Sport",
                                color = DarkTextSecondary,
                                fontSize = 12.sp,
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
                                        color = DarkSurface,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = sport.take(4),
                                                color = DarkTextSecondary,
                                                fontSize = 10.sp
                                            )
                                            Text(
                                                text = "₹${amount.toInt()}",
                                                color = CourtGreen,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        AnalyticsCard(
                            title = "Data Growth Simulation Sandbox",
                            subtitle = "Simulate real booking accumulation to evaluate model blending"
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Current Simulated Extra Observations: ${uiState.simulatedBookingsCount}",
                                    color = DarkTextSecondary,
                                    fontSize = 12.sp
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(10, 25, 50, 100).forEach { count ->
                                        Button(
                                            onClick = { viewModel.simulateAdditionalBookings(count) },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (uiState.simulatedBookingsCount == count) AccentAmber else DarkSurface
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "+$count",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (uiState.simulatedBookingsCount == count) Color.Black else DarkTextPrimary
                                            )
                                        }
                                    }
                                }

                                if (uiState.simulatedBookingsCount > 0) {
                                    OutlinedButton(
                                        onClick = { viewModel.resetSimulation() },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Reset to Baseline", color = StatusBooked, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
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
                    color = if (realWeight > 0.5) SoftGreenSurface else ElectricLime.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (realWeight > 0.5) "REAL DOMINANT" else if (realWeight > 0.0) "BLENDED" else "SYNTHETIC BASELINE",
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
            Text("Forecast Horizon", color = DarkTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(3, 7, 14).forEach { days ->
                    val isSelected = selectedHorizon == days
                    Surface(
                        modifier = Modifier.clickable { onHorizonSelect(days) },
                        color = if (isSelected) CourtGreen else DarkSurfaceElevated,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "$days Days",
                            color = if (isSelected) Color.Black else DarkTextSecondary,
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
                    color = if (isSelected) ChipSelectedBg else ChipUnselectedBg,
                    shape = RoundedCornerShape(20.dp),
                    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                ) {
                    Text(
                        text = sport.lowercase().replaceFirstChar { it.uppercase() },
                        color = if (isSelected) Color.White else DarkTextSecondary,
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
private fun KPIOverviewSection(
    totalPredictions: Int,
    peakSport: String,
    peakSlot: String,
    trend: TrendDirection,
    projectedRevenue: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        KPICard(
            modifier = Modifier.weight(1f),
            label = "Est. Bookings",
            value = "$totalPredictions",
            icon = Icons.Default.ConfirmationNumber,
            tint = CourtGreen
        )

        KPICard(
            modifier = Modifier.weight(1f),
            label = "Peak Slot",
            value = peakSlot.split("-").firstOrNull() ?: peakSlot,
            icon = Icons.Default.AccessTime,
            tint = AccentAmber
        )

        KPICard(
            modifier = Modifier.weight(1f),
            label = "Demand Trend",
            value = trend.name.take(4),
            icon = when (trend) {
                TrendDirection.INCREASING -> Icons.AutoMirrored.Filled.TrendingUp
                TrendDirection.DECREASING -> Icons.AutoMirrored.Filled.TrendingDown
                TrendDirection.STABLE -> Icons.AutoMirrored.Filled.TrendingFlat
            },
            tint = when (trend) {
                TrendDirection.INCREASING -> CourtGreen
                TrendDirection.DECREASING -> StatusBooked
                TrendDirection.STABLE -> AccentBlue
            }
        )
    }
}

@Composable
private fun KPICard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
            Text(text = value, color = TextDeep, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(text = label, color = TextMuted, fontSize = 10.sp)
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
                        text = recommendation.timeSlot,
                        color = TextDeep,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = SoftGreenSurface,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${(recommendation.score * 100).toInt()}% Match",
                            color = ForestGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = recommendation.explanation,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${recommendation.estimatedPrice.toInt()}",
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
