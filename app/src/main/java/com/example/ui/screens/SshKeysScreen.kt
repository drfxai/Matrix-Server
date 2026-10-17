package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.ui.components.MatrixDialogButtons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AuthorizedKey
import com.example.server.AuthorizedKeyValidator
import com.example.ui.theme.MatrixBlack
import com.example.ui.theme.MatrixDarkSurface
import com.example.ui.theme.MatrixGreen
import com.example.ui.theme.MatrixRed
import com.example.ui.theme.MatrixSurfaceBorder
import com.example.ui.theme.MatrixSurfaceVariant
import com.example.ui.theme.MatrixTextPrimary
import com.example.ui.theme.MatrixTextSecondary
import com.example.ui.viewmodel.MatrixServerViewModel

@Composable
fun SshKeysScreen(
    viewModel: MatrixServerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val keys by viewModel.authorizedKeysFlow.collectAsState()
    val metrics by viewModel.metricsState.collectAsState()
    val config by viewModel.configState.collectAsState()

    var showAddKeyDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MatrixBlack)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SSH KEYS & ACCESS",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MatrixGreen
                )
                Text(
                    text = "Host Identity & Authorized Public Keys",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MatrixTextSecondary
                )
            }

            Button(
                onClick = { showAddKeyDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MatrixSurfaceVariant, contentColor = MatrixGreen),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.border(1.dp, MatrixGreen, RoundedCornerShape(6.dp))
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "ADD KEY", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Host Key Fingerprint Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MatrixSurfaceBorder, RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(containerColor = MatrixDarkSurface),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = MatrixGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SERVER HOST KEY FINGERPRINT",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MatrixGreen
                        )
                    }

                    IconButton(
                        onClick = {
                            val fp = viewModel.linuxEnv.getHostKeyFingerprint()
                            clipboardManager.setText(AnnotatedString(fp))
                            Toast.makeText(context, "Fingerprint copied", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = MatrixGreen, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(MatrixBlack)
                        .padding(10.dp)
                ) {
                    Text(
                        text = viewModel.linuxEnv.getHostKeyFingerprint(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MatrixTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Verify this SHA-256 fingerprint on your remote SSH client during first connection.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 11.sp,
                    color = MatrixTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Connection Instructions Card
        Text(
            text = "CONNECTION CLIENT CHEATSHEET",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MatrixTextSecondary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MatrixSurfaceBorder, RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(containerColor = MatrixDarkSurface),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                ConnectionCommandSnippet(
                    title = "Windows Terminal / PowerShell",
                    cmd = "ssh ${config.username}@${metrics.lanIpAddress} -p ${config.port}"
                )

                Spacer(modifier = Modifier.height(10.dp))

                ConnectionCommandSnippet(
                    title = "macOS / Linux Terminal",
                    cmd = "ssh ${config.username}@${metrics.lanIpAddress} -p ${config.port}"
                )

                Spacer(modifier = Modifier.height(10.dp))

                ConnectionCommandSnippet(
                    title = "Remote SFTP (WinSCP / FileZilla / VS Code)",
                    cmd = "sftp -P ${config.port} ${config.username}@${metrics.lanIpAddress}"
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Authorized Public Keys Section
        Text(
            text = "AUTHORIZED PUBLIC KEYS (${keys.size})",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MatrixTextSecondary
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (keys.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MatrixDarkSurface)
                    .border(1.dp, MatrixSurfaceBorder, RoundedCornerShape(8.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (config.passwordAuthEnabled) "No SSH public keys configured.\nPassword login is enabled." else "No SSH public keys configured.\nSSH requires a configured authentication method.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MatrixTextSecondary
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                keys.forEach { key ->
                    AuthorizedKeyRow(
                        key = key,
                        onDelete = { viewModel.deleteAuthorizedKey(key) }
                    )
                }
            }
        }
    }

    if (showAddKeyDialog) {
        var label by remember { mutableStateOf("") }
        var keyText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddKeyDialog = false },
            containerColor = MatrixDarkSurface,
            title = { Text("ADD AUTHORIZED PUBLIC KEY", fontFamily = FontFamily.Monospace, color = MatrixGreen, fontSize = 15.sp) },
            text = {
                Column {
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("Device Label (e.g. My Laptop)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MatrixTextPrimary,
                            unfocusedTextColor = MatrixTextPrimary,
                            focusedBorderColor = MatrixGreen,
                            unfocusedBorderColor = MatrixSurfaceBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = keyText,
                        onValueChange = { keyText = it },
                        label = { Text("Public Key (ssh-rsa or ssh-ed25519)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MatrixTextPrimary,
                            unfocusedTextColor = MatrixTextPrimary,
                            focusedBorderColor = MatrixGreen,
                            unfocusedBorderColor = MatrixSurfaceBorder
                        ),
                        modifier = Modifier.fillMaxWidth().height(140.dp)
                    )
                }
            },
            confirmButton = {
                MatrixDialogButtons(
                    confirmText = "SAVE KEY",
                    confirmIcon = Icons.Default.Add,
                    confirmEnabled = AuthorizedKeyValidator.parse(keyText) != null,
                    onConfirm = {
                        if (keyText.isNotBlank()) {
                            if (viewModel.addAuthorizedKey(label, keyText)) {
                                showAddKeyDialog = false
                            }
                        }
                    },
                    onDismiss = { showAddKeyDialog = false }
                )
            },
            dismissButton = {}
        )
    }
}

@Composable
private fun ConnectionCommandSnippet(title: String, cmd: String) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Column {
        Text(
            text = title,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = MatrixTextSecondary
        )
        Spacer(modifier = Modifier.height(3.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(MatrixBlack)
                .clickable {
                    clipboardManager.setText(AnnotatedString(cmd))
                    Toast.makeText(context, "Command copied", Toast.LENGTH_SHORT).show()
                }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = cmd,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = MatrixGreen,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy",
                tint = MatrixGreen,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun AuthorizedKeyRow(key: AuthorizedKey, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MatrixSurfaceBorder, RoundedCornerShape(6.dp)),
        colors = CardDefaults.cardColors(containerColor = MatrixDarkSurface),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = MatrixGreen, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = key.label,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MatrixTextPrimary
                )
                Text(
                    text = key.fingerprint,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = MatrixTextSecondary
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MatrixRed, modifier = Modifier.size(16.dp))
            }
        }
    }
}
