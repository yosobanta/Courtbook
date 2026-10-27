package com.courtbook.data.model

import com.google.gson.annotations.SerializedName

data class UserDto(
    @SerializedName("_id") val id: String? = null,
    @SerializedName("id") val alternativeId: String? = null,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("role") val role: String, // "PLAYER" | "ADMIN"
    @SerializedName("isActive") val isActive: Boolean = true
) {
    val effectiveId: String get() = id ?: alternativeId ?: ""
    val isAdmin: Boolean get() = role.equals("ADMIN", ignoreCase = true)
}

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class RegisterRequest(
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("password") val password: String,
    @SerializedName("role") val role: String = "PLAYER"
)

data class AuthResponseData(
    @SerializedName("token") val token: String,
    @SerializedName("user") val user: UserDto
)
