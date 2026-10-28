package com.courtbook.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoGraph
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
    onNavigateToAdmin: () -> Unit,
    onNavigateToAIInsights: () -> Unit = {}
) {
  val user = TokenManager.getUser()
  val isAdmin = TokenManager.isAdmin()

  Column(
      modifier = Modifier
          .fillMaxSize()
          .background(WarmBg)
          .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Spacer(modifier = Modifier.height(24.dp))

    // Profile Avatar
    Box(
        modifier = Modifier
            .size(88.dp)
            .clip(CircleShape)
            .background(SoftGreenSurface),
        contentAlignment = Alignment.Center
    ) {
      Icon(
          imageVector = Icons.Default.Person,
          contentDescription = null,
          tint = ForestGreen,
          modifier = Modifier.size(48.dp)
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = user?.name ?: "Sports Player",
        color = TextDeep,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.4).sp
    )

    Text(
        text = user?.email ?: "player@example.com",
        color = TextMuted,
        fontSize = 14.sp
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Role badge
    Surface(
        color = if (isAdmin) ElectricLime else SoftGreenSurface,
        shape = RoundedCornerShape(8.dp)
    ) {
      Text(
          text = if (isAdmin) "ADMINISTRATOR" else "PLAYER",
          color = ForestGreen,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
      )
    }

    Spacer(modifier = Modifier.height(32.dp))

    // Info Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, BorderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
      Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ProfileInfoRow(label = "Phone", value = user?.phone ?: "+91 98765 43210")
        HorizontalDivider(color = BorderSubtle)
        ProfileInfoRow(label = "Member Since", value = "September 2026")
        HorizontalDivider(color = BorderSubtle)
        ProfileInfoRow(label = "Account Status", value = "Active")
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // AI Insights Button
    Button(
        onClick = onNavigateToAIInsights,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
    ) {
      Icon(Icons.Default.AutoGraph, contentDescription = null, tint = ElectricLime)
      Spacer(modifier = Modifier.width(8.dp))
      Text("CourtBook AI Insights", color = ElectricLime, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Admin Control Shortcut
    if (isAdmin) {
      OutlinedButton(
          onClick = onNavigateToAdmin,
          modifier = Modifier
              .fillMaxWidth()
              .height(50.dp),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.outlinedButtonColors(containerColor = CardSurface),
          border = BorderStroke(1.dp, BorderSubtle)
      ) {
        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = StatusWarning)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Switch to Admin Portal", color = TextDeep, fontWeight = FontWeight.Bold, fontSize = 14.sp)
      }

      Spacer(modifier = Modifier.height(12.dp))
    }

    // Logout Button
    OutlinedButton(
        onClick = {
          TokenManager.clear()
          onLogout()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, Color(0xFFFECACA))
    ) {
      Icon(Icons.Default.ExitToApp, contentDescription = null, tint = StatusDestructive)
      Spacer(modifier = Modifier.width(8.dp))
      Text("Sign Out", color = StatusDestructive, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
  }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(label, color = TextMuted, fontSize = 13.sp)
    Text(value, color = TextDeep, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
  }
}
