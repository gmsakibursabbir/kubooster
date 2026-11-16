package com.kuduy.kubooster.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kuduy.kubooster.di.AppContainer
import com.kuduy.kubooster.feature.dashboard.DashboardScreen
import com.kuduy.kubooster.feature.dashboard.DashboardViewModel
import com.kuduy.kubooster.feature.diagnostics.DiagnosticsScreen
import com.kuduy.kubooster.feature.diagnostics.DiagnosticsViewModel
import com.kuduy.kubooster.feature.fps.FpsScreen
import com.kuduy.kubooster.feature.fps.FpsViewModel
import com.kuduy.kubooster.feature.graphics.GraphicsScreen
import com.kuduy.kubooster.feature.graphics.GraphicsViewModel
import com.kuduy.kubooster.feature.onboarding.OnboardingScreen
import com.kuduy.kubooster.feature.onboarding.OnboardingViewModel
import com.kuduy.kubooster.feature.optimization.OptimizationScreen
import com.kuduy.kubooster.feature.optimization.OptimizationViewModel
import com.kuduy.kubooster.feature.settings.SettingsScreen
import com.kuduy.kubooster.feature.settings.SettingsViewModel
import com.kuduy.kubooster.feature.shizuku.ShizukuScreen
import com.kuduy.kubooster.feature.shizuku.ShizukuViewModel

object Routes {
    const val ONBOARDING = "onboarding"
    const val DASHBOARD = "dashboard"
    const val FPS = "fps"
    const val GRAPHICS = "graphics"
    const val DIAGNOSTICS = "diagnostics"
    const val SHIZUKU = "shizuku"
    const val OPTIMIZATION = "optimization"
    const val SETTINGS = "settings"
}

private data class TopDestination(val route: String, val label: String, val icon: ImageVector)

private val topDestinations = listOf(
    TopDestination(Routes.DASHBOARD, "Home", Icons.Filled.Home),
    TopDestination(Routes.FPS, "FPS", Icons.Filled.Speed),
    TopDestination(Routes.GRAPHICS, "Graphics", Icons.Filled.Tune),
    TopDestination(Routes.SETTINGS, "Settings", Icons.Filled.Settings),
)

/**
 * Root: decides onboarding vs main flow, hosts the bottom navigation bar for
 * the phone-sized top destinations, and pushes detail screens on top.
 */
@Composable
fun KuBoosterRoot(container: AppContainer) {
    val navController: NavHostController = rememberNavController()
    val onboardingVm: OnboardingViewModel = viewModel(factory = OnboardingViewModel.factory(container))
    val onboarded by onboardingVm.onboardingComplete.collectAsState()

    val startRoute = when (onboarded) {
        null -> null
        true -> Routes.DASHBOARD
        false -> Routes.ONBOARDING
    }

    val start: String? = startRoute
    if (start == null) {
        // One quiet frame while preferences load; the brand background already shows.
        return
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in topDestinations.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    topDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(Routes.DASHBOARD) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = start,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    viewModel = onboardingVm,
                    onFinished = {
                        navController.navigate(Routes.DASHBOARD) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.DASHBOARD) {
                val vm: DashboardViewModel = viewModel(factory = DashboardViewModel.factory(container))
                val dashboardState by vm.state.collectAsState()
                DashboardScreen(
                    state = dashboardState,
                    onBoost = { navController.navigate(Routes.OPTIMIZATION) },
                    onOpenFps = { navController.navigate(Routes.FPS) },
                    onOpenGraphics = { navController.navigate(Routes.GRAPHICS) },
                    onOpenDiagnostics = { navController.navigate(Routes.DIAGNOSTICS) },
                    onOpenShizuku = { navController.navigate(Routes.SHIZUKU) },
                    onRefresh = { vm.refresh() },
                )
            }
            composable(Routes.FPS) {
                val vm: FpsViewModel = viewModel(factory = FpsViewModel.factory(container))
                val fpsState by vm.state.collectAsState()
                FpsScreen(state = fpsState, onRetry = { vm.refresh() })
            }
            composable(Routes.GRAPHICS) {
                val vm: GraphicsViewModel = viewModel(factory = GraphicsViewModel.factory(container))
                val graphicsState by vm.state.collectAsState()
                GraphicsScreen(state = graphicsState, onSelectPreset = { vm.selectPreset(it) })
            }
            composable(Routes.DIAGNOSTICS) {
                val vm: DiagnosticsViewModel = viewModel(factory = DiagnosticsViewModel.factory(container))
                val diagState by vm.state.collectAsState()
                DiagnosticsScreen(state = diagState, onRetry = { vm.refresh() })
            }
            composable(Routes.SHIZUKU) {
                val vm: ShizukuViewModel = viewModel(factory = ShizukuViewModel.factory(container))
                val shizukuState by vm.state.collectAsState()
                ShizukuScreen(
                    state = shizukuState,
                    onRequestPermission = { vm.requestPermission() },
                    onGrantWriteSecure = { vm.grantWriteSecureSettings() },
                )
            }
            composable(Routes.OPTIMIZATION) {
                val vm: OptimizationViewModel = viewModel(factory = OptimizationViewModel.factory(container))
                val optState by vm.state.collectAsState()
                OptimizationScreen(
                    state = optState,
                    onConfirm = { vm.apply() },
                    onRestore = { vm.restoreAll() },
                    onToggle = { vm.toggle(it) },
                    onDone = { navController.popBackStack() },
                )
            }
            composable(Routes.SETTINGS) {
                val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container))
                val settingsState by vm.state.collectAsState()
                val pending by vm.pendingRestoreCount.collectAsState()
                SettingsScreen(
                    state = settingsState,
                    onSetThemeFollowSystem = { vm.setThemeFollowSystem(it) },
                    onClearPreferences = { vm.clearPreferences() },
                    pendingCount = pending,
                )
            }
        }
    }

    // Re-emit onboarding state when returning to top destinations.
    LaunchedEffect(currentRoute) {
        if (currentRoute == Routes.DASHBOARD) {
            // No-op: dashboards refresh themselves on ViewModel init.
        }
    }
}
