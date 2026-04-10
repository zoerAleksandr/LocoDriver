package com.z_company.domain.use_cases

import com.z_company.core.ResultState
import com.z_company.core.ResultState.Companion.flowRequest
import com.z_company.domain.entities.ReleaseDay
import com.z_company.domain.entities.ReleasePeriod
import com.z_company.domain.repositories.ReleaseDayRepository
import com.z_company.domain.util.generateId
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus

class ReleaseDayUseCase(private val repository: ReleaseDayRepository) {

    /** Поток всех дней отвлечений */
    fun getAllFlow(): Flow<List<ReleaseDay>> = repository.getAllFlow()

    /** Поток дней отвлечений за конкретный месяц */
    fun getByYearMonthFlow(year: Int, month: Int): Flow<List<ReleaseDay>> =
        repository.getByYearMonthFlow(year, month)

    /** Все дни (для синхронизации) */
    fun getAll(): List<ReleaseDay> = repository.getAll()

    /**
     * Сохранить период отвлечения.
     * ReleasePeriod → список ReleaseDay (один ReleaseDay на каждый день периода).
     */
    fun savePeriod(period: ReleasePeriod): Flow<ResultState<Unit>> {
        val days = period.days.map { date ->
            ReleaseDay(
                id = generateId(),
                year = date.year,
                month = date.monthNumber - 1, // 0-based
                dayOfMonth = date.dayOfMonth,
                releaseType = period.type ?: return@map null
            )
        }.filterNotNull()
        return repository.saveAll(days)
    }

    /** Удалить все дни отвлечений в период (по списку дат) */
    fun deletePeriod(period: ReleasePeriod): Flow<ResultState<Unit>> = flowRequest {
        // Определяем затронутые месяцы и даты для удаления
        val affectedMonths = period.days.map { it.year to (it.monthNumber - 1) }.toSet()
        val periodDates = period.days.toSet()
        val allDays = repository.getAll()

        // Дни затронутых месяцев, которые нужно сохранить (не входят в удаляемый период)
        val daysToKeep = allDays.filter { day ->
            (day.year to day.month) in affectedMonths && day.toLocalDate() !in periodDates
        }

        // 1. Синхронно удаляем все дни в затронутых месяцах
        affectedMonths.forEach { (year, month) ->
            repository.deleteByYearMonthSync(year, month)
        }

        // 2. Возвращаем на место дни, которые не входили в удаляемый период
        if (daysToKeep.isNotEmpty()) {
            repository.saveAll(daysToKeep).collect {}
        }
    }

    /** Добавить/обновить список дней без удаления существующих */
    fun saveAll(days: List<ReleaseDay>): Flow<ResultState<Unit>> =
        repository.saveAll(days)

    /** Загрузить с сервера и полностью заменить локальные данные */
    fun replaceAllFromRemote(days: List<ReleaseDay>): Flow<ResultState<Unit>> =
        repository.replaceAll(days)

    /**
     * Конвертировать список ReleaseDay за месяц в список ReleasePeriod.
     * Объединяет последовательные дни одного типа в один период.
     */
    fun toReleasePeriods(days: List<ReleaseDay>): List<ReleasePeriod> {
        if (days.isEmpty()) return emptyList()
        val sorted = days.sortedWith(compareBy({ it.year }, { it.month }, { it.dayOfMonth }))
        val periods = mutableListOf<ReleasePeriod>()
        var currentDays = mutableListOf<LocalDate>()
        var currentType = sorted.first().releaseType

        for (day in sorted) {
            val dayDate = day.toLocalDate()
            // Период продолжается только если: тот же тип И дата идёт сразу после предыдущей
            val isConsecutive = currentDays.isEmpty() ||
                    dayDate == currentDays.last().plus(1, DateTimeUnit.DAY)
            if (day.releaseType == currentType && isConsecutive) {
                currentDays.add(dayDate)
            } else {
                if (currentDays.isNotEmpty()) {
                    periods.add(ReleasePeriod(days = currentDays.toList(), type = currentType))
                }
                currentDays = mutableListOf(dayDate)
                currentType = day.releaseType
            }
        }
        if (currentDays.isNotEmpty()) {
            periods.add(ReleasePeriod(days = currentDays.toList(), type = currentType))
        }
        return periods
    }
}
