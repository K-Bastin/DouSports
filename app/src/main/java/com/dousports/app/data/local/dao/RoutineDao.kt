package com.dousports.app.data.local.dao

import androidx.room.*
import com.dousports.app.data.local.entity.RoutineEntity
import com.dousports.app.data.local.entity.RoutineExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {

    @Query("SELECT * FROM routines ORDER BY createdAt DESC")
    fun getAllRoutines(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun getRoutineById(id: Long): RoutineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: RoutineEntity): Long

    @Update
    suspend fun updateRoutine(routine: RoutineEntity)

    @Delete
    suspend fun deleteRoutine(routine: RoutineEntity)

    @Query("SELECT * FROM routine_exercises WHERE routineId = :routineId ORDER BY orderIndex ASC")
    fun getExercisesForRoutine(routineId: Long): Flow<List<RoutineExerciseEntity>>

    @Query("SELECT * FROM routine_exercises WHERE routineId = :routineId ORDER BY orderIndex ASC")
    suspend fun getExercisesForRoutineSync(routineId: Long): List<RoutineExerciseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineExercise(re: RoutineExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineExercises(items: List<RoutineExerciseEntity>)

    @Update
    suspend fun updateRoutineExercise(re: RoutineExerciseEntity)

    @Delete
    suspend fun deleteRoutineExercise(re: RoutineExerciseEntity)

    @Query("DELETE FROM routine_exercises WHERE routineId = :routineId")
    suspend fun clearRoutineExercises(routineId: Long)

    @Query("UPDATE routine_exercises SET restSeconds = :restSeconds WHERE id = :id")
    suspend fun updateRestSeconds(id: Long, restSeconds: Int)
}
