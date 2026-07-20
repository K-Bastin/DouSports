package com.dousports.app.data.local.dao

import androidx.room.*
import com.dousports.app.data.local.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAllExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseById(id: String): ExerciseEntity?

    @Query("""
        SELECT * FROM exercises
        WHERE name LIKE '%' || :query || '%'
           OR bodyPart LIKE '%' || :query || '%'
           OR equipment LIKE '%' || :query || '%'
           OR muscleGroup LIKE '%' || :query || '%'
        ORDER BY name ASC
    """)
    fun searchExercises(query: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE bodyPart = :bodyPart ORDER BY name ASC")
    fun getExercisesByBodyPart(bodyPart: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE equipment = :equipment ORDER BY name ASC")
    fun getExercisesByEquipment(equipment: String): Flow<List<ExerciseEntity>>

    @Query("SELECT DISTINCT bodyPart FROM exercises ORDER BY bodyPart ASC")
    suspend fun getAllBodyParts(): List<String>

    @Query("SELECT DISTINCT equipment FROM exercises ORDER BY equipment ASC")
    suspend fun getAllEquipment(): List<String>

    @Query("SELECT DISTINCT category FROM exercises ORDER BY category ASC")
    suspend fun getAllCategories(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<ExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity)

    @Update
    suspend fun updateExercise(exercise: ExerciseEntity)

    @Delete
    suspend fun deleteExercise(exercise: ExerciseEntity)

    @Query("SELECT * FROM exercises WHERE id = :id")
    fun getExerciseByIdFlow(id: String): Flow<ExerciseEntity?>

    @Query("SELECT DISTINCT target FROM exercises WHERE target != '' ORDER BY target ASC")
    suspend fun getAllTargets(): List<String>

    @Query("SELECT DISTINCT muscleGroup FROM exercises WHERE muscleGroup != '' ORDER BY muscleGroup ASC")
    suspend fun getAllMuscleGroups(): List<String>

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int

    @Query("SELECT * FROM exercises WHERE id IN (:ids)")
    suspend fun getExercisesByIds(ids: List<String>): List<ExerciseEntity>
}
