package com.courtbook.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.courtbook.data.remote.NetworkModule
import com.courtbook.ui.components.ErrorBanner
import com.courtbook.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: (isAdmin: Boolean) -> Unit,
    onNavigateToRegister: () -> Unit
) {
  val context = LocalContext.current
  var email by remember { mutableStateOf("alex@example.com") }
  var password by remember { mutableStateOf("Player@123") }
  var showWakeUpHint by remember { mutableStateOf(false) }
  var showServerDialog by remember { mutableStateOf(false) }
  var activeUrl by remember { mutableStateOf(NetworkModule.getBaseUrl()) }
  var customUrlInput by remember { mutableStateOf(NetworkModule.getBaseUrl()) }

  val authState by viewModel.authState.collectAsState()

  LaunchedEffect(authState) {
    if (authState is AuthState.Success) {
      val user = (authState as AuthState.Success).user
      showWakeUpHint = false
      viewModel.resetState()
      onLoginSuccess(user.isAdmin)
    }
    // Show a hint after 5s of loading (Render free tier cold start)
    if (authState is AuthState.Loading) {
      delay(5_000)
      if (authState is AuthState.Loading) showWakeUpHint = true
    } else {
      showWakeUpHint = false
    }
  }

  Box(
      modifier = Modifier
          .fillMaxSize()
          .background(WarmBg)
          .padding(24.dp)
  ) {
    // Top Server Status Chip
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopEnd)
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.End
    ) {
      Surface(
          onClick = { showServerDialog = true },
          shape = RoundedCornerShape(20.dp),
          color = CardSurface,
          border = BorderStroke(1.dp, BorderSubtle),
          shadowElevation = 1.dp
      ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
              modifier = Modifier
                  .size(8.dp)
                  .background(
                      if (activeUrl.contains("onrender")) StatusSuccess else StatusWarning,
                      shape = CircleShape
                  )
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
              text = if (activeUrl.contains("onrender")) "Render Cloud" else if (activeUrl.contains("127.0.0.1")) "USB (127.0.0.1)" else "Server",
              color = TextDeep,
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold
          )
          Spacer(modifier = Modifier.width(4.dp))
          Icon(
              imageVector = Icons.Default.Settings,
              contentDescription = "Change Server",
              tint = TextMuted,
              modifier = Modifier.size(13.dp)
          )
        }
      }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
      // App Branding
      Box(
          modifier = Modifier
              .size(72.dp)
              .clip(CircleShape)
              .background(SoftGreenSurface),
          contentAlignment = Alignment.Center
      ) {
        Icon(
            imageVector = Icons.Default.SportsTennis,
            contentDescription = "Logo",
            tint = ForestGreen,
            modifier = Modifier.size(40.dp)
        )
      }
      Spacer(modifier = Modifier.height(16.dp))
      Text(
          text = "CourtBook",
          color = TextDeep,
          fontSize = 32.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = (-0.5).sp
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
          text = "Find. Book. Play.",
          color = ForestGreen,
          fontSize = 14.sp,
          fontWeight = FontWeight.SemiBold
      )

      Spacer(modifier = Modifier.height(32.dp))

      // Error message banner with 1-click cloud recovery
      if (authState is AuthState.Error) {
        val errorMsg = (authState as AuthState.Error).message
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
          ErrorBanner(errorMessage = errorMsg)

          if (errorMsg.contains("10.0.2.2") || errorMsg.contains("timeout") || errorMsg.contains("timed out") || errorMsg.contains("Failed to connect") || !activeUrl.contains("onrender")) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                  NetworkModule.setBaseUrl(context, NetworkModule.DEFAULT_RENDER_URL)
                  activeUrl = NetworkModule.DEFAULT_RENDER_URL
                  viewModel.resetState()
                },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
            ) {
              Text("Switch to Render Cloud (Fix Connection)", color = ElectricLime, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      // Input fields
      OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text("Email Address") },
          leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = TextMuted) },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = CardSurface,
              unfocusedContainerColor = CardSurface,
              focusedBorderColor = ForestGreen,
              unfocusedBorderColor = BorderSubtle,
              focusedTextColor = TextDeep,
              unfocusedTextColor = TextDeep,
              focusedLabelColor = ForestGreen,
              unfocusedLabelColor = TextMuted
          ),
          singleLine = true
      )

      Spacer(modifier = Modifier.height(16.dp))

      OutlinedTextField(
          value = password,
          onValueChange = { password = it },
          label = { Text("Password") },
          leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TextMuted) },
          visualTransformation = PasswordVisualTransformation(),
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = CardSurface,
              unfocusedContainerColor = CardSurface,
              focusedBorderColor = ForestGreen,
              unfocusedBorderColor = BorderSubtle,
              focusedTextColor = TextDeep,
              unfocusedTextColor = TextDeep,
              focusedLabelColor = ForestGreen,
              unfocusedLabelColor = TextMuted
          ),
          singleLine = true
      )

      Spacer(modifier = Modifier.height(24.dp))

      // Sign In Button
      Button(
          onClick = { viewModel.login(email, password) },
          enabled = authState !is AuthState.Loading,
          modifier = Modifier
              .fillMaxWidth()
              .height(52.dp),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
              containerColor = ForestGreen,
              disabledContainerColor = ForestGreen.copy(alpha = 0.6f)
          )
      ) {
        if (authState is AuthState.Loading) {
          CircularProgressIndicator(color = ElectricLime, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
        } else {
          Text("Sign In", color = ElectricLime, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
      }

      // Cold-start hint — visible after 5s of loading
      if (showWakeUpHint) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "⏳ Server is waking up, please wait...",
            color = StatusWarning,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Quick Demo Credentials Buttons
      Text("Or test with pre-seeded accounts:", color = TextMuted, fontSize = 12.sp)
      Spacer(modifier = Modifier.height(10.dp))

      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            onClick = {
              email = "alex@example.com"
              password = "Player@123"
            },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = CardSurface),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
          Text("Player Demo", color = ForestGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }

        OutlinedButton(
            onClick = {
              email = "admin@courtbook.com"
              password = "Admin@123"
            },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = CardSurface),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
          Text("Admin Demo", color = StatusWarning, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
      }

      Spacer(modifier = Modifier.height(32.dp))

      // Link to Register
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Don't have an account?", color = TextMuted, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(6.dp))
        TextButton(onClick = onNavigateToRegister) {
          Text("Register", color = ForestGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
      }
    }

    if (showServerDialog) {
      AlertDialog(
          onDismissRequest = { showServerDialog = false },
          title = {
            Text("Backend Server Environment", color = TextDeep, fontWeight = FontWeight.Bold, fontSize = 18.sp)
          },
          text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Text(
                  "Choose which backend server CourtBook connects to:",
                  color = TextMuted,
                  fontSize = 12.sp
              )

              // Preset 1: Render Cloud
              Card(
                  onClick = {
                    NetworkModule.setBaseUrl(context, NetworkModule.DEFAULT_RENDER_URL)
                    activeUrl = NetworkModule.DEFAULT_RENDER_URL
                    customUrlInput = NetworkModule.DEFAULT_RENDER_URL
                    viewModel.resetState()
                    showServerDialog = false
                  },
                  colors = CardDefaults.cardColors(
                      containerColor = if (activeUrl == NetworkModule.DEFAULT_RENDER_URL) SoftGreenSurface else CardSurface
                  ),
                  border = BorderStroke(1.dp, if (activeUrl == NetworkModule.DEFAULT_RENDER_URL) ForestGreen else BorderSubtle)
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Text("🌐 Render Cloud (Recommended)", fontWeight = FontWeight.Bold, color = ForestGreen, fontSize = 14.sp)
                  Text("https://courtbook-b9yl.onrender.com/api/", color = TextMuted, fontSize = 11.sp)
                  Text("Works anywhere without cables (live Atlas database)", color = TextMuted, fontSize = 10.sp)
                }
              }

              // Preset 2: Localhost USB (127.0.0.1)
              Card(
                  onClick = {
                    NetworkModule.setBaseUrl(context, NetworkModule.LOCALHOST_DEVICE_URL)
                    activeUrl = NetworkModule.LOCALHOST_DEVICE_URL
                    customUrlInput = NetworkModule.LOCALHOST_DEVICE_URL
                    viewModel.resetState()
                    showServerDialog = false
                  },
                  colors = CardDefaults.cardColors(
                      containerColor = if (activeUrl == NetworkModule.LOCALHOST_DEVICE_URL) SoftGreenSurface else CardSurface
                  ),
                  border = BorderStroke(1.dp, if (activeUrl == NetworkModule.LOCALHOST_DEVICE_URL) ForestGreen else BorderSubtle)
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Text("⚡ Localhost USB Reverse (127.0.0.1)", fontWeight = FontWeight.Bold, color = TextDeep, fontSize = 14.sp)
                  Text("http://127.0.0.1:5000/api/", color = TextMuted, fontSize = 11.sp)
                  Text("For real phones via USB (requires: adb reverse tcp:5000 tcp:5000)", color = TextMuted, fontSize = 10.sp)
                }
              }

              // Preset 3: Localhost Emulator (10.0.2.2)
              Card(
                  onClick = {
                    NetworkModule.setBaseUrl(context, NetworkModule.LOCALHOST_EMULATOR_URL)
                    activeUrl = NetworkModule.LOCALHOST_EMULATOR_URL
                    customUrlInput = NetworkModule.LOCALHOST_EMULATOR_URL
                    viewModel.resetState()
                    showServerDialog = false
                  },
                  colors = CardDefaults.cardColors(
                      containerColor = if (activeUrl == NetworkModule.LOCALHOST_EMULATOR_URL) SoftGreenSurface else CardSurface
                  ),
                  border = BorderStroke(1.dp, if (activeUrl == NetworkModule.LOCALHOST_EMULATOR_URL) ForestGreen else BorderSubtle)
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Text("💻 Localhost PC Emulator (10.0.2.2)", fontWeight = FontWeight.Bold, color = TextDeep, fontSize = 14.sp)
                  Text("http://10.0.2.2:5000/api/", color = TextMuted, fontSize = 11.sp)
                  Text("⚠️ Android Studio Emulator ONLY. Fails on physical phones!", color = StatusWarning, fontSize = 10.sp)
                }
              }

              Spacer(modifier = Modifier.height(4.dp))
              Text("Custom Server IP / Wi-Fi URL:", color = TextDeep, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
              OutlinedTextField(
                  value = customUrlInput,
                  onValueChange = { customUrlInput = it },
                  modifier = Modifier.fillMaxWidth(),
                  singleLine = true,
                  colors = OutlinedTextFieldDefaults.colors(
                      focusedBorderColor = ForestGreen,
                      unfocusedBorderColor = BorderSubtle,
                      focusedTextColor = TextDeep,
                      unfocusedTextColor = TextDeep
                  )
              )
            }
          },
          confirmButton = {
            Button(
                onClick = {
                  NetworkModule.setBaseUrl(context, customUrlInput)
                  activeUrl = NetworkModule.getBaseUrl()
                  viewModel.resetState()
                  showServerDialog = false
                },
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
            ) {
              Text("Apply", color = ElectricLime, fontWeight = FontWeight.Bold)
            }
          },
          dismissButton = {
            TextButton(onClick = { showServerDialog = false }) {
              Text("Cancel", color = TextMuted)
            }
          },
          containerColor = CardSurface
      )
    }
  }
}
