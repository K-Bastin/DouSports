package com.dousports.app

import android.app.Application
import com.dousports.app.utils.ExerciseLoader
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class DouSportsApp : Application() {

    @Inject
    lateinit var exerciseLoader: ExerciseLoader

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            exerciseLoader.loadIfNeeded()
        }
    }
}
