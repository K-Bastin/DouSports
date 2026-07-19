package com.dousports.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "body_measurements")
data class BodyMeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val heightCm: Float,
    val weightKg: Float,
    val recordedAt: Long = System.currentTimeMillis()
)
