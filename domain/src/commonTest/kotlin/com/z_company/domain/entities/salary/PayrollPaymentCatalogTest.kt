package com.z_company.domain.entities.salary

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PayrollPaymentCatalogTest {

    @Test
    fun `every salary row id has catalog definition`() {
        assertEquals(
            SalaryPaymentId.entries.toSet(),
            PayrollPaymentCatalog.entries.map { it.id }.toSet(),
        )
        assertEquals(
            PayrollPaymentCatalog.entries.size,
            PayrollPaymentCatalog.entries.map { it.id }.distinct().size,
        )
    }

    @Test
    fun `confirmed codes from supplied payroll sheets are preserved`() {
        assertEquals("004L", PayrollPaymentCatalog[SalaryPaymentId.TARIFF].codeLabel)
        assertEquals("023L", PayrollPaymentCatalog[SalaryPaymentId.NIGHT].codeLabel)
        assertEquals("018L", PayrollPaymentCatalog[SalaryPaymentId.PASSENGER].codeLabel)
        assertEquals("052L", PayrollPaymentCatalog[SalaryPaymentId.RESERVE].codeLabel)
        assertEquals("035L/076L", PayrollPaymentCatalog[SalaryPaymentId.HOLIDAY].codeLabel)
        assertEquals("072L", PayrollPaymentCatalog[SalaryPaymentId.OVERTIME_BASE].codeLabel)
        assertEquals("073L", PayrollPaymentCatalog[SalaryPaymentId.OVERTIME_HALF].codeLabel)
        assertEquals("151L/151P", PayrollPaymentCatalog[SalaryPaymentId.EXTENDED_SERVICE].codeLabel)
        assertEquals("152P", PayrollPaymentCatalog[SalaryPaymentId.HEAVY_TRAIN].codeLabel)
        assertEquals("152P", PayrollPaymentCatalog[SalaryPaymentId.LONG_TRAIN].codeLabel)
        assertEquals("153L", PayrollPaymentCatalog[SalaryPaymentId.ONE_PERSON_FREIGHT].codeLabel)
        assertEquals("153L", PayrollPaymentCatalog[SalaryPaymentId.ONE_PERSON_PASSENGER].codeLabel)
        assertEquals("883A", PayrollPaymentCatalog[SalaryPaymentId.NDFL].codeLabel)
        assertEquals("902A", PayrollPaymentCatalog[SalaryPaymentId.UNION].codeLabel)
        assertEquals("932A", PayrollPaymentCatalog[SalaryPaymentId.WELFARE].codeLabel)
        assertEquals("889A", PayrollPaymentCatalog[SalaryPaymentId.ALIMONY].codeLabel)
    }

    @Test
    fun `plain and payroll sheet names are both available`() {
        val payment = PayrollPaymentCatalog[SalaryPaymentId.TARIFF]

        assertEquals("Оплата по тарифу", payment.displayName(PayrollNameMode.PLAIN))
        assertEquals("ПоврОплатаПоТарифСтавкам", payment.displayName(PayrollNameMode.PAYROLL_SHEET))
        assertTrue(payment.payrollSheetName.isNotBlank())
        assertTrue(payment.plainName.isNotBlank())
    }

    @Test
    fun `unknown or depot-specific code is shown explicitly`() {
        assertEquals("—", PayrollPaymentCatalog[SalaryPaymentId.AVERAGE].codeLabel)
        assertTrue(PayrollPaymentCatalog[SalaryPaymentId.AVERAGE].codes.isEmpty())
    }
}
