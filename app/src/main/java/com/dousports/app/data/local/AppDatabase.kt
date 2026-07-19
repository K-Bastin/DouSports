package com.dousports.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dousports.app.data.local.dao.ExerciseDao
import com.dousports.app.data.local.dao.RoutineDao
import com.dousports.app.data.local.dao.WorkoutDao
import com.dousports.app.data.local.entity.*

@Database(
    entities = [
        ExerciseEntity::class,
        RoutineEntity::class,
        RoutineExerciseEntity::class,
        WorkoutSessionEntity::class,
        WorkoutSetEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun routineDao(): RoutineDao
    abstract fun workoutDao(): WorkoutDao

    companion object {
        const val DATABASE_NAME = "dousports.db"
    }
}
