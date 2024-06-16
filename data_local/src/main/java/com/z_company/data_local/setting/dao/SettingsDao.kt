package com.z_company.data_local.setting.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.TypeConverters
import com.z_company.data_local.setting.entity.MonthOfYear
import com.z_company.data_local.setting.entity.NightTime
import com.z_company.data_local.setting.entity.UserSettings
import com.z_company.data_local.setting.type_converter.NightTimeToPrimitiveConverter
import kotlinx.coroutines.flow.Flow

@Dao
internal interface SettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(userSettings: UserSettings)

    @Query("SELECT * FROM UserSettings")
    fun getSettings(): Flow<UserSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    @JvmSuppressWildcards
    suspend fun saveMonthOfYearList(monthList: List<MonthOfYear>)

    @Query("SELECT * FROM MonthOfYear")
    fun getMonthOfYearList(): Flow<List<MonthOfYear>>

    @TypeConverters(NightTimeToPrimitiveConverter::class)
    @Query("UPDATE UserSettings SET nightTime =:nightTime WHERE settingsKey =:key")
    fun updateNightTime(nightTime: NightTime, key: String)

    @Query("UPDATE UserSettings SET updateAt =:timestamp WHERE settingsKey =:key")
    fun setUpdateAt(timestamp: Long, key: String)

    @Query("UPDATE UserSettings SET defaultWorkTime =:timeInMillis WHERE settingsKey =:key")
    fun setWorkTimeDefault(timeInMillis: Long, key: String)
}