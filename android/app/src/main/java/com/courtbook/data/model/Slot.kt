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
    @SerializedName("blockReason") val blockReason: String? = null
) {
    val isAvailable: Boolean get() = status.equals("AVAILABLE", ignoreCase = true)
    val isBooked: Boolean get() = status.equals("BOOKED", ignoreCase = true)
    val isHeld: Boolean get() = status.equals("HELD", ignoreCase = true)
    val isBlocked: Boolean get() = status.equals("BLOCKED", ignoreCase = true)

    val timeRange: String get() = "$startTime - $endTime"
}

data class HoldSlotsRequest(
    @SerializedName("slotIds") val slotIds: List<String>,
    @SerializedName("holdMinutes") val holdMinutes: Int = 5
)

data class HoldSlotsResponse(
    @SerializedName("holdExpiresAt") val holdExpiresAt: String,
    @SerializedName("heldSlots") val heldSlots: List<SlotDto>
)

data class BlockSlotsRequest(
    @SerializedName("slotIds") val slotIds: List<String>,
    @SerializedName("blockReason") val blockReason: String = "Maintenance"
)
