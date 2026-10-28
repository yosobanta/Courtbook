package com.courtbook.analytics.model

import java.util.Locale
import kotlin.math.max

/**
 * Utility for formatting Indian Rupee currency following the Indian numbering system.
 * Examples: ₹43,961, ₹1,76,110, ₹4,34,109 or compact ₹1.76L, ₹4.34L.
 */
object CurrencyFormatter {
    fun formatInr(amount: Double, compact: Boolean = false): String {
        val longVal = amount.toLong()
        if (compact) {
            return when {
                longVal >= 10_000_000 -> String.format(Locale.getDefault(), "₹%.2fCr", amount / 10_000_000.0)
                longVal >= 100_000 -> String.format(Locale.getDefault(), "₹%.2fL", amount / 100_000.0)
                longVal >= 1_000 -> String.format(Locale.getDefault(), "₹%.1fk", amount / 1_000.0)
                else -> "₹$longVal"
            }
        }

        val str = longVal.toString()
        if (str.length <= 3) return "₹$str"

        val lastThree = str.takeLast(3)
        val remaining = str.dropLast(3)

        val parts = mutableListOf<String>()
        var i = remaining.length
        while (i > 0) {
            val start = max(0, i - 2)
            parts.add(0, remaining.substring(start, i))
            i -= 2
        }
        return "₹" + parts.joinToString(",") + "," + lastThree
    }
}

/**
 * A single synthetic booking observation — entirely separate from production BookingDto.
 * Never sent to the backend or stored in MongoDB.
 */
data class SyntheticBooking(
    val dayIndex: Int,
    val sport: String,
    val courtCategory: String,
    val timeSlot: String,
    val bookingCount: Int,
    val capacity: Int,
    val averagePrice: Double,
    val isWeekend: Boolean
)

/** Source transparency for forecasts. */
enum class DataSource {
    SYNTHETIC_ONLY,
    BLENDED,
    REAL_DOMINANT
}

/** Direction of a demand trend estimated via regression over forecast horizon. */
enum class TrendDirection {
    INCREASING, DECREASING, STABLE
}

/** Origin of a historical data point on the forecast chart. */
enum class DataPointSource {
    SYNTHETIC, REAL, PREDICTED
}

// ── Forecast outputs ────────────────────────────────────────────────────

data class DemandPrediction(
    val dayOffset: Int,
    val sport: String,
    val timeSlot: String,
    val predictedDemand: Double,
    val capacity: Int,
    val utilization: Double,
    val averagePrice: Double,
    val dataSource: DataSource
)

data class SlotRecommendation(
    val sport: String,
    val timeSlot: String,
    val score: Double, // 0.0 to 100.0 percentage score
    val availabilityScore: Double,
    val affordabilityScore: Double,
    val estimatedPrice: Double,
    val predictedUtilization: Double,
    val explanation: String,
    val recommendationBadge: String = "Best Value"
)

data class PeakDemandInfo(
    val busiestSport: String,
    val busiestTimeSlot: String,
    val leastBusyTimeSlot: String,
    val peakDaySport: String,
    val peakDayOffset: Int,
    val demandTrend: TrendDirection
)

data class DailyRevenue(
    val dayOffset: Int,
    val revenue: Double
)

data class RevenueProjection(
    val totalProjectedRevenue: Double,
    val dailyProjections: List<DailyRevenue>,
    val bySport: Map<String, Double>,
    val dataSource: DataSource
)

data class HistoricalDataPoint(
    val dayIndex: Int,
    val bookingCount: Double,
    val source: DataPointSource
)

/** The combined output of the analytics engine — everything the UI needs. */
data class ForecastResult(
    val predictions: List<DemandPrediction>,
    val recommendations: List<SlotRecommendation>,
    val peakDemand: PeakDemandInfo,
    val revenueProjection: RevenueProjection,
    val historicalData: List<HistoricalDataPoint>,
    val realDataCoverage: Double,
    val dataSourceLabel: String,
    val syntheticWeight: Double,
    val realWeight: Double,
    val actualRevenue: Double = 0.0,
    val actualBookingCount: Int = 0
)

// ── Real booking summary (extracted from existing API responses) ─────

data class RealBookingSummary(
    val sport: String,
    val timeSlot: String,
    val bookingCount: Int,
    val totalRevenue: Double,
    val averagePrice: Double,
    val activeDays: Int = 1
)

// ── UI State ─────────────────────────────────────────────────────────

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val forecast: ForecastResult? = null,
    val selectedSport: String = "ALL",
    val forecastHorizon: Int = 7,
    val availableSports: List<String> = emptyList(),
    val simulatedBookingsCount: Int = 0,
    val isAdmin: Boolean = false,
    val realBookingCount: Int = 0,
    val actualTotalBookings: Int? = null,
    val actualTotalRevenue: Double = 0.0,
    val errorMessage: String? = null,
    val realDataAvailable: Boolean = false
)
