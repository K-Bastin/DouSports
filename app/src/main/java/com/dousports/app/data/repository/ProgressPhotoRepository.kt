package com.dousports.app.data.repository

import android.content.Context
import android.net.Uri
import com.dousports.app.data.local.dao.ProgressPhotoDao
import com.dousports.app.data.local.entity.ProgressPhotoEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressPhotoRepository @Inject constructor(
    private val dao: ProgressPhotoDao,
    @ApplicationContext private val context: Context
) {
    fun getAllPhotos(): Flow<List<ProgressPhotoEntity>> = dao.getAllPhotos()

    fun createPhotoFile(): File {
        val dir = File(context.filesDir, "photos").also { it.mkdirs() }
        return File(dir, "${UUID.randomUUID()}.jpg")
    }

    suspend fun savePhotoFromUri(uri: Uri): ProgressPhotoEntity? {
        val file = createPhotoFile()
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            val entity = ProgressPhotoEntity(filePath = file.absolutePath)
            val id = dao.insertPhoto(entity)
            entity.copy(id = id)
        } catch (e: Exception) {
            file.delete()
            null
        }
    }

    suspend fun savePhotoFromFile(file: File): ProgressPhotoEntity? {
        if (!file.exists()) return null
        val entity = ProgressPhotoEntity(filePath = file.absolutePath)
        val id = dao.insertPhoto(entity)
        return entity.copy(id = id)
    }

    suspend fun deletePhoto(photo: ProgressPhotoEntity) {
        File(photo.filePath).delete()
        dao.deletePhoto(photo)
    }
}
