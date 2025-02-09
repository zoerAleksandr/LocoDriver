package com.z_company.data_local.calendar

import com.z_company.core.ResultState
import com.z_company.core.ResultState.Companion.flowMap
import com.z_company.core.ResultState.Companion.flowRequest
import com.z_company.data_local.setting.dao.SettingsDao
import com.z_company.data_local.setting.entity_converter.MonthOfYearConverter
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.repositories.CalendarRepositories
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RoomCalendarRepository : CalendarRepositories, KoinComponent {
    private val dao: SettingsDao by inject()
    override fun getFlowMonthOfYearListState(): Flow<ResultState<List<MonthOfYear>>> {
        return flowMap {
            dao.getFlowMonthOfYearList().map { monthList ->
                ResultState.Success(
                    monthList.map { monthOfYear ->
                        MonthOfYearConverter.toData(monthOfYear)
                    }
                )
            }
        }
    }

    override fun updateMonthOfYear(monthOfYear: MonthOfYear): Flow<ResultState<Unit>> {
        return flowRequest {
            dao.updateMonthOfYear(MonthOfYearConverter.fromData(monthOfYear))
        }
    }

    override suspend fun getMonthOfYearById(id: String): MonthOfYear {
        val month = dao.getMonthOfYearById(id)
        return MonthOfYearConverter.toData(
            month
        )
    }

    override fun clearCalendar(): Flow<ResultState<Unit>> {
        return flowRequest {
            dao.clearCalendar()
        }
    }

    override fun getMonthOfYearList(): List<MonthOfYear> {
        return MonthOfYearConverter.toDataList(
            dao.getMonthOfYearList()
        )
    }

    override fun saveCalendar(calendar: List<MonthOfYear>): Flow<ResultState<Unit>> {
        return flowRequest {
            dao.saveMonthOfYearList(MonthOfYearConverter.fromDataList(calendar))
        }
    }


}