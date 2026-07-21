package com.dousports.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.workoutDataStore: DataStore<Preferences> by preferencesDataStore(name = "workout_prefs")

@Singleton
class WorkoutPreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val REST_DURATION_KEY = intPreferencesKey("rest_duration_seconds")

    val defaultRestDuration: Flow<Int> = context.workoutDataStore.data.map { prefs ->
        prefs[REST_DURATION_KEY] ?: 90
    }

    suspend fun setDefaultRestDuration(seconds: Int) {
        context.workoutDataStore.edit { prefs ->
            prefs[REST_DURATION_KEY] = seconds.coerceIn(15, 600)
        }
    }
}
