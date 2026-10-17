package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TerminalLineType
import com.example.ui.theme.MatrixBlack
import com.example.ui.theme.MatrixBlue
import com.example.ui.theme.MatrixDarkSurface
import com.example.ui.theme.MatrixGreen
import com.example.ui.theme.MatrixRed
import com.example.ui.theme.MatrixSurfaceBorder
import com.example.ui.theme.MatrixTextPrimary
import com.example.ui.theme.MatrixTextSecondary
import com.example.ui.theme.MatrixYellow
import com.example.ui.viewmodel.MatrixServerViewModel

@Composable
fun TerminalScreen(
    viewModel: MatrixServerViewModel,
    modifier: Modifier = Modifier
) {
    val lines by viewModel.terminalLines.collectAsState()
    val isExecuting by viewModel.terminalRunner.isExecuting.collectAsState()
    var inputCommand by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom on new output
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            listState.animateScrollToItem(lines.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MatrixBlack)
            .imePadding()
    ) {
        // Terminal Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MatrixDarkSurface)
                .border(1.dp, MatrixSurfaceBorder)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "matrix@android:~$",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MatrixGreen
                )
                Text(
                    text = "Linux Shell (/system/bin/sh)",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MatrixTextSecondary
                )
            }

            IconButton(
                onClick = { viewModel.clearTerminal() },
                modifier = Modifier.size(32.dp).testTag("clear_terminal_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Clear Terminal",
                    tint = MatrixTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Quick Command Shortcuts
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MatrixDarkSurface)
                .border(1.dp, MatrixSurfaceBorder)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val quickCommands = listOf(
                "apt update" to "APT UPDATE",
                "matrix-cli status" to "STATUS",
                "neofetch" to "NEOFETCH",
                "ls -la" to "LS",
                "uname -a" to "UNAME",
                "top -n 1" to "TOP",
                "df -h" to "STORAGE",
                "ps" to "PROCS"
            )

            quickCommands.forEach { (cmd, label) ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MatrixBlack)
                        .border(1.dp, MatrixGreen.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .clickable { viewModel.executeTerminalCommand(cmd) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = ">",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MatrixGreen
                        )
                        Text(
                            text = cmd,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = MatrixTextPrimary
                        )
                    }
                }
            }
        }

        // Terminal Output LazyColumn
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MatrixBlack)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            items(lines, key = { it.id }) { line ->
                val textColor = when (line.type) {
                    TerminalLineType.PROMPT -> MatrixGreen
                    TerminalLineType.STDOUT -> MatrixTextPrimary
                    TerminalLineType.STDERR -> MatrixRed
                    TerminalLineType.SYSTEM -> MatrixYellow
                    TerminalLineType.SUCCESS -> MatrixBlue
                }

                Text(
                    text = line.text,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = textColor,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }

        // Bottom Input Prompt Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MatrixDarkSurface)
                .border(1.dp, MatrixSurfaceBorder)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$ ",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MatrixGreen
            )

            BasicTextField(
                value = inputCommand,
                onValueChange = { inputCommand = it },
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = MatrixTextPrimary
                ),
                cursorBrush = SolidColor(MatrixGreen),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (inputCommand.isNotBlank()) {
                            viewModel.executeTerminalCommand(inputCommand)
                            inputCommand = ""
                        }
                    }
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
                    .testTag("terminal_input_field")
            )

            IconButton(
                onClick = {
                    if (inputCommand.isNotBlank()) {
                        viewModel.executeTerminalCommand(inputCommand)
                        inputCommand = ""
                    }
                },
                enabled = inputCommand.isNotBlank() && !isExecuting,
                modifier = Modifier.size(36.dp).testTag("terminal_send_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Run Command",
                    tint = if (inputCommand.isNotBlank()) MatrixGreen else MatrixTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
