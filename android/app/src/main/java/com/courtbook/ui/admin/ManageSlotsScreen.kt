package com.courtbook.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.courtbook.data.model.SlotDto
import com.courtbook.ui.components.SportCategoryChip
import com.courtbook.ui.components.StateLoader
import com.courtbook.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageSlotsScreen(
    viewModel: AdminViewModel,
    onNavigateBack: () -> Unit
) {
  LaunchedEffect(Unit) {
    viewModel.initManageSlots()
  }

  val uiState by viewModel.uiState.collectAsState()

  Scaffold(
      topBar = {
        TopAppBar(
            title = {
              Text(
                  text = "Slot Management",
                  fontWeight = FontWeight.Bold,
                  fontSize = 18.sp,
                  color = TextDeep
              )
            },
            navigationIcon = {
              IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextDeep)
              }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = CardSurface,
                titleContentColor = TextDeep
            )
        )
      },
      containerColor = WarmBg
  ) { paddingValues ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 20.dp)
    ) {
      Spacer(modifier = Modifier.height(16.dp))

      // Court Selector Horizontal Strip
      Text("Select Court", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
      Spacer(modifier = Modifier.height(10.dp))

      Row(
          modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        uiState.courts.forEach { court ->
          val isSelected = uiState.selectedCourtId == court.id
          SportCategoryChip(
              sport = court.name,
              isSelected = isSelected,
              onSelect = { viewModel.selectCourt(court.id) }
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      Text(
          text = "Slots for ${uiState.selectedDate}",
          color = TextDeep,
          fontSize = 17.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = (-0.3).sp
      )

      Spacer(modifier = Modifier.height(12.dp))

      if (uiState.slots.isEmpty()) {
        StateLoader("Loading slots for selected court...")
      } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
          items(uiState.slots) { slot ->
            AdminSlotRow(
                slot = slot,
                onBlockClick = { viewModel.blockSlot(slot.id, "Maintenance") },
                onUnblockClick = { viewModel.unblockSlot(slot.id) }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun AdminSlotRow(
    slot: SlotDto,
    onBlockClick: () -> Unit,
    onUnblockClick: () -> Unit
) {
  Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = CardSurface),
      border = BorderStroke(1.dp, BorderSubtle),
      elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(slot.timeRange, color = TextDeep, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Status: ${slot.status} ${if (slot.isBlocked) "(${slot.blockReason ?: "Maintenance"})" else ""}",
            color = when {
              slot.isAvailable -> StatusSuccess
              slot.isBlocked -> StatusUnavailable
              slot.isBooked -> StatusDestructive
              else -> StatusWarning
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
      }

      when {
        slot.isAvailable -> {
          Button(
              onClick = onBlockClick,
              colors = ButtonDefaults.buttonColors(containerColor = SoftGreenSurface),
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
          ) {
            Text("Block Slot", fontSize = 12.sp, color = TextDeep, fontWeight = FontWeight.SemiBold)
          }
        }
        slot.isBlocked -> {
          Button(
              onClick = onUnblockClick,
              colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
          ) {
            Text("Unblock", fontSize = 12.sp, color = ElectricLime, fontWeight = FontWeight.Bold)
          }
        }
        slot.isBooked -> {
          Surface(
              color = Color(0xFFFEF2F2),
              shape = RoundedCornerShape(6.dp)
          ) {
            Text(
                text = "Booked",
                color = StatusDestructive,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }
      }
    }
  }
}
