package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AuthorizedKey
import com.example.data.model.ProjectItem

@Database(entities = [ProjectItem::class, AuthorizedKey::class], version = 1, exportSchema = false)
abstract class ServerDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun authorizedKeyDao(): AuthorizedKeyDao

    companion object {
        @Volatile
        private var INSTANCE: ServerDatabase? = null

        fun getInstance(context: Context): ServerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ServerDatabase::class.java,
                    "matrix_server.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
