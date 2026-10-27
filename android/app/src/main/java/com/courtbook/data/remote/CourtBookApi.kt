package com.courtbook.data.remote

import com.courtbook.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface CourtBookApi {

  // --- AUTHENTICATION ---
  @POST("auth/register")
  suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<AuthResponseData>>

  @POST("auth/login")
  suspend fun login(@Body request: LoginRequest): Response<ApiResponse<AuthResponseData>>

  @GET("auth/me")
  suspend fun getMe(): Response<ApiResponse<Map<String, UserDto>>>

  // --- VENUES & COURTS ---
  @GET("venues")
  suspend fun getVenues(
      @Query("city") city: String? = null,
      @Query("search") search: String? = null
  ): Response<ApiListResponse<VenueDto>>

  @GET("venues/{id}")
  suspend fun getVenueById(@Path("id") id: String): Response<ApiResponse<VenueDto>>

  @GET("courts")
  suspend fun getCourts(
      @Query("sport") sport: String? = null,
      @Query("venueId") venueId: String? = null,
      @Query("isIndoor") isIndoor: Boolean? = null,
      @Query("minPrice") minPrice: Double? = null,
      @Query("maxPrice") maxPrice: Double? = null,
      @Query("search") search: String? = null
  ): Response<ApiListResponse<CourtDto>>

  @GET("courts/{id}")
  suspend fun getCourtById(@Path("id") id: String): Response<ApiResponse<CourtDto>>

  @GET("courts/nearby")
  suspend fun getNearbyCourts(
      @Query("lng") lng: Double,
      @Query("lat") lat: Double,
      @Query("maxDistanceMeters") maxDistanceMeters: Int = 15000
  ): Response<ApiListResponse<VenueDto>>

  // --- SLOTS & AVAILABILITY ---
  @GET("courts/{id}/slots")
  suspend fun getCourtSlots(
      @Path("id") courtId: String,
      @Query("date") date: String
  ): Response<ApiListResponse<SlotDto>>

  @POST("slots/hold")
  suspend fun holdSlots(@Body request: HoldSlotsRequest): Response<ApiResponse<HoldSlotsResponse>>

  // --- BOOKINGS ---
  @POST("bookings")
  suspend fun createBooking(@Body request: CreateBookingRequest): Response<ApiResponse<BookingDto>>

  @GET("bookings/my")
  suspend fun getMyBookings(): Response<ApiListResponse<BookingDto>>

  @GET("bookings/{id}")
  suspend fun getBookingById(@Path("id") id: String): Response<ApiResponse<BookingDto>>

  @POST("bookings/{id}/cancel")
  suspend fun cancelBooking(
      @Path("id") id: String,
      @Body body: Map<String, String> = emptyMap()
  ): Response<ApiResponse<BookingDto>>

  // --- OFFERS ---
  @GET("offers/active")
  suspend fun getActiveOffers(@Query("sport") sport: String? = null): Response<ApiListResponse<OfferDto>>

  @POST("offers/validate")
  suspend fun validateOffer(@Body request: ValidateOfferRequest): Response<ApiResponse<ValidatedOfferData>>

  // --- ADMIN ---
  @GET("admin/dashboard")
  suspend fun getAdminDashboard(): Response<ApiResponse<DashboardSummaryDto>>

  @GET("admin/bookings")
  suspend fun getAdminBookings(
      @Query("date") date: String? = null,
      @Query("status") status: String? = null
  ): Response<ApiResponse<List<BookingDto>>>

  @POST("admin/slots/block")
  suspend fun blockSlots(@Body request: BlockSlotsRequest): Response<ApiResponse<Map<String, Int>>>

  @POST("admin/slots/unblock")
  suspend fun unblockSlots(@Body body: Map<String, List<String>>): Response<ApiResponse<Map<String, Int>>>

  @GET("admin/analytics/revenue")
  suspend fun getRevenueAnalytics(): Response<ApiResponse<List<RevenueByVenueDto>>>

  @GET("admin/analytics/popular-sports")
  suspend fun getPopularSportsAnalytics(): Response<ApiResponse<List<PopularSportDto>>>

  @GET("admin/users")
  suspend fun getAllUsers(): Response<ApiListResponse<UserDto>>
}
