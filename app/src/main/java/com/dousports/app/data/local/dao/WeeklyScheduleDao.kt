package com.dousports.app.data.local.dao

import androidx.room.*
import com.dousports.app.data.local.entity.WeeklyScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeeklyScheduleDao {

    @Query("SELECT * FROM weekly_schedule ORDER BY dayOfWeek ASC")
    fun getSchedule(): Flow<List<WeeklyScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setDay(entry: WeeklyScheduleEntity)

    @Query("DELETE FROM weekly_schedule WHERE dayOfWeek = :day")
    suspend fun clearDay(day: Int)

    @Query("DELETE FROM weekly_schedule")
    suspend fun clearAll()

    @Query("SELECT * FROM weekly_schedule WHERE dayOfWeek = :day LIMIT 1")
    suspend fun getScheduleForDay(day: Int): WeeklyScheduleEntity?
}
