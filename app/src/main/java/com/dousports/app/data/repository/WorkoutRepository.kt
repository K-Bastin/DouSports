package com.dousports.app.data.repository

import com.dousports.app.data.local.dao.RoutineDao
import com.dousports.app.data.local.dao.WorkoutDao
import com.dousports.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepository @Inject constructor(
    private val routineDao: RoutineDao,
    private val workoutDao: WorkoutDao
) {
    // Routines
    fun getAllRoutines(): Flow<List<RoutineEntity>> = routineDao.getAllRoutines()

    fun getExercisesForRoutine(routineId: Long): Flow<List<RoutineExerciseEntity>> =
        routineDao.getExercisesForRoutine(routineId)

    suspend fun getRoutineById(id: Long): RoutineEntity? = routineDao.getRoutineById(id)

    suspend fun getExercisesForRoutineSync(routineId: Long): List<RoutineExerciseEntity> =
        routineDao.getExercisesForRoutineSync(routineId)

    suspend fun insertRoutine(routine: RoutineEntity): Long = routineDao.insertRoutine(routine)

    suspend fun updateRoutine(routine: RoutineEntity) = routineDao.updateRoutine(routine)

    suspend fun deleteRoutine(routine: RoutineEntity) = routineDao.deleteRoutine(routine)

    suspend fun saveRoutineExercises(routineId: Long, items: List<RoutineExerciseEntity>) {
        routineDao.clearRoutineExercises(routineId)
        routineDao.insertRoutineExercises(items)
    }

    suspend fun deleteRoutineExercise(re: RoutineExerciseEntity) =
        routineDao.deleteRoutineExercise(re)

    // Sessions
    fun getAllSessions(): Flow<List<WorkoutSessionEntity>> = workoutDao.getAllSessions()

    fun getRecentSessions(limit: Int = 10): Flow<List<WorkoutSessionEntity>> =
        workoutDao.getRecentSessions(limit)

    fun getSetsForSession(sessionId: Long): Flow<List<WorkoutSetEntity>> =
        workoutDao.getSetsForSession(sessionId)

    suspend fun getSessionById(id: Long): WorkoutSessionEntity? = workoutDao.getSessionById(id)

    suspend fun insertSession(session: WorkoutSessionEntity): Long =
        workoutDao.insertSession(session)

    suspend fun updateSession(session: WorkoutSessionEntity) = workoutDao.updateSession(session)

    suspend fun deleteSession(session: WorkoutSessionEntity) = workoutDao.deleteSession(session)

    suspend fun insertSet(set: WorkoutSetEntity): Long = workoutDao.insertSet(set)

    suspend fun deleteSet(set: WorkoutSetEntity) = workoutDao.deleteSet(set)

    // Stats
    suspend fun countSessionsSince(since: Long): Int = workoutDao.countSessionsSince(since)

    suspend fun totalVolumeSince(since: Long): Float? = workoutDao.totalVolumeSince(since)

    suspend fun maxWeightForExercise(exerciseId: String): Float? =
        workoutDao.maxWeightForExercise(exerciseId)

    suspend fun getRecentSetsForExercise(exerciseId: String, limit: Int = 20): List<WorkoutSetEntity> =
        workoutDao.getRecentSetsForExercise(exerciseId, limit)

    suspend fun getAllTrackedExerciseIds(): List<String> =
        workoutDao.getAllTrackedExerciseIds()

    suspend fun getSessionsInRange(startMs: Long, endMs: Long): List<WorkoutSessionEntity> =
        workoutDao.getSessionsInRange(startMs, endMs)

    suspend fun getSetsForSessionSync(sessionId: Long): List<WorkoutSetEntity> =
        workoutDao.getSetsForSessionSync(sessionId)
}
