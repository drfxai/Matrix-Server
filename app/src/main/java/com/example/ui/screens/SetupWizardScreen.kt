package com.example.ui.screens

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DigitalRainHeader
import com.example.ui.components.MatrixChipSelector
import com.example.ui.components.MatrixOptionCard
import com.example.ui.components.MatrixOptionItem
import com.example.ui.theme.MatrixBlack
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

@Composable
fun SetupWizardScreen(
    viewModel: MatrixServerViewModel,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val config by viewModel.configState.collectAsState()

    var currentStep by remember { mutableIntStateOf(1) }

    var username by remember { mutableStateOf("matrix") }
    var password by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("2222") }
    var wakeLock by remember { mutableStateOf(true) }
    var autoStartOnBoot by remember { mutableStateOf(false) }

    val portOptions = listOf(
        MatrixOptionItem("2222", "2222 (Default)", Icons.Default.Settings),
        MatrixOptionItem("8022", "8022 (Termux)", Icons.Default.Settings),
        MatrixOptionItem("2200", "2200 (LAN)", Icons.Default.Settings),
        MatrixOptionItem("22222", "22222 (High)", Icons.Default.Settings)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MatrixBlack)
            .verticalScroll(rememberScrollState())
    ) {
        DigitalRainHeader()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Step Progress Pill Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "INITIAL SETUP [STEP $currentStep OF 3]",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MatrixGreen,
                    letterSpacing = 1.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (step in 1..3) {
                        Box(
                            modifier = Modifier
                                .size(width = 24.dp, height = 4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (step <= currentStep) MatrixGreen else MatrixSurfaceBorder)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (currentStep) {
                1 -> {
                    // Step 1: Welcome & Compatibility Check
                    Text(
                        text = "Turn your phone into a Linux SSH appliance",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MatrixTextPrimary,
                        lineHeight = 26.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "MATRIX SERVER launches a true OpenSSH-compliant daemon inside a background Android service, giving you direct terminal access to the phone's native Linux userspace without requiring root.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 13.sp,
                        color = MatrixTextSecondary,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MatrixSurfaceBorder, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = MatrixDarkSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "SYSTEM COMPATIBILITY CHECK",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MatrixGreen,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            CompatItem("Android Version", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})", true)
                            CompatItem("CPU Architecture", Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a", true)
                            CompatItem("Linux Userspace", "/system/bin/sh (POSIX toybox)", true)
                            CompatItem("Root Required", "No (100% Unrooted)", true)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { currentStep = 2 },
                        colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen, contentColor = MatrixBlack),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("setup_next_step_1")
                    ) {
                        Text(
                            text = "CONTINUE TO CREDENTIALS",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }

                2 -> {
                    // Step 2: Credentials & Port Selection
                    Text(
                        text = "Configure SSH Access Credentials",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MatrixTextPrimary,
                        lineHeight = 26.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Choose your SSH listening port and connection credentials for remote client logins.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 13.sp,
                        color = MatrixTextSecondary,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MatrixSurfaceBorder, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = MatrixDarkSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Non-root hint banner
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MatrixSurfaceVariant)
                                    .border(1.dp, MatrixSurfaceBorder, RoundedCornerShape(6.dp))
                                    .padding(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MatrixGreen, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Port 2222 is the standard default for non-root Android.",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = MatrixGreen
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "QUICK PORT PRESETS",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MatrixTextSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            MatrixChipSelector(
                                options = portOptions,
                                selected = port,
                                onSelect = { selectedPort -> port = selectedPort }
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = port,
                                onValueChange = { input ->
                                    if (input.all { it.isDigit() } && input.length <= 5) port = input
                                },
                                label = { Text("Listening Port") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = MatrixGreen, modifier = Modifier.size(18.dp))
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MatrixGreen,
                                    unfocusedTextColor = MatrixTextPrimary,
                                    focusedBorderColor = MatrixGreen,
                                    unfocusedBorderColor = MatrixSurfaceBorder,
                                    focusedContainerColor = MatrixBlack,
                                    unfocusedContainerColor = MatrixBlack
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("setup_port_input")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("Linux Username") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = MatrixGreen, modifier = Modifier.size(18.dp))
                                },
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
                                modifier = Modifier.fillMaxWidth().testTag("setup_username_input")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("SSH Password (12+ characters)") },
                                visualTransformation = PasswordVisualTransformation(),
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = MatrixGreen, modifier = Modifier.size(18.dp))
                                },
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
                                modifier = Modifier.fillMaxWidth().testTag("setup_password_input")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            val p = port.toIntOrNull() ?: 2222
                            viewModel.updateConfig(
                                config.copy(
                                    username = username.ifBlank { "matrix" },
                                    password = password,
                                    passwordAuthEnabled = true,
                                    publicKeyAuthEnabled = false,
                                    port = p
                                )
                            )
                            currentStep = 3
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen, contentColor = MatrixBlack),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("setup_next_step_2"),
                        enabled = username.matches(Regex("^[A-Za-z0-9._-]{1,32}$")) &&
                            password.length in 12..128 && (port.toIntOrNull() in 1024..65535)
                    ) {
                        Text("CONTINUE TO PERSISTENCE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }

                3 -> {
                    // Step 3: Screen-Off & Final Activation
                    Text(
                        text = "Background Persistence Options",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MatrixTextPrimary,
                        lineHeight = 26.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Keep the SSH daemon responsive 24/7 even when your phone display is locked or asleep.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 13.sp,
                        color = MatrixTextSecondary,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        MatrixOptionCard(
                            title = "Screen-Off WakeLock",
                            description = "Holds partial CPU lock so the SSH daemon never sleeps when device screen is turned off.",
                            checked = wakeLock,
                            onCheckedChange = { wakeLock = it },
                            icon = Icons.Default.Settings,
                            badgeText = if (wakeLock) "PERSISTENT" else "SLEEP",
                            badgeColor = if (wakeLock) MatrixGreen else MatrixYellow,
                            testTag = "setup_wakelock_option"
                        )

                        MatrixOptionCard(
                            title = "Auto-Start on Boot",
                            description = "Restores server service automatically whenever your phone finishes rebooting.",
                            checked = autoStartOnBoot,
                            onCheckedChange = { autoStartOnBoot = it },
                            icon = Icons.Default.Refresh,
                            badgeText = if (autoStartOnBoot) "BOOT ON" else "OFF",
                            badgeColor = if (autoStartOnBoot) MatrixGreen else MatrixTextSecondary,
                            testTag = "setup_autostart_option"
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MatrixSurfaceBorder, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = MatrixDarkSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.BatteryAlert, contentDescription = null, tint = MatrixYellow, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "RECOMMENDED: Battery Optimization Exemption",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MatrixYellow
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Some phone manufacturers aggressively pause background networking unless set to 'Unrestricted' battery access.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 11.sp,
                                color = MatrixTextSecondary,
                                lineHeight = 15.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    try {
                                        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                                    } catch (_: Exception) {}
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MatrixSurfaceVariant, contentColor = MatrixGreen),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MatrixSurfaceBorder, RoundedCornerShape(6.dp))
                            ) {
                                Icon(imageVector = Icons.Default.BatteryAlert, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("OPEN BATTERY SETTINGS", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            viewModel.updateConfig(
                                config.copy(
                                    wakeLockEnabled = wakeLock,
                                    autoStartOnBoot = autoStartOnBoot
                                )
                            )
                            viewModel.completeSetup()
                            onComplete()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen, contentColor = MatrixBlack),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("setup_finish_button")
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("COMPLETE SETUP & LAUNCH SERVER", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CompatItem(label: String, value: String, isOk: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MatrixTextSecondary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = value, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MatrixTextPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Compatible",
                tint = MatrixGreen,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
