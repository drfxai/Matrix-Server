package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.ui.components.ConnectionHealthDialog
import com.example.ui.components.GlobalStatusBar
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.navigation.MatrixScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DiagnosticsScreen
import com.example.ui.screens.FileManagerScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SetupWizardScreen
import com.example.ui.screens.SshKeysScreen
import com.example.ui.screens.TerminalScreen
import com.example.ui.theme.MatrixBlack
import com.example.ui.theme.MatrixDarkSurface
import com.example.ui.theme.MatrixGreen
import com.example.ui.theme.MatrixSurfaceBorder
import com.example.ui.theme.MatrixSurfaceVariant
import com.example.ui.theme.MatrixTextPrimary
import com.example.ui.theme.MatrixTextSecondary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MatrixServerViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MatrixServerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                // Request Notification permission for Android 13+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { _ -> }

                    LaunchedEffect(Unit) {
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                MatrixAppRoot(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MatrixAppRoot(viewModel: MatrixServerViewModel) {
    val config by viewModel.configState.collectAsState()
    val health by viewModel.connectionHealth.collectAsState()
    var showHealthDialog by remember { mutableStateOf(false) }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Handle back button to return to Dashboard if on another screen
    if (currentRoute != null && currentRoute != MatrixScreen.Dashboard.route && currentRoute != MatrixScreen.Setup.route) {
        BackHandler {
            navController.navigate(MatrixScreen.Dashboard.route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val startDestination = if (config.setupCompleted) MatrixScreen.Dashboard.route else MatrixScreen.Setup.route

    val navItems = listOf(
        MatrixScreen.Dashboard,
        MatrixScreen.Terminal,
        MatrixScreen.Projects,
        MatrixScreen.Files,
        MatrixScreen.Diagnostics,
        MatrixScreen.Settings
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (currentRoute != MatrixScreen.Setup.route) {
                Box(
                    modifier = Modifier
                        .background(MatrixDarkSurface)
                        .windowInsetsPadding(WindowInsets.statusBars)
                ) {
                    GlobalStatusBar(
                        health = health,
                        onClick = { showHealthDialog = true }
                    )
                }
            }
        },
        bottomBar = {
            if (currentRoute != MatrixScreen.Setup.route) {
                NavigationBar(
                    containerColor = MatrixDarkSurface,
                    contentColor = MatrixGreen,
                    modifier = Modifier
                        .border(1.dp, MatrixSurfaceBorder)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    navItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = selected,
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
                                screen.icon?.let { icon ->
                                    Icon(imageVector = icon, contentDescription = screen.title)
                                }
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MatrixBlack,
                                selectedTextColor = MatrixGreen,
                                indicatorColor = MatrixGreen,
                                unselectedIconColor = MatrixTextSecondary,
                                unselectedTextColor = MatrixTextSecondary
                            ),
                            modifier = Modifier.testTag("nav_item_${screen.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MatrixBlack)
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
                composable(MatrixScreen.Setup.route) {
                    SetupWizardScreen(
                        viewModel = viewModel,
                        onComplete = {
                            navController.navigate(MatrixScreen.Dashboard.route) {
                                popUpTo(MatrixScreen.Setup.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(MatrixScreen.Dashboard.route) {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigate = { route -> navController.navigate(route) }
                    )
                }

                composable(MatrixScreen.Terminal.route) {
                    TerminalScreen(viewModel = viewModel)
                }

                composable(MatrixScreen.Projects.route) {
                    ProjectsScreen(viewModel = viewModel)
                }

                composable(MatrixScreen.Files.route) {
                    FileManagerScreen(viewModel = viewModel)
                }

                composable(MatrixScreen.SshKeys.route) {
                    SshKeysScreen(viewModel = viewModel)
                }

                composable(MatrixScreen.Diagnostics.route) {
                    DiagnosticsScreen(viewModel = viewModel)
                }

                composable(MatrixScreen.Settings.route) {
                    SettingsScreen(viewModel = viewModel)
                }
            }

            if (showHealthDialog) {
                ConnectionHealthDialog(
                    health = health,
                    onProbeNow = { viewModel.triggerHealthProbe() },
                    onDismiss = { showHealthDialog = false }
                )
            }
        }
    }
}
