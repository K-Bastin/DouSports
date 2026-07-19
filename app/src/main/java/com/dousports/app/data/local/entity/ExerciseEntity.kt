package com.dousports.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val bodyPart: String,
    val equipment: String,
    val target: String,
    val muscleGroup: String,
    val secondaryMuscles: String,
    val instructionSteps: String,
    val imagePath: String,
    val gifPath: String,
    val mediaId: String
)
