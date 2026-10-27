package com.courtbook.ui.mybookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.courtbook.data.model.BookingDto
import com.courtbook.data.repository.BookingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MyBookingsUiState(
    val isLoading: Boolean = true,
    val bookings: List<BookingDto> = emptyList(),
    val isCancelling: Boolean = false,
    val errorMessage: String? = null
)

class MyBookingsViewModel(private val repository: BookingRepository = BookingRepository()) : ViewModel() {

  private val _uiState = MutableStateFlow(MyBookingsUiState())
  val uiState: StateFlow<MyBookingsUiState> = _uiState.asStateFlow()

  init {
    loadMyBookings()
  }

  fun loadMyBookings() {
    _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

    viewModelScope.launch {
      val result = repository.getMyBookings()
      result.fold(
          onSuccess = { list ->
            _uiState.value = _uiState.value.copy(isLoading = false, bookings = list)
          },
          onFailure = { error ->
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = error.message ?: "Failed to fetch bookings"
            )
          }
      )
    }
  }

  fun cancelBooking(bookingId: String, reason: String = "Changed plans") {
    _uiState.value = _uiState.value.copy(isCancelling = true, errorMessage = null)

    viewModelScope.launch {
      val result = repository.cancelBooking(bookingId, reason)
      result.fold(
          onSuccess = {
            _uiState.value = _uiState.value.copy(isCancelling = false)
            loadMyBookings()
          },
          onFailure = { error ->
            _uiState.value = _uiState.value.copy(
                isCancelling = false,
                errorMessage = error.message ?: "Failed to cancel booking"
            )
          }
      )
    }
  }
}
