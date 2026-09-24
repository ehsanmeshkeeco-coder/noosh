package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.presentation.components.InteractiveTourOverlay
import com.example.presentation.navigation.BottomNavScreens
import com.example.presentation.navigation.Screen
import com.example.presentation.screens.AuthScreen
import com.example.presentation.screens.DashboardScreen
import com.example.presentation.screens.HealthCompanionScreen
import com.example.presentation.screens.MonthlyReportScreen
import com.example.presentation.screens.OnboardingWizardScreen
import com.example.presentation.screens.ProfileSettingsScreen
import com.example.presentation.screens.RemindersScreen
import com.example.presentation.screens.WeeklyAnalyticsScreen
import com.example.presentation.theme.NooshPrimary
import com.example.presentation.theme.NooshTheme
import com.example.presentation.viewmodel.MainViewModel
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import android.content.Intent
import com.example.presentation.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        val app = application as NooshApplication
        MainViewModelFactory(
            getDashboardDataUseCase = app.getDashboardDataUseCase,
            addWaterIntakeUseCase = app.addWaterIntakeUseCase,
            waterRepository = app.waterRepository,
            userRepository = app.userRepository,
            reminderRepository = app.reminderRepository,
            syncRepository = app.syncRepository,
            clerkAuthManager = app.clerkAuthManager,
            reminderScheduler = app.reminderScheduler,
            healthCompanionManager = app.healthCompanionManager,
            fcmTokenManager = app.fcmTokenManager,
            supabaseClient = app.supabaseClient
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NooshTheme {
                RequestNotificationPermission()
                MainAppScaffold(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Interacting with the app stops any background reminder alarm
        com.example.alarms.WaterAlarmRingingService.stop(applicationContext)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        com.example.alarms.WaterAlarmRingingService.stop(applicationContext)
    }
}

@Composable
fun RequestNotificationPermission() {
    val context = LocalContext.current
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permission = Manifest.permission.POST_NOTIFICATIONS
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { /* Handled gracefully */ }

        LaunchedEffect(Unit) {
            if (!hasPermission) {
                launcher.launch(permission)
            }
        }
    }
}

@Composable
fun MainAppScaffold(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val authState by viewModel.authState.collectAsState()
    val dashboardState by viewModel.dashboardState.collectAsState()
    val onboardingCompletedInSession by viewModel.onboardingCompletedInSession.collectAsState()
    val isAlarmRinging by viewModel.isAlarmRinging.collectAsState()

    val showBottomBar = currentRoute != Screen.Auth.route && currentRoute != Screen.Onboarding.route

    // Interactive Tour Overlay State
    var showInteractiveTour by remember { mutableStateOf(false) }
    var tourStepIndex by remember { mutableIntStateOf(0) }
    var ringBounds by remember { mutableStateOf<Rect?>(null) }
    var quickAddBounds by remember { mutableStateOf<Rect?>(null) }
    var bottomNavBounds by remember { mutableStateOf<Rect?>(null) }

    // If unauthenticated at startup, route to Auth
    LaunchedEffect(authState) {
        if (authState is com.example.data.remote.clerk.AuthState.Unauthenticated && currentRoute != Screen.Auth.route) {
            navController.navigate(Screen.Auth.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // Check if user needs onboarding wizard on initial launch only (never re-trigger when route changes or after completion)
    LaunchedEffect(dashboardState?.profile?.onboardingCompleted, authState, onboardingCompletedInSession) {
        val profile = dashboardState?.profile
        val isCompleted = onboardingCompletedInSession || (profile?.onboardingCompleted == true)
        if (!isCompleted &&
            authState is com.example.data.remote.clerk.AuthState.Authenticated &&
            profile != null &&
            currentRoute != Screen.Auth.route &&
            currentRoute != Screen.Onboarding.route
        ) {
            navController.navigate(Screen.Onboarding.route) {
                popUpTo(Screen.Dashboard.route) { inclusive = false }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(
                        containerColor = Color.White,
                        tonalElevation = 4.dp,
                        modifier = Modifier
                            .testTag("bottom_navigation_bar")
                            .onGloballyPositioned { coordinates ->
                                bottomNavBounds = coordinates.boundsInRoot()
                            }
                    ) {
                        BottomNavScreens.forEach { screen ->
                            val isSelected = currentRoute == screen.route
                            NavigationBarItem(
                                selected = isSelected,
                                alwaysShowLabel = false,
                                onClick = {
                                    if (currentRoute != screen.route) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                        contentDescription = stringResource(screen.titleResId),
                                        tint = if (isSelected) NooshPrimary else Color(0xFF94A3B8)
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = Color(0xFFE0F2FE)
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Dashboard.route) {
                    DashboardScreen(
                        viewModel = viewModel,
                        onProgressRingPositioned = { ringBounds = it },
                        onQuickAddPositioned = { quickAddBounds = it }
                    )
                }

                composable(Screen.Onboarding.route) {
                    OnboardingWizardScreen(
                        viewModel = viewModel,
                        onCompleteOnboarding = {
                            // After finishing wizard, navigate to Dashboard and show interactive tour
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                            tourStepIndex = 0
                            showInteractiveTour = true
                        }
                    )
                }

                composable(Screen.Weekly.route) {
                    WeeklyAnalyticsScreen(
                        viewModel = viewModel,
                        onNavigateToRechartsTrend = { navController.navigate(Screen.RechartsTrend.route) }
                    )
                }

                composable(Screen.RechartsTrend.route) {
                    com.example.presentation.screens.RechartsWeeklyTrendScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Monthly.route) {
                    MonthlyReportScreen(viewModel = viewModel)
                }

                composable(Screen.Reminders.route) {
                    RemindersScreen(viewModel = viewModel)
                }

                composable(Screen.Companion.route) {
                    HealthCompanionScreen(viewModel = viewModel)
                }

                composable(Screen.Profile.route) {
                    ProfileSettingsScreen(
                        viewModel = viewModel,
                        onNavigateToAuth = { navController.navigate(Screen.Auth.route) },
                        onNavigateToOnboarding = { navController.navigate(Screen.Onboarding.route) }
                    )
                }

                composable(Screen.Auth.route) {
                    AuthScreen(
                        viewModel = viewModel,
                        onNavigateBack = {
                            if (!navController.popBackStack()) {
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        },
                        onAuthSuccess = {
                            // After login, check if onboarding is needed
                            val profile = dashboardState?.profile
                            val isCompleted = viewModel.onboardingCompletedInSession.value || (profile?.onboardingCompleted == true)
                            if (!isCompleted) {
                                navController.navigate(Screen.Onboarding.route) {
                                    popUpTo(Screen.Auth.route) { inclusive = true }
                                }
                            } else {
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(Screen.Auth.route) { inclusive = true }
                                }
                            }
                        }
                    )
                }
            }
        }

        // Spotlight interactive tour overlay
        if (showInteractiveTour && currentRoute == Screen.Dashboard.route) {
            InteractiveTourOverlay(
                activeStepIndex = tourStepIndex,
                onNextStep = {
                    if (tourStepIndex < 2) {
                        tourStepIndex++
                    } else {
                        showInteractiveTour = false
                    }
                },
                onSkipTour = { showInteractiveTour = false },
                ringBounds = ringBounds,
                quickAddBounds = quickAddBounds,
                bottomNavBounds = bottomNavBounds
            )
        }

        // Active background reminder alarm banner
        if (isAlarmRinging) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .align(Alignment.TopCenter)
                    .testTag("active_alarm_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = NooshPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "زنگ یادآور آب فعال است 💧",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "برای توقف زنگ و ثبت آب لمس کنید",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                    Button(
                        onClick = {
                            viewModel.stopAlarmService()
                            viewModel.addWater(250)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NooshPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("نوشیدم (+۲۵۰)", fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }
    }
}
