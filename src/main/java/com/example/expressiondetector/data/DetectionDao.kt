package com.example.expressiondetector.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DetectionDao {

    @Insert
    suspend fun insert(detection: DetectionEntity)

    @Query("SELECT * FROM detections ORDER BY timestamp DESC")
    fun getAll(): Flow<List<DetectionEntity>>

    @Query("DELETE FROM detections")
    suspend fun clearAll()
}
