package com.courtbook.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.courtbook.data.model.UserDto
import com.courtbook.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
  object Idle : AuthState()
  object Loading : AuthState()
  data class Success(val user: UserDto) : AuthState()
  data class Error(val message: String) : AuthState()
}

class AuthViewModel(private val repository: AuthRepository = AuthRepository()) : ViewModel() {

  private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
  val authState: StateFlow<AuthState> = _authState.asStateFlow()

  fun login(email: String, password: String) {
    if (email.isBlank() || password.isBlank()) {
      _authState.value = AuthState.Error("Please enter both email and password")
      return
    }

    _authState.value = AuthState.Loading
    viewModelScope.launch {
      val result = repository.login(email, password)
      result.fold(
          onSuccess = { user -> _authState.value = AuthState.Success(user) },
          onFailure = { error ->
            _authState.value = AuthState.Error(error.message ?: "Authentication failed")
          })
    }
  }

  fun register(name: String, email: String, phone: String, password: String, role: String = "PLAYER") {
    if (name.isBlank() || email.isBlank() || phone.isBlank() || password.isBlank()) {
      _authState.value = AuthState.Error("Please fill out all registration fields")
      return
    }

    _authState.value = AuthState.Loading
    viewModelScope.launch {
      val result = repository.register(name, email, phone, password, role)
      result.fold(
          onSuccess = { user -> _authState.value = AuthState.Success(user) },
          onFailure = { error ->
            _authState.value = AuthState.Error(error.message ?: "Registration failed")
          })
    }
  }

  fun resetState() {
    _authState.value = AuthState.Idle
  }
}
