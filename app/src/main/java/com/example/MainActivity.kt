package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Earbuds
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Earbuds
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.EarbudsUiState
import com.example.ui.EarbudsViewModel
import com.example.ui.components.PinSecurityDialog
import com.example.ui.screens.ControlsFirmwareScreen
import com.example.ui.screens.DeviceAncHubScreen
import com.example.ui.screens.SecurityLockScreen
import com.example.ui.screens.SoundStudioScreen
import com.example.ui.theme.AetherBudsTheme

private enum class AetherDestination(
    val route: String,
    val labelRes: Int,
    val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DEVICE(
        route = "device",
        labelRes = R.string.nav_hub,
        titleRes = R.string.hub_title,
        selectedIcon = Icons.Filled.Earbuds,
        unselectedIcon = Icons.Outlined.Earbuds
    ),
    SOUND(
        route = "sound",
        labelRes = R.string.nav_sound,
        titleRes = R.string.sound_title,
        selectedIcon = Icons.Filled.Equalizer,
        unselectedIcon = Icons.Outlined.Equalizer
    ),
    CONTROLS(
        route = "controls",
        labelRes = R.string.nav_controls,
        titleRes = R.string.controls_title,
        selectedIcon = Icons.Filled.Tune,
        unselectedIcon = Icons.Outlined.Tune
    ),
    SECURITY(
        route = "security",
        labelRes = R.string.nav_security,
        titleRes = R.string.security_title,
        selectedIcon = Icons.Filled.Security,
        unselectedIcon = Icons.Outlined.Security
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        setContent {
            AetherBudsTheme {
                AetherBudsApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AetherBudsApp(
    viewModel: EarbudsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.statusBannerMessage) {
        val message = state.statusBannerMessage
        if (!message.isNullOrEmpty()) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearBanner()
        }
    }

    val activeTab = AetherDestination.entries.firstOrNull { dest ->
        currentDestination?.hierarchy?.any { it.route == dest.route } == true
    } ?: AetherDestination.DEVICE

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(activeTab.titleRes),
                            style = MaterialTheme.typography.headlineSmall
                        )
                    },
                    actions = {
                        if (state.firmware.hasUpdate) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(end = 12.dp)
                            ) {
                                Text(
                                    text = "OTA",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        AetherDestination.entries.forEach { dest ->
                            val selected = activeTab == dest
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(navController, dest) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag("nav_${dest.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        AetherDestination.entries.forEach { dest ->
                            val selected = activeTab == dest
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(navController, dest) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag("rail_${dest.route}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    AetherNavHost(
                        navController = navController,
                        state = state,
                        viewModel = viewModel,
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 720.dp)
                    )
                }
            }
        }
    }

    PinSecurityDialog(
        state = state.pinDialog,
        onDismiss = viewModel::dismissPinDialog,
        onSubmit = viewModel::submitPin
    )
}

@Composable
private fun AetherNavHost(
    navController: NavHostController,
    state: EarbudsUiState,
    viewModel: EarbudsViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = AetherDestination.DEVICE.route,
        modifier = modifier
    ) {
        composable(AetherDestination.DEVICE.route) {
            DeviceAncHubScreen(
                state = state,
                onAncModeSelected = viewModel::setAncMode,
                onAncDepthChanged = viewModel::setAncDepth,
                onVoiceFocusToggle = viewModel::setVoiceFocusBoost,
                onSidetoneToggle = viewModel::setCallSidetone,
                onRunFitTest = viewModel::runEarTipFitTest,
                onRingBuds = viewModel::toggleFindMyBuds,
                onOpenSecurityTab = { navigateToTab(navController, AetherDestination.SECURITY) }
            )
        }
        composable(AetherDestination.SOUND.route) {
            SoundStudioScreen(
                state = state,
                onSelectProfile = viewModel::selectSoundProfile,
                onBandChanged = viewModel::updateEqBand,
                onBassBoostChanged = viewModel::updateBassBoost,
                onResetFlat = viewModel::resetEqFlat,
                onSaveCustomProfile = viewModel::saveCustomProfile,
                onDeleteCustomProfile = viewModel::deleteCustomProfile,
                onSpatialAudioToggle = viewModel::setSpatialAudio,
                onHeadTrackingToggle = viewModel::setHeadTracking,
                onGamingModeToggle = viewModel::setGamingLowLatency,
                onCodecSelected = viewModel::setAudioCodec
            )
        }
        composable(AetherDestination.CONTROLS.route) {
            ControlsFirmwareScreen(
                state = state,
                onUpdateGesture = viewModel::updateGesture,
                onInEarDetectionToggle = viewModel::setInEarDetection,
                onAutoAnswerToggle = viewModel::setAutoAnswerCalls,
                onCheckFirmware = viewModel::checkForFirmwareUpdate,
                onInstallFirmware = viewModel::startFirmwareOtaUpdate
            )
        }
        composable(AetherDestination.SECURITY.route) {
            SecurityLockScreen(
                state = state,
                onToggleExclusiveLock = viewModel::toggleExclusiveLock,
                onToggleLostFreeze = viewModel::toggleLostFreeze,
                onOpenPinSetup = viewModel::openPinSetupDialog,
                onRotateHostKey = viewModel::requestKeyRotation,
                onRefreshBluetooth = viewModel::refreshBluetoothHardware,
                onBindEarbudsDevice = viewModel::bindEarbuds,
                onSimulateIntruderAttempt = viewModel::simulateUnauthorizedPhoneAttempt
            )
        }
    }
}

private fun navigateToTab(navController: NavHostController, destination: AetherDestination) {
    navController.navigate(destination.route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
