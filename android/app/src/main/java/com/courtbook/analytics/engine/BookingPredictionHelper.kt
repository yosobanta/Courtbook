package com.courtbook.analytics.engine

import com.courtbook.data.model.SlotDto

/**
 * Provides advisory booking-time predictive insights directly into the slot selection flow.
 *
 * Rules:
 * - Live availability and slot status are ALWAYS authoritative.
 * - Forecast insights are strictly advisory and never block booking.
 * - Evaluates peak hours, relative demand velocity, and pricing advantages.
 */
object BookingPredictionHelper {

    enum class PredictionTagType {
        POPULAR,
        QUIETER,
        BEST_VALUE,
        FEW_LEFT,
        FILLING_FAST
    }

    data class SlotTag(
        val type: PredictionTagType,
        val label: String
    )

    data class AlternativeSuggestion(
        val busySlot: SlotDto,
        val suggestedSlot: SlotDto,
        val tipTitle: String,
        val tipMessage: String
    )

    /**
     * Context-aware evaluation of a single slot against the court, date, and overall availability.
     */
    fun evaluateSlotTag(
        slot: SlotDto,
        allSlotsForDate: List<SlotDto>,
        courtBasePrice: Double,
        isWeekend: Boolean
    ): SlotTag? {
        // 1. Authoritative unavailable / held states
        if (slot.isHeld && !slot.isHoldExpired()) {
            return SlotTag(PredictionTagType.FILLING_FAST, "Filling fast")
        }

        if (!slot.isAvailable) {
            return null // Booked or blocked slots need no predictive label
        }

        // 2. Authoritative capacity scarcity signal
        val totalAvailable = allSlotsForDate.count { it.isAvailable }
        if (totalAvailable in 1..2 && allSlotsForDate.size >= 6) {
            return SlotTag(PredictionTagType.FEW_LEFT, "Few slots left")
        }

        // 3. Hourly demand pattern check
        val startHour = slot.startTime.split(":").firstOrNull()?.toIntOrNull() ?: 12

        val isPeakHour = when {
            isWeekend -> (startHour in 8..12) || (startHour in 16..21)
            else -> (startHour in 6..8) || (startHour in 17..21)
        }

        return when {
            isPeakHour -> SlotTag(PredictionTagType.POPULAR, "Popular time")
            slot.price <= courtBasePrice && courtBasePrice > 0 -> SlotTag(PredictionTagType.BEST_VALUE, "Best value")
            else -> SlotTag(PredictionTagType.QUIETER, "Quieter time")
        }
    }

    /**
     * If the player selects a popular/peak time slot, find a quieter alternative on the same day.
     */
    fun findQuieterAlternative(
        selectedSlot: SlotDto,
        allSlotsForDate: List<SlotDto>,
        courtBasePrice: Double,
        isWeekend: Boolean
    ): AlternativeSuggestion? {
        val selectedTag = evaluateSlotTag(selectedSlot, allSlotsForDate, courtBasePrice, isWeekend)
        if (selectedTag?.type != PredictionTagType.POPULAR) {
            return null // No alternative needed if not a peak time
        }

        // Find available off-peak slots on the same day
        val candidateAlternatives = allSlotsForDate.filter { candidate ->
            candidate.id != selectedSlot.id && candidate.isAvailable &&
                    evaluateSlotTag(candidate, allSlotsForDate, courtBasePrice, isWeekend)?.type in listOf(
                PredictionTagType.QUIETER,
                PredictionTagType.BEST_VALUE
            )
        }

        val bestAlternative = candidateAlternatives.minByOrNull { it.price } ?: return null

        val priceDifference = (selectedSlot.price - bestAlternative.price).toInt()
        val savingNote = if (priceDifference > 0) " and save ₹$priceDifference" else ""

        return AlternativeSuggestion(
            busySlot = selectedSlot,
            suggestedSlot = bestAlternative,
            tipTitle = "${selectedSlot.timeRange} is expected to be busy",
            tipMessage = "Try ${bestAlternative.timeRange} (₹${bestAlternative.price.toInt()}) for lower predicted crowd$savingNote."
        )
    }
}

