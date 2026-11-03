package com.z_company.repository.remote_rest

import com.z_company.domain.util.RailLookupResult
import com.z_company.repository.remote_rest.RemoteRailStationLookup.Companion.toRailResult
import com.z_company.repository.remote_rest.response.NearestStationRequest
import com.z_company.repository.remote_rest.response.NearestStationResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class NearestStationDtoTest {
    private val json = RemoteRestClient.appJson

    @Test
    fun `old response without new fields still parses`() {
        val r = json.decodeFromString<NearestStationResponse>(
            """{"station":{"osmId":1,"name":"Луга","kind":"station","latitude":58.7,"longitude":29.8}}""",
        )
        assertEquals("Луга", r.station?.name)
        assertEquals(false, r.alongRail)
        assertEquals(null, r.distanceAlongM)
        assertEquals("Луга", (r.toRailResult() as RailLookupResult.Found).station.name)
    }

    @Test
    fun `along rail response`() {
        val r = json.decodeFromString<NearestStationResponse>(
            """{"station":{"osmId":7,"name":"Суйда","kind":"halt","esr":"123","operator":"РЖД","latitude":59.4,"longitude":30.0},
               "alongRail":true,"distanceAlongM":420,"offsetM":12,"extra":"x"}""",
        )
        val f = assertIs<RailLookupResult.Found>(r.toRailResult())
        assertEquals(420, f.station.distanceAlongM)
        assertEquals(12, f.station.offsetM)
        assertEquals("halt", f.station.kind)
    }

    @Test
    fun `null station with reason`() {
        val r = json.decodeFromString<NearestStationResponse>("""{"station":null,"alongRail":true,"reason":"off_rails"}""")
        assertEquals(RailLookupResult.NoStation("off_rails"), r.toRailResult())
    }

    @Test
    fun `request - direct mode is unchanged, rail mode is explicit`() {
        val direct = json.encodeToString(NearestStationRequest.serializer(), NearestStationRequest(1.0, 2.0))
        assertTrue(direct.contains("\"alongRail\":false"))
        val rail = json.encodeToString(NearestStationRequest.serializer(), NearestStationRequest(1.0, 2.0, 1000.0, alongRail = true))
        assertTrue(rail.contains("\"alongRail\":true") && rail.contains("\"radiusM\":1000.0"))
    }

    @Test
    fun `request - candidates are optional and additive`() {
        val plain = json.encodeToString(NearestStationRequest.serializer(), NearestStationRequest(1.0, 2.0, alongRail = true))
        assertTrue(!plain.contains("candidates\":[") )
        val withC = json.encodeToString(
            NearestStationRequest.serializer(),
            NearestStationRequest(1.0, 2.0, alongRail = true, candidates = listOf(
                com.z_company.repository.remote_rest.response.NearestStationCandidate("k", "Имя", 1.5, 2.5),
            )),
        )
        assertTrue(withC.contains("\"candidates\":[{\"key\":\"k\",\"name\":\"Имя\",\"latitude\":1.5,\"longitude\":2.5}]"))
    }

    @Test
    fun `candidate response - null osmId, key and source`() {
        val r = json.decodeFromString<NearestStationResponse>(
            """{"station":{"osmId":null,"name":"Моя","kind":"station","esr":null,"operator":null,"latitude":59.0,"longitude":30.0},
               "alongRail":true,"source":"candidate","candidateKey":"моя","distanceAlongM":100,"offsetM":3}""",
        )
        val f = assertIs<RailLookupResult.Found>(r.toRailResult())
        assertEquals("моя", f.station.candidateKey)
        assertEquals(null, f.station.osmId)
    }

    @Test
    fun `osm source has no candidate key`() {
        val r = json.decodeFromString<NearestStationResponse>(
            """{"station":{"osmId":9,"name":"Осм","latitude":59.0,"longitude":30.0},"source":"osm","candidateKey":"x"}""",
        )
        assertEquals(null, (r.toRailResult() as RailLookupResult.Found).station.candidateKey)
    }
}
