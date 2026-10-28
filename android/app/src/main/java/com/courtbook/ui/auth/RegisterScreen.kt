package com.courtbook.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
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
fun RegisterScreen(
    viewModel: AuthViewModel,
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
  var name by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var phone by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var confirmPassword by remember { mutableStateOf("") }
  var localError by remember { mutableStateOf<String?>(null) }

  val authState by viewModel.authState.collectAsState()

  LaunchedEffect(authState) {
    if (authState is AuthState.Success) {
      viewModel.resetState()
      onRegisterSuccess()
    }
  }

  Box(
      modifier = Modifier
          .fillMaxSize()
          .background(WarmBg)
          .padding(24.dp)
  ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
      Text(
          text = "Create Account",
          color = TextDeep,
          fontSize = 28.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = (-0.5).sp
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
          text = "Join thousands of players discovering courts",
          color = TextMuted,
          fontSize = 14.sp
      )

      Spacer(modifier = Modifier.height(28.dp))

      val errorMessage = localError ?: (authState as? AuthState.Error)?.message
      if (errorMessage != null) {
        ErrorBanner(errorMessage = errorMessage, modifier = Modifier.padding(bottom = 16.dp))
      }

      OutlinedTextField(
          value = name,
          onValueChange = { name = it; localError = null },
          label = { Text("Full Name") },
          leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TextMuted) },
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

      Spacer(modifier = Modifier.height(14.dp))

      OutlinedTextField(
          value = email,
          onValueChange = { email = it; localError = null },
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

      Spacer(modifier = Modifier.height(14.dp))

      OutlinedTextField(
          value = phone,
          onValueChange = { phone = it; localError = null },
          label = { Text("Phone Number") },
          leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = TextMuted) },
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

      Spacer(modifier = Modifier.height(14.dp))

      OutlinedTextField(
          value = password,
          onValueChange = { password = it; localError = null },
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

      Spacer(modifier = Modifier.height(14.dp))

      OutlinedTextField(
          value = confirmPassword,
          onValueChange = { confirmPassword = it; localError = null },
          label = { Text("Confirm Password") },
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

      Button(
          onClick = {
            if (name.isBlank() || email.isBlank() || password.isBlank()) {
              localError = "Please fill in all required fields"
              return@Button
            }
            if (password != confirmPassword) {
              localError = "Passwords do not match"
              return@Button
            }
            viewModel.register(name, email, password, phone)
          },
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
          Text("Create Account", color = ElectricLime, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Already have an account?", color = TextMuted, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(6.dp))
        TextButton(onClick = onNavigateToLogin) {
          Text("Sign In", color = ForestGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
      }
    }
  }
}
