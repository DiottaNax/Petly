package com.unibo.rootly.data.repositories

import androidx.annotation.WorkerThread
import com.unibo.rootly.data.database.Water
import com.unibo.rootly.data.database.daos.WaterDao
import java.time.LocalDate
import javax.inject.Inject

class WaterRepository @Inject constructor(
    private val waterDao: WaterDao
) {
    @WorkerThread
    suspend fun insert(water: Water) = waterDao.insertWater(water)

    @WorkerThread
    fun getSoon(userId: Int) = waterDao.getSoonWater(userId)

    @WorkerThread
    fun getToday(userId: Int) = waterDao.getTodayWater(userId)

    @WorkerThread
    fun getFavoritesSoon(userId: Int) = waterDao.getSoonFavoriteWater(userId)

    @WorkerThread
    fun getFavoritesToday(userId: Int) = waterDao.getTodayFavoriteWater(userId)
    @WorkerThread
    suspend fun remove(plantId: Int, date: LocalDate) {
        waterDao.removeWater(plantId,date)
    }

    @WorkerThread
    fun getLastWaterDate(plantId: Int) = waterDao.getLastWateredDate(plantId)

    @WorkerThread
    suspend fun getTimesWatered(userId:Int ) = waterDao.getTimesWatered(userId)
}