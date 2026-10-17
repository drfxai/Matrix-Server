package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectItem(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: String, // "Python", "Node.js", "Shell", "Custom"
    val command: String,
    val workingDir: String,
    val port: Int? = null,
    val autoRestart: Boolean = true,
    val status: String = "STOPPED", // "STOPPED", "RUNNING", "FAILED", "STARTING"
    val pid: Int? = null,
    val restartCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastStartedAt: Long? = null
)
