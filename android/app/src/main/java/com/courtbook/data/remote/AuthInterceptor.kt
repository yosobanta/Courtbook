package com.courtbook.data.remote

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val originalRequest = chain.request()
    val token = TokenManager.getToken()

    val requestBuilder = originalRequest.newBuilder()
    if (!token.isNullOrEmpty()) {
      requestBuilder.addHeader("Authorization", "Bearer $token")
    }
    requestBuilder.addHeader("Accept", "application/json")

    val response = chain.proceed(requestBuilder.build())

    // If server returns 401 and we had a token, it has expired → auto-logout
    if (response.code == 401 && !token.isNullOrEmpty()) {
      TokenManager.clear()
    }

    return response
  }
}
