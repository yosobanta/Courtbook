package com.courtbook.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.courtbook.data.model.CourtDto
import com.courtbook.data.model.OfferDto
import com.courtbook.data.model.UserDto
import com.courtbook.data.model.VenueDto
import com.courtbook.data.repository.BookingRepository
import com.courtbook.data.repository.CourtRepository
import com.courtbook.data.remote.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val user: UserDto? = null,
    val selectedSport: String = "ALL",
    val venues: List<VenueDto> = emptyList(),
    val courts: List<CourtDto> = emptyList(),
    val offers: List<OfferDto> = emptyList(),
    val errorMessage: String? = null
)

class HomeViewModel(
    private val courtRepository: CourtRepository = CourtRepository(),
    private val bookingRepository: BookingRepository = BookingRepository()
) : ViewModel() {

  private val _uiState = MutableStateFlow(HomeUiState())
  val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

  val sportsCategories = listOf("ALL", "BADMINTON", "TENNIS", "FOOTBALL", "CRICKET", "TABLE_TENNIS")

  init {
    loadHomeData()
  }

  fun loadHomeData() {
    _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, user = TokenManager.getUser())

    viewModelScope.launch {
      val venuesResult = courtRepository.getVenues()
      val courtsResult = courtRepository.getCourts(
          sport = if (_uiState.value.selectedSport == "ALL") null else _uiState.value.selectedSport
      )
      val offersResult = bookingRepository.getActiveOffers()

      var currentVenues: List<VenueDto> = emptyList()
      var currentCourts: List<CourtDto> = emptyList()
      var currentOffers: List<OfferDto> = emptyList()
      var error: String? = null

      venuesResult.fold(
          onSuccess = { currentVenues = it },
          onFailure = { /* non-fatal */ }
      )

      courtsResult.fold(
          onSuccess = { currentCourts = it },
          onFailure = { error = it.message ?: "Failed to load courts" }
      )

      offersResult.fold(
          onSuccess = { currentOffers = it },
          onFailure = { /* non-fatal */ }
      )

      _uiState.value = _uiState.value.copy(
          isLoading = false,
          venues = currentVenues,
          courts = currentCourts,
          offers = currentOffers,
          errorMessage = error
      )
    }
  }

  fun selectSport(sport: String) {
    if (_uiState.value.selectedSport == sport) return
    _uiState.value = _uiState.value.copy(selectedSport = sport)
    loadHomeData()
  }
}
