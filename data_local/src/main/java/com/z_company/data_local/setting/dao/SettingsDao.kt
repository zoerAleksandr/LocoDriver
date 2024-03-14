package com.z_company.data_local.setting.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.z_company.data_local.setting.entity.MonthOfYear
import com.z_company.data_local.setting.entity.UserSettings
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
}