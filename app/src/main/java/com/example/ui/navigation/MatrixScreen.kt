package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.ui.graphics.vector.ImageVector

sealed class MatrixScreen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Dashboard : MatrixScreen("dashboard", "Console", Icons.Default.Storage)
    object Terminal : MatrixScreen("terminal", "Terminal", Icons.Default.Terminal)
    object Projects : MatrixScreen("projects", "Projects", Icons.Default.PlayArrow)
    object Files : MatrixScreen("files", "Files", Icons.Default.Folder)
    object SshKeys : MatrixScreen("ssh_keys", "SSH Keys", Icons.Default.Key)
    object Diagnostics : MatrixScreen("diagnostics", "Diagnostics", Icons.Default.Build)
    object Settings : MatrixScreen("settings", "Settings", Icons.Default.Settings)
    object Setup : MatrixScreen("setup", "Setup Wizard")
}
