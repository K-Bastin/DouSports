package com.dousports.app.utils

import com.dousports.app.data.local.entity.ExerciseEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

const val MEDIA_BASE_URL = "https://raw.githubusercontent.com/k-bastin/dousports/main/"

fun ExerciseEntity.imageUrl() = "$MEDIA_BASE_URL$imagePath"
fun ExerciseEntity.gifUrl() = "$MEDIA_BASE_URL$gifPath"

fun ExerciseEntity.stepsAsList(): List<String> {
    val type = object : TypeToken<List<String>>() {}.type
    return try { Gson().fromJson(instructionSteps, type) } catch (e: Exception) { emptyList() }
}

fun ExerciseEntity.secondaryMusclesList(): List<String> {
    val type = object : TypeToken<List<String>>() {}.type
    return try { Gson().fromJson(secondaryMuscles, type) } catch (e: Exception) { emptyList() }
}

fun Long.toFormattedDate(): String {
    val sdf = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.toFormattedTime(): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.toDurationString(): String {
    val hours = TimeUnit.SECONDS.toHours(this)
    val minutes = TimeUnit.SECONDS.toMinutes(this) % 60
    val seconds = this % 60
    return if (hours > 0) {
        "%dh %02dm %02ds".format(hours, minutes, seconds)
    } else {
        "%dm %02ds".format(minutes, seconds)
    }
}

fun Float.formatWeight(): String =
    if (this == this.toLong().toFloat()) "${this.toLong()} kg" else "%.1f kg".format(this)

fun startOfWeekMillis(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
    return cal.timeInMillis
}

fun startOfMonthMillis(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    cal.set(Calendar.DAY_OF_MONTH, 1)
    return cal.timeInMillis
}
