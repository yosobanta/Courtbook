package com.courtbook.ui.components

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
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column {
      // Court Image Banner with Sport Chip Overlay
      Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
        val imageUrl = court.images.firstOrNull() ?: "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?auto=format&fit=crop&w=800&q=80"
        AsyncImage(
            model = imageUrl,
            contentDescription = court.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Sport Category Badge
        Box(
            modifier = Modifier
                .padding(12.dp)
                .align(Alignment.TopStart)
                .clip(RoundedCornerShape(8.dp))
                .background(CourtGreen.copy(alpha = 0.9f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
              text = court.sport,
              color = Color.Black,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
          )
        }

        // Indoor / Outdoor Pill
        Box(
            modifier = Modifier
                .padding(12.dp)
                .align(Alignment.TopEnd)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
              text = if (court.isIndoor) "Indoor" else "Outdoor",
              color = Color.White,
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium
          )
        }
      }

      // Details Body
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
              text = court.name,
              style = MaterialTheme.typography.titleLarge,
              color = DarkTextPrimary,
              modifier = Modifier.weight(1f)
          )

          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Rating",
                tint = AccentAmber,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "4.7",
                color = DarkTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
          }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Venue Address
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
              imageVector = Icons.Default.LocationOn,
              contentDescription = "Location",
              tint = DarkTextSecondary,
              modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
              text = court.venueAddress.ifEmpty { "Bengaluru Arena" },
              color = DarkTextSecondary,
              fontSize = 12.sp,
              maxLines = 1
          )
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(12.dp))

        // Price & Book Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
                text = "Starting from",
                color = DarkTextSecondary,
                fontSize = 11.sp
            )
            Text(
                text = "₹${court.basePrice.toInt()}/hr",
                color = CourtGreen,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
          }

          Button(
              onClick = onClick,
              colors = ButtonDefaults.buttonColors(containerColor = CourtGreen),
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
          ) {
            Text(
                text = "Book Court",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
          }
        }
      }
    }
  }
}
