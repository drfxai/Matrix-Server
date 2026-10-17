package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.MatrixBlue
import com.example.ui.theme.MatrixGreen
import com.example.ui.theme.MatrixRed
import com.example.ui.theme.MatrixYellow

enum class ServerState(val displayName: String) {
    OFF("STOPPED"),
    STARTING("STARTING"),
    ON("ONLINE"),
    DEGRADED("ISSUE"),
    STOPPING("STOPPING");

    val isRunning: Boolean
        get() = this == ON || this == DEGRADED

    val isIssue: Boolean
        get() = this == DEGRADED

    val isTransitioning: Boolean
        get() = this == STARTING || this == STOPPING

    val color: Color
        get() = when (this) {
            ON -> MatrixGreen
            STARTING -> MatrixBlue
            DEGRADED -> MatrixYellow
            STOPPING -> MatrixYellow
            OFF -> MatrixRed
        }
}
