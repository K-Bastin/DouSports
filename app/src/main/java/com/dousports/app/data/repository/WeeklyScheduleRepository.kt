package com.dousports.app.data.repository

import com.dousports.app.data.local.dao.WeeklyScheduleDao
import com.dousports.app.data.local.entity.WeeklyScheduleEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeeklyScheduleRepository @Inject constructor(
    private val dao: WeeklyScheduleDao
) {
    fun getSchedule(): Flow<List<WeeklyScheduleEntity>> = dao.getSchedule()

    suspend fun setDay(dayOfWeek: Int, routineId: Long, routineName: String) {
        dao.setDay(WeeklyScheduleEntity(dayOfWeek = dayOfWeek, routineId = routineId, routineName = routineName))
    }

    suspend fun clearDay(dayOfWeek: Int) = dao.clearDay(dayOfWeek)

    suspend fun getScheduleForDay(dayOfWeek: Int): WeeklyScheduleEntity? = dao.getScheduleForDay(dayOfWeek)
}
