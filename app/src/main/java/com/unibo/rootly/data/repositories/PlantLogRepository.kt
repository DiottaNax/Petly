package com.unibo.rootly.data.repositories

import androidx.annotation.WorkerThread
import com.unibo.rootly.data.database.PlantLog
import com.unibo.rootly.data.database.daos.PlantLogDao
import javax.inject.Inject

class PlantLogRepository @Inject constructor(
    private val plantLogDao: PlantLogDao
) {
    @WorkerThread
    suspend fun insert(plantLog: PlantLog) = plantLogDao.insertPlantLog(plantLog)

    @WorkerThread
    suspend fun getByPlant(userId: Int, plantId: Int)
            = plantLogDao.getPlantLogsByPlant(userId, plantId)

    @WorkerThread
    suspend fun getSoon(userId: Int) = plantLogDao.getSoonLogs(userId)

    @WorkerThread
    suspend fun getToday(userId: Int) = plantLogDao.getTodayLogs(userId)
}