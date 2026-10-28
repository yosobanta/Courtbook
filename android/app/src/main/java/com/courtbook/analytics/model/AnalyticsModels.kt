package com.courtbook.analytics.model

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

/** Direction of a demand trend estimated via simple linear regression. */
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
    val score: Double,
    val availabilityScore: Double,
    val affordabilityScore: Double,
    val estimatedPrice: Double,
    val predictedUtilization: Double,
    val explanation: String
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
    val realWeight: Double
)

// ── Real booking summary (extracted from existing API responses) ─────

data class RealBookingSummary(
    val sport: String,
    val timeSlot: String,
    val bookingCount: Int,
    val totalRevenue: Double,
    val averagePrice: Double
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
    val errorMessage: String? = null,
    val realDataAvailable: Boolean = false
)
