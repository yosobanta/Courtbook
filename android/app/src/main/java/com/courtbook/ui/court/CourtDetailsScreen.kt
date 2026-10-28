package com.courtbook.ui.court

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.courtbook.data.model.SlotDto
import com.courtbook.ui.components.*
import com.courtbook.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourtDetailsScreen(
    courtId: String,
    viewModel: CourtViewModel,
    onNavigateBack: () -> Unit,
    onProceedToSummary: (courtId: String, slotIds: List<String>) -> Unit
) {
  LaunchedEffect(courtId) {
    viewModel.initCourt(courtId)
  }

  val uiState by viewModel.uiState.collectAsState()

  Scaffold(
      topBar = {
        TopAppBar(
            title = {
              Text(
                  text = uiState.court?.name ?: "Court Details",
                  maxLines = 1,
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
      bottomBar = {
        // Sticky Bottom Booking Bar
        if (uiState.selectedSlots.isNotEmpty()) {
          Surface(
              color = CardSurface,
              tonalElevation = 8.dp,
              border = BorderStroke(1.dp, BorderSubtle),
              modifier = Modifier.fillMaxWidth()
          ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                    text = "${uiState.selectedSlots.size} slot(s) selected",
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "₹${uiState.totalPrice.toInt()}",
                    color = ForestGreen,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
              }

              Button(
                  onClick = { viewModel.proceedToHold(onProceedToSummary) },
                  enabled = uiState.canProceed,
                  colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.height(48.dp)
              ) {
                if (uiState.isHoldingSlots || uiState.isReleasingHold) {
                  CircularProgressIndicator(color = ElectricLime, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                  val hasMyHold = uiState.selectedSlots.any { it.isHeldByMe(uiState.currentUserId) }
                  Text(
                      text = if (hasMyHold) "Proceed to Checkout" else "Proceed to Book",
                      color = ElectricLime,
                      fontWeight = FontWeight.Bold,
                      fontSize = 14.sp
                  )
                }
              }
            }
          }
        }
      },
      containerColor = WarmBg
  ) { paddingValues ->
    if (uiState.isLoadingCourt) {
      StateLoader("Loading court details...", modifier = Modifier.padding(paddingValues))
    } else if (uiState.court == null) {
      ErrorBanner(
          errorMessage = uiState.errorMessage ?: "Court details unavailable",
          onRetry = { viewModel.initCourt(courtId) },
          modifier = Modifier.padding(paddingValues).padding(16.dp)
      )
    } else {
      val court = uiState.court!!

      LazyColumn(
          modifier = Modifier
              .fillMaxSize()
              .padding(paddingValues)
              .padding(horizontal = 20.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
          contentPadding = PaddingValues(
              top = 14.dp,
              bottom = if (uiState.selectedSlots.isNotEmpty()) 100.dp else 36.dp
          )
      ) {
        // 1. Compact Venue Image & Sport Badge
        item {
          Box(
              modifier = Modifier
                  .fillMaxWidth()
                  .height(140.dp)
                  .clip(RoundedCornerShape(16.dp))
          ) {
            val image = court.images.firstOrNull()
                ?: "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?auto=format&fit=crop&w=800&q=80"
            AsyncImage(
                model = image,
                contentDescription = court.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Surface(
                modifier = Modifier
                    .padding(10.dp)
                    .align(Alignment.TopStart),
                color = ElectricLime,
                shape = RoundedCornerShape(8.dp),
                shadowElevation = 2.dp
            ) {
              Text(
                  text = court.sport.uppercase(),
                  color = ForestGreen,
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.sp,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
              )
            }
          }
        }

        // 2. Compact Court & Venue Meta
        item {
          Column {
            Text(
                text = court.name,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = TextDeep,
                letterSpacing = (-0.3).sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                  modifier = Modifier.weight(1f),
                  verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = TextMuted, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = court.venueAddress.ifEmpty { "Bengaluru Arena" },
                    color = TextMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
              }

              Spacer(modifier = Modifier.width(8.dp))

              Surface(
                  color = SoftGreenSurface,
                  shape = RoundedCornerShape(6.dp)
              ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.Star, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(13.dp))
                  Spacer(modifier = Modifier.width(3.dp))
                  Text(
                      text = "4.8 (56)",
                      color = TextDeep,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Surface & Capacity Badges
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Surface(
                  color = SoftGreenSurface,
                  shape = RoundedCornerShape(8.dp),
                  border = BorderStroke(1.dp, BorderSubtle)
              ) {
                Text(
                    text = court.surfaceType.replace("_", " "),
                    color = ForestGreen,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
              Surface(
                  color = SoftGreenSurface,
                  shape = RoundedCornerShape(8.dp),
                  border = BorderStroke(1.dp, BorderSubtle)
              ) {
                Text(
                    text = "Capacity: ${court.capacity} players",
                    color = ForestGreen,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
            }
          }
        }

        // 3. Error Banner (e.g. 409 Conflict)
        if (uiState.errorMessage != null) {
          item {
            ErrorBanner(
                errorMessage = uiState.errorMessage!!,
                isConflict = uiState.isConflictError,
                onRetry = { viewModel.loadSlotsForCurrentDate(court.id) }
            )
          }
        }

        // 4. Compact Expandable 14-Day Calendar
        item {
          Expandable14DayCalendar(
              selectedDate = uiState.selectedDate,
              onDateSelected = { dateStr ->
                viewModel.selectDate(dateStr)
              }
          )
        }

        // 5. Slot Status Legend (Directly under calendar)
        item {
          AvailabilityLegend()
        }

        // 6. Slots Section Header
        item {
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
                text = "Available Time Slots",
                color = TextDeep,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp
            )

            if (uiState.slots.isNotEmpty()) {
              val availableCount = uiState.slots.count { it.isAvailable }
              Surface(
                  color = SoftGreenSurface,
                  shape = RoundedCornerShape(6.dp)
              ) {
                Text(
                    text = "$availableCount Available",
                    color = ForestGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
          }
        }

        // Active Hold Banner (if user currently has held slots)
        if (uiState.myHeldSlots.isNotEmpty() && !uiState.isLoadingSlots) {
          item {
            val remainingSec = uiState.remainingHoldSeconds
            val minutes = remainingSec / 60
            val seconds = remainingSec % 60
            val timerText = String.format("%02d:%02d", minutes, seconds)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SoftGreenSurface),
                border = BorderStroke(1.5.dp, ForestGreen.copy(alpha = 0.4f))
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(ForestGreen)
                    )
                    Text(
                        text = "Reserved for You",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreen
                    )
                  }

                  Surface(
                      color = ForestGreen,
                      shape = RoundedCornerShape(8.dp)
                  ) {
                    Text(
                        text = "⏱ $timerText",
                        color = ElectricLime,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                  }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "You have ${uiState.myHeldSlots.size} slot(s) reserved on temporary hold. Complete your booking before the timer expires or release it for others.",
                    fontSize = 12.sp,
                    color = TextDeep,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  Button(
                      onClick = { viewModel.proceedToHold(onProceedToSummary) },
                      colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                      shape = RoundedCornerShape(10.dp),
                      modifier = Modifier.weight(1f).height(40.dp)
                  ) {
                    Text(
                        text = "Proceed to Pay",
                        color = ElectricLime,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                  }

                  OutlinedButton(
                      onClick = { viewModel.releaseHold() },
                      enabled = !uiState.isReleasingHold,
                      border = BorderStroke(1.dp, ForestGreen.copy(alpha = 0.5f)),
                      shape = RoundedCornerShape(10.dp),
                      modifier = Modifier.weight(1f).height(40.dp)
                  ) {
                    if (uiState.isReleasingHold) {
                      CircularProgressIndicator(modifier = Modifier.size(16.dp), color = ForestGreen, strokeWidth = 2.dp)
                    } else {
                      Text(
                          text = "Release Slot",
                          color = ForestGreen,
                          fontWeight = FontWeight.SemiBold,
                          fontSize = 13.sp
                      )
                    }
                  }
                }
              }
            }
          }
        }

        // 7. Responsive 2-Column Slot Grid
        when {
          uiState.isLoadingSlots -> {
            item { StateLoader("Loading slots for ${uiState.selectedDate}...") }
          }
          uiState.slots.isEmpty() -> {
            item {
              EmptyStateView(
                  title = "No slots available",
                  description = "No slots have been generated for this date or all slots are blocked."
              )
            }
          }
          else -> {
            val slotPairs = uiState.slots.chunked(2)
            items(slotPairs) { pair ->
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Box(modifier = Modifier.weight(1f)) {
                  SlotChip(
                      slot = pair[0],
                      isSelected = uiState.selectedSlots.any { it.id == pair[0].id },
                      onSelectToggle = { viewModel.toggleSlotSelection(it) },
                      isHeldByMe = pair[0].isHeldByMe(uiState.currentUserId)
                  )
                }

                if (pair.size > 1) {
                  Box(modifier = Modifier.weight(1f)) {
                    SlotChip(
                        slot = pair[1],
                        isSelected = uiState.selectedSlots.any { it.id == pair[1].id },
                        onSelectToggle = { viewModel.toggleSlotSelection(it) },
                        isHeldByMe = pair[1].isHeldByMe(uiState.currentUserId)
                    )
                  }
                } else {
                  Spacer(modifier = Modifier.weight(1f))
                }
              }
            }
          }
        }
      }
    }
  }
}

// ------------------------------------------------------------------------------------------------
// COMPACT EXPANDABLE 14-DAY CALENDAR COMPOSABLE
// ------------------------------------------------------------------------------------------------

@Composable
fun Expandable14DayCalendar(
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
  var isExpanded by remember { mutableStateOf(false) }

  val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

  // Today at 00:00:00
  val todayCal = remember {
    Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
  }

  // Exactly 14 days booking window: Today through Today + 13 days
  val maxSelectableDateCal = remember(todayCal) {
    (todayCal.clone() as Calendar).apply {
      add(Calendar.DAY_OF_YEAR, 13)
    }
  }

  val todayStr = remember(todayCal) { sdf.format(todayCal.time) }

  // Calendar month currently shown in the expanded grid
  var viewingCal by remember {
    val initCal = Calendar.getInstance().apply {
      time = try {
        sdf.parse(selectedDate) ?: todayCal.time
      } catch (e: Exception) {
        todayCal.time
      }
      set(Calendar.DAY_OF_MONTH, 1)
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    mutableStateOf(initCal)
  }

  // Check month navigation bounds
  val canNavigatePrev = remember(viewingCal, todayCal) {
    val prevMonth = (viewingCal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
    val lastDayOfPrevMonth = (prevMonth.clone() as Calendar).apply {
      set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
    }
    !lastDayOfPrevMonth.before(todayCal)
  }

  val canNavigateNext = remember(viewingCal, maxSelectableDateCal) {
    val nextMonth = (viewingCal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
    val firstDayOfNextMonth = (nextMonth.clone() as Calendar).apply {
      set(Calendar.DAY_OF_MONTH, 1)
    }
    !firstDayOfNextMonth.after(maxSelectableDateCal)
  }

  val monthTitle = remember(viewingCal) {
    SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(viewingCal.time)
  }

  // Formatted date string for collapsed summary
  val selectedDateDisplay = remember(selectedDate) {
    try {
      val parsed = sdf.parse(selectedDate) ?: todayCal.time
      val parsedCal = Calendar.getInstance().apply { time = parsed }
      val isTodaySelected = (parsedCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
          parsedCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR))
      val tomorrowCal = (todayCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
      val isTomorrowSelected = (parsedCal.get(Calendar.YEAR) == tomorrowCal.get(Calendar.YEAR) &&
          parsedCal.get(Calendar.DAY_OF_YEAR) == tomorrowCal.get(Calendar.DAY_OF_YEAR))

      val prefix = when {
        isTodaySelected -> "Today • "
        isTomorrowSelected -> "Tomorrow • "
        else -> ""
      }
      val dateFormatted = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(parsed)
      "$prefix$dateFormatted"
    } catch (e: Exception) {
      selectedDate
    }
  }

  Card(
      modifier = modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = CardSurface),
      border = BorderStroke(1.dp, BorderSubtle)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      // Header: Title & Subtitle + Expand/Collapse trigger
      Row(
          modifier = Modifier
              .fillMaxWidth()
              .clickable { isExpanded = !isExpanded },
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = null,
                tint = ForestGreen,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Choose your date",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextDeep
            )
          }
          Text(
              text = "Book up to 14 days ahead",
              fontSize = 11.sp,
              color = TextMuted,
              modifier = Modifier.padding(start = 22.dp)
          )
        }

        Surface(
            color = if (isExpanded) SoftGreenSurface else ElectricLime,
            shape = RoundedCornerShape(8.dp)
        ) {
          Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
                text = if (isExpanded) "Close" else "Change",
                color = ForestGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = ForestGreen,
                modifier = Modifier.size(14.dp)
            )
          }
        }
      }

      // Compact Collapsed Summary Strip
      AnimatedVisibility(
          visible = !isExpanded,
          enter = fadeIn() + expandVertically(),
          exit = fadeOut() + shrinkVertically()
      ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .clickable { isExpanded = true },
            color = SoftGreenSurface,
            shape = RoundedCornerShape(10.dp)
        ) {
          Row(
              modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 9.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
                text = selectedDateDisplay,
                color = ForestGreen,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Tap to expand",
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
          }
        }
      }

      // Expanded Calendar Grid
      AnimatedVisibility(
          visible = isExpanded,
          enter = fadeIn() + expandVertically(),
          exit = fadeOut() + shrinkVertically()
      ) {
        Column(modifier = Modifier.padding(top = 12.dp)) {
          HorizontalDivider(color = BorderSubtle)

          Spacer(modifier = Modifier.height(10.dp))

          // Month Navigation Row
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(
                onClick = {
                  viewingCal = (viewingCal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
                },
                enabled = canNavigatePrev,
                modifier = Modifier.size(32.dp)
            ) {
              Icon(
                  imageVector = Icons.Default.ChevronLeft,
                  contentDescription = "Previous Month",
                  tint = if (canNavigatePrev) ForestGreen else TextMuted.copy(alpha = 0.3f)
              )
            }

            Text(
                text = monthTitle,
                color = ForestGreen,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = {
                  viewingCal = (viewingCal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                },
                enabled = canNavigateNext,
                modifier = Modifier.size(32.dp)
            ) {
              Icon(
                  imageVector = Icons.Default.ChevronRight,
                  contentDescription = "Next Month",
                  tint = if (canNavigateNext) ForestGreen else TextMuted.copy(alpha = 0.3f)
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          // 7-Column Weekday Header
          val weekdayHeaders = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
          ) {
            weekdayHeaders.forEach { dayName ->
              Text(
                  text = dayName,
                  color = TextMuted,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold,
                  modifier = Modifier.weight(1f),
                  textAlign = TextAlign.Center
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          // 7-Column Date Grid Calculation
          val firstDayOfWeek = viewingCal.get(Calendar.DAY_OF_WEEK)
          val offset = (firstDayOfWeek - Calendar.MONDAY + 7) % 7
          val maxDays = viewingCal.getActualMaximum(Calendar.DAY_OF_MONTH)
          val totalCells = offset + maxDays
          val totalRows = (totalCells + 6) / 7

          for (rowIndex in 0 until totalRows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
              for (colIndex in 0..6) {
                val cellIndex = rowIndex * 7 + colIndex
                val dayOfMonth = cellIndex - offset + 1

                if (dayOfMonth in 1..maxDays) {
                  val dayCal = (viewingCal.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                  }
                  val isPast = dayCal.before(todayCal)
                  val isFutureBeyond14 = dayCal.after(maxSelectableDateCal)
                  val isSelectable = !isPast && !isFutureBeyond14
                  val cellDateStr = sdf.format(dayCal.time)
                  val isSelected = cellDateStr == selectedDate
                  val isToday = cellDateStr == todayStr

                  Box(
                      modifier = Modifier
                          .weight(1f)
                          .aspectRatio(1f)
                          .padding(2.dp)
                          .clip(RoundedCornerShape(8.dp))
                          .background(
                              when {
                                isSelected -> ForestGreen
                                isSelectable -> SoftGreenSurface.copy(alpha = 0.45f)
                                else -> Color.Transparent
                              }
                          )
                          .then(
                              if (isSelectable && !isSelected) {
                                Modifier.clickable {
                                  onDateSelected(cellDateStr)
                                  isExpanded = false
                                }
                              } else Modifier
                          ),
                      contentAlignment = Alignment.Center
                  ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                      Text(
                          text = "$dayOfMonth",
                          fontSize = 12.sp,
                          fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                          color = when {
                            isSelected -> ElectricLime
                            isSelectable -> TextDeep
                            else -> TextMuted.copy(alpha = 0.3f)
                          }
                      )
                      if (isToday && !isSelected) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(ForestGreen)
                        )
                      }
                    }
                  }
                } else {
                  Spacer(modifier = Modifier.weight(1f))
                }
              }
            }
          }
        }
      }
    }
  }
}

// ------------------------------------------------------------------------------------------------
// AVAILABILITY LEGEND COMPOSABLE
// ------------------------------------------------------------------------------------------------

@Composable
fun AvailabilityLegend(modifier: Modifier = Modifier) {
  Surface(
      color = CardSurface,
      shape = RoundedCornerShape(12.dp),
      border = BorderStroke(1.dp, BorderSubtle),
      modifier = modifier.fillMaxWidth()
  ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
      LegendItem(color = StatusSuccess, label = "Available")
      LegendItem(color = ForestGreen, label = "Your Hold")
      LegendItem(color = StatusWarning, label = "Held")
      LegendItem(color = StatusDestructive, label = "Booked")
    }
  }
}

@Composable
private fun LegendItem(color: Color, label: String) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
    )
    Spacer(modifier = Modifier.width(5.dp))
    Text(
        text = label,
        color = TextDeep,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium
    )
  }
}
