package com.z_company.domain.brake.storage

import com.z_company.domain.brake.BrakeCertificateData
import com.z_company.domain.entities.route.Route
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeRepo : BrakeCertificateRepository {
    val rows = mutableMapOf<String, StoredBrakeCertificate>()
    override fun getByTrain(trainId: String) = rows.values.filter { it.trainId == trainId }.maxByOrNull { it.updatedAt }
    override fun listByRoute(routeId: String, includeDeleted: Boolean) =
        rows.values.filter { it.routeId == routeId && (includeDeleted || !it.isDeleted) }
    override fun upsert(cert: StoredBrakeCertificate) { rows[cert.certificateId] = cert }
    override fun markDeletedByTrain(trainId: String, now: Long) {
        rows.replaceAll { _, v -> if (v.trainId == trainId) v.copy(isDeleted = true, updatedAt = now) else v }
    }
    override fun deleteByRoute(routeId: String) { rows.values.removeAll { it.routeId == routeId } }
    override fun purgeTombstones(routeId: String) { rows.values.removeAll { it.routeId == routeId && it.isDeleted } }
}

class BrakeCertificateStorageTest {
    private var clock = 1_000L
    private val repo = FakeRepo()
    private val uc = BrakeCertificateUseCase(repo) { clock }
    private val data = BrakeCertificateData(categoryId = "FREIGHT_LOADED", weight = "3040,5", totalAxles = "128", avgPressure = "8,25")

    @Test
    fun saveAndLoadRoundTrip_onePerTrain() {
        val saved = uc.save(data, "r1", "t1") { "c1" }
        assertEquals("c1", saved.id)
        clock = 2_000
        val again = uc.save(data.copy(weight = "3000"), "r1", "t1") { "other" } // id берётся существующий
        assertEquals("c1", again.id)
        assertEquals(1, repo.rows.size)
        val loaded = uc.loadForTrain("t1")!!
        assertEquals("3000", loaded.weight)
        assertEquals(2_000L, loaded.updatedAt)
        assertEquals("r1", repo.rows["c1"]!!.routeId)
    }

    @Test
    fun forUpload_nullWhenNothing_listWhenCertsOrTombstones() {
        assertNull(uc.forUpload("r1"))
        uc.save(data, "r1", "t1") { "c1" }
        val list = uc.forUpload("r1")!!
        assertEquals(1, list.size)
        assertEquals("c1", list[0].certificateId)
        assertEquals("t1", list[0].trainId)
        assertTrue(list[0].data is JsonObject)
        // удаление поезда: надгробие -> пустой список (full-replace очистит сервер), затем purge -> снова null
        clock = 5_000
        uc.deleteForTrain("t1")
        assertEquals(emptyList(), uc.forUpload("r1"))
        uc.purgeTombstones("r1")
        assertNull(uc.forUpload("r1"))
        assertNull(uc.loadForTrain("t1"))
    }

    @Test
    fun mergeRemote_lww() {
        uc.save(data, "r1", "t1") { "c1" } // updatedAt 1000
        val newer = BrakeCertificateSync("c1", "r1", "t1", 3_000, Json.parseToJsonElement("""{"weight":"111"}"""))
        val older = BrakeCertificateSync("c1", "r1", "t1", 500, Json.parseToJsonElement("""{"weight":"222"}"""))
        assertEquals(0, uc.mergeRemote("r1", listOf(older)))
        assertEquals("3040,5", uc.loadForTrain("t1")!!.weight)
        assertEquals(1, uc.mergeRemote("r1", listOf(newer)))
        assertEquals("111", uc.loadForTrain("t1")!!.weight)
        // новая справка с сервера вставляется
        val other = BrakeCertificateSync("c2", "r1", "t2", 10, Json.parseToJsonElement("""{"weight":"5"}"""))
        assertEquals(1, uc.mergeRemote("r1", listOf(other)))
        assertNotNull(uc.loadForTrain("t2"))
    }

    @Test
    fun mergeRemote_nullOrEmpty_doesNotDelete() {
        uc.save(data, "r1", "t1") { "c1" }
        assertEquals(0, uc.mergeRemote("r1", null))
        assertEquals(0, uc.mergeRemote("r1", emptyList()))
        assertNotNull(uc.loadForTrain("t1"))
    }

    @Test
    fun mergeRemote_localTombstoneWinsOverOlderRemote() {
        uc.save(data, "r1", "t1") { "c1" }
        clock = 4_000
        uc.deleteForTrain("t1")
        val remote = BrakeCertificateSync("c1", "r1", "t1", 2_000, Json.parseToJsonElement("""{"weight":"1"}"""))
        assertEquals(0, uc.mergeRemote("r1", listOf(remote)))
        assertNull(uc.loadForTrain("t1"))
    }

    @Test
    fun deleteForRoute_removesAll() {
        uc.save(data, "r1", "t1") { "c1" }
        uc.deleteForRoute("r1")
        assertTrue(repo.rows.isEmpty())
    }

    @Test
    fun routeSerialization_fieldOptional_oldAndNew() {
        val j = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false }
        val plain = j.encodeToString(Route.serializer(), Route())
        assertFalse(plain.contains("brakeCertificates"), plain) // null не отправляется - сервер не трогает справки
        val withCert = Route(brakeCertificates = listOf(BrakeCertificateSync("c1", "r1", null, 7, Json.parseToJsonElement("""{"a":1}"""))))
        val text = j.encodeToString(Route.serializer(), withCert)
        val el = j.parseToJsonElement(text).jsonObject["brakeCertificates"]!!
        assertTrue(el.toString().contains("\"certificateId\":\"c1\""))
        assertTrue(el.toString().contains("\"updatedAt\":7"))
        assertFalse(el.toString().contains("trainId")) // null опущен
        // ответ сервера без поля / с полем
        assertNull(j.decodeFromString(Route.serializer(), plain).brakeCertificates)
        val back = j.decodeFromString(Route.serializer(), text)
        assertEquals(1, back.brakeCertificates!!.size)
        // пустой список остаётся пустым (а не null): [] стирает справки рейса на сервере
        val empty = j.encodeToString(Route.serializer(), Route(brakeCertificates = emptyList()))
        assertTrue(empty.contains("\"brakeCertificates\":[]"), empty)
    }
}
