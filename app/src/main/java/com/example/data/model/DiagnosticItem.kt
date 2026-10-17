package com.example.data.model

enum class DiagnosticStatus {
    PASS,
    WARN,
    FAIL,
    RUNNING
}

data class DiagnosticItem(
    val id: String,
    val name: String,
    val status: DiagnosticStatus,
    val message: String,
    val canAutoFix: Boolean = false
)
