package com.courtbook.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.courtbook.ui.components.*
import com.courtbook.ui.theme.*

@Composable
fun ExploreScreen(
    viewModel: ExploreViewModel,
    onNavigateToCourtDetails: (courtId: String) -> Unit
) {
  val uiState by viewModel.uiState.collectAsState()

  Column(
      modifier = Modifier
          .fillMaxSize()
          .background(DarkBg)
          .padding(horizontal = 16.dp)
  ) {
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = "Explore Courts",
        color = DarkTextPrimary,
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(14.dp))

    // Search Input Field
    OutlinedTextField(
        value = uiState.searchQuery,
        onValueChange = { viewModel.updateSearchQuery(it) },
        placeholder = { Text("Search by name, area or sport...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = DarkTextSecondary) },
        trailingIcon = {
          if (uiState.searchQuery.isNotEmpty()) {
            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
              Icon(Icons.Default.Close, contentDescription = "Clear", tint = DarkTextSecondary)
            }
          }
        },
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

    // Horizontal Filter Strip
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      viewModel.sports.forEach { sport ->
        SportCategoryChip(
            sport = if (sport == "ALL") "All Sports" else sport.replace("_", " "),
            isSelected = uiState.selectedSport == sport,
            onSelect = { viewModel.selectSport(sport) }
        )
      }

      // Filter: Indoor Only
      FilterChip(
          selected = uiState.isIndoorOnly,
          onClick = { viewModel.toggleIndoorOnly() },
          label = { Text("Indoor Only") },
          colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = CourtGreen,
              selectedLabelColor = Color.Black
          )
      )

      // Filter: Under ₹500
      FilterChip(
          selected = uiState.maxPrice == 500.0,
          onClick = { viewModel.setMaxPrice(if (uiState.maxPrice == 500.0) null else 500.0) },
          label = { Text("Under ₹500") },
          colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = CourtGreen,
              selectedLabelColor = Color.Black
          )
      )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Results List
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
      when {
        uiState.isLoading -> {
          item { StateLoader("Searching available courts...") }
        }
        uiState.errorMessage != null -> {
          item {
            ErrorBanner(
                errorMessage = uiState.errorMessage!!,
                onRetry = { viewModel.searchCourts() }
            )
          }
        }
        uiState.courts.isEmpty() -> {
          item {
            EmptyStateView(
                title = "No matching courts found",
                description = "Try adjusting your search criteria or price filters."
            )
          }
        }
        else -> {
          item {
            Text(
                text = "${uiState.courts.size} courts found",
                color = DarkTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
          }
          items(uiState.courts) { court ->
            CourtCard(
                court = court,
                onClick = { onNavigateToCourtDetails(court.id) }
            )
          }
        }
      }
    }
  }
}
