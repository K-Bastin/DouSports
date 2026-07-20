package com.dousports.app.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

class UpdateDownloadReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface InstallerEntryPoint {
        fun installer(): UpdateInstaller
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
        val completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        if (completedId == -1L) return

        val installer = EntryPointAccessors.fromApplication(
            context.applicationContext,
            InstallerEntryPoint::class.java
        ).installer()

        if (completedId == installer.getPendingId()) {
            installer.installOnCompletion(completedId)
        }
    }
}
