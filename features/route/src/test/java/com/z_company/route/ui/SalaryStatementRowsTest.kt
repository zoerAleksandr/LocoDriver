package com.z_company.route.ui

import com.z_company.domain.entities.salary.SalaryPaymentId
import com.z_company.route.viewmodel.LinearMileageAccrual
import com.z_company.route.viewmodel.SalaryCalculationUIState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SalaryStatementRowsTest {

    @Test
    fun `underwork is an explicit coded accrual row`() {
        val rows = buildAccrualRows(
            SalaryCalculationUIState(
                underworkHours = 3_600_000L,
                underworkMoney = 1250.0,
            )
        )

        assertEquals(1, rows.size)
        assertEquals(SalaryPaymentId.UNDERWORK, rows.single().paymentId)
        assertEquals("Оплата недоработки", rows.single().title)
    }

    @Test
    fun `zero and negative accruals are not rendered`() {
        val rows = buildAccrualRows(
            SalaryCalculationUIState(
                paymentAtTariffMoney = 0.0,
                paymentNightTimeMoney = -1.0,
                underworkMoney = 100.0,
            )
        )

        assertFalse(rows.any { it.paymentId == SalaryPaymentId.TARIFF })
        assertFalse(rows.any { it.paymentId == SalaryPaymentId.NIGHT })
        assertTrue(rows.any { it.paymentId == SalaryPaymentId.UNDERWORK })
    }

    @Test
    fun `tier rows retain their payment ids`() {
        val rows = buildAccrualRows(
            SalaryCalculationUIState(
                surchargeExtendedServicePhaseHour = listOf(3_600_000L),
                surchargeExtendedServicePhasePercent = listOf("10"),
                surchargeExtendedServicePhaseMoney = listOf(100.0),
                surchargeHeavyTransHour = listOf(3_600_000L),
                surchargeHeavyTransPercent = listOf("5"),
                surchargeHeavyTransMoney = listOf(50.0),
                surchargeLongTrainHour = listOf(3_600_000L),
                surchargeLongTrainPercent = listOf("7"),
                surchargeLongTrainMoney = listOf(70.0),
            )
        )

        assertEquals(
            listOf(
                SalaryPaymentId.EXTENDED_SERVICE,
                SalaryPaymentId.HEAVY_TRAIN,
                SalaryPaymentId.LONG_TRAIN,
            ),
            rows.map { it.paymentId },
        )
    }

    @Test
    fun `all supported deductions have stable ids and positive amounts`() {
        val rows = buildDeductionRows(
            SalaryCalculationUIState(
                retentionNdfl = 1.0,
                unionistsRetention = 2.0,
                otherRetention = 3.0,
                welfareRetention = 4.0,
                alimonyRetention = 5.0,
            )
        )

        assertEquals(
            listOf(
                SalaryPaymentId.NDFL,
                SalaryPaymentId.UNION,
                SalaryPaymentId.WELFARE,
                SalaryPaymentId.ALIMONY,
                SalaryPaymentId.OTHER_DEDUCTION,
            ),
            rows.map { it.paymentId },
        )
        assertEquals(listOf(1.0, 2.0, 4.0, 5.0, 3.0), rows.map { it.money })
        assertEquals("НДФЛ", rows.first().title)
        assertEquals(13.0, rows.first().percent)
    }

    @Test
    fun `zero and negative deductions are not rendered`() {
        val rows = buildDeductionRows(
            SalaryCalculationUIState(
                retentionNdfl = 0.0,
                unionistsRetention = -1.0,
                alimonyRetention = 10.0,
            )
        )

        assertEquals(listOf(SalaryPaymentId.ALIMONY), rows.map { it.paymentId })
    }

    @Test
    fun `statement snapshot contains every supported accrual in stable order`() {
        val state = SalaryCalculationUIState(
            paymentAtTariffMoney = 1.0,
            paymentNightTimeMoney = 1.0,
            paymentAtPassengerMoney = 1.0,
            paymentAtSingleLocomotiveMoney = 1.0,
            paymentHolidayMoney = 1.0,
            averagePaymentMoney = 1.0,
            underworkMoney = 1.0,
            caringForDisableChildrenMoney = 1.0,
            businessTripHours = 1L,
            businessTripMoney = 1.0,
            technicalStudyHours = 1L,
            technicalStudyMoney = 1.0,
            zonalSurchargePercent = 1.0,
            zonalSurchargeMoney = 1.0,
            surchargeQualificationClassPercent = 1.0,
            surchargeQualificationClassMoney = 1.0,
            linearMileageAccruals = listOf(LinearMileageAccrual("p", "Плечо", 1.0, 1.0, 1.0)),
            onePersonOperationPercent = 1.0,
            onePersonOperationMoney = 1.0,
            onePersonOperationPassengerTrainPercent = 1.0,
            onePersonOperationPassengerTrainMoney = 1.0,
            harmfulnessSurchargePercent = 1.0,
            harmfulnessSurchargeMoney = 1.0,
            districtSurchargeCoefficient = 1.0,
            districtSurchargeMoney = 1.0,
            nordicSurchargePercent = 1.0,
            nordicSurchargeMoney = 1.0,
            otherSurchargePercent = 1.0,
            otherSurchargeMoney = 1.0,
            restInExcessOfTheNormMoney = 1.0,
            surchargeExtendedServicePhaseHour = listOf(1L),
            surchargeExtendedServicePhasePercent = listOf("1"),
            surchargeExtendedServicePhaseMoney = listOf(1.0),
            surchargeHeavyTransHour = listOf(1L),
            surchargeHeavyTransPercent = listOf("1"),
            surchargeHeavyTransMoney = listOf(1.0),
            surchargeLongTrainHour = listOf(1L),
            surchargeLongTrainPercent = listOf("1"),
            surchargeLongTrainMoney = listOf(1.0),
            surchargeHeavyLongDistanceTrainsMoney = 1.0,
            surchargeDoubledTrainFirstMoney = 1.0,
            surchargeDoubledTrainSecondMoney = 1.0,
            paymentAtOvertimeMoney = 1.0,
            surchargeAtOvertime05Money = 1.0,
            surchargeAtOvertimeMoney = 1.0,
        )

        assertEquals(
            listOf(
                SalaryPaymentId.TARIFF, SalaryPaymentId.NIGHT, SalaryPaymentId.PASSENGER,
                SalaryPaymentId.RESERVE, SalaryPaymentId.HOLIDAY, SalaryPaymentId.AVERAGE,
                SalaryPaymentId.UNDERWORK, SalaryPaymentId.DISABLED_CHILD_CARE,
                SalaryPaymentId.BUSINESS_TRIP, SalaryPaymentId.TECHNICAL_STUDY,
                SalaryPaymentId.ZONAL, SalaryPaymentId.QUALIFICATION_CLASS,
                SalaryPaymentId.LINEAR_MILEAGE, SalaryPaymentId.ONE_PERSON_FREIGHT,
                SalaryPaymentId.ONE_PERSON_PASSENGER, SalaryPaymentId.HARMFULNESS,
                SalaryPaymentId.DISTRICT, SalaryPaymentId.NORDIC,
                SalaryPaymentId.EXCESS_REST,
                SalaryPaymentId.EXTENDED_SERVICE, SalaryPaymentId.HEAVY_TRAIN,
                SalaryPaymentId.LONG_TRAIN, SalaryPaymentId.HEAVY_LONG_DISTANCE,
                SalaryPaymentId.DOUBLED_TRAIN, SalaryPaymentId.DOUBLED_TRAIN,
                SalaryPaymentId.OVERTIME_BASE, SalaryPaymentId.OVERTIME_HALF,
                SalaryPaymentId.OVERTIME_FULL, SalaryPaymentId.OTHER_SURCHARGE,
            ),
            buildAccrualRows(state).map { it.paymentId },
        )
    }
}
