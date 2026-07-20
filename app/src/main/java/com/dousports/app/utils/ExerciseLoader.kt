package com.dousports.app.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.dousports.app.data.local.entity.ExerciseEntity
import com.dousports.app.data.repository.ExerciseRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ExerciseRepository
) {
    suspend fun loadIfNeeded() {
        if (repository.countBuiltIn() > 0) return

        val json = context.assets.open("exercises.json").bufferedReader().readText()
        val type = object : TypeToken<List<ExerciseJson>>() {}.type
        val raw: List<ExerciseJson> = Gson().fromJson(json, type)

        val entities = raw.map { ex ->
            ExerciseEntity(
                id = ex.id,
                name = ex.name,
                category = ex.category,
                bodyPart = ex.body_part,
                equipment = ex.equipment,
                target = ex.target ?: "",
                muscleGroup = ex.muscle_group ?: "",
                secondaryMuscles = Gson().toJson(ex.secondary_muscles ?: emptyList<String>()),
                instructionSteps = Gson().toJson(ex.steps ?: emptyList<String>()),
                imagePath = ex.image,
                gifPath = ex.gif_url,
                mediaId = ex.media_id
            )
        }

        repository.insertAll(entities)
    }
}

data class ExerciseJson(
    val id: String,
    val name: String,
    val category: String,
    val body_part: String,
    val equipment: String,
    val target: String?,
    val muscle_group: String?,
    val secondary_muscles: List<String>?,
    val steps: List<String>?,
    val image: String,
    val gif_url: String,
    val media_id: String
)
