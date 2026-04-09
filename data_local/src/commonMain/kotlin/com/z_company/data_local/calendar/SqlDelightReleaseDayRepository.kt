package com.z_company.data_local.calendar

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.z_company.core.ResultState
import com.z_company.core.ResultState.Companion.flowRequest
import com.z_company.data_local.setting.db.SettingsDatabase
import com.z_company.domain.entities.ReleaseDay
import com.z_company.domain.entities.ReleaseType
import com.z_company.domain.repositories.ReleaseDayRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SqlDelightReleaseDayRepository : ReleaseDayRepository, KoinComponent {
    private val db: SettingsDatabase by inject()

    override fun getAllFlow(): Flow<List<ReleaseDay>> =
        db.releaseDayQueries.getAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    override fun getByYearMonthFlow(year: Int, month: Int): Flow<List<ReleaseDay>> =
        db.releaseDayQueries.getByYearMonth(year.toLong(), month.toLong())
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    override fun getAll(): List<ReleaseDay> =
        db.releaseDayQueries.getAll().executeAsList().map { it.toDomain() }

    override fun saveAll(days: List<ReleaseDay>): Flow<ResultState<Unit>> = flowRequest {
        days.forEach { day ->
            db.releaseDayQueries.insertOrReplace(
                id = day.id,
                year = day.year.toLong(),
                month = day.month.toLong(),
                dayOfMonth = day.dayOfMonth.toLong(),
                releaseType = day.releaseType.text
            )
        }
    }

    override fun delete(id: String): Flow<ResultState<Unit>> = flowRequest {
        db.releaseDayQueries.deleteById(id)
    }

    override fun deleteByYearMonthSync(year: Int, month: Int) {
        db.releaseDayQueries.deleteByYearMonth(year.toLong(), month.toLong())
    }

    override fun deleteByYearMonth(year: Int, month: Int): Flow<ResultState<Unit>> = flowRequest {
        db.releaseDayQueries.deleteByYearMonth(year.toLong(), month.toLong())
    }

    override fun replaceAll(days: List<ReleaseDay>): Flow<ResultState<Unit>> = flowRequest {
        db.releaseDayQueries.deleteAll()
        days.forEach { day ->
            db.releaseDayQueries.insertOrReplace(
                id = day.id,
                year = day.year.toLong(),
                month = day.month.toLong(),
                dayOfMonth = day.dayOfMonth.toLong(),
                releaseType = day.releaseType.text
            )
        }
    }

    private fun com.zcompany.datalocal.setting.db.ReleaseDay.toDomain(): ReleaseDay =
        ReleaseDay(
            id = id,
            year = year.toInt(),
            month = month.toInt(),
            dayOfMonth = dayOfMonth.toInt(),
            releaseType = releaseTypeFromText(releaseType)
        )

    private fun releaseTypeFromText(text: String): ReleaseType = when (text) {
        "Отпуск" -> ReleaseType.Vacation
        "Больничный" -> ReleaseType.SickLeave
        "Курсы" -> ReleaseType.Courses
        "Донорские" -> ReleaseType.Donor
        "По уходу за ребенком-инвалидом" -> ReleaseType.ChildCare
        else -> ReleaseType.Other
    }
}
