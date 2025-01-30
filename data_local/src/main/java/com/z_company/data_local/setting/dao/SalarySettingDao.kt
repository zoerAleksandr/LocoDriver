package com.z_company.data_local.setting.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.z_company.data_local.setting.entity.SalarySetting
import kotlinx.coroutines.flow.Flow

@Dao
internal interface SalarySettingDao {
    @Query("SELECT * FROM SalarySetting")
    fun getSalarySetting(): SalarySetting
    @Query("SELECT * FROM SalarySetting")
    fun getFlowSalarySetting(): Flow<SalarySetting?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveSalarySetting(salarySetting: SalarySetting)
}