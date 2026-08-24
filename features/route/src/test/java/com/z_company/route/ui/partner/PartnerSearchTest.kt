package com.z_company.route.ui.partner

import com.z_company.domain.entities.partner.Partner
import org.junit.Assert.assertEquals
import org.junit.Test

class PartnerSearchTest {
    private val partners = listOf(
        Partner("1", "Иванов Иван Иванович", "1234", "Москва"),
        Partner("2", "Петров Пётр", "9876", "Тула"),
    )

    @Test
    fun `filters by name ignoring case`() {
        assertEquals(listOf("1"), filterPartners(partners, "ивАНов").map { it.partnerId })
    }

    @Test
    fun `filters by tab number and notes`() {
        assertEquals(listOf("2"), filterPartners(partners, "9876 тула").map { it.partnerId })
    }

    @Test
    fun `blank query keeps all partners`() {
        assertEquals(partners, filterPartners(partners, "   "))
    }
}
