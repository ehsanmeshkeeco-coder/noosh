package com.example.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val titleResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Dashboard : Screen(
        route = "dashboard",
        titleResId = com.example.R.string.nav_dashboard,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    object Weekly : Screen(
        route = "weekly",
        titleResId = com.example.R.string.nav_weekly,
        selectedIcon = Icons.Filled.BarChart,
        unselectedIcon = Icons.Outlined.BarChart
    )

    object Monthly : Screen(
        route = "monthly",
        titleResId = com.example.R.string.nav_monthly,
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth
    )

    object Reminders : Screen(
        route = "reminders",
        titleResId = com.example.R.string.nav_reminders,
        selectedIcon = Icons.Filled.Alarm,
        unselectedIcon = Icons.Outlined.Alarm
    )

    object Companion : Screen(
        route = "companion",
        titleResId = com.example.R.string.companion_title,
        selectedIcon = Icons.Filled.Favorite,
        unselectedIcon = Icons.Outlined.FavoriteBorder
    )

    object Profile : Screen(
        route = "profile",
        titleResId = com.example.R.string.nav_profile,
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )

    object Auth : Screen(
        route = "auth",
        titleResId = com.example.R.string.clerk_account_title,
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )

    object Onboarding : Screen(
        route = "onboarding",
        titleResId = com.example.R.string.onboarding_title,
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )

    object RechartsTrend : Screen(
        route = "recharts_trend",
        titleResId = com.example.R.string.recharts_trend_title,
        selectedIcon = Icons.AutoMirrored.Filled.ShowChart,
        unselectedIcon = Icons.AutoMirrored.Outlined.ShowChart
    )
}

val BottomNavScreens = listOf(
    Screen.Dashboard,
    Screen.Weekly,
    Screen.Companion,
    Screen.Reminders,
    Screen.Profile
)
