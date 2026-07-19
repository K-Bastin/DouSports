package com.dousports.app.data.repository

import com.dousports.app.data.local.dao.ExerciseDao
import com.dousports.app.data.local.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseRepository @Inject constructor(
    private val exerciseDao: ExerciseDao
) {
    fun getAllExercises(): Flow<List<ExerciseEntity>> = exerciseDao.getAllExercises()

    fun searchExercises(query: String): Flow<List<ExerciseEntity>> =
        if (query.isBlank()) exerciseDao.getAllExercises()
        else exerciseDao.searchExercises(query)

    fun getExercisesByBodyPart(bodyPart: String): Flow<List<ExerciseEntity>> =
        exerciseDao.getExercisesByBodyPart(bodyPart)

    suspend fun getExerciseById(id: String): ExerciseEntity? = exerciseDao.getExerciseById(id)

    suspend fun getAllBodyParts(): List<String> = exerciseDao.getAllBodyParts()

    suspend fun getAllEquipment(): List<String> = exerciseDao.getAllEquipment()

    suspend fun count(): Int = exerciseDao.count()

    suspend fun insertAll(exercises: List<ExerciseEntity>) = exerciseDao.insertAll(exercises)
}
