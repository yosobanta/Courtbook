package com.courtbook.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.courtbook.ui.components.ErrorBanner
import com.courtbook.ui.theme.*

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: (isAdmin: Boolean) -> Unit,
    onNavigateToRegister: () -> Unit
) {
  var email by remember { mutableStateOf("alex@example.com") }
  var password by remember { mutableStateOf("Player@123") }

  val authState by viewModel.authState.collectAsState()

  LaunchedEffect(authState) {
    if (authState is AuthState.Success) {
      val user = (authState as AuthState.Success).user
      viewModel.resetState()
      onLoginSuccess(user.isAdmin)
    }
  }

  Box(
      modifier = Modifier
          .fillMaxSize()
          .background(DarkBg)
          .padding(24.dp)
  ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
      // App Branding
      Icon(
          imageVector = Icons.Default.SportsTennis,
          contentDescription = "Logo",
          tint = CourtGreen,
          modifier = Modifier.size(64.dp)
      )
      Spacer(modifier = Modifier.height(12.dp))
      Text(
          text = "CourtBook",
          color = DarkTextPrimary,
          fontSize = 32.sp,
          fontWeight = FontWeight.Bold
      )
      Text(
          text = "Find. Book. Play.",
          color = CourtGreen,
          fontSize = 14.sp,
          fontWeight = FontWeight.SemiBold
      )

      Spacer(modifier = Modifier.height(32.dp))

      // Error message banner
      if (authState is AuthState.Error) {
        ErrorBanner(
            errorMessage = (authState as AuthState.Error).message,
            modifier = Modifier.padding(bottom = 16.dp)
        )
      }

      // Input fields
      OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text("Email Address") },
          leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = DarkTextSecondary) },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = CourtGreen,
              unfocusedBorderColor = DarkBorder,
              focusedTextColor = DarkTextPrimary,
              unfocusedTextColor = DarkTextPrimary
          ),
          singleLine = true
      )

      Spacer(modifier = Modifier.height(16.dp))

      OutlinedTextField(
          value = password,
          onValueChange = { password = it },
          label = { Text("Password") },
          leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = DarkTextSecondary) },
          visualTransformation = PasswordVisualTransformation(),
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = CourtGreen,
              unfocusedBorderColor = DarkBorder,
              focusedTextColor = DarkTextPrimary,
              unfocusedTextColor = DarkTextPrimary
          ),
          singleLine = true
      )

      Spacer(modifier = Modifier.height(24.dp))

      // Sign In Button
      Button(
          onClick = { viewModel.login(email, password) },
          enabled = authState !is AuthState.Loading,
          modifier = Modifier.fillMaxWidth().height(52.dp),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = CourtGreen)
      ) {
        if (authState is AuthState.Loading) {
          CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        } else {
          Text("Sign In", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Quick Demo Credentials Buttons
      Text("Or test with pre-seeded accounts:", color = DarkTextSecondary, fontSize = 12.sp)
      Spacer(modifier = Modifier.height(8.dp))

      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            onClick = {
              email = "alex@example.com"
              password = "Player@123"
            },
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CourtGreen.copy(alpha = 0.5f))
        ) {
          Text("Player Demo", color = CourtGreen, fontSize = 12.sp)
        }

        OutlinedButton(
            onClick = {
              email = "admin@courtbook.com"
              password = "Admin@123"
            },
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, AccentAmber.copy(alpha = 0.5f))
        ) {
          Text("Admin Demo", color = AccentAmber, fontSize = 12.sp)
        }
      }

      Spacer(modifier = Modifier.height(32.dp))

      // Link to Register
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Don't have an account?", color = DarkTextSecondary, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(6.dp))
        TextButton(onClick = onNavigateToRegister) {
          Text("Register", color = CourtGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
      }
    }
  }
}
