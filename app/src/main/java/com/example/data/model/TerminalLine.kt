package com.example.data.model

enum class TerminalLineType {
    PROMPT,
    STDOUT,
    STDERR,
    SYSTEM,
    SUCCESS
}

data class TerminalLine(
    val id: Long = System.nanoTime(),
    val text: String,
    val type: TerminalLineType = TerminalLineType.STDOUT
)
