package com.courtbook.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.courtbook.data.model.CourtDto
import com.courtbook.data.repository.CourtRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExploreUiState(
    val searchQuery: String = "",
    val selectedSport: String = "ALL",
    val isIndoorOnly: Boolean = false,
    val maxPrice: Double? = null,
    val isLoading: Boolean = false,
    val courts: List<CourtDto> = emptyList(),
    val errorMessage: String? = null
)

class ExploreViewModel(private val repository: CourtRepository = CourtRepository()) : ViewModel() {

  private val _uiState = MutableStateFlow(ExploreUiState())
  val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

  val sports = listOf("ALL", "BADMINTON", "TENNIS", "FOOTBALL", "CRICKET", "TABLE_TENNIS")

  init {
    searchCourts()
  }

  fun updateSearchQuery(query: String) {
    _uiState.value = _uiState.value.copy(searchQuery = query)
    searchCourts()
  }

  fun selectSport(sport: String) {
    _uiState.value = _uiState.value.copy(selectedSport = sport)
    searchCourts()
  }

  fun toggleIndoorOnly() {
    _uiState.value = _uiState.value.copy(isIndoorOnly = !_uiState.value.isIndoorOnly)
    searchCourts()
  }

  fun setMaxPrice(price: Double?) {
    _uiState.value = _uiState.value.copy(maxPrice = price)
    searchCourts()
  }

  fun searchCourts() {
    _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

    viewModelScope.launch {
      val state = _uiState.value
      val result = repository.getCourts(
          sport = if (state.selectedSport == "ALL") null else state.selectedSport,
          search = if (state.searchQuery.isBlank()) null else state.searchQuery.trim(),
          isIndoor = if (state.isIndoorOnly) true else null,
          maxPrice = state.maxPrice
      )

      result.fold(
          onSuccess = { list ->
            _uiState.value = _uiState.value.copy(isLoading = false, courts = list)
          },
          onFailure = { error ->
            _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = error.message)
          }
      )
    }
  }
}
