package com.courtbook.ui.admin

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
            title = { Text("Slot Management") },
            navigationIcon = {
              IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DarkTextPrimary)
              }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg, titleContentColor = DarkTextPrimary)
        )
      },
      containerColor = DarkBg
  ) { paddingValues ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp)
    ) {
      Spacer(modifier = Modifier.height(12.dp))

      // Court Selector Horizontal Strip
      Text("Select Court", color = DarkTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
      Spacer(modifier = Modifier.height(8.dp))

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

      Spacer(modifier = Modifier.height(16.dp))

      Text(
          text = "Slots for ${uiState.selectedDate}",
          style = MaterialTheme.typography.titleMedium,
          color = DarkTextPrimary,
          fontWeight = FontWeight.Bold
      )

      Spacer(modifier = Modifier.height(10.dp))

      if (uiState.slots.isEmpty()) {
        StateLoader("Loading slots for selected court...")
      } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
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
      shape = RoundedCornerShape(10.dp),
      colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
  ) {
    Row(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(slot.timeRange, color = DarkTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(
            text = "Status: ${slot.status} ${if (slot.isBlocked) "(${slot.blockReason ?: "Maintenance"})" else ""}",
            color = when {
              slot.isAvailable -> StatusAvailable
              slot.isBlocked -> StatusBlocked
              slot.isBooked -> StatusBooked
              else -> StatusHeld
            },
            fontSize = 12.sp
        )
      }

      when {
        slot.isAvailable -> {
          Button(
              onClick = onBlockClick,
              colors = ButtonDefaults.buttonColors(containerColor = StatusBlocked),
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
          ) {
            Text("Block Slot", fontSize = 12.sp, color = Color.White)
          }
        }
        slot.isBlocked -> {
          Button(
              onClick = onUnblockClick,
              colors = ButtonDefaults.buttonColors(containerColor = CourtGreen),
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
          ) {
            Text("Unblock", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
          }
        }
        slot.isBooked -> {
          Text("Booked", color = StatusBooked, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
