package com.courtbook.data.remote

import android.content.Context
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
  const val DEFAULT_RENDER_URL = "https://courtbook-b9yl.onrender.com/api/"
  const val LOCALHOST_EMULATOR_URL = "http://10.0.2.2:5000/api/"
  const val LOCALHOST_DEVICE_URL = "http://127.0.0.1:5000/api/"

  private const val PREFS_NAME = "courtbook_server_prefs"
  private const val KEY_BASE_URL = "custom_base_url"

  private var activeBaseUrl: String = DEFAULT_RENDER_URL

  fun isEmulator(): Boolean {
    val fingerprint = android.os.Build.FINGERPRINT ?: ""
    val model = android.os.Build.MODEL ?: ""
    val manufacturer = android.os.Build.MANUFACTURER ?: ""
    val brand = android.os.Build.BRAND ?: ""
    val device = android.os.Build.DEVICE ?: ""
    val product = android.os.Build.PRODUCT ?: ""

    return (fingerprint.startsWith("generic")
        || fingerprint.startsWith("unknown")
        || model.contains("google_sdk", ignoreCase = true)
        || model.contains("Emulator", ignoreCase = true)
        || model.contains("Android SDK built for", ignoreCase = true)
        || manufacturer.contains("Genymotion", ignoreCase = true)
        || (brand.startsWith("generic") && device.startsWith("generic"))
        || "google_sdk" == product)
  }

  fun init(context: Context) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    var savedUrl = prefs.getString(KEY_BASE_URL, DEFAULT_RENDER_URL) ?: DEFAULT_RENDER_URL

    // Safety guard: 10.0.2.2 only exists inside the Android PC emulator.
    // If a physical phone saved 10.0.2.2, auto-reset to Render Cloud so the user is never stuck in a 60s timeout loop!
    if (!isEmulator() && savedUrl.contains("10.0.2.2")) {
      savedUrl = DEFAULT_RENDER_URL
      prefs.edit().putString(KEY_BASE_URL, savedUrl).apply()
    }

    activeBaseUrl = savedUrl
  }

  fun getBaseUrl(): String = activeBaseUrl

  fun setBaseUrl(context: Context?, newUrl: String) {
    var formatted = newUrl.trim()
    if (!formatted.endsWith("/")) formatted += "/"
    activeBaseUrl = formatted
    context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        ?.edit()
        ?.putString(KEY_BASE_URL, formatted)
        ?.apply()
  }

  private val dynamicUrlInterceptor = Interceptor { chain ->
    var request = chain.request()
    val targetUrl = activeBaseUrl.toHttpUrlOrNull()
    if (targetUrl != null) {
      val newHttpUrl = request.url.newBuilder()
          .scheme(targetUrl.scheme)
          .host(targetUrl.host)
          .port(targetUrl.port)
          .build()
      request = request.newBuilder().url(newHttpUrl).build()
    }
    chain.proceed(request)
  }

  private val loggingInterceptor = HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BODY
  }

  private val okHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .addInterceptor(dynamicUrlInterceptor)
        .addInterceptor(AuthInterceptor())
        .addInterceptor(loggingInterceptor)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS) // Render free tier cold start = 30-50s
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
  }

  val api: CourtBookApi by lazy {
    Retrofit.Builder()
        .baseUrl(DEFAULT_RENDER_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(CourtBookApi::class.java)
  }
}
