package com.courtbook

import android.app.Application
import com.courtbook.data.remote.NetworkModule
import com.courtbook.data.remote.TokenManager

class CourtBookApp : Application() {
  override fun onCreate() {
    super.onCreate()
    // Initialize token and network module
    TokenManager.init(this)
    NetworkModule.init(this)
  }
}
