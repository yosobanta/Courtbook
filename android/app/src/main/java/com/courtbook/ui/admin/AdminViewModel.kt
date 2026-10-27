package com.courtbook.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.courtbook.data.model.*
import com.courtbook.data.repository.AdminRepository
import com.courtbook.data.repository.CourtRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminUiState(
    val isLoading: Boolean = true,
    val summary: DashboardSummaryDto? = null,
    val revenueAnalytics: List<RevenueByVenueDto> = emptyList(),
    val popularSports: List<PopularSportDto> = emptyList(),
    val courts: List<CourtDto> = emptyList(),
    val selectedCourtId: String? = null,
    val selectedDate: String = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
    val slots: List<SlotDto> = emptyList(),
    val isModifyingSlot: Boolean = false,
    val errorMessage: String? = null
)

class AdminViewModel(
    private val adminRepository: AdminRepository = AdminRepository(),
    private val courtRepository: CourtRepository = CourtRepository()
) : ViewModel() {

  private val _uiState = MutableStateFlow(AdminUiState())
  val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

  init {
    loadDashboard()
  }

  fun loadDashboard() {
    _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

    viewModelScope.launch {
      val dashResult = adminRepository.getDashboard()
      dashResult.fold(
          onSuccess = { summary ->
            _uiState.value = _uiState.value.copy(isLoading = false, summary = summary)
          },
          onFailure = { error ->
            _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = error.message)
          }
      )
    }
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

  fun blockSlot(slotId: String, reason: String = "Maintenance") {
    _uiState.value = _uiState.value.copy(isModifyingSlot = true)

    viewModelScope.launch {
      val result = adminRepository.blockSlots(listOf(slotId), reason)
      result.fold(
          onSuccess = {
            _uiState.value = _uiState.value.copy(isModifyingSlot = false)
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
            _uiState.value = _uiState.value.copy(isModifyingSlot = false)
            loadCourtSlotsForAdmin()
          },
          onFailure = { error ->
            _uiState.value = _uiState.value.copy(isModifyingSlot = false, errorMessage = error.message)
          }
      )
    }
  }
}
