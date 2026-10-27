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

    return chain.proceed(requestBuilder.build())
  }
}
