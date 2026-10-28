package com.courtbook.ui.explore

import androidx.compose.foundation.BorderStroke
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
  LaunchedEffect(Unit) {
    viewModel.searchCourts()
  }

  val uiState by viewModel.uiState.collectAsState()

  Column(
      modifier = Modifier
          .fillMaxSize()
          .background(WarmBg)
          .padding(horizontal = 20.dp)
  ) {
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = "Explore Courts",
        color = TextDeep,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Search Input Field
    OutlinedTextField(
        value = uiState.searchQuery,
        onValueChange = { viewModel.updateSearchQuery(it) },
        placeholder = { Text("Search by court name, venue or location...", color = TextMuted) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ForestGreen) },
        trailingIcon = {
          if (uiState.searchQuery.isNotEmpty()) {
            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
              Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
            }
          }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = CardSurface,
            unfocusedContainerColor = CardSurface,
            focusedBorderColor = ForestGreen,
            unfocusedBorderColor = BorderSubtle,
            focusedTextColor = TextDeep,
            unfocusedTextColor = TextDeep
        ),
        singleLine = true
    )

    Spacer(modifier = Modifier.height(14.dp))

    // Horizontal Filter Strip
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
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
          label = {
            Text(
                text = "Indoor Only",
                fontWeight = if (uiState.isIndoorOnly) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.sp
            )
          },
          shape = RoundedCornerShape(20.dp),
          colors = FilterChipDefaults.filterChipColors(
              containerColor = CardSurface,
              selectedContainerColor = ForestGreen,
              labelColor = TextDeep,
              selectedLabelColor = ElectricLime
          ),
          border = if (uiState.isIndoorOnly) null else FilterChipDefaults.filterChipBorder(
              enabled = true,
              selected = false,
              borderColor = BorderSubtle
          )
      )

      // Filter: Under ₹500
      FilterChip(
          selected = uiState.maxPrice == 500.0,
          onClick = { viewModel.setMaxPrice(if (uiState.maxPrice == 500.0) null else 500.0) },
          label = {
            Text(
                text = "Under ₹500",
                fontWeight = if (uiState.maxPrice == 500.0) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.sp
            )
          },
          shape = RoundedCornerShape(20.dp),
          colors = FilterChipDefaults.filterChipColors(
              containerColor = CardSurface,
              selectedContainerColor = ForestGreen,
              labelColor = TextDeep,
              selectedLabelColor = ElectricLime
          ),
          border = if (uiState.maxPrice == 500.0) null else FilterChipDefaults.filterChipBorder(
              enabled = true,
              selected = false,
              borderColor = BorderSubtle
          )
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Results List
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 88.dp)
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
                description = "Try adjusting your search query, selecting another sport, or clearing price filters."
            )
          }
        }
        else -> {
          item {
            Text(
                text = "Showing ${uiState.courts.size} court${if (uiState.courts.size > 1) "s" else ""}",
                color = TextMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
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
