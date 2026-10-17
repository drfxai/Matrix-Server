package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ProjectItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun getAllProjectsFlow(): Flow<List<ProjectItem>>

    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    suspend fun getAllProjects(): List<ProjectItem>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: String): ProjectItem?

    @Query("SELECT * FROM projects WHERE status = 'RUNNING'")
    suspend fun getRunningProjects(): List<ProjectItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: ProjectItem)

    @Update
    suspend fun update(project: ProjectItem)

    @Query("UPDATE projects SET status = :status, pid = :pid, lastStartedAt = :timestamp WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, pid: Int?, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE projects SET status = 'STOPPED', pid = null")
    suspend fun resetAllToStopped()

    @Delete
    suspend fun delete(project: ProjectItem)
}
