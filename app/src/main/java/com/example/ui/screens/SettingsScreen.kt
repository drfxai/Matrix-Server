package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AuthorizedKey
import com.example.ui.components.MatrixChipSelector
import com.example.ui.components.MatrixOptionCard
import com.example.ui.components.MatrixOptionItem
import com.example.ui.theme.MatrixBlack
import com.example.ui.theme.MatrixBlue
import com.example.ui.theme.MatrixDarkSurface
import com.example.ui.theme.MatrixGreen
import com.example.ui.theme.MatrixGreenDark
import com.example.ui.theme.MatrixRed
import com.example.ui.theme.MatrixSurfaceBorder
import com.example.ui.theme.MatrixSurfaceVariant
import com.example.ui.theme.MatrixTextPrimary
import com.example.ui.theme.MatrixTextSecondary
import com.example.ui.theme.MatrixYellow
import com.example.ui.viewmodel.MatrixServerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: MatrixServerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val config by viewModel.configState.collectAsState()
    val serverState by viewModel.serverState.collectAsState()
    val authorizedKeys by viewModel.authorizedKeysFlow.collectAsState()

    var selectedSectionTab by remember { mutableIntStateOf(0) } // 0: Credentials & Port, 1: Public Keys, 2: Daemon Policies

    var portText by remember(config) { mutableStateOf(config.port.toString()) }
    var username by remember(config) { mutableStateOf(config.username) }
    var password by remember(config) { mutableStateOf(config.password) }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var passwordAuth by remember(config) { mutableStateOf(config.passwordAuthEnabled) }
    var keyAuth by remember(config) { mutableStateOf(config.publicKeyAuthEnabled) }
    var wakeLock by remember(config) { mutableStateOf(config.wakeLockEnabled) }
    var autoStartOnBoot by remember(config) { mutableStateOf(config.autoStartOnBoot) }
    var lowPowerMode by remember(config) { mutableStateOf(config.lowPowerMode) }

    var showAddKeyDialog by remember { mutableStateOf(false) }

    val parsedPort = portText.toIntOrNull()
    val isDefaultPort = parsedPort == 2222
    val isPrivilegedPort = parsedPort != null && parsedPort in 1..1023
    val isPortOutOfRange = parsedPort != null && (parsedPort <= 0 || parsedPort > 65535)
    val isValidPort = parsedPort != null && parsedPort in 1024..65535
    val isPortModified = parsedPort != null && parsedPort != config.port

    // Quick port chips focusing on unprivileged non-root ports
    val portChips = listOf(
        MatrixOptionItem(2222, "2222 (Default)", Icons.Default.Settings),
        MatrixOptionItem(8022, "8022 (Termux)", Icons.Default.Settings),
        MatrixOptionItem(2200, "2200 (LAN)", Icons.Default.Settings),
        MatrixOptionItem(22222, "22222 (High)", Icons.Default.Settings)
    )

    val passwordStrength = remember(password) {
        when {
            password.length >= 10 -> "STRONG" to MatrixGreen
            password.length >= 6 -> "MODERATE" to MatrixYellow
            password.isNotBlank() -> "WEAK" to MatrixRed
            else -> "EMPTY" to MatrixTextSecondary
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MatrixBlack)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("settings_screen")
    ) {
        // Top Header with properly balanced layout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text(
                    text = "SERVER CREDENTIALS & SETTINGS",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MatrixGreen,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Manage listening port, SSH keys, passwords & policies",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 11.sp,
                    color = MatrixTextSecondary
                )
            }

            Button(
                onClick = {
                    val p = parsedPort ?: 2222
                    val updated = config.copy(
                        port = p,
                        username = username.ifBlank { "matrix" },
                        password = password,
                        passwordAuthEnabled = passwordAuth,
                        publicKeyAuthEnabled = keyAuth,
                        wakeLockEnabled = wakeLock,
                        autoStartOnBoot = autoStartOnBoot,
                        lowPowerMode = lowPowerMode
                    )
                    viewModel.updateConfig(updated)
                    Toast.makeText(context, "Settings saved successfully (Port: $p)", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen, contentColor = MatrixBlack),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(40.dp)
                    .testTag("save_settings_button")
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("SAVE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION TABS: CREDENTIALS & PORT / PUBLIC KEYS / DAEMON POLICIES
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MatrixDarkSurface)
                .border(1.dp, MatrixSurfaceBorder, RoundedCornerShape(8.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val tabs = listOf(
                "CREDENTIALS & PORT" to Icons.Default.Lock,
                "PUBLIC KEYS (${authorizedKeys.size})" to Icons.Default.VpnKey,
                "POLICIES" to Icons.Default.Settings
            )

            tabs.forEachIndexed { index, (label, icon) ->
                val isSelected = selectedSectionTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) MatrixGreen.copy(alpha = 0.16f) else Color.Transparent)
                        .border(
                            width = if (isSelected) 1.dp else 0.dp,
                            color = if (isSelected) MatrixGreen else Color.Transparent,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { selectedSectionTab = index }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) MatrixGreen else MatrixTextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = label,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp,
                            color = if (isSelected) MatrixGreen else MatrixTextSecondary,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        when (selectedSectionTab) {
            0 -> {
                // TAB 0: CREDENTIALS & PORT CONFIGURATION
                // SECTION: ONE-TAP CONFIGURATION PRESETS
                Text(
                    text = "ONE-TAP CONFIGURATION PRESETS",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MatrixTextSecondary,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PresetTile(
                        title = "Standard",
                        subtitle = "Port 2222",
                        badge = "DEFAULT",
                        icon = Icons.Default.Terminal,
                        accentColor = MatrixGreen,
                        isSelected = isDefaultPort && passwordAuth && keyAuth && wakeLock,
                        onClick = {
                            portText = "2222"
                            passwordAuth = true
                            keyAuth = true
                            wakeLock = true
                            lowPowerMode = false
                            Toast.makeText(context, "Applied Standard Preset (Port 2222)", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    )

                    PresetTile(
                        title = "Hardened",
                        subtitle = "Key-Only",
                        badge = "SECURE",
                        icon = Icons.Default.Security,
                        accentColor = MatrixBlue,
                        isSelected = !passwordAuth && keyAuth,
                        onClick = {
                            portText = "8022"
                            passwordAuth = false
                            keyAuth = true
                            wakeLock = true
                            lowPowerMode = false
                            Toast.makeText(context, "Applied Hardened Security Preset", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    )

                    PresetTile(
                        title = "Eco Saver",
                        subtitle = "Low Battery",
                        badge = "ECO",
                        icon = Icons.Default.BatteryAlert,
                        accentColor = MatrixYellow,
                        isSelected = lowPowerMode && !wakeLock,
                        onClick = {
                            wakeLock = false
                            lowPowerMode = true
                            Toast.makeText(context, "Applied Eco Saver Preset", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // SECTION: SSH SERVER PORT CONFIGURATION
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SSH SERVER PORT CONFIGURATION",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MatrixGreen,
                        letterSpacing = 0.5.sp
                    )

                    // Current Active Port Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (serverState.isRunning) MatrixGreen.copy(alpha = 0.15f) else MatrixSurfaceVariant)
                            .border(1.dp, if (serverState.isRunning) MatrixGreen else MatrixSurfaceBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (serverState.isRunning) "RUNNING ON :${config.port}" else "SAVED :${config.port}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (serverState.isRunning) MatrixGreen else MatrixTextSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MatrixSurfaceBorder, RoundedCornerShape(12.dp))
                        .testTag("settings_port_panel"),
                    colors = CardDefaults.cardColors(containerColor = MatrixDarkSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Non-Root Notice Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MatrixSurfaceVariant)
                                .border(1.dp, MatrixSurfaceBorder, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MatrixGreen,
                                    modifier = Modifier.size(16.dp).padding(top = 1.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "NON-ROOT ANDROID DEFAULT: PORT 2222",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = MatrixGreen
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Android unprivileged user space enforces ports >= 1024 without root. Standard port 2222 is configured by default.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontSize = 11.sp,
                                        color = MatrixTextSecondary,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick Port Presets
                        Text(
                            text = "SELECT PORT PRESET",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MatrixTextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        MatrixChipSelector(
                            options = portChips,
                            selected = parsedPort ?: 2222,
                            onSelect = { selectedPort ->
                                portText = selectedPort.toString()
                            }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Custom Port TextField with Number Pad
                        OutlinedTextField(
                            value = portText,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() } && input.length <= 5) {
                                    portText = input
                                }
                            },
                            label = { Text("Custom Listening Port") },
                            placeholder = { Text("2222") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = if (isValidPort) MatrixGreen else if (isPrivilegedPort) MatrixYellow else MatrixRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (!isDefaultPort) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MatrixSurfaceVariant)
                                            .clickable { portText = "2222" }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                            .testTag("reset_default_port_button")
                                    ) {
                                        Text(
                                            text = "RESET (2222)",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MatrixGreen
                                        )
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MatrixGreen,
                                unfocusedTextColor = MatrixTextPrimary,
                                focusedBorderColor = if (isPrivilegedPort) MatrixYellow else MatrixGreen,
                                unfocusedBorderColor = if (isPrivilegedPort) MatrixYellow.copy(alpha = 0.6f) else MatrixSurfaceBorder,
                                focusedContainerColor = MatrixBlack,
                                unfocusedContainerColor = MatrixBlack
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("settings_port_input")
                        )

                        // Live Port Validation Feedback
                        Spacer(modifier = Modifier.height(8.dp))
                        when {
                            isPortOutOfRange || parsedPort == null -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MatrixRed.copy(alpha = 0.12f))
                                        .border(1.dp, MatrixRed.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                        .padding(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = MatrixRed, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "INVALID PORT: Must be between 1024 and 65535.",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            color = MatrixRed
                                        )
                                    }
                                }
                            }
                            isPrivilegedPort -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MatrixYellow.copy(alpha = 0.12f))
                                        .border(1.dp, MatrixYellow.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                        .padding(8.dp)
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = MatrixYellow, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "PRIVILEGED PORT WARNING (< 1024)",
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                color = MatrixYellow
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Port $parsedPort requires root privileges on Android. Daemon socket binding will fail without root. Use default 2222 instead.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontSize = 10.sp,
                                            color = MatrixYellow,
                                            lineHeight = 13.sp
                                        )
                                    }
                                }
                            }
                            isValidPort -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MatrixGreenDark.copy(alpha = 0.12f))
                                        .border(1.dp, MatrixGreen.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                        .padding(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = MatrixGreen, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isDefaultPort) "DEFAULT NON-ROOT PORT (OPTIMAL)" else "VALID UNPRIVILEGED PORT ($parsedPort)",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MatrixGreen
                                        )
                                    }
                                }
                            }
                        }

                        // If port changed while server is active, show Save & Restart
                        AnimatedVisibility(visible = isPortModified && serverState.isRunning && isValidPort) {
                            Column {
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MatrixBlue.copy(alpha = 0.12f))
                                        .border(1.dp, MatrixBlue.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "APPLY NEW PORT :$parsedPort TO ACTIVE SERVER",
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = MatrixBlue
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Server currently listens on :${config.port}. Tap below to restart daemon on port $parsedPort.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontSize = 10.sp,
                                            color = MatrixTextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                val updated = config.copy(
                                                    port = parsedPort ?: 2222,
                                                    username = username.ifBlank { "matrix" },
                                                    password = password,
                                                    passwordAuthEnabled = passwordAuth,
                                                    publicKeyAuthEnabled = keyAuth,
                                                    wakeLockEnabled = wakeLock,
                                                    autoStartOnBoot = autoStartOnBoot,
                                                    lowPowerMode = lowPowerMode
                                                )
                                                viewModel.updateConfigAndRestart(updated)
                                                Toast.makeText(context, "Restarting server on port $parsedPort...", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MatrixBlue, contentColor = MatrixBlack),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.fillMaxWidth().testTag("save_restart_port_button")
                                        ) {
                                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "SAVE & RESTART SERVER (PORT $parsedPort)",
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // SECTION: LINUX USER CREDENTIALS
                Text(
                    text = "LINUX SHELL USER & CREDENTIALS",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MatrixTextSecondary,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MatrixSurfaceBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = MatrixDarkSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Linux Shell Username") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = MatrixGreen, modifier = Modifier.size(18.dp))
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MatrixGreen,
                                unfocusedTextColor = MatrixTextPrimary,
                                focusedBorderColor = MatrixGreen,
                                unfocusedBorderColor = MatrixSurfaceBorder,
                                focusedContainerColor = MatrixBlack,
                                unfocusedContainerColor = MatrixBlack
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("settings_username_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("SSH Password") },
                            singleLine = true,
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = MatrixGreen, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle Password Visibility",
                                        tint = MatrixTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MatrixGreen,
                                unfocusedTextColor = MatrixTextPrimary,
                                focusedBorderColor = MatrixGreen,
                                unfocusedBorderColor = MatrixSurfaceBorder,
                                focusedContainerColor = MatrixBlack,
                                unfocusedContainerColor = MatrixBlack
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("settings_password_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PASSWORD STRENGTH:",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = MatrixTextSecondary
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(passwordStrength.second.copy(alpha = 0.15f))
                                    .border(0.8.dp, passwordStrength.second, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = passwordStrength.first,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = passwordStrength.second
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // SECTION: AUTHENTICATION METHODS
                Text(
                    text = "AUTHENTICATION METHODS",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MatrixTextSecondary,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MatrixOptionCard(
                        title = "Password Authentication",
                        description = "Permits SSH logins using the username and password defined above.",
                        checked = passwordAuth,
                        onCheckedChange = { passwordAuth = it },
                        icon = Icons.Default.Lock,
                        badgeText = if (passwordAuth) "ENABLED" else "DISABLED",
                        badgeColor = if (passwordAuth) MatrixGreen else MatrixRed,
                        testTag = "option_password_auth"
                    )

                    MatrixOptionCard(
                        title = "Public Key Authentication",
                        description = "Accepts Ed25519 & RSA public keys managed in the Public Keys tab.",
                        checked = keyAuth,
                        onCheckedChange = { keyAuth = it },
                        icon = Icons.Default.VpnKey,
                        badgeText = if (keyAuth) "ACTIVE" else "DISABLED",
                        badgeColor = if (keyAuth) MatrixGreen else MatrixRed,
                        testTag = "option_key_auth"
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Strict Key-Only Security Mode Quick Toggle
                Button(
                    onClick = {
                        passwordAuth = false
                        keyAuth = true
                        Toast.makeText(context, "Strict Key-Only Authentication Enabled", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MatrixSurfaceVariant, contentColor = MatrixBlue),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MatrixBlue, RoundedCornerShape(8.dp))
                        .testTag("enforce_key_only_button")
                ) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ENFORCE STRICT KEY-ONLY MODE", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            1 -> {
                // TAB 1: AUTHORIZED PUBLIC KEYS & KEY MANAGEMENT
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "AUTHORIZED PUBLIC KEYS",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MatrixGreen,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "${authorizedKeys.size} key(s) in ~/.ssh/authorized_keys",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 10.sp,
                            color = MatrixTextSecondary
                        )
                    }

                    Button(
                        onClick = { showAddKeyDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen, contentColor = MatrixBlack),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.testTag("add_key_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ ADD KEY", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (authorizedKeys.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MatrixSurfaceBorder, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = MatrixDarkSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = MatrixTextSecondary, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "NO AUTHORIZED PUBLIC KEYS REGISTERED",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MatrixTextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Add client public keys (Ed25519 or RSA) to allow passwordless SSH logins.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 11.sp,
                                color = MatrixTextSecondary,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        authorizedKeys.forEach { keyItem ->
                            AuthorizedKeyCard(
                                keyItem = keyItem,
                                onDelete = {
                                    viewModel.deleteAuthorizedKey(keyItem)
                                    Toast.makeText(context, "Key '${keyItem.label}' removed", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Host Key Fingerprint Card
                Text(
                    text = "SERVER HOST KEY FINGERPRINT",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MatrixTextSecondary,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MatrixSurfaceBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = MatrixDarkSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = MatrixBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ED25519 Host Key Fingerprint", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MatrixBlue)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val hostFp = "SHA256:MatrixHostKey998a7c2b04f1e"
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(MatrixBlack)
                                .border(1.dp, MatrixSurfaceBorder, RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = hostFp,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = MatrixGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(hostFp))
                                Toast.makeText(context, "Host key fingerprint copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MatrixBlue),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth().border(1.dp, MatrixBlue, RoundedCornerShape(6.dp))
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("COPY HOST FINGERPRINT", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                    }
                }
            }

            2 -> {
                // TAB 2: DAEMON POLICIES & POWER OPTIONS
                Text(
                    text = "BACKGROUND PERSISTENCE & POWER POLICIES",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MatrixTextSecondary,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MatrixOptionCard(
                        title = "Screen-Off Persistence (WakeLock)",
                        description = "Holds partial CPU lock so the SSH daemon never sleeps when device screen is turned off.",
                        checked = wakeLock,
                        onCheckedChange = { wakeLock = it },
                        icon = Icons.Default.Settings,
                        badgeText = if (wakeLock) "PERSISTENT" else "IDLE SLEEP",
                        badgeColor = if (wakeLock) MatrixGreen else MatrixYellow,
                        testTag = "option_wakelock"
                    )

                    MatrixOptionCard(
                        title = "Auto-Start on Device Reboot",
                        description = "Spawns the SSH foreground service automatically after phone finishes booting.",
                        checked = autoStartOnBoot,
                        onCheckedChange = { autoStartOnBoot = it },
                        icon = Icons.Default.Refresh,
                        badgeText = if (autoStartOnBoot) "BOOT ON" else "MANUAL",
                        badgeColor = if (autoStartOnBoot) MatrixGreen else MatrixTextSecondary,
                        testTag = "option_autostart"
                    )

                    MatrixOptionCard(
                        title = "Low-Power Mode",
                        description = "Disables digital rain background rendering and reduces CPU telemetry polling rate.",
                        checked = lowPowerMode,
                        onCheckedChange = { lowPowerMode = it },
                        icon = Icons.Default.BatteryAlert,
                        badgeText = if (lowPowerMode) "ECO ACTIVE" else "FULL POWER",
                        badgeColor = if (lowPowerMode) MatrixYellow else MatrixBlue,
                        testTag = "option_lowpower"
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Battery optimization settings shortcut
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            Toast.makeText(context, "Open Android Settings > Battery > Unrestricted", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MatrixSurfaceVariant, contentColor = MatrixGreen),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MatrixSurfaceBorder, RoundedCornerShape(8.dp))
                        .testTag("battery_optimization_button")
                ) {
                    Icon(imageVector = Icons.Default.BatteryAlert, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("OPEN ANDROID BATTERY OPTIMIZATIONS", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // SECTION: REMOTE ACCESS OUTSIDE HOME NETWORK
                Text(
                    text = "REMOTE ACCESS OUTSIDE HOME NETWORK",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MatrixTextSecondary,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MatrixSurfaceBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = MatrixDarkSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.VpnLock, contentDescription = null, tint = MatrixBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tailscale Private Mesh VPN", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MatrixBlue)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "To access your Android Matrix Server securely from cellular data or outside home WiFi (zero port forwarding required):\n\n" +
                                    "1. Install Tailscale on this Android phone.\n" +
                                    "2. Sign in to your Tailscale tailnet.\n" +
                                    "3. Install Tailscale on your laptop/client machine.\n" +
                                    "4. Matrix Server automatically discovers your 100.x.y.z IP and displays it on the Dashboard!\n" +
                                    "5. Connect from anywhere: ssh matrix@100.x.y.z -p ${portText.ifBlank { "2222" }}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 11.sp,
                            color = MatrixTextSecondary,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://tailscale.com/download/android"))
                                context.startActivity(browserIntent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MatrixSurfaceVariant, contentColor = MatrixBlue),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth().border(1.dp, MatrixBlue, RoundedCornerShape(6.dp))
                        ) {
                            Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("GET TAILSCALE APP", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }

    // Modal Dialog: Add Authorized Key
    if (showAddKeyDialog) {
        AddAuthorizedKeyDialog(
            onDismiss = { showAddKeyDialog = false },
            onAddKey = { label, keyContent ->
                viewModel.addAuthorizedKey(label, keyContent)
                Toast.makeText(context, "Added new authorized key", Toast.LENGTH_SHORT).show()
                showAddKeyDialog = false
            }
        )
    }
}

@Composable
private fun AuthorizedKeyCard(
    keyItem: AuthorizedKey,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val formattedDate = remember(keyItem.addedAt) { dateFormat.format(Date(keyItem.addedAt)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MatrixSurfaceBorder, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = MatrixDarkSurface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MatrixGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.VpnKey, contentDescription = null, tint = MatrixGreen, modifier = Modifier.size(15.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = keyItem.label,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MatrixTextPrimary
                        )
                        Text(
                            text = "Added $formattedDate",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.5.sp,
                            color = MatrixTextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete key", tint = MatrixRed, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(MatrixBlack)
                    .border(1.dp, MatrixSurfaceBorder, RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                Column {
                    Text(
                        text = keyItem.fingerprint,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MatrixGreen
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = keyItem.publicKey,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.5.sp,
                        color = MatrixTextSecondary,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
private fun AddAuthorizedKeyDialog(
    onDismiss: () -> Unit,
    onAddKey: (label: String, keyContent: String) -> Unit
) {
    var label by remember { mutableStateOf("") }
    var keyContent by remember { mutableStateOf("") }

    val sampleKey = "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIG3v8qX84xQ9RzL0mN1P5S8T2vB9kL4wP3xQ2yZ1aB0c developer@matrix-laptop"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.VpnKey, contentDescription = null, tint = MatrixGreen, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("ADD AUTHORIZED PUBLIC KEY", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MatrixGreen)
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Key Label / Description") },
                    placeholder = { Text("e.g. Work Laptop ed25519") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MatrixGreen,
                        unfocusedTextColor = MatrixTextPrimary,
                        focusedBorderColor = MatrixGreen,
                        unfocusedBorderColor = MatrixSurfaceBorder,
                        focusedContainerColor = MatrixBlack,
                        unfocusedContainerColor = MatrixBlack
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = keyContent,
                    onValueChange = { keyContent = it },
                    label = { Text("Public Key String (Ed25519 / RSA)") },
                    placeholder = { Text("ssh-ed25519 AAAAC3...") },
                    minLines = 3,
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MatrixGreen,
                        unfocusedTextColor = MatrixTextPrimary,
                        focusedBorderColor = MatrixGreen,
                        unfocusedBorderColor = MatrixSurfaceBorder,
                        focusedContainerColor = MatrixBlack,
                        unfocusedContainerColor = MatrixBlack
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = {
                        label = "Developer Ed25519 Key"
                        keyContent = sampleKey
                    }
                ) {
                    Text("Paste Sample Ed25519 Key", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MatrixBlue)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (keyContent.isNotBlank()) {
                        onAddKey(label, keyContent)
                    }
                },
                enabled = keyContent.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen, contentColor = MatrixBlack),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("ADD KEY", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", fontFamily = FontFamily.Monospace, color = MatrixTextSecondary)
            }
        },
        containerColor = MatrixDarkSurface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.border(1.dp, MatrixSurfaceBorder, RoundedCornerShape(12.dp))
    )
}

@Composable
private fun PresetTile(
    title: String,
    subtitle: String,
    badge: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) accentColor.copy(alpha = 0.1f) else MatrixDarkSurface)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) accentColor else MatrixSurfaceBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = if (isSelected) accentColor else MatrixTextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 9.sp,
                color = MatrixTextSecondary
            )
        }
    }
}
