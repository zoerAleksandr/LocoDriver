package com.z_company.data_local.setting.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.TypeConverters
import androidx.room.Update
import com.z_company.data_local.setting.entity.MonthOfYear
import com.z_company.data_local.setting.entity.NightTime
import com.z_company.data_local.setting.entity.UserSettings
import com.z_company.data_local.setting.type_converter.MonthOfYearToPrimitiveConverter
import com.z_company.data_local.setting.type_converter.NightTimeToPrimitiveConverter
import com.z_company.data_local.setting.type_converter.StringListToPrimitiveConverter
import kotlinx.coroutines.flow.Flow

@Dao
internal interface SettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(userSettings: UserSettings)

    @Query("SELECT * FROM UserSettings")
    fun getFlowSettings(): Flow<UserSettings?>
    @Query("SELECT * FROM UserSettings")
    fun getUserSettings(): UserSettings


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveMonthOfYearList(monthList: List<MonthOfYear>)

    @Query("DELETE FROM MonthOfYear")
    fun clearCalendar()

    @TypeConverters(MonthOfYearToPrimitiveConverter::class)
    @Query("UPDATE UserSettings SET monthOfYear =:monthOfYear WHERE settingsKey =:key")
    fun updateMonthOfYearInUserSetting(monthOfYear: MonthOfYear, key: String)

    @Update
    fun updateMonthOfYear(monthOfYear: MonthOfYear)

    @Query("SELECT * FROM MonthOfYear")
    fun getMonthOfYearList(): List<MonthOfYear>
    @Query("SELECT * FROM MonthOfYear")
    fun getFlowMonthOfYearList(): Flow<List<MonthOfYear>>

    @Query("SELECT * FROM MonthOfYear WHERE id =:id")
    suspend fun getMonthOfYearById(id: String): MonthOfYear

    @TypeConverters(NightTimeToPrimitiveConverter::class)
    @Query("UPDATE UserSettings SET nightTime =:nightTime WHERE settingsKey =:key")
    fun updateNightTime(nightTime: NightTime, key: String)

    @Query("UPDATE UserSettings SET updateAt =:timestamp WHERE settingsKey =:key")
    fun setUpdateAt(timestamp: Long, key: String)

    @Query("UPDATE UserSettings SET defaultWorkTime =:timeInMillis WHERE settingsKey =:key")
    fun setWorkTimeDefault(timeInMillis: Long, key: String)
    @TypeConverters(MonthOfYearToPrimitiveConverter::class)
    @Query("UPDATE UserSettings SET monthOfYear =:monthOfYear WHERE settingsKey =:key")
    fun setCurrentMonthOfYear(monthOfYear: MonthOfYear, key: String)

    @Query("UPDATE UserSettings SET lastEnteredDieselCoefficient =:coefficient WHERE settingsKey =:key")
    fun setDieselCoefficient(coefficient: Double, key: String)

    @TypeConverters(StringListToPrimitiveConverter::class)
    @Query("UPDATE UserSettings SET stationList =:stations WHERE settingsKey =:key")
    fun setStationList(stations: List<String>, key: String)

    @TypeConverters(StringListToPrimitiveConverter::class)
    @Query("SELECT stationList FROM UserSettings")
    fun getStations(): List<String>

    @TypeConverters(StringListToPrimitiveConverter::class)
    @Query("UPDATE UserSettings SET locomotiveSeriesList =:locomotiveSeries WHERE settingsKey =:key")
    fun setLocomotiveSeriesList(locomotiveSeries: List<String>, key: String)

    @TypeConverters(StringListToPrimitiveConverter::class)
    @Query("SELECT locomotiveSeriesList FROM UserSettings")
    fun getLocomotiveSeriesList(): List<String>
}