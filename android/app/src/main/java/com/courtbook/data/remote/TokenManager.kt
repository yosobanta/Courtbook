package com.courtbook.data.remote

import android.content.Context
import android.content.SharedPreferences
import com.courtbook.data.model.UserDto
import com.google.gson.Gson

object TokenManager {
  private const val PREFS_NAME = "courtbook_auth_prefs"
  private const val KEY_TOKEN = "jwt_token"
  private const val KEY_USER = "user_json"

  private var inMemoryToken: String? = null
  private var inMemoryUser: UserDto? = null
  private var prefs: SharedPreferences? = null
  private val gson = Gson()

  fun init(context: Context) {
    prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    inMemoryToken = prefs?.getString(KEY_TOKEN, null)
    val userJson = prefs?.getString(KEY_USER, null)
    if (!userJson.isNullOrEmpty()) {
      try {
        inMemoryUser = gson.fromJson(userJson, UserDto::class.java)
      } catch (_: Exception) {
      }
    }
  }

  fun saveAuth(token: String, user: UserDto) {
    inMemoryToken = token
    inMemoryUser = user
    prefs?.edit()?.apply {
      putString(KEY_TOKEN, token)
      putString(KEY_USER, gson.toJson(user))
      apply()
    }
  }

  fun getToken(): String? = inMemoryToken

  fun getUser(): UserDto? = inMemoryUser

  fun isLoggedIn(): Boolean = !inMemoryToken.isNullOrEmpty()

  fun isAdmin(): Boolean = inMemoryUser?.isAdmin == true

  fun clear() {
    inMemoryToken = null
    inMemoryUser = null
    prefs?.edit()?.clear()?.apply()
  }
}
