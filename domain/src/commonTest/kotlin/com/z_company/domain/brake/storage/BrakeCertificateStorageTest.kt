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
    override fun getById(certificateId: String) = rows[certificateId]
    override fun listAllActive() = rows.values.filter { !it.isDeleted }.sortedByDescending { it.updatedAt }
    override fun markDeletedById(certificateId: String, now: Long) {
        rows[certificateId]?.let { rows[certificateId] = it.copy(isDeleted = true, updatedAt = now) }
    }
    override fun markDeletedByTrain(trainId: String, now: Long) {
        for (k in rows.keys.toList()) rows[k]?.let { v -> if (v.trainId == trainId) rows[k] = v.copy(isDeleted = true, updatedAt = now) }
    }
    override fun deleteById(certificateId: String) { rows.remove(certificateId) }
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

    @Test
    fun serverGetResponse_decodesIntoRoute_andMergesAsSecondDevice() {
        // Реальный ответ GET /v1/route/ локального стенда (элемент списка), фрагмент контракта VU45_CERTIFICATES_API.md
        val j = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false }
        val body = """{"basicData":{"id":"11111111-1111-4111-8111-111111111111","timeStartWork":1787468481382},
            "locomotives":[],"trains":[],"passengers":[],"partners":[],"otherWorks":[],
            "brakeCertificates":[{"certificateId":"c1111111-1111-4111-8111-111111111111",
            "routeId":"11111111-1111-4111-8111-111111111111","trainId":"33333333-3333-4333-8333-333333333333",
            "updatedAt":1787468999000,"data":{"category":"A","turnaround":[{"station":"Рыбацкое","inspectorTab":"12345"}]}}]}"""
        val route = j.decodeFromString(Route.serializer(), body)
        val certs = route.brakeCertificates!!
        assertEquals(1, certs.size)
        assertEquals("33333333-3333-4333-8333-333333333333", certs[0].trainId)
        assertEquals(1787468999000, certs[0].updatedAt)
        assertTrue(certs[0].data is JsonObject)
        // ответ без поля (сервер без миграции / старый ответ) - null, локальные справки не трогаются
        val noField = j.decodeFromString(Route.serializer(), """{"basicData":{"id":"x"}}""")
        assertNull(noField.brakeCertificates)
    }

    @Test
    fun listAllAndDeleteByIds_tombstoneSyncsAndDoesNotResurrect() {
        uc.save(data, "r1", "t1") { "c1" }
        clock = 2_000
        uc.save(data.copy(weight = "100"), "r1", "t2") { "c2" }
        clock = 3_000
        uc.save(data, "r2", "t3") { "c3" }
        assertEquals(listOf("c3", "c2", "c1"), uc.listAll().map { it.certificateId }) // новые сверху
        assertEquals("3040,5", uc.decode(uc.listAll().last())!!.weight)
        clock = 4_000
        val routes = uc.deleteByIds(listOf("c1", "c3", "missing"))
        assertEquals(setOf("r1", "r2"), routes)
        assertEquals(listOf("c2"), uc.listAll().map { it.certificateId })
        // в рейс r1 уходит список без удалённой; r2 - пустой список (full-replace очистит сервер)
        assertEquals(listOf("c2"), uc.forUpload("r1")!!.map { it.certificateId })
        assertEquals(emptyList(), uc.forUpload("r2")!!)
        // старая версия с сервера (updatedAt меньше надгробия) не воскрешает
        val old = BrakeCertificateSync("c1", "r1", "t1", 1_000L, kotlinx.serialization.json.JsonObject(emptyMap()))
        assertEquals(0, uc.mergeRemote("r1", listOf(old)))
        assertNull(uc.loadForTrain("t1"))
        // после доставки надгробия очищаются
        uc.purgeTombstones("r2")
        assertNull(uc.forUpload("r2"))
    }

    // ── удаление на другом устройстве ──

    private fun remoteOf(vararg ids: String) = ids.map { BrakeCertificateSync(it, "r1", null, 0L, JsonObject(emptyMap())) }

    @Test
    fun pruneRemovesCertificatesMissingOnServer_onlySyncedOnes() {
        clock = 1_000; val a = uc.save(data, "r1", "t1") { "c1" }
        clock = 2_000; uc.save(data, "r1", "t2") { "c2" }
        clock = 9_000; uc.save(data, "r1", "t3") { "c3" } // создана после снимка рейса (2 000...5 000)
        // сервер знает только c2; снимок рейса сделан в 5 000: c1 удалена на другом устройстве, c3 ещё не отправлена
        assertEquals(1, uc.pruneDeletedRemotely("r1", remoteOf("c2"), notNewerThan = 5_000))
        assertNull(repo.getById(a.id))
        assertNotNull(repo.getById("c2"))
        assertNotNull(repo.getById("c3"))
    }

    @Test
    fun pruneDoesNothingWhenServerDoesNotKnowTheField() {
        uc.save(data, "r1", "t1") { "c1" }
        assertEquals(0, uc.pruneDeletedRemotely("r1", null, notNewerThan = Long.MAX_VALUE))
        assertNotNull(repo.getById("c1"))
    }

    @Test
    fun pruneWithEmptyServerListRemovesAllSynced_butKeepsLocalTombstones() {
        clock = 1_000; uc.save(data, "r1", "t1") { "c1" }
        clock = 1_500; uc.save(data, "r1", "t2") { "c2" }
        repo.markDeletedById("c2", 1_600) // локально удалена, надгробие ждёт отправки
        assertEquals(1, uc.pruneDeletedRemotely("r1", emptyList(), notNewerThan = 5_000))
        assertNull(repo.getById("c1"))
        assertTrue(repo.getById("c2")!!.isDeleted) // надгробие остаётся до доставки
    }

    @Test
    fun pruneDoesNotTouchOtherRoutes() {
        uc.save(data, "r1", "t1") { "c1" }
        uc.save(data, "r2", "t2") { "c2" }
        uc.pruneDeletedRemotely("r1", emptyList(), notNewerThan = Long.MAX_VALUE)
        assertNull(repo.getById("c1"))
        assertNotNull(repo.getById("c2"))
    }
}
