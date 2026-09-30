package com.z_company.domain.use_cases

import com.z_company.domain.entities.partner.Partner
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.RoutePartner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class PermanentPartnerTest {
    private val permanent = Partner(
        partnerId = "p1",
        fullName = "Иванов Иван",
        tabNumber = "123",
        isPermanent = true,
    )

    @Test
    fun noPermanentPartnerKeepsRoute() {
        val route = Route(basicData = BasicData(id = "b1"))
        assertSame(route, PartnerUseCase.withPermanentPartner(route, null))
    }

    @Test
    fun addsCopyOfPermanentPartnerToNewRoute() {
        val route = Route(basicData = BasicData(id = "b1"))
        val result = PartnerUseCase.withPermanentPartner(route, permanent)
        val copy = result.partners.single()
        assertEquals("b1", copy.basicId)
        assertEquals("p1", copy.sourcePartnerId)
        assertEquals("Иванов Иван", copy.fullName)
        assertEquals("123", copy.tabNumber)
    }

    @Test
    fun doesNotDuplicatePermanentPartner() {
        val route = Route(
            basicData = BasicData(id = "b1"),
            partners = mutableListOf(RoutePartner(basicId = "b1", sourcePartnerId = "p1")),
        )
        assertSame(route, PartnerUseCase.withPermanentPartner(route, permanent))
    }
}
