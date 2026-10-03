package com.z_company.domain.util_test

import com.z_company.domain.entities.setting.ServicePhaseStation
import com.z_company.domain.entities.setting.ServicePhaseStationKind
import com.z_company.domain.entities.setting.ServicePhaseStationSource
import com.z_company.domain.util.ServicePhaseStationsMerge
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ServicePhaseStationsMergeTest {

    private fun osm(name: String, km: Double, kind: ServicePhaseStationKind = ServicePhaseStationKind.STATION) =
        ServicePhaseStation(
            name = name, kmFromStart = km, kind = kind, latitude = 59.0, longitude = 30.0,
            esr = "0301${km.toInt()}", osmId = km.toLong(), source = ServicePhaseStationSource.OSM,
        )

    @Test
    fun `selected new stations are inserted by km, manual without km keep place`() {
        val a = ServicePhaseStation(name = "Суйда", kmFromStart = 10.0)
        val x = ServicePhaseStation(name = "Кемка", kind = ServicePhaseStationKind.HALT)
        val b = ServicePhaseStation(name = "Мшинская", kmFromStart = 53.6)
        val n1 = osm("Сиверская", 25.1)
        val n2 = osm("Дивенская", 40.2)
        val skipped = osm("Строганово", 30.4, ServicePhaseStationKind.HALT)

        val (merged, added) = ServicePhaseStationsMerge.merge(listOf(a, x, b), listOf(n1, skipped, n2), setOf(n1.id, n2.id))

        assertEquals(listOf("Суйда", "Кемка", "Сиверская", "Дивенская", "Мшинская"), merged.map { it.name })
        assertEquals(setOf(n1.id, n2.id), added)
    }

    @Test
    fun `already present station is enriched not duplicated`() {
        val manual = ServicePhaseStation(name = "Толмачёво")
        val found = osm("толмачево", 68.3)

        val (merged, added) = ServicePhaseStationsMerge.merge(listOf(manual), listOf(found), setOf(found.id))

        assertEquals(1, merged.size)
        assertEquals(manual.id, merged[0].id)
        assertEquals("Толмачёво", merged[0].name)
        assertEquals(68.3, merged[0].kmFromStart)
        assertEquals(59.0, merged[0].latitude)
        assertEquals(ServicePhaseStationSource.MANUAL, merged[0].source)
        assertEquals(emptySet(), added)
    }

    @Test
    fun `same name but other kind is a different station`() {
        val halt = ServicePhaseStation(name = "Кемка", kind = ServicePhaseStationKind.HALT)
        assertNull(ServicePhaseStationsMerge.existingMatch(osm("Кемка", 59.1), listOf(halt)))
    }

    @Test
    fun `new names skip known ones and duplicates`() {
        val names = ServicePhaseStationsMerge.newNames(
            listOf(osm("Сиверская", 1.0), osm("Суйда", 2.0), osm("суйда", 3.0)),
            listOf("СИВЕРСКАЯ"),
        )
        assertEquals(listOf("Суйда"), names)
    }
}
