package com.courtbook.analytics.engine

import com.courtbook.analytics.model.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Pure-Kotlin statistical forecasting engine.
 *
 * All methods are pure functions — no Android UI code, no network,
 * no side-effects.  Safe to call from any dispatcher.
 *
 * Key algorithms:
 *  • Weighted Moving Average (WMA) for demand prediction
 *  • Simple linear regression for trend analysis
 *  • Normalized scoring for slot recommendations
 *  • Sigmoid blending for synthetic → real data transition
 */
object AnalyticsEngine {

    // ━━ WMA weights (most-recent → oldest) ━━━━━━━━━━━━━━━━━━━━━━━━━
    private const val W1 = 0.50  // most recent
    private const val W2 = 0.30  // previous
    private const val W3 = 0.20  // third-most-recent

    // ━━ Recommendation score weights ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    private const val AVAILABILITY_WEIGHT = 0.70
    private const val AFFORDABILITY_WEIGHT = 0.30

    // ━━ Blending threshold ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    /** Number of real observations at which real data has ≈ 88 % weight. */
    private const val BLENDING_THRESHOLD = 30

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * Main entry point — produce a complete [ForecastResult].
     *
     * @param syntheticData  baseline synthetic dataset
     * @param realSummaries  optional real-booking summaries (may be empty)
     * @param sportFilter    "ALL" or a specific sport name
     * @param horizonDays    how many days to forecast (3, 7, or 14)
     */
    fun computeForecast(
        syntheticData: List<SyntheticBooking>,
        realSummaries: List<RealBookingSummary>,
        sportFilter: String,
        horizonDays: Int
    ): ForecastResult {

        // ── 1. filter ──────────────────────────────────────────────
        val filteredSynthetic = if (sportFilter == "ALL") syntheticData
        else syntheticData.filter { it.sport.equals(sportFilter, ignoreCase = true) }

        val filteredReal = if (sportFilter == "ALL") realSummaries
        else realSummaries.filter { it.sport.equals(sportFilter, ignoreCase = true) }

        // ── 2. blending weights ────────────────────────────────────
        val realCount = filteredReal.sumOf { it.bookingCount }
        val blendWeights = computeBlendingWeights(realCount)
        val synW = blendWeights.first
        val realW = blendWeights.second
        val dataSource = when {
            realCount == 0 -> DataSource.SYNTHETIC_ONLY
            realW >= 0.6 -> DataSource.REAL_DOMINANT
            else -> DataSource.BLENDED
        }

        // ── 3. demand predictions ──────────────────────────────────
        val predictions = generatePredictions(
            filteredSynthetic, filteredReal, synW, realW,
            horizonDays, dataSource
        )

        // ── 4. historical chart data ───────────────────────────────
        val historical = buildHistoricalData(filteredSynthetic, filteredReal)

        // ── 5. peak demand analysis ────────────────────────────────
        val peakDemand = analyzePeakDemand(predictions, filteredSynthetic)

        // ── 6. slot recommendations ────────────────────────────────
        val recommendations = recommendSlots(predictions)

        // ── 7. revenue projection ──────────────────────────────────
        val revenueProjection = projectRevenue(predictions, dataSource)

        // ── 8. data-source label ───────────────────────────────────
        val label = when (dataSource) {
            DataSource.SYNTHETIC_ONLY ->
                "Using synthetic baseline · Real booking history unavailable"
            DataSource.BLENDED ->
                "Forecast informed by available booking history"
            DataSource.REAL_DOMINANT ->
                "Forecast informed by available booking history"
        }

        val coverage = if (filteredSynthetic.isEmpty()) 0.0
        else min(1.0, realCount.toDouble() / (filteredSynthetic.size * 0.5))

        return ForecastResult(
            predictions = predictions,
            recommendations = recommendations,
            peakDemand = peakDemand,
            revenueProjection = revenueProjection,
            historicalData = historical,
            realDataCoverage = coverage,
            dataSourceLabel = label,
            syntheticWeight = synW,
            realWeight = realW
        )
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Blending
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * Sigmoid-like transition: realWeight = realCount / (realCount + K).
     * Returns (syntheticWeight, realWeight) summing to 1.
     */
    fun computeBlendingWeights(realObservationCount: Int): Pair<Double, Double> {
        if (realObservationCount <= 0) return 1.0 to 0.0
        val realW = realObservationCount.toDouble() /
                (realObservationCount.toDouble() + BLENDING_THRESHOLD)
        return (1.0 - realW) to realW
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Demand Predictions (WMA)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    private fun generatePredictions(
        synthetic: List<SyntheticBooking>,
        real: List<RealBookingSummary>,
        synW: Double,
        realW: Double,
        horizonDays: Int,
        dataSource: DataSource
    ): List<DemandPrediction> {

        val sports = synthetic.map { it.sport }.distinct().ifEmpty { listOf("BADMINTON") }
        val timeSlots = synthetic.map { it.timeSlot }.distinct()
            .ifEmpty { listOf("18:00-20:00") }

        val results = mutableListOf<DemandPrediction>()

        for (sport in sports) {
            for (slot in timeSlots) {
                // synthetic daily series for this sport+slot
                val synSeries = synthetic
                    .filter { it.sport == sport && it.timeSlot == slot }
                    .sortedBy { it.dayIndex }
                    .map { it.bookingCount.toDouble() }

                // real booking count for this sport+slot (aggregated)
                val realEntry = real.firstOrNull {
                    it.sport.equals(sport, ignoreCase = true) &&
                            it.timeSlot == slot
                }

                val capacity = synthetic
                    .firstOrNull { it.sport == sport && it.timeSlot == slot }
                    ?.capacity ?: 4
                val avgPrice = if (realEntry != null && realEntry.averagePrice > 0)
                    blendValue(
                        synW,
                        synthetic.filter { it.sport == sport && it.timeSlot == slot }
                            .map { it.averagePrice }.average(),
                        realW,
                        realEntry.averagePrice
                    )
                else
                    synthetic.filter { it.sport == sport && it.timeSlot == slot }
                        .map { it.averagePrice }.averageOrNull() ?: 400.0

                // Build rolling forecast
                val forecastSeries = synSeries.toMutableList()
                for (day in 0 until horizonDays) {
                    val wma = weightedMovingAverage(forecastSeries)

                    // Blend synthetic WMA with real if available
                    val blendedPrediction = if (realEntry != null && realEntry.bookingCount > 0) {
                        val realAvgDaily = realEntry.bookingCount.toDouble() /
                                max(1, synSeries.size / 5) // approximate days
                        blendValue(synW, wma, realW, realAvgDaily)
                    } else wma

                    val predicted = blendedPrediction.coerceAtLeast(0.0)
                    forecastSeries.add(predicted)

                    val utilization = if (capacity > 0)
                        (predicted / capacity).coerceIn(0.0, 1.0)
                    else 0.0

                    results.add(
                        DemandPrediction(
                            dayOffset = day,
                            sport = sport,
                            timeSlot = slot,
                            predictedDemand = (predicted * 100).toInt() / 100.0,
                            capacity = capacity,
                            utilization = utilization,
                            averagePrice = (avgPrice * 100).toInt() / 100.0,
                            dataSource = dataSource
                        )
                    )
                }
            }
        }
        return results
    }

    /**
     * Weighted Moving Average using the three most recent values.
     * Handles fewer-than-three observations gracefully.
     */
    fun weightedMovingAverage(series: List<Double>): Double {
        if (series.isEmpty()) return 0.0
        val n = series.size
        return when {
            n >= 3 -> W1 * series[n - 1] + W2 * series[n - 2] + W3 * series[n - 3]
            n == 2 -> 0.6 * series[n - 1] + 0.4 * series[n - 2]
            else -> series.last()
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Trend Analysis (Simple Linear Regression)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * Returns the direction of the overall demand trend.
     * Uses OLS slope of daily totals.
     */
    fun estimateTrend(dailyTotals: List<Double>): TrendDirection {
        if (dailyTotals.size < 3) return TrendDirection.STABLE
        val n = dailyTotals.size
        val xs = (0 until n).map { it.toDouble() }
        val xMean = xs.average()
        val yMean = dailyTotals.average()

        var num = 0.0
        var den = 0.0
        for (i in 0 until n) {
            num += (xs[i] - xMean) * (dailyTotals[i] - yMean)
            den += (xs[i] - xMean) * (xs[i] - xMean)
        }

        if (den == 0.0) return TrendDirection.STABLE
        val slope = num / den

        // Normalize slope relative to mean to judge significance
        val normalizedSlope = if (yMean != 0.0) slope / yMean else slope
        return when {
            normalizedSlope > 0.005 -> TrendDirection.INCREASING
            normalizedSlope < -0.005 -> TrendDirection.DECREASING
            else -> TrendDirection.STABLE
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Peak Demand Analysis
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    private fun analyzePeakDemand(
        predictions: List<DemandPrediction>,
        synthetic: List<SyntheticBooking>
    ): PeakDemandInfo {

        val bySport = predictions.groupBy { it.sport }
            .mapValues { (_, preds) -> preds.sumOf { it.predictedDemand } }
        val bySlot = predictions.groupBy { it.timeSlot }
            .mapValues { (_, preds) -> preds.sumOf { it.predictedDemand } }

        val busiestSport = bySport.maxByOrNull { it.value }?.key ?: "N/A"
        val busiestSlot = bySlot.maxByOrNull { it.value }?.key ?: "N/A"
        val leastBusySlot = bySlot.minByOrNull { it.value }?.key ?: "N/A"

        // Peak day+sport
        val byDaySport = predictions.groupBy { "${it.dayOffset}-${it.sport}" }
            .mapValues { (_, preds) -> preds.sumOf { it.predictedDemand } }
        val peakEntry = byDaySport.maxByOrNull { it.value }
        val peakParts = peakEntry?.key?.split("-") ?: listOf("0", "N/A")
        val peakDay = peakParts.firstOrNull()?.toIntOrNull() ?: 0
        val peakDaySport = peakParts.drop(1).joinToString("-").ifEmpty { "N/A" }

        // Trend from synthetic daily totals
        val dailyTotals = synthetic
            .groupBy { it.dayIndex }
            .entries.sortedBy { it.key }
            .map { it.value.sumOf { b -> b.bookingCount.toDouble() } }
        val trend = estimateTrend(dailyTotals)

        return PeakDemandInfo(
            busiestSport = busiestSport,
            busiestTimeSlot = busiestSlot,
            leastBusyTimeSlot = leastBusySlot,
            peakDaySport = peakDaySport,
            peakDayOffset = peakDay,
            demandTrend = trend
        )
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Slot Recommendations
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    private fun recommendSlots(predictions: List<DemandPrediction>): List<SlotRecommendation> {
        if (predictions.isEmpty()) return emptyList()

        // Group by sport+slot and take the average across forecast days
        data class Key(val sport: String, val slot: String)

        val grouped = predictions.groupBy { Key(it.sport, it.timeSlot) }

        val candidates = grouped.map { (key, preds) ->
            val avgUtil = preds.map { it.utilization }.average()
            val avgPrice = preds.map { it.averagePrice }.average()
            Triple(key, avgUtil, avgPrice)
        }

        if (candidates.isEmpty()) return emptyList()

        // Normalize scores to 0–100
        val maxPrice = candidates.maxOf { it.third }.coerceAtLeast(1.0)
        val minPrice = candidates.minOf { it.third }
        val priceRange = (maxPrice - minPrice).coerceAtLeast(1.0)

        return candidates.map { (key, avgUtil, avgPrice) ->
            val availScore = ((1.0 - avgUtil) * 100.0).coerceIn(0.0, 100.0)
            val affordScore = ((maxPrice - avgPrice) / priceRange * 100.0).coerceIn(0.0, 100.0)
            val totalScore = AVAILABILITY_WEIGHT * availScore + AFFORDABILITY_WEIGHT * affordScore

            val explanation = buildString {
                append("Recommended because predicted demand is ")
                when {
                    avgUtil < 0.3 -> append("low")
                    avgUtil < 0.6 -> append("moderate")
                    else -> append("manageable")
                }
                if (affordScore > 50) {
                    append(" and the estimated price is comparatively affordable")
                }
                append(".")
            }

            SlotRecommendation(
                sport = key.sport,
                timeSlot = key.slot,
                score = (totalScore * 10).toInt() / 10.0,
                availabilityScore = (availScore * 10).toInt() / 10.0,
                affordabilityScore = (affordScore * 10).toInt() / 10.0,
                estimatedPrice = (avgPrice * 100).toInt() / 100.0,
                predictedUtilization = (avgUtil * 1000).toInt() / 1000.0,
                explanation = explanation
            )
        }
            .sortedByDescending { it.score }
            .take(3)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Revenue Projection
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    private fun projectRevenue(
        predictions: List<DemandPrediction>,
        dataSource: DataSource
    ): RevenueProjection {

        val dailyRevenues = predictions
            .groupBy { it.dayOffset }
            .entries.sortedBy { it.key }
            .map { (day, preds) ->
                DailyRevenue(
                    dayOffset = day,
                    revenue = preds.sumOf { it.predictedDemand * it.averagePrice }
                )
            }

        val bySport = predictions
            .groupBy { it.sport }
            .mapValues { (_, preds) ->
                preds.sumOf { it.predictedDemand * it.averagePrice }
            }

        return RevenueProjection(
            totalProjectedRevenue = dailyRevenues.sumOf { it.revenue },
            dailyProjections = dailyRevenues,
            bySport = bySport,
            dataSource = dataSource
        )
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Historical Data for Charts
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    private fun buildHistoricalData(
        synthetic: List<SyntheticBooking>,
        real: List<RealBookingSummary>
    ): List<HistoricalDataPoint> {

        val points = mutableListOf<HistoricalDataPoint>()

        // Aggregate synthetic by day
        synthetic.groupBy { it.dayIndex }
            .entries.sortedBy { it.key }
            .forEach { (day, bookings) ->
                points.add(
                    HistoricalDataPoint(
                        dayIndex = day,
                        bookingCount = bookings.sumOf { it.bookingCount.toDouble() },
                        source = DataPointSource.SYNTHETIC
                    )
                )
            }

        // If we have real data, add a summary point at the end
        if (real.isNotEmpty()) {
            val maxDay = points.maxOfOrNull { it.dayIndex } ?: 0
            points.add(
                HistoricalDataPoint(
                    dayIndex = maxDay + 1,
                    bookingCount = real.sumOf { it.bookingCount.toDouble() },
                    source = DataPointSource.REAL
                )
            )
        }

        return points
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Utilization helpers
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /** Safe utilization percentage. Returns null if capacity is zero or negative. */
    fun safeUtilization(predicted: Double, capacity: Int): Double? {
        if (capacity <= 0) return null
        return (predicted / capacity).coerceIn(0.0, 1.0)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Private utility
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    private fun blendValue(
        synW: Double, synVal: Double,
        realW: Double, realVal: Double
    ): Double = synW * synVal + realW * realVal

    private fun List<Double>.averageOrNull(): Double? =
        if (isEmpty()) null else average()
}
