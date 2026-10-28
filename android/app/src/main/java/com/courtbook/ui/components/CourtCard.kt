package com.courtbook.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.courtbook.data.model.CourtDto
import com.courtbook.ui.theme.*

@Composable
fun CourtCard(
    court: CourtDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  Card(
      modifier = modifier
          .fillMaxWidth()
          .clickable(onClick = onClick),
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = CardSurface),
      border = BorderStroke(1.dp, BorderSubtle),
      elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Column {
      // Court Image Banner with Sport Chip Overlay
      Box(
          modifier = Modifier
              .fillMaxWidth()
              .height(160.dp)
      ) {
        val imageUrl = court.images.firstOrNull() ?: "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?auto=format&fit=crop&w=800&q=80"
        AsyncImage(
            model = imageUrl,
            contentDescription = court.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Sport Category Badge (Vivid Electric Lime)
        Surface(
            modifier = Modifier
                .padding(12.dp)
                .align(Alignment.TopStart),
            color = ElectricLime,
            shape = RoundedCornerShape(8.dp),
            shadowElevation = 2.dp
        ) {
          Text(
              text = court.sport.uppercase(),
              color = ForestGreen,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
          )
        }

        // Indoor / Outdoor Pill
        Surface(
            modifier = Modifier
                .padding(12.dp)
                .align(Alignment.TopEnd),
            color = Color.Black.copy(alpha = 0.65f),
            shape = RoundedCornerShape(8.dp)
        ) {
          Text(
              text = if (court.isIndoor) "Indoor Court" else "Outdoor Court",
              color = Color.White,
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      // Details Body
      Column(modifier = Modifier.padding(18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
              text = court.name,
              color = TextDeep,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.weight(1f)
          )

          Surface(
              color = Color(0xFFFEF3C7),
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
                  modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                  text = "4.8",
                  color = TextDeep,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Venue Address
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
              imageVector = Icons.Default.LocationOn,
              contentDescription = "Location",
              tint = TextMuted,
              modifier = Modifier.size(15.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
              text = court.venueAddress.ifEmpty { "Sports Complex Arena" },
              color = TextMuted,
              fontSize = 13.sp,
              maxLines = 1
          )
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = BorderSubtle)
        Spacer(modifier = Modifier.height(14.dp))

        // Price & Book Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
                text = "Starting from",
                color = TextMuted,
                fontSize = 11.sp
            )
            Text(
                text = "₹${court.basePrice.toInt()}/hr",
                color = ForestGreen,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
          }

          Button(
              onClick = onClick,
              colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
              shape = RoundedCornerShape(12.dp),
              contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
          ) {
            Text(
                text = "Book Court",
                color = ElectricLime,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
          }
        }
      }
    }
  }
}
