package com.courtbook.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.courtbook.data.remote.TokenManager
import com.courtbook.ui.theme.*

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onNavigateToAdmin: () -> Unit
) {
  val user = TokenManager.getUser()
  val isAdmin = TokenManager.isAdmin()

  Column(
      modifier = Modifier
          .fillMaxSize()
          .background(DarkBg)
          .padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Spacer(modifier = Modifier.height(24.dp))

    // Profile Avatar
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(CourtGreen.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
      Icon(
          imageVector = Icons.Default.Person,
          contentDescription = null,
          tint = CourtGreen,
          modifier = Modifier.size(44.dp)
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = user?.name ?: "Sports Player",
        color = DarkTextPrimary,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold
    )

    Text(
        text = user?.email ?: "player@example.com",
        color = DarkTextSecondary,
        fontSize = 14.sp
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Role badge
    Surface(
        color = if (isAdmin) AccentAmber.copy(alpha = 0.2f) else CourtGreen.copy(alpha = 0.2f),
        shape = RoundedCornerShape(8.dp)
    ) {
      Text(
          text = if (isAdmin) "ADMINISTRATOR" else "PLAYER",
          color = if (isAdmin) AccentAmber else CourtGreen,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
      )
    }

    Spacer(modifier = Modifier.height(32.dp))

    // Info Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ProfileInfoRow(label = "Phone", value = user?.phone ?: "+91 98765 43210")
        HorizontalDivider(color = DarkBorder)
        ProfileInfoRow(label = "Member Since", value = "September 2026")
        HorizontalDivider(color = DarkBorder)
        ProfileInfoRow(label = "Account Status", value = "Active")
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Admin Control Shortcut
    if (isAdmin) {
      Button(
          onClick = onNavigateToAdmin,
          modifier = Modifier.fillMaxWidth().height(48.dp),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = AccentAmber)
      ) {
        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color.Black)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Open Admin Dashboard", color = Color.Black, fontWeight = FontWeight.Bold)
      }

      Spacer(modifier = Modifier.height(12.dp))
    }

    // Logout Button
    OutlinedButton(
        onClick = {
          TokenManager.clear()
          onLogout()
        },
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusBooked)
    ) {
      Icon(Icons.Default.ExitToApp, contentDescription = null, tint = StatusBooked)
      Spacer(modifier = Modifier.width(8.dp))
      Text("Sign Out", fontWeight = FontWeight.SemiBold)
    }
  }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(label, color = DarkTextSecondary, fontSize = 13.sp)
    Text(value, color = DarkTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
  }
}
