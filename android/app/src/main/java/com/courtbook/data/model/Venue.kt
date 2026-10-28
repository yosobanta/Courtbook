package com.courtbook.data.model

import com.google.gson.annotations.SerializedName

data class VenueDto(
    @SerializedName("_id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String = "",
    @SerializedName("address") val address: String,
    @SerializedName("city") val city: String,
    @SerializedName("location") val location: LocationDto? = null,
    @SerializedName("amenities") val amenities: List<String> = emptyList(),
    @SerializedName("operatingHours") val operatingHours: OperatingHoursDto? = null,
    @SerializedName("images") val images: List<String> = emptyList(),
    @SerializedName("rating") val rating: RatingDto? = null,
    @SerializedName("distanceKm") val distanceKm: Double? = null,
    @SerializedName("courts") val courts: List<CourtDto> = emptyList(),
    @SerializedName("isActive") val isActive: Boolean = true
)

data class LocationDto(
    @SerializedName("type") val type: String = "Point",
    @SerializedName("coordinates") val coordinates: List<Double> = emptyList() // [lng, lat]
) {
    val longitude: Double get() = coordinates.getOrNull(0) ?: 0.0
    val latitude: Double get() = coordinates.getOrNull(1) ?: 0.0
}

data class OperatingHoursDto(
    @SerializedName("open") val open: String = "06:00",
    @SerializedName("close") val close: String = "23:00"
)

data class RatingDto(
    @SerializedName("average") val average: Double = 4.5,
    @SerializedName("count") val count: Int = 0
)

data class CreateVenueRequest(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String = "",
    @SerializedName("address") val address: String,
    @SerializedName("city") val city: String,
    @SerializedName("amenities") val amenities: List<String> = listOf("PARKING", "DRINKING_WATER", "RESTROOM"),
    @SerializedName("images") val images: List<String> = emptyList(),
    @SerializedName("operatingHours") val operatingHours: OperatingHoursDto = OperatingHoursDto("06:00", "23:00"),
    @SerializedName("sport") val sport: String? = null,
    @SerializedName("basePrice") val basePrice: Double? = null
)

data class UploadImageRequest(
    @SerializedName("image") val image: String? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("filename") val filename: String? = null
)

data class UploadImageResponse(
    @SerializedName("url") val url: String,
    @SerializedName("path") val path: String? = null
)
