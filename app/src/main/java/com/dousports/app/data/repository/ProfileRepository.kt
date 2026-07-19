package com.dousports.app.data.repository

import com.dousports.app.data.local.dao.ProfileDao
import com.dousports.app.data.local.entity.BodyMeasurementEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val profileDao: ProfileDao
) {
    fun getAllMeasurements(): Flow<List<BodyMeasurementEntity>> = profileDao.getAllMeasurements()

    suspend fun getLatestMeasurement(): BodyMeasurementEntity? = profileDao.getLatestMeasurement()

    suspend fun insertMeasurement(measurement: BodyMeasurementEntity) =
        profileDao.insertMeasurement(measurement)

    suspend fun deleteMeasurement(measurement: BodyMeasurementEntity) =
        profileDao.deleteMeasurement(measurement)
}
