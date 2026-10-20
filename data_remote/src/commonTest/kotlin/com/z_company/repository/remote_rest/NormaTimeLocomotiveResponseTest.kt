package com.z_company.repository.remote_rest

import com.z_company.domain.entities.norma_time.LocomotiveSeries
import com.z_company.domain.entities.route.LocoType
import com.z_company.repository.remote_rest.response.NormaTimeLocomotiveResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NormaTimeLocomotiveResponseTest {
    private val json = RemoteRestClient.appJson

    @Test
    fun `old server response without passport fields parses to nulls`() {
        val r = json.decodeFromString<NormaTimeLocomotiveResponse>(
            """{"seriesId":"a","name":"2ЭС6","type":"ELECTRIC","acceptanceDurationMin":10,"sectionNumberingType":"NUMERIC","updatedAt":1779032766922.0}""",
        ).toDomain()
        assertNull(r.sectionCount)
        assertNull(r.brakeVolumeL)
        assertNull(r.axleLoadTf)
        assertEquals(10, r.acceptanceDurationMin)
    }

    @Test
    fun `new server response carries passport fields`() {
        val r = json.decodeFromString<NormaTimeLocomotiveResponse>(
            """{"seriesId":"a","name":"2ЭС6","type":"ELECTRIC","brakeVolumeL":2000,"axleCount":8,"axleLoadTf":14.0,"weightT":192.5,"sectionCount":2,"updatedAt":1.0}""",
        ).toDomain()
        assertEquals(2000, r.brakeVolumeL)
        assertEquals(8, r.axleCount)
        assertEquals(14.0, r.axleLoadTf)
        assertEquals(192.5, r.weightT)
        assertEquals(2, r.sectionCount)
    }

    @Test
    fun `client always marks passport fields so the server can clear nulls`() {
        val body = json.encodeToString(
            NormaTimeLocomotiveResponse.fromDomain(
                LocomotiveSeries(seriesId = "a", name = "ВЛ80С", type = LocoType.ELECTRIC, weightT = 192.0, sectionCount = 1, updatedAt = 5),
            ),
        )
        assertTrue("\"hasBrakeFields\":true" in body, body)
        assertTrue("\"weightT\":192.0" in body, body)
        // null не отправляется (explicitNulls = false) - поэтому и нужен маркер
        assertTrue("brakeVolumeL" !in body, body)
        // надгробие маркера не шлёт
        val tomb = json.encodeToString(
            NormaTimeLocomotiveResponse.fromTombstone(
                com.z_company.domain.entities.norma_time.LocomotiveSeriesTombstone("a", "ВЛ80С", LocoType.ELECTRIC, 7),
            ),
        )
        assertTrue("hasBrakeFields" !in tomb, tomb)
    }
}
