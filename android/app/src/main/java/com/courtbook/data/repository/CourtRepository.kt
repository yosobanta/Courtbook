package com.courtbook.data.repository

import com.courtbook.data.model.CourtDto
import com.courtbook.data.model.HoldSlotsRequest
import com.courtbook.data.model.HoldSlotsResponse
import com.courtbook.data.model.ReleaseSlotsRequest
import com.courtbook.data.model.SlotDto
import com.courtbook.data.model.VenueDto
import com.courtbook.data.remote.CourtBookApi
import com.courtbook.data.remote.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CourtRepository(private val api: CourtBookApi = NetworkModule.api) {

  suspend fun getCourts(
      sport: String? = null,
      search: String? = null,
      isIndoor: Boolean? = null,
      minPrice: Double? = null,
      maxPrice: Double? = null,
      venueId: String? = null,
      includeInactive: Boolean? = null
  ): Result<List<CourtDto>> =
      withContext(Dispatchers.IO) {
        try {
          val response =
              api.getCourts(
                  sport = if (sport == "ALL") null else sport,
                  venueId = venueId,
                  search = search,
                  isIndoor = isIndoor,
                  minPrice = minPrice,
                  maxPrice = maxPrice,
                  includeInactive = includeInactive)
          if (response.isSuccessful && response.body() != null) {
            Result.success(response.body()!!.data)
          } else {
            Result.failure(Exception(response.errorBody()?.string() ?: "Failed to load courts"))
          }
        } catch (e: Exception) {
          Result.failure(e)
        }
      }

  suspend fun getCourtDetails(courtId: String): Result<CourtDto> =
      withContext(Dispatchers.IO) {
        try {
          val response = api.getCourtById(courtId)
          if (response.isSuccessful && response.body()?.data != null) {
            Result.success(response.body()!!.data!!)
          } else {
            Result.failure(
                Exception(response.errorBody()?.string() ?: "Failed to load court details"))
          }
        } catch (e: Exception) {
          Result.failure(e)
        }
      }

  suspend fun getCourtSlots(courtId: String, date: String): Result<List<SlotDto>> =
      withContext(Dispatchers.IO) {
        try {
          val response = api.getCourtSlots(courtId, date)
          if (response.isSuccessful && response.body() != null) {
            Result.success(response.body()!!.data)
          } else {
            Result.failure(Exception(response.errorBody()?.string() ?: "Failed to load slots"))
          }
        } catch (e: Exception) {
          Result.failure(e)
        }
      }

  suspend fun holdSlots(slotIds: List<String>): Result<HoldSlotsResponse> =
      withContext(Dispatchers.IO) {
        try {
          val response = api.holdSlots(HoldSlotsRequest(slotIds))
          if (response.isSuccessful && response.body()?.data != null) {
            Result.success(response.body()!!.data!!)
          } else {
            val code = response.code()
            val errorMsg = response.errorBody()?.string() ?: "Failed to reserve slot hold"
            Result.failure(Exception("HTTP $code: $errorMsg"))
          }
        } catch (e: Exception) {
          Result.failure(e)
        }
      }

  suspend fun releaseHold(slotIds: List<String> = emptyList()): Result<Boolean> =
      withContext(Dispatchers.IO) {
        try {
          val response = api.releaseHold(ReleaseSlotsRequest(slotIds))
          if (response.isSuccessful) {
            Result.success(true)
          } else {
            Result.failure(Exception(response.errorBody()?.string() ?: "Failed to release held slots"))
          }
        } catch (e: Exception) {
          Result.failure(e)
        }
      }

  suspend fun getNearbyCourts(
      lng: Double = 77.5946,
      lat: Double = 12.9716
  ): Result<List<VenueDto>> =
      withContext(Dispatchers.IO) {
        try {
          val response = api.getNearbyCourts(lng, lat)
          if (response.isSuccessful && response.body() != null) {
            Result.success(response.body()!!.data)
          } else {
            Result.failure(
                Exception(response.errorBody()?.string() ?: "Failed to fetch nearby venues"))
          }
        } catch (e: Exception) {
          Result.failure(e)
        }
      }

  suspend fun getVenues(
      city: String? = null,
      search: String? = null,
      includeInactive: Boolean? = null
  ): Result<List<VenueDto>> =
      withContext(Dispatchers.IO) {
        try {
          val response = api.getVenues(city = city, search = search, includeInactive = includeInactive)
          if (response.isSuccessful && response.body() != null) {
            Result.success(response.body()!!.data)
          } else {
            Result.failure(Exception(response.errorBody()?.string() ?: "Failed to load venues"))
          }
        } catch (e: Exception) {
          Result.failure(e)
        }
      }
}
