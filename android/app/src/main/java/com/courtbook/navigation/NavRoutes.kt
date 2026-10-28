package com.courtbook.navigation

sealed class Screen(val route: String) {
  object Splash : Screen("splash")
  object Login : Screen("login")
  object Register : Screen("register")
  object MainApp : Screen("main_app")

  object CourtDetails : Screen("court_details/{courtId}") {
    fun createRoute(courtId: String) = "court_details/$courtId"
  }

  object BookingSummary : Screen("booking_summary/{courtId}/{slotIds}") {
    fun createRoute(courtId: String, slotIds: List<String>) =
        "booking_summary/$courtId/${slotIds.joinToString(",")}"
  }

  object MockPayment : Screen("mock_payment/{courtId}/{slotIds}") {
    fun createRoute(courtId: String, slotIds: List<String>) =
        "mock_payment/$courtId/${slotIds.joinToString(",")}"
  }

  object Confirmation : Screen("confirmation/{bookingNumber}") {
    fun createRoute(bookingNumber: String) = "confirmation/$bookingNumber"
  }

  // Admin Flow
  object AdminDashboard : Screen("admin_dashboard")
  object ManageSlots : Screen("manage_slots")
  object Analytics : Screen("analytics")
  object AIInsights : Screen("ai_insights")
}

sealed class BottomTab(val route: String, val title: String, val iconName: String) {
  object Home : BottomTab("tab_home", "Home", "Home")
  object Explore : BottomTab("tab_explore", "Explore", "Explore")
  object Bookings : BottomTab("tab_bookings", "Bookings", "Bookings")
  object Profile : BottomTab("tab_profile", "Profile", "Profile")
}
