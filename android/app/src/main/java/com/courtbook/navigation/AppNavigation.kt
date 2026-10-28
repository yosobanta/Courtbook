package com.courtbook.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.courtbook.data.remote.TokenManager
import com.courtbook.ui.admin.*
import com.courtbook.ui.auth.*
import com.courtbook.ui.booking.*
import com.courtbook.ui.court.*
import com.courtbook.ui.explore.*
import com.courtbook.ui.home.*
import com.courtbook.ui.mybookings.*
import com.courtbook.analytics.ui.AIInsightsScreen
import com.courtbook.analytics.ui.AIInsightsViewModel
import com.courtbook.ui.profile.*
import com.courtbook.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
  NavHost(
      navController = navController,
      startDestination = Screen.Splash.route
  ) {
    // --- SPLASH SCREEN ---
    composable(Screen.Splash.route) {
      SplashScreen(
          onFinish = {
            if (TokenManager.isLoggedIn()) {
              if (TokenManager.isAdmin()) {
                navController.navigate(Screen.AdminDashboard.route) {
                  popUpTo(Screen.Splash.route) { inclusive = true }
                }
              } else {
                navController.navigate(Screen.MainApp.route) {
                  popUpTo(Screen.Splash.route) { inclusive = true }
                }
              }
            } else {
              navController.navigate(Screen.Login.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
              }
            }
          }
      )
    }

    // --- AUTH FLOW ---
    composable(Screen.Login.route) {
      val authViewModel: AuthViewModel = viewModel()
      LoginScreen(
          viewModel = authViewModel,
          onLoginSuccess = { isAdmin ->
            if (isAdmin) {
              navController.navigate(Screen.AdminDashboard.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
              }
            } else {
              navController.navigate(Screen.MainApp.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
              }
            }
          },
          onNavigateToRegister = {
            navController.navigate(Screen.Register.route)
          }
      )
    }

    composable(Screen.Register.route) {
      val authViewModel: AuthViewModel = viewModel()
      RegisterScreen(
          viewModel = authViewModel,
          onRegisterSuccess = {
            navController.navigate(Screen.MainApp.route) {
              popUpTo(Screen.Register.route) { inclusive = true }
            }
          },
          onNavigateToLogin = {
            navController.popBackStack()
          }
      )
    }

    // --- MAIN APP (TAB CONTAINER) ---
    composable(Screen.MainApp.route) {
      MainAppContainer(
          onNavigateToCourtDetails = { courtId ->
            navController.navigate(Screen.CourtDetails.createRoute(courtId))
          },
          onNavigateToAdmin = {
            navController.navigate(Screen.AdminDashboard.route)
          },
          onNavigateToAIInsights = {
            navController.navigate(Screen.AIInsights.route)
          },
          onLogout = {
            navController.navigate(Screen.Login.route) {
              popUpTo(Screen.MainApp.route) { inclusive = true }
            }
          }
      )
    }

    // --- COURT DETAILS & SLOT SELECTION ---
    composable(
        route = Screen.CourtDetails.route,
        arguments = listOf(navArgument("courtId") { type = NavType.StringType })
    ) { backStackEntry ->
      val courtId = backStackEntry.arguments?.getString("courtId") ?: ""
      val courtViewModel: CourtViewModel = viewModel()
      CourtDetailsScreen(
          courtId = courtId,
          viewModel = courtViewModel,
          onNavigateBack = { navController.popBackStack() },
          onProceedToSummary = { cId, slotIds ->
            navController.navigate(Screen.BookingSummary.createRoute(cId, slotIds))
          }
      )
    }

    // --- BOOKING SUMMARY & PROMO CODES ---
    composable(
        route = Screen.BookingSummary.route,
        arguments = listOf(
            navArgument("courtId") { type = NavType.StringType },
            navArgument("slotIds") { type = NavType.StringType }
        )
    ) { backStackEntry ->
      val courtId = backStackEntry.arguments?.getString("courtId") ?: ""
      val slotIdsStr = backStackEntry.arguments?.getString("slotIds") ?: ""
      val slotIds = slotIdsStr.split(",").filter { it.isNotBlank() }
      val bookingViewModel: BookingViewModel = viewModel()

      BookingSummaryScreen(
          courtId = courtId,
          slotIds = slotIds,
          viewModel = bookingViewModel,
          onNavigateBack = { navController.popBackStack() },
          onProceedToPayment = {
            navController.navigate(Screen.MockPayment.createRoute(courtId, slotIds))
          }
      )
    }

    // --- MOCK PAYMENT ---
    composable(
        route = Screen.MockPayment.route,
        arguments = listOf(
            navArgument("courtId") { type = NavType.StringType },
            navArgument("slotIds") { type = NavType.StringType }
        )
    ) { backStackEntry ->
      val courtId = backStackEntry.arguments?.getString("courtId") ?: ""
      val slotIdsStr = backStackEntry.arguments?.getString("slotIds") ?: ""
      val slotIds = slotIdsStr.split(",").filter { it.isNotBlank() }
      val bookingViewModel: BookingViewModel = viewModel()

      MockPaymentScreen(
          courtId = courtId,
          slotIds = slotIds,
          viewModel = bookingViewModel,
          onNavigateBack = { navController.popBackStack() },
          onPaymentSuccess = { bookingNumber ->
            navController.navigate(Screen.Confirmation.createRoute(bookingNumber)) {
              popUpTo(Screen.MainApp.route)
            }
          },
          onSlotConflict = {
            // If slot conflict occurs, navigate back to CourtDetails to pick fresh slot
            navController.popBackStack(Screen.CourtDetails.createRoute(courtId), inclusive = false)
          }
      )
    }

    // --- CONFIRMATION SCREEN ---
    composable(
        route = Screen.Confirmation.route,
        arguments = listOf(navArgument("bookingNumber") { type = NavType.StringType })
    ) { backStackEntry ->
      val bookingNumber = backStackEntry.arguments?.getString("bookingNumber") ?: ""
      ConfirmationScreen(
          bookingNumber = bookingNumber,
          onNavigateToMyBookings = {
            navController.navigate(Screen.MainApp.route) {
              popUpTo(Screen.MainApp.route) { inclusive = true }
            }
          },
          onNavigateToHome = {
            navController.navigate(Screen.MainApp.route) {
              popUpTo(Screen.MainApp.route) { inclusive = true }
            }
          }
      )
    }

    // --- ADMIN ROUTES ---
    composable(Screen.AdminDashboard.route) {
      val adminViewModel: AdminViewModel = viewModel()
      AdminDashboardScreen(
          viewModel = adminViewModel,
          onNavigateBack = { navController.popBackStack() },
          onNavigateToPlayerView = { navController.navigate(Screen.MainApp.route) },
          onNavigateToAIInsights = { navController.navigate(Screen.AIInsights.route) },
          onLogout = {
            navController.navigate(Screen.Login.route) {
              popUpTo(0) { inclusive = true }
            }
          }
      )
    }

    composable(Screen.ManageSlots.route) {
      val adminViewModel: AdminViewModel = viewModel()
      ManageSlotsScreen(
          viewModel = adminViewModel,
          onNavigateBack = { navController.popBackStack() }
      )
    }

    composable(Screen.Analytics.route) {
      val adminViewModel: AdminViewModel = viewModel()
      AnalyticsScreen(
          viewModel = adminViewModel,
          onNavigateBack = { navController.popBackStack() }
      )
    }

    composable(Screen.AIInsights.route) {
      val aiInsightsViewModel: AIInsightsViewModel = viewModel()
      AIInsightsScreen(
          viewModel = aiInsightsViewModel,
          onNavigateBack = { navController.popBackStack() }
      )
    }
  }
}

@Composable
fun MainAppContainer(
    onNavigateToCourtDetails: (courtId: String) -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToAIInsights: () -> Unit,
    onLogout: () -> Unit
) {
  var selectedTab by remember { mutableStateOf<BottomTab>(BottomTab.Home) }

  val homeViewModel: HomeViewModel = viewModel()
  val exploreViewModel: ExploreViewModel = viewModel()
  val myBookingsViewModel: MyBookingsViewModel = viewModel()

  Scaffold(
      bottomBar = {
        Surface(
            color = CardSurface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
          NavigationBar(
              containerColor = CardSurface,
              tonalElevation = 0.dp
          ) {
            val tabs = listOf(BottomTab.Home, BottomTab.Explore, BottomTab.Bookings, BottomTab.Profile)
            tabs.forEach { tab ->
              val isSelected = selectedTab == tab
              NavigationBarItem(
                  selected = isSelected,
                  onClick = { selectedTab = tab },
                  icon = {
                    when (tab) {
                      BottomTab.Home -> Icon(Icons.Default.Home, contentDescription = tab.title)
                      BottomTab.Explore -> Icon(Icons.Default.Explore, contentDescription = tab.title)
                      BottomTab.Bookings -> Icon(Icons.Default.ConfirmationNumber, contentDescription = tab.title)
                      BottomTab.Profile -> Icon(Icons.Default.Person, contentDescription = tab.title)
                    }
                  },
                  label = {
                    Text(
                        text = tab.title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                  },
                  colors = NavigationBarItemDefaults.colors(
                      selectedIconColor = ForestGreen,
                      selectedTextColor = ForestGreen,
                      indicatorColor = ElectricLime,
                      unselectedIconColor = TextMuted,
                      unselectedTextColor = TextMuted
                  )
              )
            }
          }
        }
      },
      containerColor = WarmBg
  ) { paddingValues ->
    Box(modifier = Modifier.padding(paddingValues)) {
      when (selectedTab) {
        BottomTab.Home -> HomeScreen(
            viewModel = homeViewModel,
            onNavigateToCourtDetails = onNavigateToCourtDetails,
            onNavigateToExplore = { selectedTab = BottomTab.Explore }
        )
        BottomTab.Explore -> ExploreScreen(
            viewModel = exploreViewModel,
            onNavigateToCourtDetails = onNavigateToCourtDetails
        )
        BottomTab.Bookings -> MyBookingsScreen(
            viewModel = myBookingsViewModel,
            onNavigateToHome = { selectedTab = BottomTab.Home }
        )
        BottomTab.Profile -> ProfileScreen(
            onLogout = onLogout,
            onNavigateToAdmin = onNavigateToAdmin,
            onNavigateToAIInsights = onNavigateToAIInsights
        )
      }
    }
  }
}

@Composable
fun SplashScreen(onFinish: () -> Unit) {
  LaunchedEffect(Unit) {
    delay(1200) // Brief animated branding splash
    onFinish()
  }

  Box(
      modifier = Modifier
          .fillMaxSize()
          .background(WarmBg),
      contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Box(
          modifier = Modifier
              .size(96.dp)
              .clip(CircleShape)
              .background(SoftGreenSurface),
          contentAlignment = Alignment.Center
      ) {
        Icon(
            imageVector = Icons.Default.SportsTennis,
            contentDescription = "CourtBook",
            tint = ForestGreen,
            modifier = Modifier.size(54.dp)
        )
      }
      Spacer(modifier = Modifier.height(20.dp))
      Text(
          text = "CourtBook",
          color = TextDeep,
          fontSize = 34.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = (-0.5).sp
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
          text = "Find. Book. Play.",
          color = ForestGreen,
          fontSize = 15.sp,
          fontWeight = FontWeight.SemiBold
      )
    }
  }
}
