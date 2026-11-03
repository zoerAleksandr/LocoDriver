package com.z_company.repository.remote_rest

import com.z_company.repository.remote_rest.response.ServicePhaseSuggestResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SuggestResponseNullOsmIdTest {
    /** Крайняя станция, указанная пользователем вручную и не найденная в OSM, приходит с osmId=null. */
    @Test
    fun `terminal with null osmId parses`() {
        val r = RemoteRestClient.appJson.decodeFromString<ServicePhaseSuggestResponse>(
            """{"status":"ok","departure":{"osmId":5,"name":"Луга","kind":"station","esr":null,"operator":null,"region":null,"latitude":58.7,"longitude":29.8,"neighbours":[]},
            "arrival":{"osmId":null,"name":"Ультрамар","kind":"station","esr":null,"operator":null,"region":null,"latitude":59.6,"longitude":28.4,"neighbours":[]},
            "lengthKm":120.5,"stations":[]}""",
        )
        assertEquals("ok", r.status)
        assertNull(r.arrival?.osmId)
        assertEquals(5L, r.departure?.osmId)
    }
}
