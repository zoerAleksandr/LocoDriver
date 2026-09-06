package com.z_company.domain.use_cases

import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.repositories.SalarySettingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class SalarySettingUseCase(
    val repository: SalarySettingRepository,
    private val calendarUseCase: CalendarUseCase,
) {
    fun salarySettingFlow(): Flow<SalarySetting> {
        return repository.getSalarySettingFlow()
    }

    fun getSalarySetting(): SalarySetting = repository.getSalarySetting()
    fun getFlowSalarySetting(): Flow<ResultState<SalarySetting>> =
        repository.getSalarySettingState()

    fun saveSalarySetting(setting: SalarySetting): Flow<ResultState<Unit>> =
        repository.saveSalarySetting(setting)

    fun updateMonthOfYear(monthOfYear: MonthOfYear): Flow<ResultState<Unit>> =
        calendarUseCase.updateMonthOfYear(monthOfYear)

    /**
     * Сохранить тарифную ставку ТОЛЬКО в переданном месяце.
     * Прошлые и будущие месяцы не трогаем.
     *
     * Ищем месяц по году+месяцу, а не по id: id в UserSettings.selectMonthOfYear
     * может не совпадать с id в таблице (после синхронизации на новом устройстве),
     * а в локальной БД исторически встречаются дубликаты одного года+месяца —
     * обновляем все, чтобы UI не подхватил строку со старой ставкой.
     */
    fun updateTariffRateOnlyInOneMonthOfYear(monthOfYear: MonthOfYear): Flow<ResultState<Unit>> {
        val dispatcher = Dispatchers.Default
        return flow {
            emit(ResultState.Loading())
            applyTariffRateToMonth(monthOfYear)
            emit(ResultState.Success(Unit))
        }.catch {
            emit(ResultState.Error(ErrorEntity(it)))
        }.flowOn(dispatcher)
    }

    /**
     * Сохранить тарифную ставку в переданном месяце и во всех СЛЕДУЮЩИХ.
     * Прошлые месяцы остаются со своей ставкой — расчёт зарплаты за них
     * не должен меняться задним числом.
     *
     * В следующих месяцах [MonthOfYear.dateSetTariffRate] сбрасывается: дата
     * смены тарифа со старой ставкой относилась к прежнему тарифу и после
     * переноса новой ставки на будущее уже неверна.
     */
    fun updateTariffRateCurrentAndNextMonths(monthOfYear: MonthOfYear): Flow<ResultState<Unit>> {
        val dispatcher = Dispatchers.Default
        return flow {
            emit(ResultState.Loading())
            applyTariffRateToMonth(monthOfYear)
            calendarUseCase.loadMonthOfYearList()
                .filter { it.isAfter(monthOfYear) }
                .forEach { next ->
                    calendarUseCase.updateMonthOfYear(
                        next.copy(
                            tariffRate = monthOfYear.tariffRate,
                            dateSetTariffRate = null
                        )
                    ).collect {}
                }
            emit(ResultState.Success(Unit))
        }.catch {
            emit(ResultState.Error(ErrorEntity(it)))
        }.flowOn(dispatcher)
    }

    private suspend fun applyTariffRateToMonth(monthOfYear: MonthOfYear) {
        val storedRows = calendarUseCase.loadMonthOfYearList()
            .filter { it.year == monthOfYear.year && it.month == monthOfYear.month }
        if (storedRows.isEmpty()) {
            calendarUseCase.updateMonthOfYear(monthOfYear).collect {}
            return
        }
        storedRows.forEach { row ->
            calendarUseCase.updateMonthOfYear(
                row.copy(
                    tariffRate = monthOfYear.tariffRate,
                    dateSetTariffRate = monthOfYear.dateSetTariffRate
                )
            ).collect {}
        }
    }

    private fun MonthOfYear.isAfter(other: MonthOfYear): Boolean =
        year > other.year || (year == other.year && month > other.month)

    suspend fun getTariffRateFromCurrentMonthOfYear(monthOfYear: MonthOfYear): Double {
        val dispatcher = Dispatchers.Default
        val scope = CoroutineScope(dispatcher)

        val currentMonthOfYear =
            scope.async { calendarUseCase.loadMonthOfYearById(monthOfYear.id) }.await()
        return currentMonthOfYear.tariffRate
    }

}
