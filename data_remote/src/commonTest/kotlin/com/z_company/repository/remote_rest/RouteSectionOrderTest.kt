package com.z_company.repository.remote_rest

import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.SectionDiesel
import com.z_company.domain.entities.route.SectionElectric
import kotlin.test.Test
import kotlin.test.assertEquals

class RouteSectionOrderTest {

    @Test
    fun preservesLocalOrderAndAppendsNewRemoteSections() {
        val local = route(
            electricIds = listOf("electric-b", "electric-a"),
            dieselIds = listOf("diesel-b", "diesel-a"),
        )
        val remote = route(
            electricIds = listOf("electric-a", "electric-new", "electric-b"),
            dieselIds = listOf("diesel-a", "diesel-new", "diesel-b"),
        )

        val result = remote.preserveSectionOrderFrom(local).locomotives.single()

        assertEquals(
            listOf("electric-b", "electric-a", "electric-new"),
            result.electricSectionList.map { it.sectionId },
        )
        assertEquals(
            listOf("diesel-b", "diesel-a", "diesel-new"),
            result.dieselSectionList.map { it.sectionId },
        )
    }

    @Test
    fun keepsRemoteOrderForRouteNotStoredLocally() {
        val remote = route(
            electricIds = listOf("electric-c", "electric-a", "electric-b"),
            dieselIds = listOf("diesel-c", "diesel-a", "diesel-b"),
        )

        val result = remote.preserveSectionOrderFrom(null).locomotives.single()

        assertEquals(remote.locomotives.single().electricSectionList, result.electricSectionList)
        assertEquals(remote.locomotives.single().dieselSectionList, result.dieselSectionList)
    }

    private fun route(electricIds: List<String>, dieselIds: List<String>): Route = Route(
        basicData = BasicData(id = "route-id"),
        locomotives = mutableListOf(
            Locomotive(
                locoId = "loco-id",
                basicId = "route-id",
                electricSectionList = electricIds.map {
                    SectionElectric(sectionId = it, locoId = "loco-id")
                }.toMutableList(),
                dieselSectionList = dieselIds.map {
                    SectionDiesel(sectionId = it, locoId = "loco-id")
                }.toMutableList(),
            )
        ),
    )
}
