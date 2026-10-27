package com.courtbook

import android.app.Application
import com.courtbook.data.remote.TokenManager

class CourtBookApp : Application() {
  override fun onCreate() {
    super.onCreate()
    // Initialize token and session manager
    TokenManager.init(this)
  }
}
