package com.courtbook.ui.booking

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.courtbook.R
import com.courtbook.ui.components.ErrorBanner
import com.courtbook.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockPaymentScreen(
    courtId: String,
    slotIds: List<String>,
    viewModel: BookingViewModel,
    onNavigateBack: () -> Unit,
    onPaymentSuccess: (bookingNumber: String) -> Unit,
    onSlotConflict: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedMethod by remember { mutableStateOf("UPI") }

    // Re-initialise if the ViewModel is fresh (amount is 0, data not yet loaded).
    // This happens when MockPayment is opened directly without going through BookingSummary.
    LaunchedEffect(courtId, slotIds) {
        if (uiState.finalAmount == 0.0 && !uiState.isLoadingData) {
            viewModel.initBooking(courtId, slotIds)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Secure Checkout",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextDeep
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextDeep
                        )
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Secure",
                            tint = StatusSuccess,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "256-bit SSL",
                            color = StatusSuccess,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
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
                .verticalScroll(rememberScrollState())
        ) {
            // ── Header: Amount & Merchant ──────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(ForestGreen, ForestGreenDark)
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Merchant badge
                    Surface(
                        color = ElectricLime.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = ElectricLime,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                "CourtBook · Verified Merchant",
                                color = ElectricLime,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Amount Payable",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "₹${uiState.finalAmount.toInt()}",
                        color = Color.White,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1).sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Court Slot Booking · ${slotIds.size} slot${if (slotIds.size > 1) "s" else ""}",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
            }

            // ── Error Banner ───────────────────────────────────────────────
            if (uiState.errorMessage != null) {
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    ErrorBanner(
                        errorMessage = uiState.errorMessage!!,
                        isConflict = uiState.isConflictError,
                        onRetry = if (uiState.isConflictError) onSlotConflict else null
                    )
                }
            }

            // ── Payment Method Selector ────────────────────────────────────
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Choose Payment Method",
                    color = TextDeep,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                PaymentMethodTile(
                    label = "UPI",
                    sublabel = "Google Pay · PhonePe · Paytm · BHIM",
                    icon = Icons.Default.QrCode2,
                    isSelected = selectedMethod == "UPI",
                    onSelect = { selectedMethod = "UPI" }
                ) {
                    UpiQrPanel(amount = uiState.finalAmount.toInt())
                }

                PaymentMethodTile(
                    label = "Credit / Debit Card",
                    sublabel = "Visa · Mastercard · RuPay · Amex",
                    icon = Icons.Default.CreditCard,
                    isSelected = selectedMethod == "CARD",
                    onSelect = { selectedMethod = "CARD" }
                ) {
                    CardInputPlaceholder()
                }

                PaymentMethodTile(
                    label = "Net Banking",
                    sublabel = "SBI · HDFC · ICICI · Axis & more",
                    icon = Icons.Default.AccountBalance,
                    isSelected = selectedMethod == "NET_BANKING",
                    onSelect = { selectedMethod = "NET_BANKING" }
                ) {
                    NetBankingPlaceholder()
                }

                // ── Security Row ───────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Payments are encrypted & PCI-DSS compliant",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                Spacer(Modifier.height(4.dp))

                // ── Pay Button ────────────────────────────────────────────
                Button(
                    onClick = {
                        viewModel.confirmPayment(
                            courtId = courtId,
                            slotIds = slotIds,
                            paymentMethod = selectedMethod,
                            onSuccess = onPaymentSuccess
                        )
                    },
                    enabled = !uiState.isSubmittingBooking,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForestGreen,
                        disabledContainerColor = ForestGreen.copy(alpha = 0.5f)
                    )
                ) {
                    if (uiState.isSubmittingBooking) {
                        CircularProgressIndicator(
                            color = ElectricLime,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = ElectricLime,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Pay ₹${uiState.finalAmount.toInt()} Securely",
                                color = ElectricLime,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                TextButton(
                    onClick = {
                        viewModel.cancelAndReleaseHold(slotIds) { onNavigateBack() }
                    },
                    enabled = !uiState.isSubmittingBooking,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Cancel & Release Slot",
                        color = TextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Payment Method Tile (accordion-style, expands when selected)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PaymentMethodTile(
    label: String,
    sublabel: String,
    icon: ImageVector,
    isSelected: Boolean,
    onSelect: () -> Unit,
    expandedContent: @Composable () -> Unit
) {
    val borderColor = if (isSelected) ForestGreen else BorderSubtle
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val bgColor = if (isSelected) SoftGreenSurface else CardSurface

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(borderWidth, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSelect)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = onSelect,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = ForestGreen,
                        unselectedColor = TextMuted
                    )
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) ForestGreen else TextMuted,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        label,
                        color = TextDeep,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                    Text(
                        sublabel,
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            // Animated expanded content
            AnimatedVisibility(
                visible = isSelected,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(bottom = 14.dp))
                    expandedContent()
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// UPI QR Panel
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun UpiQrPanel(amount: Int) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Scan & Pay with any UPI app",
            color = TextDeep,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        // QR Code card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, BorderSubtle),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.upi_qr_code),
                    contentDescription = "UPI QR Code",
                    modifier = Modifier
                        .size(180.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.height(10.dp))
                Surface(
                    color = SoftGreenSurface,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "₹$amount",
                        color = ForestGreen,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // UPI ID row
        Surface(
            color = CardSurface,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("UPI ID", color = TextMuted, fontSize = 11.sp)
                    Text(
                        "courtbook@upi",
                        color = TextDeep,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Surface(
                    color = SoftGreenSurface,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        "Tap to copy",
                        color = ForestGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Text(
            "Open Google Pay / PhonePe / Paytm → Scan QR",
            color = TextMuted,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Card Input Placeholder
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CardInputPlaceholder() {
    var cardNumber by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var nameOnCard by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = nameOnCard,
            onValueChange = { nameOnCard = it },
            label = { Text("Name on Card", fontSize = 12.sp) },
            placeholder = { Text("John Doe", color = TextMuted, fontSize = 14.sp) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CardSurface,
                unfocusedContainerColor = CardSurface,
                focusedBorderColor = ForestGreen,
                unfocusedBorderColor = BorderSubtle,
                focusedTextColor = TextDeep,
                unfocusedTextColor = TextDeep,
                cursorColor = ForestGreen,
                focusedLabelColor = ForestGreen,
                unfocusedLabelColor = TextMuted
            )
        )
        OutlinedTextField(
            value = cardNumber,
            onValueChange = { if (it.length <= 16) cardNumber = it.filter { c -> c.isDigit() } },
            label = { Text("Card Number", fontSize = 12.sp) },
            placeholder = { Text("•••• •••• •••• ••••", color = TextMuted, fontSize = 14.sp) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CardSurface,
                unfocusedContainerColor = CardSurface,
                focusedBorderColor = ForestGreen,
                unfocusedBorderColor = BorderSubtle,
                focusedTextColor = TextDeep,
                unfocusedTextColor = TextDeep,
                cursorColor = ForestGreen,
                focusedLabelColor = ForestGreen,
                unfocusedLabelColor = TextMuted
            )
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = expiry,
                onValueChange = { if (it.length <= 5) expiry = it },
                label = { Text("Expiry", fontSize = 12.sp) },
                placeholder = { Text("MM / YY", color = TextMuted) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardSurface,
                    unfocusedContainerColor = CardSurface,
                    focusedBorderColor = ForestGreen,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextDeep,
                    unfocusedTextColor = TextDeep,
                    cursorColor = ForestGreen,
                    focusedLabelColor = ForestGreen,
                    unfocusedLabelColor = TextMuted
                )
            )
            OutlinedTextField(
                value = cvv,
                onValueChange = { if (it.length <= 3) cvv = it.filter { c -> c.isDigit() } },
                label = { Text("CVV", fontSize = 12.sp) },
                placeholder = { Text("•••", color = TextMuted) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardSurface,
                    unfocusedContainerColor = CardSurface,
                    focusedBorderColor = ForestGreen,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextDeep,
                    unfocusedTextColor = TextDeep,
                    cursorColor = ForestGreen,
                    focusedLabelColor = ForestGreen,
                    unfocusedLabelColor = TextMuted
                )
            )
        }
        Text(
            "Card details are visual only — no data is collected",
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Net Banking Placeholder
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun NetBankingPlaceholder() {
    val banks = listOf("State Bank of India", "HDFC Bank", "ICICI Bank", "Axis Bank", "Kotak Mahindra")
    var selectedBank by remember { mutableStateOf(banks[0]) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Select your bank", color = TextDeep, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        banks.forEach { bank ->
            val sel = selectedBank == bank
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedBank = bank },
                shape = RoundedCornerShape(10.dp),
                color = if (sel) SoftGreenSurface else CardSurface,
                border = BorderStroke(if (sel) 1.5.dp else 1.dp, if (sel) ForestGreen else BorderSubtle)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = sel,
                        onClick = { selectedBank = bank },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = ForestGreen,
                            unselectedColor = TextMuted
                        ),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        bank,
                        color = TextDeep,
                        fontSize = 13.sp,
                        fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
        Text(
            "Bank selection is visual only — payments are simulated",
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
