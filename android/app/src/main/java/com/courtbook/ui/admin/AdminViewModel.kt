package com.courtbook.ui.admin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.courtbook.data.model.*
import com.courtbook.data.remote.NetworkModule
import com.courtbook.data.repository.AdminRepository
import com.courtbook.data.repository.CourtRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminUiState(
    val isLoading: Boolean = true,
    val isActionInProgress: Boolean = false,
    val summary: DashboardSummaryDto? = null,
    val revenueAnalytics: List<RevenueByVenueDto> = emptyList(),
    val popularSports: List<PopularSportDto> = emptyList(),
    val venues: List<VenueDto> = emptyList(),
    val courts: List<CourtDto> = emptyList(),
    val allBookings: List<BookingDto> = emptyList(),
    val bookingStatusFilter: String = "ALL", // "ALL", "CONFIRMED", "CANCELLED"
    val selectedCourtId: String? = null,
    val selectedDate: String = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
    val slots: List<SlotDto> = emptyList(),
    val isModifyingSlot: Boolean = false,
    val activeBaseUrl: String = NetworkModule.getBaseUrl(),
    val successMessage: String? = null,
    val errorMessage: String? = null
)

class AdminViewModel(
    private val adminRepository: AdminRepository = AdminRepository(),
    private val courtRepository: CourtRepository = CourtRepository()
) : ViewModel() {

  private val _uiState = MutableStateFlow(AdminUiState())
  val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

  init {
    loadAllAdminData()
  }

  fun loadAllAdminData() {
    _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

    viewModelScope.launch {
      // 1. Dashboard summary
      val dashResult = adminRepository.getDashboard()
      dashResult.fold(
          onSuccess = { summary ->
            _uiState.value = _uiState.value.copy(summary = summary)
          },
          onFailure = { error ->
            _uiState.value = _uiState.value.copy(errorMessage = error.message)
          }
      )

      // 2. Venues & Courts
      loadVenuesAndCourtsInternal()

      // 3. Bookings
      loadBookingsInternal(_uiState.value.bookingStatusFilter)

      // 4. Analytics
      val revResult = adminRepository.getRevenueAnalytics()
      val sportsResult = adminRepository.getPopularSportsAnalytics()
      revResult.fold(
          onSuccess = { _uiState.value = _uiState.value.copy(revenueAnalytics = it) },
          onFailure = {}
      )
      sportsResult.fold(
          onSuccess = { _uiState.value = _uiState.value.copy(popularSports = it) },
          onFailure = {}
      )

      _uiState.value = _uiState.value.copy(isLoading = false)
    }
  }

  fun loadDashboard() {
    loadAllAdminData()
  }

  fun loadAnalytics() {
    _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
    viewModelScope.launch {
      val revResult = adminRepository.getRevenueAnalytics()
      val sportsResult = adminRepository.getPopularSportsAnalytics()
      var rev: List<RevenueByVenueDto> = emptyList()
      var sports: List<PopularSportDto> = emptyList()
      revResult.fold(onSuccess = { rev = it }, onFailure = {})
      sportsResult.fold(onSuccess = { sports = it }, onFailure = {})
      _uiState.value = _uiState.value.copy(
          isLoading = false,
          revenueAnalytics = rev,
          popularSports = sports
      )
    }
  }

  fun initManageSlots() {
    viewModelScope.launch {
      val courtsRes = courtRepository.getCourts()
      courtsRes.fold(
          onSuccess = { courts ->
            val firstCourtId = courts.firstOrNull()?.id
            _uiState.value = _uiState.value.copy(courts = courts, selectedCourtId = firstCourtId)
            loadCourtSlotsForAdmin()
          },
          onFailure = {}
      )
    }
  }

  fun refreshVenuesAndCourts() {
    viewModelScope.launch {
      loadVenuesAndCourtsInternal()
    }
  }

  private suspend fun loadVenuesAndCourtsInternal() {
    val venuesRes = courtRepository.getVenues()
    val courtsRes = courtRepository.getCourts()

    var venuesList: List<VenueDto> = _uiState.value.venues
    var courtsList: List<CourtDto> = _uiState.value.courts

    venuesRes.fold(
        onSuccess = { venuesList = it },
        onFailure = {}
    )

    courtsRes.fold(
        onSuccess = { courts ->
          courtsList = courts
          if (_uiState.value.selectedCourtId == null && courts.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(selectedCourtId = courts.first().id)
            loadCourtSlotsForAdmin()
          }
        },
        onFailure = {}
    )

    _uiState.value = _uiState.value.copy(venues = venuesList, courts = courtsList)
  }

  fun setBookingStatusFilter(status: String) {
    _uiState.value = _uiState.value.copy(bookingStatusFilter = status)
    loadBookings(status)
  }

  fun loadBookings(status: String? = null) {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isActionInProgress = true)
      loadBookingsInternal(status ?: _uiState.value.bookingStatusFilter)
      _uiState.value = _uiState.value.copy(isActionInProgress = false)
    }
  }

  private suspend fun loadBookingsInternal(status: String) {
    val queryStatus = if (status == "ALL") null else status
    val result = adminRepository.getAllBookings(status = queryStatus)
    result.fold(
        onSuccess = { bookings ->
          _uiState.value = _uiState.value.copy(allBookings = bookings)
        },
        onFailure = { error ->
          _uiState.value = _uiState.value.copy(errorMessage = error.message)
        }
    )
  }

  fun createVenue(
      name: String,
      address: String,
      city: String,
      amenities: List<String> = listOf("PARKING", "DRINKING_WATER", "RESTROOM"),
      sport: String? = null,
      basePrice: Double? = null,
      images: List<String> = emptyList(),
      onSuccess: () -> Unit
  ) {
    _uiState.value = _uiState.value.copy(isActionInProgress = true, errorMessage = null)
    viewModelScope.launch {
      val request = CreateVenueRequest(name = name, address = address, city = city, amenities = amenities, images = images)
      val result = adminRepository.createVenue(request)
      result.fold(
          onSuccess = { createdVenue ->
            if (!sport.isNullOrBlank() && basePrice != null && basePrice > 0) {
              val courtReq = CreateCourtRequest(
                  venueId = createdVenue.id,
                  name = "${createdVenue.name} - Court 1",
                  sport = sport.uppercase(),
                  basePrice = basePrice,
                  pricing = PricingDto(basePrice, basePrice + 100, basePrice + 150)
              )
              val courtRes = adminRepository.createCourt(courtReq)
              courtRes.fold(
                  onSuccess = { createdCourt ->
                    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                    val cal = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, 7) }
                    val nextWeek = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(cal.time)
                    adminRepository.generateSlots(createdCourt.id, GenerateSlotsRequest(today, nextWeek, 6, 23))
                  },
                  onFailure = {}
              )
            }
            _uiState.value = _uiState.value.copy(
                isActionInProgress = false,
                successMessage = "Turf '${name}' created and ready for bookings!"
            )
            refreshVenuesAndCourts()
            onSuccess()
          },
          onFailure = { error ->
            _uiState.value = _uiState.value.copy(isActionInProgress = false, errorMessage = error.message)
          }
      )
    }
  }

  fun createCourt(
      venueId: String,
      name: String,
      sport: String,
      isIndoor: Boolean,
      surfaceType: String,
      capacity: Int,
      basePrice: Double,
      weekdayRate: Double,
      weekendRate: Double,
      peakRate: Double,
      onSuccess: () -> Unit
  ) {
    _uiState.value = _uiState.value.copy(isActionInProgress = true, errorMessage = null)
    viewModelScope.launch {
      val request = CreateCourtRequest(
          venueId = venueId,
          name = name,
          sport = sport,
          isIndoor = isIndoor,
          surfaceType = surfaceType,
          capacity = capacity,
          basePrice = basePrice,
          pricing = PricingDto(weekday = weekdayRate, weekend = weekendRate, peak = peakRate)
      )
      val result = adminRepository.createCourt(request)
      result.fold(
          onSuccess = { createdCourt ->
            _uiState.value = _uiState.value.copy(
                isActionInProgress = false,
                successMessage = "Spot '${name}' created successfully!"
            )
            refreshVenuesAndCourts()
            onSuccess()
          },
          onFailure = { error ->
            _uiState.value = _uiState.value.copy(isActionInProgress = false, errorMessage = error.message)
          }
      )
    }
  }

  fun updateCourtRates(
      courtId: String,
      basePrice: Double,
      weekdayRate: Double,
      weekendRate: Double,
      peakRate: Double,
      isActive: Boolean,
      onSuccess: () -> Unit
  ) {
    _uiState.value = _uiState.value.copy(isActionInProgress = true, errorMessage = null)
    viewModelScope.launch {
      val request = UpdateCourtRequest(
          basePrice = basePrice,
          pricing = PricingDto(weekday = weekdayRate, weekend = weekendRate, peak = peakRate),
          isActive = isActive
      )
      val result = adminRepository.updateCourt(courtId, request)
      result.fold(
          onSuccess = { updated ->
            _uiState.value = _uiState.value.copy(
                isActionInProgress = false,
                successMessage = "Rates updated successfully for ${updated.name}!"
            )
            refreshVenuesAndCourts()
            onSuccess()
          },
          onFailure = { error ->
            _uiState.value = _uiState.value.copy(isActionInProgress = false, errorMessage = error.message)
          }
      )
    }
  }

  fun generateSlots(
      courtId: String,
      startDate: String,
      endDate: String,
      startHour: Int = 6,
      endHour: Int = 23,
      price: Double? = null,
      onSuccess: () -> Unit
  ) {
    _uiState.value = _uiState.value.copy(isActionInProgress = true, errorMessage = null)
    viewModelScope.launch {
      val request = GenerateSlotsRequest(
          startDate = startDate,
          endDate = endDate,
          startHour = startHour,
          endHour = endHour,
          price = price
      )
      val result = adminRepository.generateSlots(courtId, request)
      result.fold(
          onSuccess = { msg ->
            _uiState.value = _uiState.value.copy(
                isActionInProgress = false,
                successMessage = msg
            )
            loadCourtSlotsForAdmin()
            onSuccess()
          },
          onFailure = { error ->
            _uiState.value = _uiState.value.copy(isActionInProgress = false, errorMessage = error.message)
          }
      )
    }
  }

  fun cancelBooking(bookingId: String, reason: String = "Admin cancelled booking") {
    _uiState.value = _uiState.value.copy(isActionInProgress = true, errorMessage = null)
    viewModelScope.launch {
      val result = adminRepository.cancelBooking(bookingId, reason)
      result.fold(
          onSuccess = {
            _uiState.value = _uiState.value.copy(
                isActionInProgress = false,
                successMessage = "Booking cancelled & slots released."
            )
            loadBookings()
            loadCourtSlotsForAdmin()
          },
          onFailure = { error ->
            _uiState.value = _uiState.value.copy(isActionInProgress = false, errorMessage = error.message)
          }
      )
    }
  }

  fun loadCourtSlotsForAdmin() {
    val courtId = _uiState.value.selectedCourtId ?: return
    val date = _uiState.value.selectedDate

    viewModelScope.launch {
      val result = courtRepository.getCourtSlots(courtId, date)
      result.fold(
          onSuccess = { slots -> _uiState.value = _uiState.value.copy(slots = slots) },
          onFailure = {}
      )
    }
  }

  fun selectCourt(courtId: String) {
    _uiState.value = _uiState.value.copy(selectedCourtId = courtId)
    loadCourtSlotsForAdmin()
  }

  fun selectDate(date: String) {
    _uiState.value = _uiState.value.copy(selectedDate = date)
    loadCourtSlotsForAdmin()
  }

  fun blockSlot(slotId: String, reason: String = "Maintenance") {
    _uiState.value = _uiState.value.copy(isModifyingSlot = true)

    viewModelScope.launch {
      val result = adminRepository.blockSlots(listOf(slotId), reason)
      result.fold(
          onSuccess = {
            _uiState.value = _uiState.value.copy(isModifyingSlot = false, successMessage = "Slot blocked successfully")
            loadCourtSlotsForAdmin()
          },
          onFailure = { error ->
            _uiState.value = _uiState.value.copy(isModifyingSlot = false, errorMessage = error.message)
          }
      )
    }
  }

  fun unblockSlot(slotId: String) {
    _uiState.value = _uiState.value.copy(isModifyingSlot = true)

    viewModelScope.launch {
      val result = adminRepository.unblockSlots(listOf(slotId))
      result.fold(
          onSuccess = {
            _uiState.value = _uiState.value.copy(isModifyingSlot = false, successMessage = "Slot unblocked")
            loadCourtSlotsForAdmin()
          },
          onFailure = { error ->
            _uiState.value = _uiState.value.copy(isModifyingSlot = false, errorMessage = error.message)
          }
      )
    }
  }

  fun updateServerUrl(context: Context, newUrl: String) {
    NetworkModule.setBaseUrl(context, newUrl)
    _uiState.value = _uiState.value.copy(
        activeBaseUrl = NetworkModule.getBaseUrl(),
        successMessage = "Server URL updated to ${NetworkModule.getBaseUrl()}"
    )
    loadAllAdminData()
  }

  fun clearMessages() {
    _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
  }
}
