package com.courtbook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.courtbook.navigation.AppNavigation
import com.courtbook.ui.theme.CourtBookTheme
import com.courtbook.ui.theme.DarkBg

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      CourtBookTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DarkBg
        ) {
          AppNavigation()
        }
      }
    }
  }
}
