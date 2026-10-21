package com.z_company.domain.brake.storage

import com.z_company.domain.brake.BrakeCertificateData
import com.z_company.domain.brake.BrakeCertificateEngine
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Перенос справки PWA -> сервер -> Android без потерь. [PWA_WIRE] - настоящий ответ GET /v1/route/ (поле brakeCertificates):
 * справку создал PWA (src/e2e/dumpWire.e2e.test.js), отправил настоящий синк PWA на настоящий код сервера и БД; ответ записан как есть.
 * Тот же JSON принимает Android (BrakeCertificateSync + mergeRemote), и расчёт по нему совпадает с расчётом в PWA (1384 / 2037 тс).
 */
class BrakeCertificateCrossPlatformTest {
    // как RemoteRestClient.appJson Android
    private val androidJson = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true; explicitNulls = false; coerceInputValues = true }

    private fun sync() = androidJson.decodeFromString(ListSerializer(BrakeCertificateSync.serializer()), PWA_WIRE)

    @Test
    fun androidAcceptsCertificateCreatedInPwa() {
        val remote = sync()
        assertEquals(1, remote.size)
        val repo = FakeRepo()
        val uc = BrakeCertificateUseCase(repo) { 5_000L }
        assertEquals(1, uc.mergeRemote(ROUTE, remote))
        val data = uc.loadForTrain(TRAIN)
        assertNotNull(data)
        assertEquals("4000", data.weight)
        assertEquals("300", data.totalAxles)
        assertEquals(2, data.rows.size)
        assertEquals("3,5", data.rows[1].pressure)
        assertEquals("Луга", data.station)
        assertEquals(listOf("ЭПТ"), data.flags)
        assertEquals(75.5, data.measurements.first { it.kind == "II" }.seconds)
        assertTrue(data.measurements.first { it.kind == "II" }.manual)
        val e = data.turnaround.single()
        assertEquals("Мга", e.stationName)
        assertEquals(70.2, e.densitySec)
        assertEquals("сокращённое", e.testKind)
        assertEquals("СП", e.testKindShort)
        assertEquals("14:05", e.readinessTime)
        assertEquals("12345", e.inspectorTab)
    }

    @Test
    fun androidCalculatesTheSameAsPwa() {
        val data = androidJson.decodeFromJsonElement(BrakeCertificateData.serializer(), sync().single().data)
        val r = BrakeCertificateEngine.calculate(data)
        assertNotNull(r)
        assertEquals(1384.0, r.requiredPressure)
        assertEquals(2037.0, r.actualPressure) // 250 x 7 + 50 x 3,5 + локомотив 8 x 14
        assertEquals(4192.0, r.totalWeight)
        assertTrue(r.enough)
    }

    @Test
    fun androidWritesBackExactlyWhatPwaSent() {
        // справка, открытая и сохранённая на Android, возвращается на сервер теми же данными (ни одно поле PWA не теряется)
        val original = sync().single().data as JsonObject
        val decoded = androidJson.decodeFromJsonElement(BrakeCertificateData.serializer(), original)
        val again = androidJson.encodeToJsonElement(BrakeCertificateData.serializer(), decoded.copy(updatedAt = 0L)) as JsonObject
        // null-поля Android не пишет (explicitNulls = false) - для сервера и PWA это то же «не задано»
        assertEquals(stripNulls(original), stripNulls(again))
    }

    private fun stripNulls(e: JsonElement): JsonElement = when (e) {
        is JsonObject -> JsonObject(e.filterValues { it !is JsonNull }.mapValues { stripNulls(it.value) })
        is JsonArray -> JsonArray(e.map { stripNulls(it) })
        // 69 (JS) и 69.0 (Kotlin) - одно число
        is JsonPrimitive -> if (!e.isString && e.content.toDoubleOrNull() != null) JsonPrimitive(e.content.toDouble()) else e
    }

    private companion object {
        const val ROUTE = "11111111-1111-4111-8111-111111111111"
        const val TRAIN = "33333333-3333-4333-8333-333333333333"
        val PWA_WIRE = """
[
  {
    "certificateId": "911c076d-2ca1-4e6e-9128-7a87faf2f2a7",
    "routeId": "11111111-1111-4111-8111-111111111111",
    "trainId": "33333333-3333-4333-8333-333333333333",
    "updatedAt": 1792603691767,
    "data": {
      "id": "911c076d-2ca1-4e6e-9128-7a87faf2f2a7",
      "dpv": "",
      "date": "21.10.2026",
      "rows": [
        {
          "axles": "250",
          "pressure": "7"
        },
        {
          "axles": "50",
          "pressure": "3,5"
        }
      ],
      "tcpv": "",
      "vstr": "",
      "flags": [
        "ЭПТ"
      ],
      "grade": "UP_TO_006",
      "slope": "S006",
      "vo2hv": "",
      "weight": "4000",
      "routeId": "11111111-1111-4111-8111-111111111111",
      "station": "Луга",
      "tailCar": "",
      "trainId": "33333333-3333-4333-8333-333333333333",
      "rowsMode": true,
      "issueTime": "",
      "locoAxles": "8",
      "locoCount": "1",
      "updatedAt": 0,
      "autoblock3": false,
      "categoryId": "FREIGHT_LOADED",
      "locoNumber": "1234",
      "locoSeries": "ВЛ80С",
      "locoWeight": "192",
      "manualHave": "",
      "manualNorm": null,
      "totalAxles": "300",
      "turnaround": [
        {
          "id": "e1",
          "auto": false,
          "changes": [],
          "newAxles": "",
          "testKind": "сокращённое",
          "arrivalMs": null,
          "newActual": "",
          "newWeight": "",
          "stationId": null,
          "stopClass": "OVER_30",
          "densitySec": 70.2,
          "departureMs": null,
          "newRequired": "",
          "stationName": "Мга",
          "inspectorTab": "12345",
          "readinessTime": "14:05"
        }
      ],
      "avgPressure": "",
      "feedDensity": "",
      "locoEnabled": true,
      "tailVoltage": "",
      "trainNumber": "2604",
      "customVolume": "1800",
      "densityGroup": null,
      "locoPressure": "14",
      "measurements": [
        {
          "drop": "",
          "kind": "II",
          "grEnd": "",
          "manual": true,
          "grStart": "",
          "seconds": 75.5
        },
        {
          "drop": "",
          "kind": "IV",
          "grEnd": "",
          "manual": true,
          "grStart": "",
          "seconds": 69
        },
        {
          "drop": "",
          "kind": "ROUTE",
          "grEnd": "",
          "manual": false,
          "grStart": "",
          "seconds": null
        }
      ],
      "chargePressure": "5,4",
      "compositeShare": "P100"
    }
  }
]
"""
    }
}
