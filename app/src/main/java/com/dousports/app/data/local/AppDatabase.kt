package com.dousports.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dousports.app.data.local.dao.ExerciseDao
import com.dousports.app.data.local.dao.ProfileDao
import com.dousports.app.data.local.dao.RoutineDao
import com.dousports.app.data.local.dao.WorkoutDao
import com.dousports.app.data.local.entity.*

@Database(
    entities = [
        ExerciseEntity::class,
        RoutineEntity::class,
        RoutineExerciseEntity::class,
        WorkoutSessionEntity::class,
        WorkoutSetEntity::class,
        BodyMeasurementEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun routineDao(): RoutineDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun profileDao(): ProfileDao

    companion object {
        const val DATABASE_NAME = "dousports.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `body_measurements` " +
                    "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`heightCm` REAL NOT NULL, " +
                    "`weightKg` REAL NOT NULL, " +
                    "`recordedAt` INTEGER NOT NULL)"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE exercises ADD COLUMN isCustom INTEGER NOT NULL DEFAULT 0"
                )
            }
        }
    }
}
