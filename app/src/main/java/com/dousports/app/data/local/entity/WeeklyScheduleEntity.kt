package com.dousports.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weekly_schedule")
data class WeeklyScheduleEntity(
    @PrimaryKey val dayOfWeek: Int, // 1 = Monday, 7 = Sunday
    val routineId: Long,
    val routineName: String
)
