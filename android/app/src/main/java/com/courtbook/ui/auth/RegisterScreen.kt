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
      Text(
          text = "Create Account",
          color = DarkTextPrimary,
          fontSize = 28.sp,
          fontWeight = FontWeight.Bold
      )
      Text(
          text = "Join thousands of players discovering courts",
          color = DarkTextSecondary,
          fontSize = 13.sp
      )

      Spacer(modifier = Modifier.height(24.dp))

      val errorMessage = localError ?: (authState as? AuthState.Error)?.message
      if (errorMessage != null) {
        ErrorBanner(errorMessage = errorMessage, modifier = Modifier.padding(bottom = 16.dp))
      }

      OutlinedTextField(
          value = name,
          onValueChange = { name = it; localError = null },
          label = { Text("Full Name") },
          leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = DarkTextSecondary) },
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

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
          value = email,
          onValueChange = { email = it; localError = null },
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

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
          value = phone,
          onValueChange = { phone = it; localError = null },
          label = { Text("Phone Number") },
          leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = DarkTextSecondary) },
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

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
          value = password,
          onValueChange = { password = it; localError = null },
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

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
          value = confirmPassword,
          onValueChange = { confirmPassword = it; localError = null },
          label = { Text("Confirm Password") },
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

      Button(
          onClick = {
            if (password != confirmPassword) {
              localError = "Passwords do not match"
            } else {
              viewModel.register(name, email, phone, password)
            }
          },
          enabled = authState !is AuthState.Loading,
          modifier = Modifier.fillMaxWidth().height(52.dp),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = CourtGreen)
      ) {
        if (authState is AuthState.Loading) {
          CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        } else {
          Text("Create Account", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Already have an account?", color = DarkTextSecondary, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(6.dp))
        TextButton(onClick = onNavigateToLogin) {
          Text("Sign In", color = CourtGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
      }
    }
  }
}
