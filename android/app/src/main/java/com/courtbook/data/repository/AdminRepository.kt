package com.courtbook.data.repository

import com.courtbook.data.model.BlockSlotsRequest
import com.courtbook.data.model.BookingDto
import com.courtbook.data.model.DashboardSummaryDto
import com.courtbook.data.model.PopularSportDto
import com.courtbook.data.model.RevenueByVenueDto
import com.courtbook.data.model.UserDto
import com.courtbook.data.remote.CourtBookApi
import com.courtbook.data.remote.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AdminRepository(private val api: CourtBookApi = NetworkModule.api) {

  suspend fun getDashboard(): Result<DashboardSummaryDto> =
      withContext(Dispatchers.IO) {
        try {
          val response = api.getAdminDashboard()
          if (response.isSuccessful && response.body()?.data != null) {
            Result.success(response.body()!!.data!!)
          } else {
            Result.failure(
                Exception(response.errorBody()?.string() ?: "Failed to load admin dashboard"))
          }
        } catch (e: Exception) {
          Result.failure(e)
        }
      }

  suspend fun getAllBookings(date: String? = null, status: String? = null): Result<List<BookingDto>> =
      withContext(Dispatchers.IO) {
        try {
          val response = api.getAdminBookings(date, status)
          if (response.isSuccessful && response.body()?.data != null) {
            Result.success(response.body()!!.data!!)
          } else {
            Result.failure(
                Exception(response.errorBody()?.string() ?: "Failed to load bookings list"))
          }
        } catch (e: Exception) {
          Result.failure(e)
        }
      }

  suspend fun blockSlots(slotIds: List<String>, reason: String): Result<Int> =
      withContext(Dispatchers.IO) {
        try {
          val response = api.blockSlots(BlockSlotsRequest(slotIds, reason))
          if (response.isSuccessful && response.body()?.data != null) {
            val count = response.body()!!.data?.get("modifiedCount") ?: slotIds.size
            Result.success(count)
          } else {
            Result.failure(Exception(response.errorBody()?.string() ?: "Failed to block slots"))
          }
        } catch (e: Exception) {
          Result.failure(e)
        }
      }

  suspend fun unblockSlots(slotIds: List<String>): Result<Int> =
      withContext(Dispatchers.IO) {
        try {
          val response = api.unblockSlots(mapOf("slotIds" to slotIds))
          if (response.isSuccessful && response.body()?.data != null) {
            val count = response.body()!!.data?.get("modifiedCount") ?: slotIds.size
            Result.success(count)
          } else {
            Result.failure(Exception(response.errorBody()?.string() ?: "Failed to unblock slots"))
          }
        } catch (e: Exception) {
          Result.failure(e)
        }
      }

  suspend fun getRevenueAnalytics(): Result<List<RevenueByVenueDto>> =
      withContext(Dispatchers.IO) {
        try {
          val response = api.getRevenueAnalytics()
          if (response.isSuccessful && response.body()?.data != null) {
            Result.success(response.body()!!.data!!)
          } else {
            Result.failure(
                Exception(response.errorBody()?.string() ?: "Failed to load revenue analytics"))
          }
        } catch (e: Exception) {
          Result.failure(e)
        }
      }

  suspend fun getPopularSportsAnalytics(): Result<List<PopularSportDto>> =
      withContext(Dispatchers.IO) {
        try {
          val response = api.getPopularSportsAnalytics()
          if (response.isSuccessful && response.body()?.data != null) {
            Result.success(response.body()!!.data!!)
          } else {
            Result.failure(
                Exception(response.errorBody()?.string() ?: "Failed to load sports analytics"))
          }
        } catch (e: Exception) {
          Result.failure(e)
        }
      }

  suspend fun getAllUsers(): Result<List<UserDto>> =
      withContext(Dispatchers.IO) {
        try {
          val response = api.getAllUsers()
          if (response.isSuccessful && response.body() != null) {
            Result.success(response.body()!!.data)
          } else {
            Result.failure(Exception(response.errorBody()?.string() ?: "Failed to load users"))
          }
        } catch (e: Exception) {
          Result.failure(e)
        }
      }
}
