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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MatrixBlack
import com.example.ui.theme.MatrixDarkSurface
import com.example.ui.theme.MatrixGreen
import com.example.ui.theme.MatrixRed
import com.example.ui.theme.MatrixSurfaceBorder
import com.example.ui.theme.MatrixSurfaceVariant
import com.example.ui.theme.MatrixTextPrimary
import com.example.ui.theme.MatrixTextSecondary
import com.example.ui.viewmodel.MatrixServerViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FileManagerScreen(
    viewModel: MatrixServerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentDir by viewModel.currentBrowseDir.collectAsState()
    val files by viewModel.dirFiles.collectAsState()

    var showCreateFileDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var fileToEdit by remember { mutableStateOf<File?>(null) }
    var fileToDelete by remember { mutableStateOf<File?>(null) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MatrixBlack)
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
                    text = "STORAGE & FILES",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MatrixGreen
                )
                Text(
                    text = "Linux Project & Home Filesystem",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MatrixTextSecondary
                )
            }

            Row {
                IconButton(onClick = { showCreateFileDialog = true }) {
                    Icon(imageVector = Icons.Default.NoteAdd, contentDescription = "New File", tint = MatrixGreen)
                }
                IconButton(onClick = { showCreateFolderDialog = true }) {
                    Icon(imageVector = Icons.Default.CreateNewFolder, contentDescription = "New Folder", tint = MatrixGreen)
                }
                IconButton(onClick = { viewModel.refreshCurrentDir() }) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = MatrixTextSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Path navigation bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(MatrixDarkSurface)
                .border(1.dp, MatrixSurfaceBorder, RoundedCornerShape(6.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateUp() },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Up",
                    tint = MatrixGreen,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = currentDir.absolutePath.replace(viewModel.linuxEnv.rootDir.absolutePath, "~"),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = MatrixGreen,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (files.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Directory is empty.",
                    fontFamily = FontFamily.Monospace,
                    color = MatrixTextSecondary,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(files, key = { it.absolutePath }) { file ->
                    FileRowItem(
                        file = file,
                        dateFormat = dateFormat,
                        onOpen = {
                            if (file.isDirectory) {
                                viewModel.navigateToDirectory(file)
                            } else {
                                fileToEdit = file
                            }
                        },
                        onDelete = { fileToDelete = file }
                    )
                }
            }
        }
    }

    // New File Dialog
    if (showCreateFileDialog) {
        var fileName by remember { mutableStateOf("") }
        var fileContent by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateFileDialog = false },
            containerColor = MatrixDarkSurface,
            title = { Text("CREATE NEW FILE", fontFamily = FontFamily.Monospace, color = MatrixGreen, fontSize = 15.sp) },
            text = {
                Column {
                    OutlinedTextField(
                        value = fileName,
                        onValueChange = { fileName = it },
                        label = { Text("File Name (e.g. script.py)") },
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
                        value = fileContent,
                        onValueChange = { fileContent = it },
                        label = { Text("Initial Content") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MatrixTextPrimary,
                            unfocusedTextColor = MatrixTextPrimary,
                            focusedBorderColor = MatrixGreen,
                            unfocusedBorderColor = MatrixSurfaceBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (fileName.isNotBlank()) {
                            viewModel.createNewFile(fileName, fileContent)
                            showCreateFileDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen, contentColor = MatrixBlack)
                ) {
                    Text("CREATE", fontFamily = FontFamily.Monospace)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFileDialog = false }) {
                    Text("CANCEL", fontFamily = FontFamily.Monospace, color = MatrixTextSecondary)
                }
            }
        )
    }

    // New Folder Dialog
    if (showCreateFolderDialog) {
        var folderName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            containerColor = MatrixDarkSurface,
            title = { Text("CREATE FOLDER", fontFamily = FontFamily.Monospace, color = MatrixGreen, fontSize = 15.sp) },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Folder Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MatrixTextPrimary,
                        unfocusedTextColor = MatrixTextPrimary,
                        focusedBorderColor = MatrixGreen,
                        unfocusedBorderColor = MatrixSurfaceBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (folderName.isNotBlank()) {
                            viewModel.createNewFolder(folderName)
                            showCreateFolderDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen, contentColor = MatrixBlack)
                ) {
                    Text("CREATE", fontFamily = FontFamily.Monospace)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("CANCEL", fontFamily = FontFamily.Monospace, color = MatrixTextSecondary)
                }
            }
        )
    }

    // Edit File Dialog
    if (fileToEdit != null) {
        val target = fileToEdit!!
        var textContent by remember(target) {
            mutableStateOf(try { target.readText() } catch (e: Exception) { "[Binary or unreadable file]" })
        }

        AlertDialog(
            onDismissRequest = { fileToEdit = null },
            containerColor = MatrixDarkSurface,
            title = { Text(target.name, fontFamily = FontFamily.Monospace, color = MatrixGreen, fontSize = 15.sp) },
            text = {
                OutlinedTextField(
                    value = textContent,
                    onValueChange = { textContent = it },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MatrixTextPrimary,
                        unfocusedTextColor = MatrixTextPrimary,
                        focusedBorderColor = MatrixGreen,
                        unfocusedBorderColor = MatrixSurfaceBorder
                    ),
                    modifier = Modifier.fillMaxWidth().height(280.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            target.writeText(textContent)
                            Toast.makeText(context, "Saved ${target.name}", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error saving: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                        fileToEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen, contentColor = MatrixBlack)
                ) {
                    Text("SAVE", fontFamily = FontFamily.Monospace)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToEdit = null }) {
                    Text("CLOSE", fontFamily = FontFamily.Monospace, color = MatrixTextSecondary)
                }
            }
        )
    }

    // Delete Confirmation
    if (fileToDelete != null) {
        val target = fileToDelete!!
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            containerColor = MatrixDarkSurface,
            title = { Text("CONFIRM DELETION", fontFamily = FontFamily.Monospace, color = MatrixRed, fontSize = 15.sp) },
            text = {
                Text(
                    text = "Are you sure you want to delete '${target.name}'? This cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MatrixTextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFile(target)
                        fileToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MatrixRed, contentColor = MatrixBlack)
                ) {
                    Text("DELETE", fontFamily = FontFamily.Monospace)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("CANCEL", fontFamily = FontFamily.Monospace, color = MatrixTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun FileRowItem(
    file: File,
    dateFormat: SimpleDateFormat,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .border(1.dp, MatrixSurfaceBorder, RoundedCornerShape(6.dp)),
        colors = CardDefaults.cardColors(containerColor = MatrixDarkSurface),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                contentDescription = null,
                tint = if (file.isDirectory) MatrixGreen else MatrixTextSecondary,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = MatrixTextPrimary
                )
                Text(
                    text = if (file.isDirectory) "Directory | ${dateFormat.format(Date(file.lastModified()))}"
                    else "${file.length() / 1024} KB | ${dateFormat.format(Date(file.lastModified()))}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = MatrixTextSecondary
                )
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MatrixTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
