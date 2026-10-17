package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.AuthorizedKey
import kotlinx.coroutines.flow.Flow

@Dao
interface AuthorizedKeyDao {
    @Query("SELECT * FROM authorized_keys ORDER BY addedAt DESC")
    fun getAllKeysFlow(): Flow<List<AuthorizedKey>>

    @Query("SELECT * FROM authorized_keys ORDER BY addedAt DESC")
    suspend fun getAllKeys(): List<AuthorizedKey>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(key: AuthorizedKey)

    @Delete
    suspend fun delete(key: AuthorizedKey)

    @Query("DELETE FROM authorized_keys WHERE id = :id")
    suspend fun deleteById(id: Long)
}
