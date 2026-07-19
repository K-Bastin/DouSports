package com.dousports.app.data.local.dao

import androidx.room.*
import com.dousports.app.data.local.entity.WorkoutSessionEntity
import com.dousports.app.data.local.entity.WorkoutSetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    @Query("SELECT * FROM workout_sessions ORDER BY startedAt DESC")
    fun getAllSessions(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions ORDER BY startedAt DESC LIMIT :limit")
    fun getRecentSessions(limit: Int = 10): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): WorkoutSessionEntity?

    @Insert
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Update
    suspend fun updateSession(session: WorkoutSessionEntity)

    @Delete
    suspend fun deleteSession(session: WorkoutSessionEntity)

    @Query("SELECT * FROM workout_sets WHERE sessionId = :sessionId ORDER BY exerciseId, setNumber ASC")
    fun getSetsForSession(sessionId: Long): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM workout_sets WHERE exerciseId = :exerciseId ORDER BY loggedAt DESC LIMIT :limit")
    suspend fun getRecentSetsForExercise(exerciseId: String, limit: Int = 20): List<WorkoutSetEntity>

    @Insert
    suspend fun insertSet(set: WorkoutSetEntity): Long

    @Update
    suspend fun updateSet(set: WorkoutSetEntity)

    @Delete
    suspend fun deleteSet(set: WorkoutSetEntity)

    @Query("SELECT COUNT(*) FROM workout_sessions WHERE finishedAt IS NOT NULL AND startedAt >= :since")
    suspend fun countSessionsSince(since: Long): Int

    @Query("SELECT SUM(weight * reps) FROM workout_sets WHERE loggedAt >= :since")
    suspend fun totalVolumeSince(since: Long): Float?

    @Query("SELECT MAX(weight) FROM workout_sets WHERE exerciseId = :exerciseId")
    suspend fun maxWeightForExercise(exerciseId: String): Float?

    @Query("SELECT * FROM workout_sets WHERE exerciseId = :exerciseId ORDER BY weight DESC LIMIT 1")
    suspend fun personalRecordForExercise(exerciseId: String): WorkoutSetEntity?

    @Query("SELECT DISTINCT exerciseId FROM workout_sets")
    suspend fun getAllTrackedExerciseIds(): List<String>

    @Query("SELECT * FROM workout_sessions WHERE startedAt >= :startMs AND startedAt < :endMs ORDER BY startedAt ASC")
    suspend fun getSessionsInRange(startMs: Long, endMs: Long): List<WorkoutSessionEntity>

    @Query("SELECT * FROM workout_sets WHERE sessionId = :sessionId ORDER BY exerciseId, setNumber ASC")
    suspend fun getSetsForSessionSync(sessionId: Long): List<WorkoutSetEntity>
}
