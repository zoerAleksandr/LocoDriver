package com.z_company.repository.remote_rest.tracks

import kotlinx.coroutines.runBlocking
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class MemProfileCache : SectionProfileCache {
    var index: String? = null
    val sections = mutableMapOf<String, String>()
    override fun loadIndex() = index
    override fun saveIndex(text: String) { index = text }
    override fun load(sectionId: String) = sections[sectionId]
    override fun save(sectionId: String, text: String) { sections[sectionId] = text }
    override fun retain(keepIds: Set<String>) { sections.keys.retainAll(keepIds) }
}

private class ProfileApi : AdvisorApi {
    var updatedAt = 1000.0
    var offline = false
    var sectionCalls = 0

    var withBox = false
    var sectionCallsById = mutableMapOf<String, Int>()

    override suspend fun sections(): List<AdvisorSectionSummaryDto> {
        if (offline) throw IOException("no network")
        val near = if (withBox) listOf(55.0, 37.0, 55.02, 37.0) else null
        val far = if (withBox) listOf(10.0, 10.0, 10.1, 10.0) else null
        return listOf(
            AdvisorSectionSummaryDto("s1", true, "h1", "А — Б", "А", "Б", 2.0, 1, 2, updatedAt, near),
            AdvisorSectionSummaryDto("s2", true, "h2", "В — Г", "В", "Г", 2.0, 1, 2, updatedAt, far),
        )
    }

    override suspend fun section(id: String): AdvisorSectionDto {
        if (offline) throw IOException("no network")
        sectionCalls++
        sectionCallsById[id] = (sectionCallsById[id] ?: 0) + 1
        return AdvisorSectionDto(
            id = id, axisHash = "h1",
            geometry = listOf(AdvisorGeoPointDto(0.0, 55.0, 37.0), AdvisorGeoPointDto(2.0, 55.02, 37.0)),
            name = "А — Б", fromStation = "А", toStation = "Б", lengthKm = 2.0, updatedAt = updatedAt,
            elements = listOf(AdvisorElementDto(0.0, 2.0, 3.0)),
            kmMarks = listOf(AdvisorKmMarkDto(0.0, 100.0, 1)),
        )
    }

    override suspend fun postAltitudeContrib(token: String, body: AltitudeContribRequest) = error("not used")
}

class SectionProfileRepositoryTest {
    @Test
    fun downloads_once_then_uses_cache_until_updatedAt_changes() = runBlocking {
        val api = ProfileApi()
        val cache = MemProfileCache()
        val repo = SectionProfileRepository(api, cache)
        val first = repo.section("s1")
        assertNotNull(first)
        assertEquals(100.0, first.kmMarks[0].kmAbs)
        repo.section("s1")
        assertEquals(1, api.sectionCalls)

        // Новый процесс: память пуста, диск и список те же - сеть за участком не нужна.
        val again = SectionProfileRepository(api, cache)
        assertNotNull(again.section("s1"))
        assertEquals(1, api.sectionCalls)

        api.updatedAt = 2000.0
        val updated = SectionProfileRepository(api, cache)
        updated.section("s1")
        assertEquals(2, api.sectionCalls)
    }

    @Test
    fun works_offline_from_cache_and_empty_without_cache() = runBlocking {
        val api = ProfileApi()
        val cache = MemProfileCache()
        SectionProfileRepository(api, cache).apply { section("s1"); summaries() }
        api.offline = true
        val offline = SectionProfileRepository(api, cache)
        assertEquals(2, offline.summaries().size)
        assertTrue(offline.section("s1")!!.hasProfile)
        assertEquals(1, offline.allSections().size)

        val empty = SectionProfileRepository(api, MemProfileCache())
        assertEquals(0, empty.summaries().size)
        assertNull(empty.section("s1"))
    }

    @Test
    fun near_loads_only_close_sections_when_server_gives_bbox() = runBlocking {
        val api = ProfileApi().apply { withBox = true }
        val repo = SectionProfileRepository(api, MemProfileCache())
        repo.summaries()
        val near = repo.sectionsNear(55.01, 37.0)
        assertEquals(listOf("s1"), near.map { it.id })
        assertEquals(null, api.sectionCallsById["s2"])
        assertEquals(55.0, repo.summaries(refresh = false).first().box!!.minLat)
    }

    @Test
    fun near_falls_back_to_all_sections_for_old_server() = runBlocking {
        val api = ProfileApi().apply { withBox = false }
        val repo = SectionProfileRepository(api, MemProfileCache())
        repo.summaries()
        assertEquals(setOf("s1", "s2"), repo.sectionsNear(55.01, 37.0).map { it.id }.toSet())
    }
}
