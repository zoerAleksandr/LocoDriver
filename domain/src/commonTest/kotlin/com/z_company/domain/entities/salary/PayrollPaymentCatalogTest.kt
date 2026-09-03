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
        assertEquals("048A", PayrollPaymentCatalog[SalaryPaymentId.UNDERWORK].codeLabel)
        assertEquals("023L", PayrollPaymentCatalog[SalaryPaymentId.NIGHT].codeLabel)
        assertEquals("018L", PayrollPaymentCatalog[SalaryPaymentId.PASSENGER].codeLabel)
        assertEquals("052L", PayrollPaymentCatalog[SalaryPaymentId.RESERVE].codeLabel)
        assertEquals("035L/076L", PayrollPaymentCatalog[SalaryPaymentId.HOLIDAY].codeLabel)
        assertEquals("072L", PayrollPaymentCatalog[SalaryPaymentId.OVERTIME_BASE].codeLabel)
        assertEquals("073L", PayrollPaymentCatalog[SalaryPaymentId.OVERTIME_HALF].codeLabel)
        assertEquals("073M", PayrollPaymentCatalog[SalaryPaymentId.OVERTIME_FULL].codeLabel)
        assertEquals("151L", PayrollPaymentCatalog[SalaryPaymentId.EXTENDED_SERVICE].codeLabel)
        assertEquals("152L", PayrollPaymentCatalog[SalaryPaymentId.HEAVY_TRAIN].codeLabel)
        assertEquals("152L", PayrollPaymentCatalog[SalaryPaymentId.LONG_TRAIN].codeLabel)
        assertEquals("158L", PayrollPaymentCatalog[SalaryPaymentId.DOUBLED_TRAIN].codeLabel)
        assertEquals("153L", PayrollPaymentCatalog[SalaryPaymentId.ONE_PERSON_FREIGHT].codeLabel)
        assertEquals("153L", PayrollPaymentCatalog[SalaryPaymentId.ONE_PERSON_PASSENGER].codeLabel)
        assertEquals("030A/030B", PayrollPaymentCatalog[SalaryPaymentId.AVERAGE].codeLabel)
        assertEquals("030A/030B", PayrollPaymentCatalog[SalaryPaymentId.BUSINESS_TRIP].codeLabel)
        assertEquals("027A", PayrollPaymentCatalog[SalaryPaymentId.NORDIC].codeLabel)
        assertEquals("026A", PayrollPaymentCatalog[SalaryPaymentId.DISTRICT].codeLabel)
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
    fun `all definitions have both names and normalized distinct codes`() {
        val codePattern = Regex("^[0-9A-Z]+$")

        PayrollPaymentCatalog.entries.forEach { payment ->
            assertTrue(payment.plainName.isNotBlank(), payment.id.name)
            assertTrue(payment.payrollSheetName.isNotBlank(), payment.id.name)
            assertEquals(payment.codes.distinct(), payment.codes, payment.id.name)
            payment.codes.forEach { code ->
                assertTrue(codePattern.matches(code), "${payment.id}: $code")
            }
        }
    }

    @Test
    fun `calculated rows never use a manual-only payroll code`() {
        PayrollPaymentCatalog.entries.flatMap { payment ->
            payment.codes.map { payment.id to it }
        }.forEach { (paymentId, code) ->
            val references = PayrollCodeReferenceCatalog.entries.filter { it.code == code }
            if (references.isNotEmpty()) {
                assertTrue(
                    references.any { "ручн" !in it.description.lowercase() },
                    "$paymentId uses manual-only code $code",
                )
            }
        }
    }

    @Test
    fun `unknown or depot-specific code is shown explicitly`() {
        assertEquals("—", PayrollPaymentCatalog[SalaryPaymentId.OTHER_SURCHARGE].codeLabel)
        assertTrue(PayrollPaymentCatalog[SalaryPaymentId.OTHER_SURCHARGE].codes.isEmpty())
    }
}
