package com.z_company.route.ui

import com.z_company.domain.entities.salary.SalaryPaymentId
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
                SalaryPaymentId.OTHER_DEDUCTION,
                SalaryPaymentId.WELFARE,
                SalaryPaymentId.ALIMONY,
            ),
            rows.map { it.paymentId },
        )
        assertEquals(listOf(1.0, 2.0, 3.0, 4.0, 5.0), rows.map { it.money })
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
}
