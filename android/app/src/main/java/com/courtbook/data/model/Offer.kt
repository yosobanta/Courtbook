package com.courtbook.data.model

import com.google.gson.annotations.SerializedName

data class OfferDto(
    @SerializedName("_id") val id: String,
    @SerializedName("code") val code: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("type") val type: String, // "PERCENTAGE" | "FLAT"
    @SerializedName("value") val value: Double,
    @SerializedName("maxDiscount") val maxDiscount: Double = 0.0,
    @SerializedName("minBookingAmount") val minBookingAmount: Double = 0.0,
    @SerializedName("active") val active: Boolean = true
)

data class ValidateOfferRequest(
    @SerializedName("code") val code: String,
    @SerializedName("courtId") val courtId: String,
    @SerializedName("bookingAmount") val bookingAmount: Double
)

data class ValidatedOfferData(
    @SerializedName("code") val code: String,
    @SerializedName("title") val title: String,
    @SerializedName("originalAmount") val originalAmount: Double,
    @SerializedName("discountAmount") val discountAmount: Double,
    @SerializedName("finalAmount") val finalAmount: Double
)
