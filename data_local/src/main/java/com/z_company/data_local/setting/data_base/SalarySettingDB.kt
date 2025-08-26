package com.z_company.data_local.setting.data_base

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import com.z_company.data_local.setting.dao.SalarySettingDao
import com.z_company.data_local.setting.entity.SalarySetting

/**
 * version 2 added field averagePaymentHour, districtCoefficient, nordicCoefficient
 * version 3 added field onePersonOperationPercent, harmfulnessPercent, surchargeHeavyTrainsList, surchargeLongTrain
 *          remove field surchargeHeavyLongDistanceTrains
 * version 4 added field lengthLongDistanceTrain
 * version 5 added field nightTimePercent
 * version 6 added field onePersonOperationPassengerTrainPercent
 * */
@Database(
    entities = [
        SalarySetting::class
    ],
    version = 6,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3, spec = DeleteColumnSurchargeHeavyLongDistanceTrains::class),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
    ]
)
internal abstract class SalarySettingDB : RoomDatabase() {
    abstract fun salarySettingDao(): SalarySettingDao
}

@DeleteColumn(tableName = "SalarySetting", columnName = "surchargeHeavyLongDistanceTrains")
class DeleteColumnSurchargeHeavyLongDistanceTrains : AutoMigrationSpec