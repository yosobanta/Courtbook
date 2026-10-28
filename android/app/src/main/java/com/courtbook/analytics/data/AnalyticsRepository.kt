package com.courtbook.analytics.data

import com.courtbook.analytics.model.RealBookingSummary
import com.courtbook.analytics.model.SyntheticBooking
import com.courtbook.data.model.BookingDto
import com.courtbook.data.remote.CourtBookApi
import com.courtbook.data.remote.NetworkModule
import com.courtbook.data.remote.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Coordinates synthetic baseline and real booking retrieval.
 * Provides resilient cold-start fallback when real data is unavailable.
 */
class AnalyticsRepository(
    private val api: CourtBookApi = NetworkModule.api
) {
    private var baseSyntheticData: List<SyntheticBooking>? = null

    /**
     * Returns baseline deterministic synthetic booking observations.
     */
    fun getSyntheticBookings(): List<SyntheticBooking> {
        return baseSyntheticData ?: run {
            SyntheticBookingGenerator.generate().also { baseSyntheticData = it }
        }
    }

    /**
     * Generates simulated extra bookings on top of existing synthetic baseline.
     */
    fun getSimulatedBookings(additionalCount: Int, seed: Long = 12345L): List<SyntheticBooking> {
        val base = getSyntheticBookings()
        if (additionalCount <= 0) return base
        return SyntheticBookingGenerator.addSimulatedBookings(base, additionalCount, seed)
    }

    /**
     * Fetches real booking summaries asynchronously.
     * Returns a pair of (List<RealBookingSummary>, totalCount).
     * Returns empty list gracefully if network fails or user is offline.
     */
    suspend fun getRealBookingSummaries(): Pair<List<RealBookingSummary>, Int> = withContext(Dispatchers.IO) {
        try {
            val isAdmin = TokenManager.isAdmin()
            val bookings: List<BookingDto> = if (isAdmin) {
                val response = api.getAdminBookings()
                if (response.isSuccessful && response.body()?.data != null) {
                    response.body()!!.data!!
                } else {
                    emptyList()
                }
            } else {
                val response = api.getMyBookings()
                if (response.isSuccessful && response.body()?.data != null) {
                    response.body()!!.data
                } else {
                    emptyList()
                }
            }

            val confirmedBookings = bookings.filter { it.isConfirmed }
            val summaries = aggregateBookings(confirmedBookings)
            Pair(summaries, confirmedBookings.size)
        } catch (_: Exception) {
            Pair(emptyList(), 0)
        }
    }

    private fun aggregateBookings(bookings: List<BookingDto>): List<RealBookingSummary> {
        if (bookings.isEmpty()) return emptyList()

        return bookings.groupBy { booking ->
            val sport = booking.sport.uppercase()
            val slot = mapToTimeSlot(booking.startTime, booking.endTime)
            Pair(sport, slot)
        }.map { (key, group) ->
            val count = group.size
            val totalRev = group.sumOf { it.finalAmount }
            val avgPrice = if (count > 0) totalRev / count else 0.0
            RealBookingSummary(
                sport = key.first,
                timeSlot = key.second,
                bookingCount = count,
                totalRevenue = totalRev,
                averagePrice = avgPrice
            )
        }
    }

    private fun mapToTimeSlot(startTime: String, endTime: String): String {
        val hour = startTime.split(":").firstOrNull()?.toIntOrNull() ?: 18
        return when {
            hour < 8 -> "06:00-08:00"
            hour < 10 -> "08:00-10:00"
            hour < 12 -> "10:00-12:00"
            hour < 14 -> "12:00-14:00"
            hour < 16 -> "14:00-16:00"
            hour < 18 -> "16:00-18:00"
            hour < 20 -> "18:00-20:00"
            else -> "20:00-22:00"
        }
    }
}
