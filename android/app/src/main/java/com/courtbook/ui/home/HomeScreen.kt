package com.courtbook.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalOffer
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
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToCourtDetails: (courtId: String) -> Unit,
    onNavigateToExplore: () -> Unit
) {
  val uiState by viewModel.uiState.collectAsState()

  LazyColumn(
      modifier = Modifier
          .fillMaxSize()
          .background(DarkBg)
          .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      contentPadding = PaddingValues(top = 24.dp, bottom = 80.dp)
  ) {
    // 1. Personalized Header
    item {
      Column {
        val playerName = uiState.user?.name?.split(" ")?.firstOrNull() ?: "Player"
        Text(
            text = "Hi, $playerName 👋",
            color = DarkTextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Ready to play today?",
            color = DarkTextSecondary,
            fontSize = 14.sp
        )
      }
    }

    // 2. Search Bar Trigger
    item {
      Card(
          modifier = Modifier
              .fillMaxWidth()
              .clickable(onClick = onNavigateToExplore),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
      ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
              imageVector = Icons.Default.Search,
              contentDescription = "Search",
              tint = DarkTextSecondary,
              modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(12.dp))
          Text(
              text = "Search courts, sports or locations...",
              color = DarkTextSecondary,
              fontSize = 14.sp
          )
        }
      }
    }

    // 3. Horizontal Sports Category Chips
    item {
      Column {
        Text(
            text = "Sports",
            style = MaterialTheme.typography.titleLarge,
            color = DarkTextPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          viewModel.sportsCategories.forEach { sport ->
            SportCategoryChip(
                sport = if (sport == "ALL") "All Sports" else sport.replace("_", " "),
                isSelected = uiState.selectedSport == sport,
                onSelect = { viewModel.selectSport(sport) }
            )
          }
        }
      }
    }

    // 4. Promo Banner (if active offers exist)
    if (uiState.offers.isNotEmpty()) {
      item {
        val topOffer = uiState.offers.first()
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CourtGreenDark)
        ) {
          Row(
              modifier = Modifier.padding(16.dp),
              verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CourtGreen.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
              Icon(
                  imageVector = Icons.Default.LocalOffer,
                  contentDescription = "Offer",
                  tint = CourtGreen,
                  modifier = Modifier.size(24.dp)
              )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                  text = topOffer.title,
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                  fontSize = 15.sp
              )
              Text(
                  text = "Use code ${topOffer.code} • ${topOffer.description}",
                  color = Color.White.copy(alpha = 0.8f),
                  fontSize = 12.sp
              )
            }
          }
        }
      }
    }

    // 5. Courts Feed Section
    item {
      SectionHeader(
          title = if (uiState.selectedSport == "ALL") "Popular Courts" else "${uiState.selectedSport.replace("_", " ")} Courts",
          actionText = "Explore All",
          onActionClick = onNavigateToExplore
      )
    }

    // State Rendering
    when {
      uiState.isLoading -> {
        item { StateLoader("Finding courts near you...") }
      }
      uiState.errorMessage != null -> {
        item {
          ErrorBanner(
              errorMessage = uiState.errorMessage!!,
              onRetry = { viewModel.loadHomeData() }
          )
        }
      }
      uiState.courts.isEmpty() -> {
        item {
          EmptyStateView(
              title = "No courts found",
              description = "Try selecting another sport category or clearing search filters."
          )
        }
      }
      else -> {
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
