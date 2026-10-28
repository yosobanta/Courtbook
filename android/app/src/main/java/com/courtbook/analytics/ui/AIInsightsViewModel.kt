package com.courtbook.analytics.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.courtbook.analytics.data.AnalyticsRepository
import com.courtbook.analytics.data.SyntheticBookingGenerator
import com.courtbook.analytics.engine.AnalyticsEngine
import com.courtbook.analytics.model.AnalyticsUiState
import com.courtbook.analytics.model.RealBookingSummary
import com.courtbook.analytics.model.SyntheticBooking
import com.courtbook.data.remote.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel managing AI Insights state, cold-start fallback,
 * real booking enrichment, and synthetic growth simulations.
 */
class AIInsightsViewModel(
    private val repository: AnalyticsRepository = AnalyticsRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    private var cachedSynthetic: List<SyntheticBooking> = emptyList()
    private var cachedRealSummaries: List<RealBookingSummary> = emptyList()

    init {
        loadData()
    }

    fun loadData() {
        val isAdmin = TokenManager.isAdmin()
        val sports = listOf("ALL") + SyntheticBookingGenerator.SPORTS

        // 1. Immediately compute cold-start forecast using deterministic synthetic data
        cachedSynthetic = repository.getSyntheticBookings()
        val initialForecast = AnalyticsEngine.computeForecast(
            syntheticData = cachedSynthetic,
            realSummaries = emptyList(),
            sportFilter = _uiState.value.selectedSport,
            horizonDays = _uiState.value.forecastHorizon
        )

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            forecast = initialForecast,
            availableSports = sports,
            isAdmin = isAdmin,
            realBookingCount = 0,
            realDataAvailable = false
        )

        // 2. Asynchronously fetch real bookings from backend
        viewModelScope.launch {
            val (realSummaries, totalRealCount) = repository.getRealBookingSummaries()
            cachedRealSummaries = realSummaries

            val hasRealData = totalRealCount > 0
            val currentSynthetic = if (_uiState.value.simulatedBookingsCount > 0) {
                repository.getSimulatedBookings(_uiState.value.simulatedBookingsCount)
            } else {
                cachedSynthetic
            }

            val blendedForecast = AnalyticsEngine.computeForecast(
                syntheticData = currentSynthetic,
                realSummaries = cachedRealSummaries,
                sportFilter = _uiState.value.selectedSport,
                horizonDays = _uiState.value.forecastHorizon
            )

            _uiState.value = _uiState.value.copy(
                forecast = blendedForecast,
                realBookingCount = totalRealCount,
                actualTotalBookings = totalRealCount,
                realDataAvailable = hasRealData
            )
        }
    }

    fun selectSport(sport: String) {
        if (_uiState.value.selectedSport == sport) return
        _uiState.value = _uiState.value.copy(selectedSport = sport)
        recomputeForecast()
    }

    fun setForecastHorizon(days: Int) {
        if (_uiState.value.forecastHorizon == days) return
        _uiState.value = _uiState.value.copy(forecastHorizon = days)
        recomputeForecast()
    }

    fun simulateAdditionalBookings(count: Int) {
        _uiState.value = _uiState.value.copy(simulatedBookingsCount = count)
        recomputeForecast()
    }

    fun resetSimulation() {
        _uiState.value = _uiState.value.copy(simulatedBookingsCount = 0)
        recomputeForecast()
    }

    private fun recomputeForecast() {
        val state = _uiState.value
        val synthetic = if (state.simulatedBookingsCount > 0) {
            repository.getSimulatedBookings(state.simulatedBookingsCount)
        } else {
            cachedSynthetic
        }

        val updatedForecast = AnalyticsEngine.computeForecast(
            syntheticData = synthetic,
            realSummaries = cachedRealSummaries,
            sportFilter = state.selectedSport,
            horizonDays = state.forecastHorizon
        )

        _uiState.value = state.copy(forecast = updatedForecast)
    }
}
