package com.courtbook.data.model

import com.google.gson.annotations.SerializedName

data class CourtDto(
    @SerializedName("_id") val id: String,
    @SerializedName("venueId") val venue: Any? = null, // Can be String ID or populated VenueDto
    @SerializedName("name") val name: String,
    @SerializedName("sport") val sport: String,
    @SerializedName("isIndoor") val isIndoor: Boolean = true,
    @SerializedName("surfaceType") val surfaceType: String = "SYNTHETIC",
    @SerializedName("capacity") val capacity: Int = 4,
    @SerializedName("basePrice") val basePrice: Double,
    @SerializedName("pricing") val pricing: PricingDto? = null,
    @SerializedName("images") val images: List<String> = emptyList(),
    @SerializedName("isActive") val isActive: Boolean = true
) {
    val venueName: String
        get() {
            if (venue is Map<*, *>) {
                return venue["name"] as? String ?: "Sports Complex"
            }
            return "Sports Complex"
        }

    val venueAddress: String
        get() {
            if (venue is Map<*, *>) {
                val addr = venue["address"] as? String ?: ""
                val city = venue["city"] as? String ?: ""
                return if (city.isNotEmpty()) "$addr, $city" else addr
            }
            return ""
        }
}

data class PricingDto(
    @SerializedName("weekday") val weekday: Double = 400.0,
    @SerializedName("weekend") val weekend: Double = 500.0,
    @SerializedName("peak") val peak: Double = 550.0
)

data class CreateCourtRequest(
    @SerializedName("venueId") val venueId: String,
    @SerializedName("name") val name: String,
    @SerializedName("sport") val sport: String,
    @SerializedName("isIndoor") val isIndoor: Boolean = true,
    @SerializedName("surfaceType") val surfaceType: String = "SYNTHETIC",
    @SerializedName("capacity") val capacity: Int = 4,
    @SerializedName("basePrice") val basePrice: Double,
    @SerializedName("pricing") val pricing: PricingDto? = null,
    @SerializedName("images") val images: List<String> = emptyList()
)

data class UpdateCourtRequest(
    @SerializedName("name") val name: String? = null,
    @SerializedName("sport") val sport: String? = null,
    @SerializedName("basePrice") val basePrice: Double? = null,
    @SerializedName("pricing") val pricing: PricingDto? = null,
    @SerializedName("isIndoor") val isIndoor: Boolean? = null,
    @SerializedName("surfaceType") val surfaceType: String? = null,
    @SerializedName("capacity") val capacity: Int? = null,
    @SerializedName("isActive") val isActive: Boolean? = null
)

data class GenerateSlotsRequest(
    @SerializedName("startDate") val startDate: String,
    @SerializedName("endDate") val endDate: String,
    @SerializedName("startHour") val startHour: Int = 6,
    @SerializedName("endHour") val endHour: Int = 23,
    @SerializedName("slotDurationMinutes") val slotDurationMinutes: Int = 60,
    @SerializedName("price") val price: Double? = null
)
