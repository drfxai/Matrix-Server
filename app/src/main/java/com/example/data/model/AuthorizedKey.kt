package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "authorized_keys")
data class AuthorizedKey(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val label: String,
    val publicKey: String,
    val fingerprint: String,
    val addedAt: Long = System.currentTimeMillis()
)
