package com.z_company.domain.use_cases

import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.SalarySetting
import com.z_company.domain.repositories.SalarySettingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SalarySettingUseCase(
    val repository: SalarySettingRepository,
) : KoinComponent {
    private val calendarUseCase: CalendarUseCase by inject()
    suspend fun getSalarySetting(): SalarySetting = repository.getSalarySetting()
    fun getFlowSalarySetting(): Flow<ResultState<SalarySetting?>> =
        repository.getSalarySettingState()

    fun saveSalarySetting(setting: SalarySetting): Flow<ResultState<Unit>> =
        repository.saveSalarySetting(setting)

    suspend fun updateTariffRateOnlyInOneMonthOfYear(
        newTariffRate: Double,
        monthId: String
    ): Flow<ResultState<Unit>> {
        val dispatcher = Dispatchers.IO
        val scope = CoroutineScope(dispatcher)

        return flow {
            emit(ResultState.Loading)
            val oldMonth = scope.async { calendarUseCase.loadMonthOfYearById(monthId) }.await()
            val newMonth = oldMonth.copy(tariffRate = newTariffRate)
            calendarUseCase.updateMonthOfYear(newMonth).collect {
                if (it is ResultState.Success) {
                    emit(ResultState.Success(Unit))
                }
                if (it is ResultState.Error) {
                    emit(ResultState.Error(it.entity))
                }
            }
        }.catch {
            emit(ResultState.Error(ErrorEntity(it)))
        }.flowOn(dispatcher)
    }

    suspend fun updateTariffRateCurrentAndNextMonths(
        newTariffRate: Double,
        currentMonthId: String
    ): Flow<ResultState<Unit>> {
        val dispatcher = Dispatchers.IO
        val scope = CoroutineScope(dispatcher)

        return flow {
            emit(ResultState.Loading)
            val currentMonth =
                scope.async { calendarUseCase.loadMonthOfYearById(currentMonthId) }.await()
            val allMonthOfYear = scope.async { calendarUseCase.loadMonthOfYearList() }.await()
            val sortedListAllMonthOfYear =
                allMonthOfYear.sortedWith(compareBy(MonthOfYear::year, MonthOfYear::month))
            val indexCurrentMonthOfYear = sortedListAllMonthOfYear.indexOf(currentMonth)
            val nextMonths =
                sortedListAllMonthOfYear.slice(indexCurrentMonthOfYear..sortedListAllMonthOfYear.lastIndex)
            nextMonths.forEach { oldMonth ->
                updateTariffRateOnlyInOneMonthOfYear(
                    monthId = oldMonth.id,
                    newTariffRate = newTariffRate
                ).collect {}
            }
            emit(ResultState.Success(Unit))
        }.catch {
            emit(ResultState.Error(ErrorEntity(it)))
        }.flowOn(dispatcher)
    }

    suspend fun getTariffRateFromCurrentMonthOfYear(monthOfYear: MonthOfYear): Double {
        val dispatcher = Dispatchers.IO
        val scope = CoroutineScope(dispatcher)

        val currentMonthOfYear =
            scope.async { calendarUseCase.loadMonthOfYearById(monthOfYear.id) }.await()
        return currentMonthOfYear.tariffRate
    }

}