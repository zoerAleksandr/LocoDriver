package com.z_company.data_local.setting

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.z_company.data_local.setting.salarydb.SalarySettingDatabase
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class SalarySettingDatabaseRoundTripTest {
    @Test
    fun everySalarySettingColumnSurvivesNativeSqliteRoundTrip() {
        val driver = NativeSqliteDriver(
            SalarySettingDatabase.Schema,
            "salary-setting-test-${Random.nextLong()}.db",
        )
        try {
            val database = SalarySettingDatabase(driver)
            database.salarySettingQueries.insertOrReplace(
                salarySettingKey = "main",
                nightTimePercent = 40.5,
                zonalSurcharge = 25.5,
                averagePaymentHour = 777.77,
                districtCoefficient = 30.0,
                nordicCoefficient = 50.0,
                onePersonOperationPercent = 40.0,
                onePersonOperationPassengerTrainPercent = 50.0,
                harmfulnessPercent = 4.0,
                surchargeQualificationClass = 10.0,
                surchargeExtendedServicePhaseList = "extended-json",
                surchargeHeavyTrainsList = "heavy-json",
                surchargeLongTrainsList = "long-json",
                surchargeHeavyLongDistanceTrains = 5.0,
                surchargeLongTrain = 0.0,
                lengthLongDistanceTrain = 0L,
                otherSurcharge = 3.0,
                ndfl = 13.0,
                unionistsRetention = 1.0,
                otherRetention = 2.0,
                welfarePercent = 4.0,
                alimonyPercent = 25.0,
                showUnderworkPayments = 0L,
            )

            val row = database.salarySettingQueries.getByKey("main").executeAsOne()
            assertEquals("main", row.salarySettingKey)
            assertEquals(40.5, row.nightTimePercent)
            assertEquals(25.5, row.zonalSurcharge)
            assertEquals(777.77, row.averagePaymentHour)
            assertEquals(30.0, row.districtCoefficient)
            assertEquals(50.0, row.nordicCoefficient)
            assertEquals(40.0, row.onePersonOperationPercent)
            assertEquals(50.0, row.onePersonOperationPassengerTrainPercent)
            assertEquals(4.0, row.harmfulnessPercent)
            assertEquals(10.0, row.surchargeQualificationClass)
            assertEquals("extended-json", row.surchargeExtendedServicePhaseList)
            assertEquals("heavy-json", row.surchargeHeavyTrainsList)
            assertEquals("long-json", row.surchargeLongTrainsList)
            assertEquals(5.0, row.surchargeHeavyLongDistanceTrains)
            assertEquals(0.0, row.surchargeLongTrain)
            assertEquals(0L, row.lengthLongDistanceTrain)
            assertEquals(3.0, row.otherSurcharge)
            assertEquals(13.0, row.ndfl)
            assertEquals(1.0, row.unionistsRetention)
            assertEquals(2.0, row.otherRetention)
            assertEquals(4.0, row.welfarePercent)
            assertEquals(25.0, row.alimonyPercent)
            assertEquals(0L, row.showUnderworkPayments)
        } finally {
            driver.close()
        }
    }
}
