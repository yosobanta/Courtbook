package com.courtbook.data.model

import com.google.gson.annotations.SerializedName

data class BookingDto(
    @SerializedName("_id") val id: String,
    @SerializedName("bookingNumber") val bookingNumber: String,
    @SerializedName("userId") val userId: Any? = null,
    @SerializedName("courtId") val court: Any? = null,
    @SerializedName("venueId") val venue: Any? = null,
    @SerializedName("slotIds") val slotIds: Any? = null,
    @SerializedName("date") val date: String,
    @SerializedName("startTime") val startTime: String,
    @SerializedName("endTime") val endTime: String,
    @SerializedName("originalAmount") val originalAmount: Double,
    @SerializedName("discountAmount") val discountAmount: Double = 0.0,
    @SerializedName("finalAmount") val finalAmount: Double,
    @SerializedName("offerCode") val offerCode: String? = null,
    @SerializedName("status") val status: String, // "CONFIRMED" | "CANCELLED" | "PENDING"
    @SerializedName("paymentStatus") val paymentStatus: String, // "PAID" | "REFUNDED"
    @SerializedName("createdAt") val createdAt: String? = null
) {
    val courtName: String
        get() {
            if (court is Map<*, *>) {
                return court["name"] as? String ?: "Sports Court"
            }
            return "Sports Court"
        }

    val sport: String
        get() {
            if (court is Map<*, *>) {
                return court["sport"] as? String ?: "SPORTS"
            }
            return "SPORTS"
        }

    val venueName: String
        get() {
            if (venue is Map<*, *>) {
                return venue["name"] as? String ?: "Sports Venue"
            }
            return "Sports Venue"
        }

    val isConfirmed: Boolean get() = status.equals("CONFIRMED", ignoreCase = true)
    val isCancelled: Boolean get() = status.equals("CANCELLED", ignoreCase = true)

    val userName: String
        get() {
            if (userId is Map<*, *>) {
                return (userId["name"] as? String) ?: "Customer"
            }
            return "Customer"
        }

    val userEmail: String
        get() {
            if (userId is Map<*, *>) {
                return (userId["email"] as? String) ?: ""
            }
            return ""
        }

    val userPhone: String
        get() {
            if (userId is Map<*, *>) {
                return (userId["phone"] as? String) ?: ""
            }
            return ""
        }
}

data class CreateBookingRequest(
    @SerializedName("courtId") val courtId: String,
    @SerializedName("slotIds") val slotIds: List<String>,
    @SerializedName("offerCode") val offerCode: String? = null,
    @SerializedName("paymentMethod") val paymentMethod: String = "MOCK_UPI"
)
