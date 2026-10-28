package com.courtbook.analytics.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.courtbook.analytics.model.CurrencyFormatter
import com.courtbook.analytics.model.DemandPrediction
import com.courtbook.analytics.model.HistoricalDataPoint
import com.courtbook.ui.theme.*

/**
 * Custom line chart with smooth gradient fill, dashed prediction line,
 * distinct historical vs predicted styling, and clear X-axis timeline markers.
 */
@Composable
fun DemandLineChart(
    historicalData: List<HistoricalDataPoint>,
    predictions: List<DemandPrediction>,
    modifier: Modifier = Modifier
) {
    if (historicalData.isEmpty() && predictions.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No forecast data available", color = TextMuted, fontSize = 12.sp)
        }
        return
    }

    // Historical baseline: past 7 days (Day -7..-1)
    val recentHist = historicalData.takeLast(7)
    val predDaily = predictions.groupBy { it.dayOffset }
        .map { (day, list) -> day to list.sumOf { it.predictedDemand } }
        .sortedBy { it.first }

    val histValues = recentHist.map { it.bookingCount }
    val predValues = predDaily.map { it.second }
    val allValues = histValues + predValues
    val maxVal = (allValues.maxOrNull() ?: 10.0).coerceAtLeast(5.0)

    Column(modifier = modifier.fillMaxWidth()) {
        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(8.dp).background(AccentBlue, CircleShape))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Historical Baseline", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.width(14.dp))
            Box(modifier = Modifier.size(8.dp).background(ForestGreen, CircleShape))
            Spacer(modifier = Modifier.width(4.dp))
            Text("AI Forecast", color = ForestGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            val width = size.width
            val height = size.height
            val paddingBottom = 20f
            val paddingTop = 16f
            val usableHeight = height - paddingBottom - paddingTop
            val totalPoints = (histValues.size + predValues.size).coerceAtLeast(2)
            val stepX = width / (totalPoints - 1).coerceAtLeast(1)

            // Grid lines
            val gridLines = 4
            for (i in 0..gridLines) {
                val y = paddingTop + (usableHeight / gridLines) * i
                drawLine(
                    color = BorderSubtle.copy(alpha = 0.5f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
            }

            // Historical path (Day -7 to Day -1)
            if (histValues.isNotEmpty()) {
                val histPath = Path()
                val histFillPath = Path()

                histValues.forEachIndexed { idx, value ->
                    val x = idx * stepX
                    val y = paddingTop + usableHeight - ((value / maxVal) * usableHeight).toFloat()
                    if (idx == 0) {
                        histPath.moveTo(x, y)
                        histFillPath.moveTo(x, paddingTop + usableHeight)
                        histFillPath.lineTo(x, y)
                    } else {
                        histPath.lineTo(x, y)
                        histFillPath.lineTo(x, y)
                    }
                }

                val lastHistX = (histValues.size - 1) * stepX
                histFillPath.lineTo(lastHistX, paddingTop + usableHeight)
                histFillPath.close()

                drawPath(
                    path = histFillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(AccentBlue.copy(alpha = 0.20f), Color.Transparent),
                        startY = paddingTop,
                        endY = paddingTop + usableHeight
                    )
                )

                drawPath(
                    path = histPath,
                    color = AccentBlue,
                    style = Stroke(width = 2.5f)
                )

                // Dots on historical points
                histValues.forEachIndexed { idx, value ->
                    val x = idx * stepX
                    val y = paddingTop + usableHeight - ((value / maxVal) * usableHeight).toFloat()
                    drawCircle(color = AccentBlue, radius = 3f, center = Offset(x, y))
                }
            }

            // Predicted path (dashed green line connecting smoothly from last historical point)
            if (predValues.isNotEmpty()) {
                val predPath = Path()
                val predFillPath = Path()

                val startIdx = (histValues.size - 1).coerceAtLeast(0)
                val startX = startIdx * stepX
                val startY = if (histValues.isNotEmpty()) {
                    paddingTop + usableHeight - ((histValues.last() / maxVal) * usableHeight).toFloat()
                } else {
                    paddingTop + usableHeight - ((predValues.first() / maxVal) * usableHeight).toFloat()
                }

                predPath.moveTo(startX, startY)
                predFillPath.moveTo(startX, paddingTop + usableHeight)
                predFillPath.lineTo(startX, startY)

                predValues.forEachIndexed { i, value ->
                    val idx = histValues.size + i
                    val x = idx * stepX
                    val y = paddingTop + usableHeight - ((value / maxVal) * usableHeight).toFloat()
                    predPath.lineTo(x, y)
                    predFillPath.lineTo(x, y)
                }

                val lastPredX = (histValues.size + predValues.size - 1) * stepX
                predFillPath.lineTo(lastPredX, paddingTop + usableHeight)
                predFillPath.close()

                drawPath(
                    path = predFillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(ForestGreen.copy(alpha = 0.18f), Color.Transparent),
                        startY = paddingTop,
                        endY = paddingTop + usableHeight
                    )
                )

                drawPath(
                    path = predPath,
                    color = ForestGreen,
                    style = Stroke(
                        width = 3.0f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                    )
                )

                // Dots on predicted points
                predValues.forEachIndexed { i, value ->
                    val idx = histValues.size + i
                    val x = idx * stepX
                    val y = paddingTop + usableHeight - ((value / maxVal) * usableHeight).toFloat()
                    drawCircle(color = CardSurface, radius = 4.5f, center = Offset(x, y))
                    drawCircle(color = ForestGreen, radius = 3.0f, center = Offset(x, y))
                }
            }
        }

        // Timeline axis labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("7d ago", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            Text("3d ago", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            Text("Today", color = ForestGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            val horizon = predDaily.size
            if (horizon > 3) {
                Text("+${horizon / 2}d", color = ForestGreen, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
            Text("+${horizon}d", color = ForestGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Bar chart showing time slot utilization percentages with capacity threshold marker.
 */
@Composable
fun SlotUtilizationBarChart(
    predictions: List<DemandPrediction>,
    modifier: Modifier = Modifier
) {
    val slotUtil = predictions.groupBy { it.timeSlot }
        .map { (slot, list) ->
            val avg = list.map { it.utilization }.average()
            slot to avg
        }
        .sortedBy { it.first }

    if (slotUtil.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.width(14.dp).height(2.dp).background(AccentOrange))
            Spacer(modifier = Modifier.width(4.dp))
            Text("75% Capacity Threshold", color = AccentOrange, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            val width = size.width
            val height = size.height
            val paddingBottom = 16f
            val usableHeight = height - paddingBottom
            val barCount = slotUtil.size
            val barSpacing = width / barCount
            val barWidth = barSpacing * 0.55f

            // 75% capacity threshold line
            val thresholdY = usableHeight * (1f - 0.75f)
            drawLine(
                color = AccentOrange.copy(alpha = 0.6f),
                start = Offset(0f, thresholdY),
                end = Offset(width, thresholdY),
                strokeWidth = 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
            )

            slotUtil.forEachIndexed { idx, pair ->
                val util = pair.second.coerceIn(0.0, 1.0).toFloat()
                val barHeight = usableHeight * util
                val x = idx * barSpacing + (barSpacing - barWidth) / 2
                val y = usableHeight - barHeight

                val barColor = when {
                    util >= 0.75f -> AccentOrange
                    util >= 0.50f -> AccentAmber
                    else -> ForestGreen
                }

                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }
        }

        // Slot labels underneath
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            slotUtil.forEach { pair ->
                val shortLabel = pair.first.split("-").firstOrNull() ?: pair.first
                Text(
                    text = shortLabel,
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Revenue projection bar chart showing daily estimated income.
 */
@Composable
fun RevenueBarChart(
    dailyRevenue: List<Pair<String, Double>>,
    modifier: Modifier = Modifier
) {
    if (dailyRevenue.isEmpty()) return
    val maxRev = (dailyRevenue.maxOfOrNull { it.second } ?: 1000.0).coerceAtLeast(100.0)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            val width = size.width
            val height = size.height
            val paddingBottom = 16f
            val usableHeight = height - paddingBottom
            val barCount = dailyRevenue.size
            val barSpacing = width / barCount
            val barWidth = barSpacing * 0.5f

            dailyRevenue.forEachIndexed { idx, pair ->
                val ratio = (pair.second / maxRev).coerceIn(0.0, 1.0).toFloat()
                val barHeight = usableHeight * ratio
                val x = idx * barSpacing + (barSpacing - barWidth) / 2
                val y = usableHeight - barHeight

                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(ForestGreen, ForestGreenDark),
                        startY = y,
                        endY = usableHeight
                    ),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }
        }

        // Day labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            dailyRevenue.forEach { pair ->
                Text(
                    text = pair.first,
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Heatmap grid of Sports vs Time Slots indicating predicted demand intensity.
 * Features full horizontal scroll to prevent clipped columns on smaller screens.
 */
@Composable
fun DemandHeatmapGrid(
    predictions: List<DemandPrediction>,
    modifier: Modifier = Modifier
) {
    val sports = predictions.map { it.sport }.distinct()
    val slots = predictions.map { it.timeSlot }.distinct().sorted()

    if (sports.isEmpty() || slots.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeatmapLegendItem(color = SoftGreenSurface, label = "<30% Quiet")
            HeatmapLegendItem(color = Color(0xFFFEF3C7), label = "30-60% Moderate")
            HeatmapLegendItem(color = Color(0xFFFFEDD5), label = "60-80% Busy")
            HeatmapLegendItem(color = Color(0xFFFEE2E2), label = ">80% Peak")
        }

        // Horizontal scrollable grid
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            // Header row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(84.dp)) {
                    Text("Sport", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                slots.forEach { slot ->
                    Box(
                        modifier = Modifier
                            .width(70.dp)
                            .padding(horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = slot.substringBefore("-"),
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Data rows
            sports.forEach { sport ->
                Row(
                    modifier = Modifier.padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.width(84.dp)) {
                        Text(
                            text = sport.lowercase().replaceFirstChar { it.uppercase() },
                            color = TextDeep,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    slots.forEach { slot ->
                        val match = predictions.filter { it.sport.equals(sport, true) && it.timeSlot == slot }
                        val avgUtil = if (match.isNotEmpty()) match.map { it.utilization }.average() else 0.0

                        val cellBg = when {
                            avgUtil >= 0.80 -> Color(0xFFFEE2E2)
                            avgUtil >= 0.60 -> Color(0xFFFFEDD5)
                            avgUtil >= 0.30 -> Color(0xFFFEF3C7)
                            else -> SoftGreenSurface
                        }

                        val cellText = when {
                            avgUtil >= 0.80 -> StatusDestructive
                            avgUtil >= 0.60 -> Color(0xFFEA580C)
                            avgUtil >= 0.30 -> StatusWarning
                            else -> ForestGreen
                        }

                        Box(
                            modifier = Modifier
                                .width(70.dp)
                                .height(28.dp)
                                .padding(horizontal = 2.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(cellBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${(avgUtil * 100).toInt()}%",
                                color = cellText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeatmapLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, color = TextMuted, fontSize = 9.sp)
    }
}
