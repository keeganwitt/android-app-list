package com.github.keeganwitt.applist.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM apps")
    fun getAllAppsFlow(): Flow<List<AppCacheEntity>>

    @Query("SELECT * FROM apps")
    suspend fun getAllApps(): List<AppCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<AppCacheEntity>)

    @Query("DELETE FROM apps WHERE packageName IN (:packageNames)")
    suspend fun deleteApps(packageNames: List<String>)

    @Query("DELETE FROM apps")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM apps")
    suspend fun getCount(): Int
}
