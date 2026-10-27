package com.courtbook.data.repository

import com.courtbook.data.model.LoginRequest
import com.courtbook.data.model.RegisterRequest
import com.courtbook.data.model.UserDto
import com.courtbook.data.remote.CourtBookApi
import com.courtbook.data.remote.NetworkModule
import com.courtbook.data.remote.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(private val api: CourtBookApi = NetworkModule.api) {

  suspend fun login(email: String, password: String): Result<UserDto> =
      withContext(Dispatchers.IO) {
        try {
          val response = api.login(LoginRequest(email.trim(), password))
          if (response.isSuccessful && response.body()?.data != null) {
            val authData = response.body()!!.data!!
            TokenManager.saveAuth(authData.token, authData.user)
            Result.success(authData.user)
          } else {
            val errorMsg = response.errorBody()?.string() ?: "Invalid login credentials"
            Result.failure(Exception(errorMsg))
          }
        } catch (e: Exception) {
          Result.failure(e)
        }
      }

  suspend fun register(
      name: String,
      email: String,
      phone: String,
      password: String,
      role: String = "PLAYER"
  ): Result<UserDto> =
      withContext(Dispatchers.IO) {
        try {
          val response =
              api.register(RegisterRequest(name.trim(), email.trim(), phone.trim(), password, role))
          if (response.isSuccessful && response.body()?.data != null) {
            val authData = response.body()!!.data!!
            TokenManager.saveAuth(authData.token, authData.user)
            Result.success(authData.user)
          } else {
            val errorMsg = response.errorBody()?.string() ?: "Registration failed"
            Result.failure(Exception(errorMsg))
          }
        } catch (e: Exception) {
          Result.failure(e)
        }
      }

  fun isLoggedIn(): Boolean = TokenManager.isLoggedIn()

  fun isAdmin(): Boolean = TokenManager.isAdmin()

  fun getCurrentUser(): UserDto? = TokenManager.getUser()

  fun logout() {
    TokenManager.clear()
  }
}
