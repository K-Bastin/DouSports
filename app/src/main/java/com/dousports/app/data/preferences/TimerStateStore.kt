package com.dousports.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.timerStateDataStore: DataStore<Preferences> by preferencesDataStore(name = "timer_state_prefs")

data class SavedTimerState(
    val sessionId: Long,
    val exerciseIndex: Int,
    val phaseOrdinal: Int,
    val phaseRemainingSeconds: Int
)

@Singleton
class TimerStateStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val KEY_SESSION_ID = longPreferencesKey("session_id")
    private val KEY_EXERCISE_INDEX = intPreferencesKey("exercise_index")
    private val KEY_PHASE_ORDINAL = intPreferencesKey("phase_ordinal")
    private val KEY_PHASE_REMAINING = intPreferencesKey("phase_remaining")

    suspend fun save(state: SavedTimerState) {
        context.timerStateDataStore.edit { prefs ->
            prefs[KEY_SESSION_ID] = state.sessionId
            prefs[KEY_EXERCISE_INDEX] = state.exerciseIndex
            prefs[KEY_PHASE_ORDINAL] = state.phaseOrdinal
            prefs[KEY_PHASE_REMAINING] = state.phaseRemainingSeconds
        }
    }

    suspend fun load(sessionId: Long): SavedTimerState? {
        val prefs = context.timerStateDataStore.data.first()
        val savedId = prefs[KEY_SESSION_ID] ?: return null
        if (savedId != sessionId) return null
        return SavedTimerState(
            sessionId = savedId,
            exerciseIndex = prefs[KEY_EXERCISE_INDEX] ?: 0,
            phaseOrdinal = prefs[KEY_PHASE_ORDINAL] ?: 0,
            phaseRemainingSeconds = prefs[KEY_PHASE_REMAINING] ?: 0
        )
    }

    suspend fun clear() {
        context.timerStateDataStore.edit { it.clear() }
    }
}
