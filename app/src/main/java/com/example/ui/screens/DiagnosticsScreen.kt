package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.data.model.DiagnosticItem
import com.example.data.model.DiagnosticStatus
import com.example.ui.theme.MatrixBlack
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
fun DiagnosticsScreen(
    viewModel: MatrixServerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val items by viewModel.diagnosticItems.collectAsState()

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
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = "SYSTEM DIAGNOSTICS",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MatrixGreen,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Self-Healing & Platform Health Verification",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 11.sp,
                    color = MatrixTextSecondary
                )
            }

            Button(
                onClick = { viewModel.runDiagnostics() },
                colors = ButtonDefaults.buttonColors(containerColor = MatrixSurfaceVariant, contentColor = MatrixGreen),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(38.dp)
                    .border(1.dp, MatrixGreen.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "SCAN", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(items, key = { it.id }) { diag ->
                DiagnosticRow(
                    diag = diag,
                    onAutoFix = { viewModel.autoFixDiagnostic(diag.id) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Export Diagnostic Report button
        Button(
            onClick = {
                val report = buildString {
                    appendLine("=== MATRIX SERVER DIAGNOSTIC REPORT ===")
                    appendLine("Timestamp: ${java.util.Date()}")
                    items.forEach { item ->
                        appendLine("[${item.status}] ${item.name}: ${item.message}")
                    }
                    appendLine("========================================")
                }
                clipboardManager.setText(AnnotatedString(report))
                Toast.makeText(context, "Diagnostic report copied to clipboard", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = MatrixDarkSurface, contentColor = MatrixGreen),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .border(1.dp, MatrixSurfaceBorder, RoundedCornerShape(8.dp))
        ) {
            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "EXPORT SANITIZED REPORT", fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DiagnosticRow(
    diag: DiagnosticItem,
    onAutoFix: () -> Unit
) {
    val statusColor = when (diag.status) {
        DiagnosticStatus.PASS -> MatrixGreen
        DiagnosticStatus.WARN -> MatrixYellow
        DiagnosticStatus.FAIL -> MatrixRed
        DiagnosticStatus.RUNNING -> MatrixTextSecondary
    }

    val statusIcon = when (diag.status) {
        DiagnosticStatus.PASS -> Icons.Default.CheckCircle
        DiagnosticStatus.WARN -> Icons.Default.Warning
        DiagnosticStatus.FAIL -> Icons.Default.Error
        DiagnosticStatus.RUNNING -> Icons.Default.Refresh
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (diag.status == DiagnosticStatus.PASS) MatrixSurfaceBorder else statusColor.copy(alpha = 0.5f),
                RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = MatrixDarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rounded Icon Squircle
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(statusColor.copy(alpha = 0.12f))
                    .border(1.dp, statusColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = diag.name,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MatrixTextPrimary,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = diag.message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 11.sp,
                    color = MatrixTextSecondary,
                    lineHeight = 15.sp
                )
            }

            if (diag.canAutoFix) {
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onAutoFix,
                    colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen.copy(alpha = 0.16f), contentColor = MatrixGreen),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .height(30.dp)
                        .border(1.dp, MatrixGreen.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                ) {
                    Icon(imageVector = Icons.Default.Build, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "FIX", fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
