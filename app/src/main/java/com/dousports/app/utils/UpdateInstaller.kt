package com.dousports.app.utils

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateInstaller @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs by lazy {
        context.getSharedPreferences("update_installer", Context.MODE_PRIVATE)
    }

    fun download(downloadUrl: String, version: String): Long {
        val fileName = "DouSports-$version.apk"
        val dest = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (dest.exists()) dest.delete()

        val request = DownloadManager.Request(Uri.parse(downloadUrl))
            .setTitle("DouSports $version")
            .setDescription("Téléchargement de la mise à jour...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val id = dm.enqueue(request)
        prefs.edit().putLong("pending_id", id).putString("pending_version", version).apply()
        return id
    }

    /** Returns progress 0f–1f, -1f on error, or null if unknown / not yet started. */
    fun getDownloadProgress(downloadId: Long): Float? {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val cursor = dm.query(DownloadManager.Query().setFilterById(downloadId))
        if (!cursor.moveToFirst()) { cursor.close(); return null }
        return try {
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            when (status) {
                DownloadManager.STATUS_FAILED -> -1f
                DownloadManager.STATUS_SUCCESSFUL -> 1f
                else -> {
                    val bytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    if (total > 0) bytes.toFloat() / total.toFloat() else null
                }
            }
        } finally {
            cursor.close()
        }
    }

    fun openUninstall() {
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun getPendingId(): Long = prefs.getLong("pending_id", -1L)

    fun canInstallPackages(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            context.packageManager.canRequestPackageInstalls()
        else true

    fun openInstallPermissionSettings() {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun installOnCompletion(downloadId: Long) {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val cursor = dm.query(DownloadManager.Query().setFilterById(downloadId))
        if (cursor.moveToFirst()) {
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                if (!canInstallPackages()) {
                    // Store pending state so the user can retry after granting permission
                    prefs.edit().putLong("pending_install_id", downloadId).apply()
                    openInstallPermissionSettings()
                    cursor.close()
                    return
                }
                val version = prefs.getString("pending_version", null)
                val fileName = "DouSports-$version.apk"
                val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
                if (!file.exists()) { cursor.close(); return }
                val apkUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file
                )
                val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
                prefs.edit().remove("pending_id").remove("pending_version")
                    .remove("pending_install_id").apply()
            }
        }
        cursor.close()
    }

    fun retryPendingInstall() {
        val pendingId = prefs.getLong("pending_install_id", -1L)
        if (pendingId != -1L && canInstallPackages()) {
            installOnCompletion(pendingId)
        }
    }
}
