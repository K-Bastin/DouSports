package com.dousports.app.utils

import android.graphics.Bitmap
import android.util.Base64
import com.dousports.app.data.local.entity.RoutineEntity
import com.dousports.app.data.local.entity.RoutineExerciseEntity
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import javax.inject.Inject
import javax.inject.Singleton

data class RoutineShareDto(
    @SerializedName("n") val name: String,
    @SerializedName("d") val description: String,
    @SerializedName("ti") val isTimed: Boolean = false,
    @SerializedName("e") val exercises: List<ExerciseShareDto>
)

data class ExerciseShareDto(
    @SerializedName("i") val exerciseId: String,
    @SerializedName("n") val exerciseName: String,
    @SerializedName("o") val orderIndex: Int,
    @SerializedName("s") val targetSets: Int,
    @SerializedName("r") val targetReps: Int,
    @SerializedName("w") val targetWeight: Float,
    @SerializedName("t") val restSeconds: Int,
    @SerializedName("ds") val durationSeconds: Int = 45
)

@Singleton
class RoutineShareManager @Inject constructor(private val gson: Gson) {

    fun encode(routine: RoutineEntity, exercises: List<RoutineExerciseEntity>): String {
        val dto = RoutineShareDto(
            name = routine.name,
            description = routine.description,
            isTimed = routine.isTimed,
            exercises = exercises.map {
                ExerciseShareDto(
                    exerciseId = it.exerciseId,
                    exerciseName = it.exerciseName,
                    orderIndex = it.orderIndex,
                    targetSets = it.targetSets,
                    targetReps = it.targetReps,
                    targetWeight = it.targetWeight,
                    restSeconds = it.restSeconds,
                    durationSeconds = it.durationSeconds
                )
            }
        )
        val json = gson.toJson(dto)
        return Base64.encodeToString(json.toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP)
    }

    fun decode(code: String): RoutineShareDto? = runCatching {
        val json = String(Base64.decode(code.trim(), Base64.URL_SAFE), Charsets.UTF_8)
        gson.fromJson(json, RoutineShareDto::class.java)
    }.getOrNull()

    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap {
        val bits = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bmp.setPixel(x, y, if (bits[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        return bmp
    }
}
