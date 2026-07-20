package com.dousports.app.data.local.dao

import androidx.room.*
import com.dousports.app.data.local.entity.ProgressPhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressPhotoDao {

    @Query("SELECT * FROM progress_photos ORDER BY recordedAt DESC")
    fun getAllPhotos(): Flow<List<ProgressPhotoEntity>>

    @Insert
    suspend fun insertPhoto(photo: ProgressPhotoEntity): Long

    @Delete
    suspend fun deletePhoto(photo: ProgressPhotoEntity)
}
