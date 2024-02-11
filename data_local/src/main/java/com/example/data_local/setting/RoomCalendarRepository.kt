package com.example.data_local.setting

import com.example.core.ResultState
import com.example.core.ResultState.Companion.flowMap
import com.example.core.ResultState.Companion.flowRequest
import com.example.data_local.setting.dao.SettingsDao
import com.example.data_local.setting.entity_converter.MonthOfYearConverter
import com.example.domain.entities.MonthOfYear
import com.example.domain.repositories.CalendarRepositories
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RoomCalendarRepository : CalendarRepositories, KoinComponent {
    private val dao: SettingsDao by inject()
    override fun getMonthOfYearList(): Flow<ResultState<List<MonthOfYear>>> {
        return flowMap {
            dao.getMonthOfYearList().map { monthList ->
                ResultState.Success(
                    monthList.map { monthOfYear ->
                        MonthOfYearConverter.toData(monthOfYear)
                    }
                )
            }
        }
    }

    override fun saveCalendar(calendar: List<MonthOfYear>): Flow<ResultState<Unit>> {
        return flowRequest {
            dao.saveMonthOfYearList(MonthOfYearConverter.fromDataList(calendar))
        }
    }
}