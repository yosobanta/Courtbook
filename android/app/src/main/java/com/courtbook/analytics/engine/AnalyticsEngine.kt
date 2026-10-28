package com.courtbook.analytics.engine

import com.courtbook.analytics.model.*
import kotlin.math.max
import kotlin.math.min

/**
 * Pure-Kotlin statistical forecasting and analytics engine.
 *
 * All methods are pure functions — safe to call from any dispatcher,
 * no network, no database modifications, and zero side-effects.
 *
 * Key capabilities:
 *  • Weighted Moving Average (WMA) for demand prediction
 *  • Rational sigmoid-like blending for synthetic → real data transition
 *  • Consistent OLS regression for forecast horizon demand trends
 *  • Normalized 0–100 scoring for slot recommendations
 *  • Smooth historical-to-forecast continuity without artificial drop cliffs
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
    /** Number of real observations at which real data has 50% weight, transitioning smoothly. */
    private const val BLENDING_THRESHOLD = 30

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * Main entry point — produce a complete [ForecastResult].
     *
     * @param syntheticData  baseline synthetic dataset
     * @param realSummaries  optional real-booking summaries (may be empty)
     * @param sportFilter    "ALL" or a specific sport name (e.g. "BADMINTON")
     * @param horizonDays    how many days to forecast (3, 7, or 14)
     * @param actualRevenue  total real confirmed revenue if available
     * @param actualBookingCount total real confirmed bookings count if available
     */
    fun computeForecast(
        syntheticData: List<SyntheticBooking>,
        realSummaries: List<RealBookingSummary>,
        sportFilter: String,
        horizonDays: Int,
        actualRevenue: Double = 0.0,
        actualBookingCount: Int = 0
    ): ForecastResult {

        // ── 1. filter by sport ─────────────────────────────────────
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

        // ── 4. historical chart data (smooth baseline, no cliff drop)
        val historical = buildHistoricalData(filteredSynthetic, filteredReal, synW, realW)

        // ── 5. peak demand & trend analysis ─────────────────────────
        val peakDemand = analyzePeakDemand(predictions, historical)

        // ── 6. slot recommendations ────────────────────────────────
        val recommendations = recommendSlots(predictions)

        // ── 7. revenue projection ──────────────────────────────────
        val revenueProjection = projectRevenue(predictions, dataSource)

        // ── 8. data-source label ───────────────────────────────────
        val label = when (dataSource) {
            DataSource.SYNTHETIC_ONLY ->
                "Simulation-led baseline · Real booking history accumulating"
            DataSource.BLENDED ->
                "Blended forecast · Informed by real booking patterns & baseline"
            DataSource.REAL_DOMINANT ->
                "Real-history-informed · Real booking patterns dominant"
        }

        val coverage = if (filteredSynthetic.isEmpty()) 0.0
        else min(1.0, realCount.toDouble() / max(1.0, filteredSynthetic.size * 0.1))

        val resolvedActualRev = if (actualRevenue > 0.0) actualRevenue else filteredReal.sumOf { it.totalRevenue }
        val resolvedActualCount = if (actualBookingCount > 0) actualBookingCount else realCount

        return ForecastResult(
            predictions = predictions,
            recommendations = recommendations,
            peakDemand = peakDemand,
            revenueProjection = revenueProjection,
            historicalData = historical,
            realDataCoverage = coverage,
            dataSourceLabel = label,
            syntheticWeight = synW,
            realWeight = realW,
            actualRevenue = resolvedActualRev,
            actualBookingCount = resolvedActualCount
        )
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Blending
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * Rational sigmoid-like transition: realWeight = realCount / (realCount + K).
     * Returns (syntheticWeight, realWeight) summing exactly to 1.0.
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

                // real booking summary for this sport+slot
                val realEntry = real.firstOrNull {
                    it.sport.equals(sport, ignoreCase = true) &&
                            it.timeSlot == slot
                }

                val capacity = synthetic
                    .firstOrNull { it.sport == sport && it.timeSlot == slot }
                    ?.capacity ?: 4

                val synAvgPrice = synthetic.filter { it.sport == sport && it.timeSlot == slot }
                    .map { it.averagePrice }.averageOrNull() ?: 400.0

                val avgPrice = if (realEntry != null && realEntry.averagePrice > 0)
                    blendValue(synW, synAvgPrice, realW, realEntry.averagePrice)
                else
                    synAvgPrice

                // Build rolling forecast
                val forecastSeries = synSeries.toMutableList()
                for (day in 0 until horizonDays) {
                    val wma = weightedMovingAverage(forecastSeries)

                    // Blend synthetic WMA with real daily demand rate if available
                    val blendedPrediction = if (realEntry != null && realEntry.bookingCount > 0) {
                        val realDailyDemand = realEntry.bookingCount.toDouble() / max(1, realEntry.activeDays)
                        blendValue(synW, wma, realW, realDailyDemand)
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
                            predictedDemand = (predicted * 10).toInt() / 10.0,
                            capacity = capacity,
                            utilization = (utilization * 1000).toInt() / 1000.0,
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
     * Evaluates demand trajectory direction using OLS slope of daily demand totals.
     */
    fun estimateTrend(dailyTotals: List<Double>): TrendDirection {
        if (dailyTotals.size < 2) return TrendDirection.STABLE
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

        // Normalize slope relative to mean
        val normalizedSlope = if (yMean > 0.0) slope / yMean else slope
        return when {
            normalizedSlope > 0.015 -> TrendDirection.INCREASING
            normalizedSlope < -0.015 -> TrendDirection.DECREASING
            else -> TrendDirection.STABLE
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Peak Demand Analysis
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    private fun analyzePeakDemand(
        predictions: List<DemandPrediction>,
        historical: List<HistoricalDataPoint>
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

        // Evaluate trend across the forecast horizon daily totals
        val forecastDailyTotals = predictions
            .groupBy { it.dayOffset }
            .entries.sortedBy { it.key }
            .map { it.value.sumOf { p -> p.predictedDemand } }

        val trend = estimateTrend(forecastDailyTotals)

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
    // Slot Recommendations (Normalized 0–100 scale)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    private fun recommendSlots(predictions: List<DemandPrediction>): List<SlotRecommendation> {
        if (predictions.isEmpty()) return emptyList()

        data class Key(val sport: String, val slot: String)
        val grouped = predictions.groupBy { Key(it.sport, it.timeSlot) }

        val candidates = grouped.map { (key, preds) ->
            val avgUtil = preds.map { it.utilization }.average()
            val avgPrice = preds.map { it.averagePrice }.average()
            Triple(key, avgUtil, avgPrice)
        }

        if (candidates.isEmpty()) return emptyList()

        val maxPrice = candidates.maxOf { it.third }.coerceAtLeast(1.0)
        val minPrice = candidates.minOf { it.third }
        val priceRange = (maxPrice - minPrice).coerceAtLeast(1.0)

        return candidates.map { (key, avgUtil, avgPrice) ->
            // availScore: 100 is best availability (0% utilization)
            val availScore = ((1.0 - avgUtil) * 100.0).coerceIn(0.0, 100.0)
            // affordScore: 100 is most affordable
            val affordScore = ((maxPrice - avgPrice) / priceRange * 100.0).coerceIn(0.0, 100.0)

            // Combined normalized score on 0..100 scale
            val totalScore = (AVAILABILITY_WEIGHT * availScore + AFFORDABILITY_WEIGHT * affordScore).coerceIn(0.0, 100.0)

            val badge = when {
                availScore >= 75.0 && affordScore >= 65.0 -> "Best Value"
                availScore >= 75.0 -> "High Availability"
                affordScore >= 75.0 -> "Budget Friendly"
                avgUtil >= 0.70 -> "Popular Time"
                else -> "Recommended"
            }

            val explanation = buildString {
                append("Recommended because predicted demand is ")
                when {
                    avgUtil < 0.30 -> append("low with great court availability")
                    avgUtil < 0.60 -> append("moderate with steady booking pace")
                    else -> append("steady during this active period")
                }
                if (affordScore > 50) {
                    append(" and the estimated price is comparatively affordable")
                }
                append(".")
            }

            SlotRecommendation(
                sport = key.sport,
                timeSlot = key.slot,
                score = (totalScore * 10).toInt() / 10.0, // e.g. 89.4
                availabilityScore = (availScore * 10).toInt() / 10.0,
                affordabilityScore = (affordScore * 10).toInt() / 10.0,
                estimatedPrice = (avgPrice * 100).toInt() / 100.0,
                predictedUtilization = (avgUtil * 1000).toInt() / 1000.0,
                explanation = explanation,
                recommendationBadge = badge
            )
        }
            .sortedByDescending { it.score }
            .take(4)
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
    // Historical Baseline Data for Charts (No Cliff Drop!)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    private fun buildHistoricalData(
        synthetic: List<SyntheticBooking>,
        real: List<RealBookingSummary>,
        synW: Double,
        realW: Double
    ): List<HistoricalDataPoint> {

        val points = mutableListOf<HistoricalDataPoint>()

        // Take the last 7 days of the historical baseline (Day -7 to Day -1)
        val recentSyntheticDays = synthetic.groupBy { it.dayIndex }
            .entries.sortedBy { it.key }
            .takeLast(7)

        val realTotalBookings = real.sumOf { it.bookingCount }
        val realDailyAvg = if (realTotalBookings > 0) {
            val totalActiveDays = max(1, real.maxOfOrNull { it.activeDays } ?: 1)
            realTotalBookings.toDouble() / totalActiveDays
        } else 0.0

        recentSyntheticDays.forEachIndexed { idx, entry ->
            val synDayTotal = entry.value.sumOf { it.bookingCount.toDouble() }
            val blendedDayTotal = if (realTotalBookings > 0) {
                blendValue(synW, synDayTotal, realW, (synDayTotal * 0.7 + realDailyAvg * 0.3))
            } else {
                synDayTotal
            }

            points.add(
                HistoricalDataPoint(
                    dayIndex = idx - 7,
                    bookingCount = (blendedDayTotal * 10).toInt() / 10.0,
                    source = if (realW > 0.5) DataPointSource.REAL else DataPointSource.SYNTHETIC
                )
            )
        }

        return points
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Helpers
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    private fun blendValue(
        synW: Double, synVal: Double,
        realW: Double, realVal: Double
    ): Double = synW * synVal + realW * realVal

    private fun List<Double>.averageOrNull(): Double? =
        if (isEmpty()) null else average()
}
