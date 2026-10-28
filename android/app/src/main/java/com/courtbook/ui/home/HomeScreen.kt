package com.courtbook.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.courtbook.ui.components.*
import com.courtbook.ui.theme.*

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToCourtDetails: (courtId: String) -> Unit,
    onNavigateToExplore: () -> Unit
) {
  LaunchedEffect(Unit) {
    viewModel.loadHomeData()
  }

  val uiState by viewModel.uiState.collectAsState()

  LazyColumn(
      modifier = Modifier
          .fillMaxSize()
          .background(WarmBg)
          .padding(horizontal = 20.dp),
      verticalArrangement = Arrangement.spacedBy(20.dp),
      contentPadding = PaddingValues(top = 24.dp, bottom = 88.dp)
  ) {
    // 1. Personalized Header
    item {
      Column {
        val playerName = uiState.user?.name?.split(" ")?.firstOrNull() ?: "Player"
        Text(
            text = "Hi, $playerName 👋",
            color = TextDeep,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = "Ready to discover and book a court today?",
            color = TextMuted,
            fontSize = 14.sp
        )
      }
    }

    // 2. Search Bar Trigger
    item {
      Surface(
          modifier = Modifier
              .fillMaxWidth()
              .clickable(onClick = onNavigateToExplore),
          shape = RoundedCornerShape(16.dp),
          color = CardSurface,
          border = BorderStroke(1.dp, BorderSubtle),
          shadowElevation = 1.dp
      ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
              imageVector = Icons.Default.Search,
              contentDescription = "Search",
              tint = ForestGreen,
              modifier = Modifier.size(22.dp)
          )
          Spacer(modifier = Modifier.width(12.dp))
          Text(
              text = "Search courts, sports, or locations...",
              color = TextMuted,
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
            color = TextDeep,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
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
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = ForestGreen),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
          Row(
              modifier = Modifier.padding(18.dp),
              verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ElectricLime.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
              Icon(
                  imageVector = Icons.Default.LocalOffer,
                  contentDescription = "Offer",
                  tint = ElectricLime,
                  modifier = Modifier.size(24.dp)
              )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                  text = topOffer.title,
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                  fontSize = 16.sp
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                  text = topOffer.description,
                  color = Color.White.copy(alpha = 0.8f),
                  fontSize = 12.sp
              )
              Spacer(modifier = Modifier.height(6.dp))
              Surface(
                  color = ElectricLime,
                  shape = RoundedCornerShape(6.dp)
              ) {
                Text(
                    text = "USE CODE: ${topOffer.code}",
                    color = ForestGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
              }
            }
          }
        }
      }
    }

    // 4b. Featured Turfs & Arenas
    if (uiState.venues.isNotEmpty()) {
      item {
        Column {
          SectionHeader(title = "Featured Arenas & Venues")
          Spacer(modifier = Modifier.height(12.dp))
          Row(
              modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            uiState.venues.forEach { venue ->
              Card(
                  modifier = Modifier
                      .width(260.dp)
                      .clickable(onClick = onNavigateToExplore),
                  shape = RoundedCornerShape(16.dp),
                  colors = CardDefaults.cardColors(containerColor = CardSurface),
                  border = BorderStroke(1.dp, BorderSubtle),
                  elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
              ) {
                Column {
                  // Venue Image Banner
                  Box(
                      modifier = Modifier
                          .fillMaxWidth()
                          .height(110.dp)
                  ) {
                    val imageUrl = venue.images.firstOrNull() ?: "https://images.unsplash.com/photo-1546519638-68e109498ffc?auto=format&fit=crop&w=800&q=80"
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = venue.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    Surface(
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.TopEnd),
                        color = CardSurface.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                      Row(
                          verticalAlignment = Alignment.CenterVertically,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                      ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = StatusWarning,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${venue.rating?.average ?: 4.8}",
                            color = TextDeep,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                      }
                    }
                  }

                  Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = venue.name,
                        color = TextDeep,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                          imageVector = Icons.Default.LocationOn,
                          contentDescription = null,
                          tint = TextMuted,
                          modifier = Modifier.size(13.dp)
                      )
                      Spacer(modifier = Modifier.width(3.dp))
                      Text(
                          text = "${venue.city} • ${venue.address}",
                          color = TextMuted,
                          fontSize = 12.sp,
                          maxLines = 1
                      )
                    }

                    if (venue.amenities.isNotEmpty()) {
                      Spacer(modifier = Modifier.height(8.dp))
                      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        venue.amenities.take(2).forEach { amenity ->
                          Surface(
                              color = SoftGreenSurface,
                              shape = RoundedCornerShape(4.dp)
                          ) {
                            Text(
                                text = amenity,
                                color = ForestGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                          }
                        }
                      }
                    }
                  }
                }
              }
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
