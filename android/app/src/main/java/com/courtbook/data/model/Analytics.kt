package com.courtbook.data.model

import com.google.gson.annotations.SerializedName

data class DashboardSummaryDto(
    @SerializedName("kpi") val kpi: KpiDto,
    @SerializedName("recentBookings") val recentBookings: List<BookingDto> = emptyList()
)

data class KpiDto(
    @SerializedName("totalUsers") val totalUsers: Int = 0,
    @SerializedName("totalVenues") val totalVenues: Int = 0,
    @SerializedName("totalCourts") val totalCourts: Int = 0,
    @SerializedName("todayBookings") val todayBookings: Int = 0,
    @SerializedName("todayRevenue") val todayRevenue: Double = 0.0,
    @SerializedName("monthlyRevenue") val monthlyRevenue: Double = 0.0,
    @SerializedName("monthlyBookings") val monthlyBookings: Int = 0
)

data class RevenueByVenueDto(
    @SerializedName("venueId") val venueId: String,
    @SerializedName("venueName") val venueName: String,
    @SerializedName("city") val city: String,
    @SerializedName("totalRevenue") val totalRevenue: Double,
    @SerializedName("totalBookings") val totalBookings: Int,
    @SerializedName("averageBookingValue") val averageBookingValue: Double
)

data class PopularSportDto(
    @SerializedName("sport") val sport: String,
    @SerializedName("bookingCount") val bookingCount: Int,
    @SerializedName("totalRevenue") val totalRevenue: Double
)
