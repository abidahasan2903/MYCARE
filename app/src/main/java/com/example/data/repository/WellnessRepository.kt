package com.example.data.repository

import com.example.core.database.dao.WellnessDao
import com.example.core.database.entity.SleepRecordEntity
import com.example.core.database.entity.WaterIntakeEntity
import kotlinx.coroutines.flow.Flow

class WellnessRepository(private val wellnessDao: WellnessDao) {
    val waterIntakes: Flow<List<WaterIntakeEntity>> = wellnessDao.observeWaterIntakes()
    val sleepRecords: Flow<List<SleepRecordEntity>> = wellnessDao.observeSleepRecords()

    suspend fun addWater(amountMl: Int, note: String = ""): Long {
        require(amountMl in 1..5000) { "Water amount must be between 1 and 5000 ml." }
        return wellnessDao.insertWaterIntake(WaterIntakeEntity(amountMl = amountMl, note = note.trim()))
    }

    suspend fun deleteWater(entry: WaterIntakeEntity) = wellnessDao.deleteWaterIntake(entry)

    suspend fun addSleep(bedtimeMillis: Long, wakeTimeMillis: Long, notes: String): Long {
        require(wakeTimeMillis > bedtimeMillis) { "Wake time must be after bedtime." }
        val duration = wakeTimeMillis - bedtimeMillis
        require(duration in 30 * 60_000L..24 * 60 * 60_000L) { "Sleep duration must be between 30 minutes and 24 hours." }
        return wellnessDao.insertSleepRecord(
            SleepRecordEntity(
                bedtimeMillis = bedtimeMillis,
                wakeTimeMillis = wakeTimeMillis,
                notes = notes.trim()
            )
        )
    }

    suspend fun deleteSleep(record: SleepRecordEntity) = wellnessDao.deleteSleepRecord(record)
}
