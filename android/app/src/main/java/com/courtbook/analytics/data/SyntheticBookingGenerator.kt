package com.courtbook.analytics.data

import com.courtbook.analytics.model.SyntheticBooking
import kotlin.random.Random

/**
 * Generates a deterministic 75-day synthetic booking dataset.
 *
 * Uses a fixed seed so the same initial conditions always produce
 * the same output — no surprise changes across recompositions.
 *
 * Patterns baked in:
 *  • Evening badminton sessions have stronger demand.
 *  • Weekend football sessions are busier than weekday ones.
 *  • Morning slots have generally lower demand.
 *  • Different sports carry different price ranges and capacities.
 */
object SyntheticBookingGenerator {

    private const val SEED = 42L
    private const val TOTAL_DAYS = 75

    val SPORTS = listOf("BADMINTON", "BASKETBALL", "TENNIS", "FOOTBALL")

    val TIME_SLOTS = listOf(
        "06:00-08:00",
        "08:00-10:00",
        "10:00-12:00",
        "12:00-14:00",
        "14:00-16:00",
        "16:00-18:00",
        "18:00-20:00",
        "20:00-22:00"
    )

    private data class SportConfig(
        val courtCategories: List<String>,
        val capacity: Int,
        val basePriceRange: ClosedFloatingPointRange<Double>,
        val weekendMultiplier: Double,
        val peakSlotIndices: Set<Int>
    )

    private val sportConfigs = mapOf(
        "BADMINTON" to SportConfig(
            courtCategories = listOf("Indoor Court A", "Indoor Court B"),
            capacity = 4,
            basePriceRange = 350.0..550.0,
            weekendMultiplier = 1.3,
            peakSlotIndices = setOf(5, 6, 7)   // 16:00–22:00
        ),
        "BASKETBALL" to SportConfig(
            courtCategories = listOf("Open Court 1"),
            capacity = 10,
            basePriceRange = 500.0..800.0,
            weekendMultiplier = 1.4,
            peakSlotIndices = setOf(4, 5, 6)    // 14:00–20:00
        ),
        "TENNIS" to SportConfig(
            courtCategories = listOf("Clay Court 1", "Hard Court 1"),
            capacity = 4,
            basePriceRange = 400.0..700.0,
            weekendMultiplier = 1.25,
            peakSlotIndices = setOf(1, 2, 5, 6) // morning + evening
        ),
        "FOOTBALL" to SportConfig(
            courtCategories = listOf("Turf Ground A"),
            capacity = 14,
            basePriceRange = 800.0..1500.0,
            weekendMultiplier = 1.5,
            peakSlotIndices = setOf(5, 6, 7)    // 16:00–22:00
        )
    )

    /**
     * Generate the full synthetic dataset.  Deterministic — safe to call
     * multiple times and always get the same result for a given seed.
     */
    fun generate(): List<SyntheticBooking> {
        val rng = Random(SEED)
        val bookings = mutableListOf<SyntheticBooking>()

        for (day in 0 until TOTAL_DAYS) {
            val isWeekend = (day % 7) >= 5  // Sat + Sun

            for (sport in SPORTS) {
                val cfg = sportConfigs[sport] ?: continue
                val court = cfg.courtCategories[day % cfg.courtCategories.size]

                for ((slotIdx, slot) in TIME_SLOTS.withIndex()) {
                    val baseOccupancy = baseOccupancyRate(slotIdx, isWeekend, cfg)

                    // Add gentle day-over-day trend (slight growth)
                    val trendFactor = 1.0 + (day.toDouble() / TOTAL_DAYS) * 0.15

                    // Random jitter ±15 %
                    val jitter = 1.0 + (rng.nextDouble() - 0.5) * 0.30

                    val occupancy = (baseOccupancy * trendFactor * jitter).coerceIn(0.0, 1.0)
                    val bookingCount = (occupancy * cfg.capacity).toInt().coerceIn(0, cfg.capacity)

                    val price = computePrice(cfg, isWeekend, slotIdx, rng)

                    bookings.add(
                        SyntheticBooking(
                            dayIndex = day,
                            sport = sport,
                            courtCategory = court,
                            timeSlot = slot,
                            bookingCount = bookingCount,
                            capacity = cfg.capacity,
                            averagePrice = price,
                            isWeekend = isWeekend
                        )
                    )
                }
            }
        }
        return bookings
    }

    /**
     * Add extra simulated observations onto the existing dataset.
     * Returns a *new* list (does not mutate the original).
     */
    fun addSimulatedBookings(
        existing: List<SyntheticBooking>,
        count: Int,
        seed: Long
    ): List<SyntheticBooking> {
        val rng = Random(seed)
        val maxDay = existing.maxOfOrNull { it.dayIndex } ?: (TOTAL_DAYS - 1)
        val extras = mutableListOf<SyntheticBooking>()

        repeat(count) {
            val sport = SPORTS[rng.nextInt(SPORTS.size)]
            val cfg = sportConfigs[sport] ?: return@repeat
            val slot = TIME_SLOTS[rng.nextInt(TIME_SLOTS.size)]
            val day = maxDay + 1 + (it / (SPORTS.size * TIME_SLOTS.size))
            val isWeekend = (day % 7) >= 5

            extras.add(
                SyntheticBooking(
                    dayIndex = day,
                    sport = sport,
                    courtCategory = cfg.courtCategories.first(),
                    timeSlot = slot,
                    bookingCount = rng.nextInt(1, cfg.capacity + 1),
                    capacity = cfg.capacity,
                    averagePrice = computePrice(cfg, isWeekend, TIME_SLOTS.indexOf(slot), rng),
                    isWeekend = isWeekend
                )
            )
        }
        return existing + extras
    }

    // ── private helpers ──────────────────────────────────────────────

    private fun baseOccupancyRate(
        slotIdx: Int,
        isWeekend: Boolean,
        cfg: SportConfig
    ): Double {
        val isPeak = slotIdx in cfg.peakSlotIndices

        return when {
            isWeekend && isPeak -> 0.80
            isWeekend && !isPeak -> 0.50
            !isWeekend && isPeak -> 0.60
            else -> 0.25 // weekday off-peak
        }
    }

    private fun computePrice(
        cfg: SportConfig,
        isWeekend: Boolean,
        slotIdx: Int,
        rng: Random
    ): Double {
        val base = cfg.basePriceRange.start +
                rng.nextDouble() * (cfg.basePriceRange.endInclusive - cfg.basePriceRange.start)
        val weekendBonus = if (isWeekend) cfg.weekendMultiplier else 1.0
        val peakBonus = if (slotIdx in cfg.peakSlotIndices) 1.15 else 1.0
        return (base * weekendBonus * peakBonus).let { (it * 100).toInt() / 100.0 }
    }
}
