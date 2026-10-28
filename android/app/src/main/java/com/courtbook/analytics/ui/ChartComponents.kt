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
import com.courtbook.analytics.model.DemandPrediction
import com.courtbook.analytics.model.HistoricalDataPoint
import com.courtbook.ui.theme.*

/**
 * Custom line chart with smooth gradient fill underneath, dashed prediction line,
 * and distinct markers for historical vs predicted demand.
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
            Text("No forecast data available", color = DarkTextSecondary, fontSize = 12.sp)
        }
        return
    }

    // Combine recent historical (last 10 days) + future predictions
    val recentHist = historicalData.takeLast(10)
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
            Text("History", color = DarkTextSecondary, fontSize = 11.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.size(8.dp).background(CourtGreen, CircleShape))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Predicted", color = DarkTextSecondary, fontSize = 11.sp)
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
        ) {
            val width = size.width
            val height = size.height
            val paddingBottom = 24f
            val paddingTop = 16f
            val usableHeight = height - paddingBottom - paddingTop
            val totalPoints = (histValues.size + predValues.size).coerceAtLeast(2)
            val stepX = width / (totalPoints - 1).coerceAtLeast(1)

            // Grid lines
            val gridLines = 4
            for (i in 0..gridLines) {
                val y = paddingTop + (usableHeight / gridLines) * i
                drawLine(
                    color = DarkBorder.copy(alpha = 0.35f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
            }

            // Historical path
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
                        colors = listOf(AccentBlue.copy(alpha = 0.25f), Color.Transparent),
                        startY = paddingTop,
                        endY = paddingTop + usableHeight
                    )
                )

                drawPath(
                    path = histPath,
                    color = AccentBlue,
                    style = Stroke(width = 3f)
                )

                // Dots on historical points
                histValues.forEachIndexed { idx, value ->
                    val x = idx * stepX
                    val y = paddingTop + usableHeight - ((value / maxVal) * usableHeight).toFloat()
                    drawCircle(color = AccentBlue, radius = 3.5f, center = Offset(x, y))
                }
            }

            // Predicted path (dashed line connecting from last historical point)
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
                        colors = listOf(CourtGreen.copy(alpha = 0.22f), Color.Transparent),
                        startY = paddingTop,
                        endY = paddingTop + usableHeight
                    )
                )

                drawPath(
                    path = predPath,
                    color = CourtGreen,
                    style = Stroke(
                        width = 3.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )
                )

                // Dots on predicted points
                predValues.forEachIndexed { i, value ->
                    val idx = histValues.size + i
                    val x = idx * stepX
                    val y = paddingTop + usableHeight - ((value / maxVal) * usableHeight).toFloat()
                    drawCircle(color = DarkBg, radius = 5f, center = Offset(x, y))
                    drawCircle(color = CourtGreen, radius = 3.5f, center = Offset(x, y))
                }
            }
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
    // Group predictions by time slot and average utilization
    val slotUtil = predictions.groupBy { it.timeSlot }
        .map { (slot, list) ->
            val avg = list.map { it.utilization }.average()
            slot to avg
        }
        .sortedBy { it.first }

    if (slotUtil.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val width = size.width
            val height = size.height
            val paddingBottom = 20f
            val usableHeight = height - paddingBottom
            val barCount = slotUtil.size
            val barSpacing = width / barCount
            val barWidth = barSpacing * 0.55f

            // 75% high-capacity dashed threshold line
            val thresholdY = usableHeight * (1f - 0.75f)
            drawLine(
                color = AccentOrange.copy(alpha = 0.7f),
                start = Offset(0f, thresholdY),
                end = Offset(width, thresholdY),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
            )

            slotUtil.forEachIndexed { idx, pair ->
                val util = pair.second.coerceIn(0.0, 1.0).toFloat()
                val barHeight = usableHeight * util
                val x = idx * barSpacing + (barSpacing - barWidth) / 2
                val y = usableHeight - barHeight

                val barColor = when {
                    util >= 0.75f -> AccentOrange
                    util >= 0.50f -> AccentAmber
                    else -> CourtGreen
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
                    color = DarkTextSecondary,
                    fontSize = 9.sp,
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
                .height(140.dp)
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
                        colors = listOf(CourtGreen, CourtGreenDark),
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
                    color = DarkTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Heatmap grid of Sport/Days vs Time Slots indicating predicted demand intensity.
 */
@Composable
fun DemandHeatmapGrid(
    predictions: List<DemandPrediction>,
    modifier: Modifier = Modifier
) {
    val sports = predictions.map { it.sport }.distinct()
    val slots = predictions.map { it.timeSlot }.distinct().sorted()

    if (sports.isEmpty() || slots.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        // Header row with slot times
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(80.dp)) {
                Text("Sport", color = DarkTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            slots.forEach { slot ->
                Box(
                    modifier = Modifier
                        .width(64.dp)
                        .padding(horizontal = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = slot.substringBefore("-"),
                        color = DarkTextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
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
                Box(modifier = Modifier.width(80.dp)) {
                    Text(
                        text = sport.lowercase().replaceFirstChar { it.uppercase() },
                        color = DarkTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                slots.forEach { slot ->
                    val match = predictions.filter { it.sport.equals(sport, true) && it.timeSlot == slot }
                    val avgUtil = if (match.isNotEmpty()) match.map { it.utilization }.average() else 0.0

                    val cellColor = when {
                        avgUtil >= 0.75 -> AccentOrange
                        avgUtil >= 0.50 -> AccentAmber
                        avgUtil >= 0.25 -> CourtGreen
                        else -> DarkSurfaceElevated
                    }

                    Box(
                        modifier = Modifier
                            .width(64.dp)
                            .height(28.dp)
                            .padding(horizontal = 2.dp)
                            .background(cellColor.copy(alpha = 0.85f), RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${(avgUtil * 100).toInt()}%",
                            color = if (avgUtil >= 0.25) DarkBg else DarkTextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
