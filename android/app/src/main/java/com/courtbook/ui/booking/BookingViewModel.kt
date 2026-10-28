package com.courtbook.ui.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.courtbook.data.model.BookingDto
import com.courtbook.data.model.CourtDto
import com.courtbook.data.model.SlotDto
import com.courtbook.data.model.ValidatedOfferData
import com.courtbook.data.repository.BookingRepository
import com.courtbook.data.repository.ConflictException
import com.courtbook.data.repository.CourtRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BookingUiState(
    val isLoadingData: Boolean = true,
    val court: CourtDto? = null,
    val slots: List<SlotDto> = emptyList(),
    val originalAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val finalAmount: Double = 0.0,
    val appliedCoupon: String? = null,
    val couponError: String? = null,
    val isApplyingCoupon: Boolean = false,
    val isSubmittingBooking: Boolean = false,
    val confirmedBooking: BookingDto? = null,
    val errorMessage: String? = null,
    val isConflictError: Boolean = false
)

class BookingViewModel(
    private val bookingRepository: BookingRepository = BookingRepository(),
    private val courtRepository: CourtRepository = CourtRepository()
) : ViewModel() {

  private val _uiState = MutableStateFlow(BookingUiState())
  val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()

  fun initBooking(courtId: String, slotIds: List<String>) {
    _uiState.value = _uiState.value.copy(isLoadingData = true, errorMessage = null)

    viewModelScope.launch {
      val courtResult = courtRepository.getCourtDetails(courtId)
      var court: CourtDto? = null
      courtResult.fold(onSuccess = { court = it }, onFailure = {})

      val slotObjects = mutableListOf<SlotDto>()
      // Fetch court slots to populate matching slot models
      val todayDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
      val slotsResult = courtRepository.getCourtSlots(courtId, todayDate)
      slotsResult.fold(
          onSuccess = { allSlots ->
            slotObjects.addAll(allSlots.filter { slotIds.contains(it.id) })
          },
          onFailure = {}
      )

      val originalAmount = if (slotObjects.isNotEmpty()) {
        slotObjects.sumOf { it.price }
      } else {
        (court?.basePrice ?: 400.0) * slotIds.size
      }

      _uiState.value = _uiState.value.copy(
          isLoadingData = false,
          court = court,
          slots = slotObjects,
          originalAmount = originalAmount,
          finalAmount = originalAmount
      )
    }
  }

  fun applyCoupon(code: String) {
    val court = _uiState.value.court ?: return
    if (code.isBlank()) return

    _uiState.value = _uiState.value.copy(isApplyingCoupon = true, couponError = null)

    viewModelScope.launch {
      val result = bookingRepository.validateOffer(code, court.id, _uiState.value.originalAmount)
      result.fold(
          onSuccess = { validated ->
            _uiState.value = _uiState.value.copy(
                isApplyingCoupon = false,
                appliedCoupon = validated.code,
                discountAmount = validated.discountAmount,
                finalAmount = validated.finalAmount,
                couponError = null
            )
          },
          onFailure = { error ->
            _uiState.value = _uiState.value.copy(
                isApplyingCoupon = false,
                couponError = error.message ?: "Invalid coupon"
            )
          }
      )
    }
  }

  fun removeCoupon() {
    _uiState.value = _uiState.value.copy(
        appliedCoupon = null,
        discountAmount = 0.0,
        finalAmount = _uiState.value.originalAmount,
        couponError = null
    )
  }

  fun confirmPayment(
      courtId: String,
      slotIds: List<String>,
      paymentMethod: String = "MOCK_UPI",
      onSuccess: (bookingNumber: String) -> Unit
  ) {
    _uiState.value = _uiState.value.copy(isSubmittingBooking = true, errorMessage = null)

    viewModelScope.launch {
      val result = bookingRepository.createBooking(
          courtId = courtId,
          slotIds = slotIds,
          offerCode = _uiState.value.appliedCoupon,
          paymentMethod = paymentMethod
      )

      result.fold(
          onSuccess = { booking ->
            _uiState.value = _uiState.value.copy(
                isSubmittingBooking = false,
                confirmedBooking = booking
            )
            onSuccess(booking.bookingNumber)
          },
          onFailure = { error ->
            val isConflict = error is ConflictException || error.message?.contains("409") == true
            _uiState.value = _uiState.value.copy(
                isSubmittingBooking = false,
                isConflictError = isConflict,
                errorMessage = error.message ?: "Failed to confirm booking"
            )
          }
      )
    }
  }

  fun cancelAndReleaseHold(slotIds: List<String>, onReleased: () -> Unit) {
    viewModelScope.launch {
      courtRepository.releaseHold(slotIds)
      onReleased()
    }
  }
}
