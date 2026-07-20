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

private val Context.goalDataStore: DataStore<Preferences> by preferencesDataStore(name = "goal_prefs")

@Singleton
class WeeklyGoalPreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val WEEKLY_GOAL_KEY = intPreferencesKey("weekly_goal")

    val weeklyGoal: Flow<Int> = context.goalDataStore.data.map { prefs ->
        prefs[WEEKLY_GOAL_KEY] ?: 3
    }

    suspend fun setWeeklyGoal(goal: Int) {
        context.goalDataStore.edit { prefs ->
            prefs[WEEKLY_GOAL_KEY] = goal.coerceIn(1, 7)
        }
    }
}
