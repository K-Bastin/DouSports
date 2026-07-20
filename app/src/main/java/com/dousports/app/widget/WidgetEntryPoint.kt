package com.dousports.app.widget

import com.dousports.app.data.repository.WeeklyScheduleRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun weeklyScheduleRepository(): WeeklyScheduleRepository
}
