package com.courtbook.ui.court

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.courtbook.data.model.CourtDto
import com.courtbook.data.model.SlotDto
import com.courtbook.data.repository.CourtRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class CourtDetailsUiState(
    val isLoadingCourt: Boolean = true,
    val isLoadingSlots: Boolean = true,
    val isHoldingSlots: Boolean = false,
    val court: CourtDto? = null,
    val selectedDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
    val slots: List<SlotDto> = emptyList(),
    val selectedSlots: List<SlotDto> = emptyList(),
    val errorMessage: String? = null,
    val isConflictError: Boolean = false
) {
  val totalPrice: Double get() = selectedSlots.sumOf { it.price }
  val canProceed: Boolean get() = selectedSlots.isNotEmpty() && !isHoldingSlots
}

class CourtViewModel(private val repository: CourtRepository = CourtRepository()) : ViewModel() {

  private val _uiState = MutableStateFlow(CourtDetailsUiState())
  val uiState: StateFlow<CourtDetailsUiState> = _uiState.asStateFlow()

  fun initCourt(courtId: String) {
    _uiState.value = _uiState.value.copy(
        isLoadingCourt = true,
        selectedSlots = emptyList(),
        errorMessage = null
    )

    viewModelScope.launch {
      val result = repository.getCourtDetails(courtId)
      result.fold(
          onSuccess = { court ->
            _uiState.value = _uiState.value.copy(isLoadingCourt = false, court = court)
            loadSlotsForCurrentDate(courtId)
          },
          onFailure = { error ->
            _uiState.value = _uiState.value.copy(
                isLoadingCourt = false,
                errorMessage = error.message ?: "Failed to load court details"
            )
          }
      )
    }
  }

  fun selectDate(date: String) {
    if (_uiState.value.selectedDate == date) return
    _uiState.value = _uiState.value.copy(
        selectedDate = date,
        selectedSlots = emptyList(),
        errorMessage = null
    )
    val courtId = _uiState.value.court?.id ?: return
    loadSlotsForCurrentDate(courtId)
  }

  fun loadSlotsForCurrentDate(courtId: String) {
    _uiState.value = _uiState.value.copy(isLoadingSlots = true, errorMessage = null)
    viewModelScope.launch {
      val result = repository.getCourtSlots(courtId, _uiState.value.selectedDate)
      result.fold(
          onSuccess = { slots ->
            _uiState.value = _uiState.value.copy(isLoadingSlots = false, slots = slots)
          },
          onFailure = { error ->
            _uiState.value = _uiState.value.copy(
                isLoadingSlots = false,
                errorMessage = error.message ?: "Failed to load time slots"
            )
          }
      )
    }
  }

  fun toggleSlotSelection(slot: SlotDto) {
    val current = _uiState.value.selectedSlots.toMutableList()
    val existingIndex = current.indexOfFirst { it.id == slot.id }

    if (existingIndex >= 0) {
      current.removeAt(existingIndex)
    } else {
      current.add(slot)
    }

    _uiState.value = _uiState.value.copy(selectedSlots = current)
  }

  fun proceedToHold(onSuccess: (courtId: String, slotIds: List<String>) -> Unit) {
    val court = _uiState.value.court ?: return
    val selected = _uiState.value.selectedSlots
    if (selected.isEmpty()) return

    _uiState.value = _uiState.value.copy(isHoldingSlots = true, errorMessage = null)

    viewModelScope.launch {
      val slotIds = selected.map { it.id }
      val result = repository.holdSlots(slotIds)

      result.fold(
          onSuccess = {
            _uiState.value = _uiState.value.copy(isHoldingSlots = false)
            onSuccess(court.id, slotIds)
          },
          onFailure = { error ->
            val isConflict = error.message?.contains("409") == true || error.message?.contains("unavailable") == true
            _uiState.value = _uiState.value.copy(
                isHoldingSlots = false,
                isConflictError = isConflict,
                errorMessage = if (isConflict) {
                  "Sorry, one or more selected slots were just booked or held by another player. Availability refreshed."
                } else {
                  error.message ?: "Failed to reserve slots"
                }
            )
            // Auto refresh slots
            loadSlotsForCurrentDate(court.id)
          }
      )
    }
  }
}
