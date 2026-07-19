package com.dousports.app.data.local.dao

import androidx.room.*
import com.dousports.app.data.local.entity.BodyMeasurementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    @Query("SELECT * FROM body_measurements ORDER BY recordedAt DESC")
    fun getAllMeasurements(): Flow<List<BodyMeasurementEntity>>

    @Query("SELECT * FROM body_measurements ORDER BY recordedAt DESC LIMIT 1")
    suspend fun getLatestMeasurement(): BodyMeasurementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasurement(measurement: BodyMeasurementEntity): Long

    @Delete
    suspend fun deleteMeasurement(measurement: BodyMeasurementEntity)
}
