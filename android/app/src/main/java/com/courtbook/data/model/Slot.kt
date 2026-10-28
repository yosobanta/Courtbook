package com.courtbook.data.model

import com.google.gson.annotations.SerializedName

data class SlotDto(
    @SerializedName("_id") val id: String,
    @SerializedName("courtId") val courtId: String,
    @SerializedName("venueId") val venueId: String,
    @SerializedName("date") val date: String,
    @SerializedName("startTime") val startTime: String,
    @SerializedName("endTime") val endTime: String,
    @SerializedName("price") val price: Double,
    @SerializedName("status") val status: String, // "AVAILABLE" | "HELD" | "BOOKED" | "BLOCKED"
    @SerializedName("heldBy") val heldBy: String? = null,
    @SerializedName("holdExpiresAt") val holdExpiresAt: String? = null,
    @SerializedName("blockReason") val blockReason: String? = null
) {
    val isAvailable: Boolean get() = status.equals("AVAILABLE", ignoreCase = true)
    val isBooked: Boolean get() = status.equals("BOOKED", ignoreCase = true)
    val isHeld: Boolean get() = status.equals("HELD", ignoreCase = true)
    val isBlocked: Boolean get() = status.equals("BLOCKED", ignoreCase = true)

    fun isHeldByMe(currentUserId: String?): Boolean {
        if (!isHeld || currentUserId.isNullOrBlank() || heldBy.isNullOrBlank()) return false
        return heldBy.equals(currentUserId, ignoreCase = true)
    }

    fun isHoldExpired(): Boolean {
        if (!isHeld || holdExpiresAt.isNullOrBlank()) return false
        val remaining = getRemainingHoldSeconds()
        return remaining <= 0
    }

    fun getRemainingHoldSeconds(): Long {
        if (!isHeld || holdExpiresAt.isNullOrBlank()) return 0L
        return try {
            val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            val date = format.parse(holdExpiresAt)
            if (date != null) {
                val diff = (date.time - System.currentTimeMillis()) / 1000
                if (diff > 0) diff else 0L
            } else {
                0L
            }
        } catch (e: Exception) {
            try {
                val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }
                val date = format.parse(holdExpiresAt)
                if (date != null) {
                    val diff = (date.time - System.currentTimeMillis()) / 1000
                    if (diff > 0) diff else 0L
                } else {
                    0L
                }
            } catch (e2: Exception) {
                0L
            }
        }
    }

    val timeRange: String get() = "$startTime - $endTime"
}

data class HoldSlotsRequest(
    @SerializedName("slotIds") val slotIds: List<String>,
    @SerializedName("holdMinutes") val holdMinutes: Int = 5
)

data class ReleaseSlotsRequest(
    @SerializedName("slotIds") val slotIds: List<String> = emptyList()
)

data class HoldSlotsResponse(
    @SerializedName("holdExpiresAt") val holdExpiresAt: String,
    @SerializedName("heldSlots") val heldSlots: List<SlotDto>
)

data class BlockSlotsRequest(
    @SerializedName("slotIds") val slotIds: List<String>,
    @SerializedName("blockReason") val blockReason: String = "Maintenance"
)

data class UnblockSlotsRequest(
    @SerializedName("slotIds") val slotIds: List<String>
)
