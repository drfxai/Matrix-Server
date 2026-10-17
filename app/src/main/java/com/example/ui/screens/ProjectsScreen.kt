package com.example.ui.screens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.ui.draw.clip
import com.example.ui.components.MatrixChipSelector
import com.example.ui.components.MatrixCyberSwitch
import com.example.ui.components.MatrixDialogButtons
import com.example.ui.components.MatrixOptionCard
import com.example.ui.components.MatrixOptionItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProjectItem
import com.example.ui.theme.MatrixBlack
import com.example.ui.theme.MatrixBlue
import com.example.ui.theme.MatrixDarkSurface
import com.example.ui.theme.MatrixGreen
import com.example.ui.theme.MatrixRed
import com.example.ui.theme.MatrixSurfaceBorder
import com.example.ui.theme.MatrixSurfaceVariant
import com.example.ui.theme.MatrixTextPrimary
import com.example.ui.theme.MatrixTextSecondary
import com.example.ui.theme.MatrixYellow
import com.example.ui.viewmodel.MatrixServerViewModel

@Composable
fun ProjectsScreen(
    viewModel: MatrixServerViewModel,
    modifier: Modifier = Modifier
) {
    val projects by viewModel.projectsFlow.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedProjectForLogs by remember { mutableStateOf<ProjectItem?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MatrixBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                        text = "PROJECT SUPERVISOR",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        letterSpacing = 1.sp,
                        color = MatrixGreen
                    )
                    Text(
                        text = "Managed Python, Node.js & Shell processes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MatrixTextSecondary
                    )
                }

                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MatrixSurfaceVariant,
                        contentColor = MatrixGreen
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.border(1.dp, MatrixGreen, RoundedCornerShape(6.dp)).testTag("new_project_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "NEW", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (projects.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No managed projects configured.\nTap '+ NEW' to create one.",
                        fontFamily = FontFamily.Monospace,
                        color = MatrixTextSecondary,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(projects, key = { it.id }) { project ->
                        ProjectCard(
                            project = project,
                            onStart = { viewModel.startProject(project) },
                            onStop = { viewModel.stopProject(project.id) },
                            onRestart = { viewModel.restartProject(project) },
                            onViewLogs = { selectedProjectForLogs = project },
                            onDelete = { viewModel.deleteProject(project) }
                        )
                    }
                }
            }
        }

        // Create Project Dialog
        if (showCreateDialog) {
            CreateProjectDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { name, type, command, port, autoRestart ->
                    viewModel.createProject(name, type, command, port, autoRestart)
                    showCreateDialog = false
                }
            )
        }

        // Live Logs Dialog
        if (selectedProjectForLogs != null) {
            val proj = selectedProjectForLogs!!
            val logs = viewModel.getProjectLogs(proj.id)
            ProjectLogsDialog(
                project = proj,
                logs = logs,
                onDismiss = { selectedProjectForLogs = null }
            )
        }
    }
}

@Composable
private fun ProjectCard(
    project: ProjectItem,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
    onViewLogs: () -> Unit,
    onDelete: () -> Unit
) {
    val isRunning = project.status == "RUNNING"
    val isFailed = project.status == "FAILED"

    val typeColor = when (project.type.lowercase()) {
        "python" -> MatrixGreen
        "node.js", "node" -> MatrixBlue
        "shell" -> MatrixYellow
        else -> Color(0xFFB388FF)
    }

    val statusColor = when {
        isRunning -> MatrixGreen
        isFailed -> MatrixRed
        else -> MatrixTextSecondary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isRunning) MatrixGreen.copy(alpha = 0.6f) else MatrixSurfaceBorder,
                RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = MatrixDarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Runtime Badge + Name + Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(typeColor.copy(alpha = 0.15f))
                            .border(0.8.dp, typeColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = project.type.uppercase(),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = typeColor
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = project.name,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MatrixTextPrimary,
                        maxLines = 1
                    )
                }

                // Status Badge with live indicator dot
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = project.status,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = statusColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Command Inset Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(MatrixBlack)
                    .border(1.dp, MatrixSurfaceBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MatrixGreen
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = project.command,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MatrixTextPrimary,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metadata Chips Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (project.port != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MatrixSurfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "PORT :${project.port}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = MatrixGreen
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MatrixSurfaceVariant)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (project.autoRestart) "RESTART ON" else "RESTART OFF",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = MatrixTextSecondary
                        )
                    }

                    if (project.pid != null) {
                        Text(
                            text = "PID ${project.pid}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = MatrixBlue
                        )
                    }
                }

                // Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onViewLogs, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.List, contentDescription = "View Logs", tint = MatrixTextSecondary, modifier = Modifier.size(16.dp))
                    }

                    if (isRunning) {
                        IconButton(onClick = onRestart, modifier = Modifier.size(32.dp)) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Restart", tint = MatrixYellow, modifier = Modifier.size(16.dp))
                        }
                        Button(
                            onClick = onStop,
                            colors = ButtonDefaults.buttonColors(containerColor = MatrixRed.copy(alpha = 0.2f), contentColor = MatrixRed),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .border(1.dp, MatrixRed.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        ) {
                            Icon(imageVector = Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "STOP", fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MatrixTextSecondary, modifier = Modifier.size(16.dp))
                        }
                        Button(
                            onClick = onStart,
                            colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen, contentColor = MatrixBlack),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp), tint = MatrixBlack)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "START", fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MatrixBlack)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, type: String, command: String, port: Int?, autoRestart: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Python") }
    var command by remember { mutableStateOf("python3 -m http.server 8080") }
    var portText by remember { mutableStateOf("8080") }
    var autoRestart by remember { mutableStateOf(true) }

    val typeOptions = listOf(
        MatrixOptionItem("Python", "Python", Icons.Default.Terminal),
        MatrixOptionItem("Node.js", "Node.js", Icons.Default.Memory),
        MatrixOptionItem("Shell", "Shell", Icons.Default.Code),
        MatrixOptionItem("Custom", "Custom", Icons.Default.Build)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MatrixDarkSurface,
        modifier = Modifier.border(1.dp, MatrixGreen.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MatrixGreen.copy(alpha = 0.15f))
                            .border(1.dp, MatrixGreen.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "WORKLOAD",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = MatrixGreen
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "NEW PROJECT",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MatrixTextPrimary,
                        fontSize = 16.sp
                    )
                }
                Text(
                    text = "Configure supervised background execution",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 11.sp,
                    color = MatrixTextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Project Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Project Name") },
                    placeholder = { Text("e.g. web-api", color = MatrixTextSecondary.copy(alpha = 0.5f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MatrixTextPrimary,
                        unfocusedTextColor = MatrixTextPrimary,
                        focusedBorderColor = MatrixGreen,
                        unfocusedBorderColor = MatrixSurfaceBorder,
                        focusedLabelColor = MatrixGreen,
                        unfocusedLabelColor = MatrixTextSecondary,
                        focusedContainerColor = MatrixBlack,
                        unfocusedContainerColor = MatrixBlack
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("project_name_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Runtime Category Selector
                Text(
                    text = "RUNTIME ENVIRONMENT",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MatrixTextSecondary,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                MatrixChipSelector(
                    options = typeOptions,
                    selected = type,
                    onSelect = { selectedType ->
                        type = selectedType
                        when (selectedType) {
                            "Python" -> { command = "python3 server.py"; portText = "8080" }
                            "Node.js" -> { command = "node app.js"; portText = "3000" }
                            "Shell" -> { command = "sh worker.sh"; portText = "" }
                            "Custom" -> { command = "./run.sh"; portText = "8000" }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Command Input
                OutlinedTextField(
                    value = command,
                    onValueChange = { command = it },
                    label = { Text("Startup Command") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MatrixGreen,
                        unfocusedTextColor = MatrixTextPrimary,
                        focusedBorderColor = MatrixGreen,
                        unfocusedBorderColor = MatrixSurfaceBorder,
                        focusedLabelColor = MatrixGreen,
                        unfocusedLabelColor = MatrixTextSecondary,
                        focusedContainerColor = MatrixBlack,
                        unfocusedContainerColor = MatrixBlack
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("project_command_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                val portChips = listOf(
                    MatrixOptionItem("8080", "8080 (Web)"),
                    MatrixOptionItem("3000", "3000 (Node)"),
                    MatrixOptionItem("5000", "5000 (Flask)"),
                    MatrixOptionItem("8000", "8000 (API)"),
                    MatrixOptionItem("", "None")
                )

                Text(
                    text = "LISTENING PORT PRESETS",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MatrixTextSecondary,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                MatrixChipSelector(
                    options = portChips,
                    selected = portText,
                    onSelect = { selectedPort -> portText = selectedPort }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Port Input
                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it },
                    label = { Text("Port (leave blank if CLI worker)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MatrixTextPrimary,
                        unfocusedTextColor = MatrixTextPrimary,
                        focusedBorderColor = MatrixGreen,
                        unfocusedBorderColor = MatrixSurfaceBorder,
                        focusedLabelColor = MatrixGreen,
                        unfocusedLabelColor = MatrixTextSecondary,
                        focusedContainerColor = MatrixBlack,
                        unfocusedContainerColor = MatrixBlack
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("project_port_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Redesigned Option Card: Auto-Restart
                MatrixOptionCard(
                    title = "Auto-Restart on Crash",
                    description = "Supervises process and automatically restarts with exponential backoff if terminated.",
                    checked = autoRestart,
                    onCheckedChange = { autoRestart = it },
                    icon = Icons.Default.Refresh,
                    badgeText = if (autoRestart) "SUPERVISED" else "ONCE",
                    badgeColor = if (autoRestart) MatrixGreen else MatrixTextSecondary,
                    testTag = "project_autorestart_option"
                )
            }
        },
        confirmButton = {
            MatrixDialogButtons(
                confirmText = "CREATE PROJECT",
                confirmIcon = Icons.Default.Add,
                confirmEnabled = name.isNotBlank(),
                onConfirm = {
                    if (name.isNotBlank()) {
                        onCreate(name, type, command, portText.toIntOrNull(), autoRestart)
                    }
                },
                onDismiss = onDismiss
            )
        },
        dismissButton = {}
    )
}

@Composable
private fun ProjectLogsDialog(
    project: ProjectItem,
    logs: List<String>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MatrixDarkSurface,
        title = {
            Text(
                text = "LOGS: ${project.name}",
                fontFamily = FontFamily.Monospace,
                color = MatrixGreen,
                fontSize = 14.sp
            )
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MatrixBlack)
                    .border(1.dp, MatrixSurfaceBorder, RoundedCornerShape(4.dp))
                    .padding(8.dp)
            ) {
                if (logs.isEmpty()) {
                    Text(
                        text = "No log output recorded yet.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MatrixTextSecondary
                    )
                } else {
                    LazyColumn {
                        items(logs) { logLine ->
                            Text(
                                text = logLine,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = if (logLine.contains("[STDERR]") || logLine.contains("ERROR")) MatrixRed else MatrixTextPrimary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen, contentColor = MatrixBlack),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp), tint = MatrixBlack)
                Spacer(modifier = Modifier.width(6.dp))
                Text("DISMISS LOGS", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MatrixBlack)
            }
        }
    )
}
