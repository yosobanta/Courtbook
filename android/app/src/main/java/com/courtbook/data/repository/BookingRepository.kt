package com.courtbook.data.repository

import com.courtbook.data.model.BookingDto
import com.courtbook.data.model.CreateBookingRequest
import com.courtbook.data.model.OfferDto
import com.courtbook.data.model.ValidateOfferRequest
import com.courtbook.data.model.ValidatedOfferData
import com.courtbook.data.remote.CourtBookApi
import com.courtbook.data.remote.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class BookingRepository(private val api: CourtBookApi = NetworkModule.api) {

  suspend fun createBooking(
      courtId: String,
      slotIds: List<String>,
      offerCode: String? = null,
      paymentMethod: String = "MOCK_UPI"
  ): Result<BookingDto> =
      withContext(Dispatchers.IO) {
        try {
          val response =
              api.createBooking(
                  CreateBookingRequest(
                      courtId = courtId,
                      slotIds = slotIds,
                      offerCode = if (offerCode.isNullOrBlank()) null else offerCode.trim(),
                      paymentMethod = paymentMethod))

          if (response.isSuccessful && response.body()?.data != null) {
            Result.success(response.body()!!.data!!)
          } else {
            val errorRaw = response.errorBody()?.string() ?: ""
            var parsedMsg = "Booking failed"
            try {
              val json = JSONObject(errorRaw)
              if (json.has("message")) {
                parsedMsg = json.getString("message")
              }
            } catch (_: Exception) {
              if (errorRaw.isNotEmpty()) parsedMsg = errorRaw
            }

            if (response.code() == 409) {
              Result.failure(
                  ConflictException(
                      parsedMsg.ifEmpty {
                        "Sorry, one or more selected slots were just booked by another player."
                      }))
            } else {
              Result.failure(Exception(parsedMsg))
            }
          }
        } catch (e: Exception) {
          Result.failure(e)
        }
      }

  suspend fun getMyBookings(): Result<List<BookingDto>> =
      withContext(Dispatchers.IO) {
        try {
          val response = api.getMyBookings()
          if (response.isSuccessful && response.body() != null) {
            Result.success(response.body()!!.data)
          } else {
            Result.failure(
                Exception(response.errorBody()?.string() ?: "Failed to fetch bookings"))
          }
        } catch (e: Exception) {
          Result.failure(e)
        }
      }

  suspend fun getBookingById(id: String): Result<BookingDto> =
      withContext(Dispatchers.IO) {
        try {
          val response = api.getBookingById(id)
          if (response.isSuccessful && response.body()?.data != null) {
            Result.success(response.body()!!.data!!)
          } else {
            Result.failure(
                Exception(response.errorBody()?.string() ?: "Booking details not found"))
          }
        } catch (e: Exception) {
          Result.failure(e)
        }
      }

  suspend fun cancelBooking(id: String, reason: String = "Cancelled by user"): Result<BookingDto> =
      withContext(Dispatchers.IO) {
        try {
          val response = api.cancelBooking(id, mapOf("reason" to reason))
          if (response.isSuccessful && response.body()?.data != null) {
            Result.success(response.body()!!.data!!)
          } else {
            Result.failure(
                Exception(response.errorBody()?.string() ?: "Failed to cancel booking"))
          }
        } catch (e: Exception) {
          Result.failure(e)
        }
      }

  suspend fun validateOffer(
      code: String,
      courtId: String,
      bookingAmount: Double
  ): Result<ValidatedOfferData> =
      withContext(Dispatchers.IO) {
        try {
          val response =
              api.validateOffer(
                  ValidateOfferRequest(
                      code = code.trim().uppercase(),
                      courtId = courtId,
                      bookingAmount = bookingAmount))

          if (response.isSuccessful && response.body()?.data != null) {
            Result.success(response.body()!!.data!!)
          } else {
            val errorRaw = response.errorBody()?.string() ?: ""
            var msg = "Invalid coupon code"
            try {
              val json = JSONObject(errorRaw)
              if (json.has("message")) msg = json.getString("message")
            } catch (_: Exception) {}
            Result.failure(Exception(msg))
          }
        } catch (e: Exception) {
          Result.failure(e)
        }
      }

  suspend fun getActiveOffers(sport: String? = null): Result<List<OfferDto>> =
      withContext(Dispatchers.IO) {
        try {
          val response = api.getActiveOffers(sport)
          if (response.isSuccessful && response.body() != null) {
            Result.success(response.body()!!.data)
          } else {
            Result.failure(
                Exception(response.errorBody()?.string() ?: "Failed to fetch offers"))
          }
        } catch (e: Exception) {
          Result.failure(e)
        }
      }
}

class ConflictException(message: String) : Exception(message)
