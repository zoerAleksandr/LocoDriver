package com.z_company.domain.brake

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Мост отдаёт ровно то, что считает общий код domain/brake (формат ответов - контракт PWA). */
class PwaBrakeBridgeTest {
    private val parse = Json.Default
    private fun call(name: String, args: String = "{}") = parse.parseToJsonElement(PwaBrakeBridge.call(name, args))

    private val data = BrakeCertificateData(
        categoryId = "FREIGHT_LOADED", weight = "4000", totalAxles = "300", avgPressure = "7", slope = SlopeStep.S008,
        locoEnabled = true, locoWeight = "192", locoAxles = "8", locoPressure = "14", locoSeries = "ВЛ80С",
    )

    private fun dataJson(d: BrakeCertificateData = data) =
        """{"data":${Json { encodeDefaults = true }.encodeToString(BrakeCertificateData.serializer(), d)}}"""

    @Test
    fun catalogHasAllCategoriesAndGroups() {
        val c = parse.parseToJsonElement(PwaBrakeBridge.catalogJson()).jsonObject
        assertEquals(BrakeNorms.categories.size, c["categories"]!!.jsonArray.size)
        assertEquals(DensityLocoGroup.values().size, c["groups"]!!.jsonArray.size)
        assertEquals(SlopeStep.values().size, c["slopeSteps"]!!.jsonArray.size)
        assertEquals(BrakeLocoCatalog.items.size, c["locoCatalog"]!!.jsonArray.size)
        assertEquals(BrakeNorms.blankAxlePressures.size, c["blankAxlePressures"]!!.jsonArray.size)
    }

    @Test
    fun evaluateMatchesDirectCalculation() {
        val direct = BrakeCertificateEngine.calculate(data)!!
        val r = call("evaluate", dataJson()).jsonObject
        val res = r["result"]!!.jsonObject
        assertEquals(direct.requiredPressure, res["requiredPressure"]!!.jsonPrimitive.double)
        assertEquals(direct.actualPressure, res["actualPressure"]!!.jsonPrimitive.double)
        assertEquals(direct.totalAxles, res["totalAxles"]!!.jsonPrimitive.int)
        assertEquals(direct.enough, res["enough"]!!.jsonPrimitive.boolean)
        assertEquals(BrakeStatusResolver.resolve(direct, true).headline, r["status"]!!.jsonObject["headline"]!!.jsonPrimitive.content)
        assertEquals("ALLOWED", res["speed"]!!.jsonObject["type"]!!.jsonPrimitive.content)
        assertEquals(DensityLocoGroup.G4.name, r["effectiveGroup"]!!.jsonObject["name"]!!.jsonPrimitive.content)
    }

    @Test
    fun evaluateWithoutWeightHasNoResult() {
        val r = call("evaluate", dataJson(BrakeCertificateData())).jsonObject
        assertEquals(false, r["hasInput"]!!.jsonPrimitive.boolean)
        assertTrue(r["result"] is JsonNull)
        assertEquals("NONE", r["status"]!!.jsonObject["level"]!!.jsonPrimitive.content)
    }

    @Test
    fun densityFunctionsMatch() {
        val lines = call("normLines", """{"group":"G4","axles":300,"chargePressure":5.4,"locoCount":1}""").jsonArray.map { it.jsonPrimitive.content }
        assertEquals(BrakeDensity.normLines(DensityLocoGroup.G4, 300, 5.4, 1), lines)
        assertEquals(BrakeDensity.volumeCaption("1450"), call("volumeCaption", """{"text":"1450"}""").jsonPrimitive.content)
        assertEquals(BrakeDensity.parseManualSeconds("69,44"), call("parseManualSeconds", """{"text":"69,44"}""").jsonPrimitive.double)
        val c = call("commitDensity", """{"text":"70","initialText":"","cleared":false}""").jsonObject
        assertEquals("SET", c["kind"]!!.jsonPrimitive.content)
        assertEquals(70.0, c["seconds"]!!.jsonPrimitive.double)
        val a = call("assessFreightII", """{"timeSec":50,"minTime":60,"groupSelected":true}""").jsonObject
        assertEquals("FAIL", a["verdict"]!!.jsonPrimitive.content)
    }

    @Test
    fun categorySuggestionsMatch() {
        val s = call("suggestByTrainNumber", """{"number":"9402","axles":380,"currentId":"FREIGHT_LOADED"}""").jsonObject
        assertEquals("EMPTY_350_400", s["categoryId"]!!.jsonPrimitive.content)
        val l = call("suggestByAxleLoad", """{"currentId":"FREIGHT_LOADED","manual":false,"numberDetermines":false,"weightT":4000,"axles":300}""").jsonObject
        assertEquals(BrakeCategory.byAxleLoad(4000.0, 300)!!.categoryId, l["categoryId"]!!.jsonPrimitive.content)
    }

    @Test
    fun turnaroundCompositionAndTimeInput() {
        val entry = TurnaroundEntry(id = "e1", newWeight = "3100", changes = listOf(WagonChange("w1", attached = false, pressureText = "7", axlesText = "20")))
        val args = """{"data":${Json { encodeDefaults = true }.encodeToString(BrakeCertificateData.serializer(), data.copy(rowsMode = true, rows = listOf(AxleRowData("7", "300"))))},"entry":${Json { encodeDefaults = true }.encodeToString(TurnaroundEntry.serializer(), entry)}}"""
        val r = call("compositionCompute", args).jsonObject
        assertEquals(280, r["derivedAxles"]!!.jsonPrimitive.int)
        assertNotNull(r["required"])
        assertEquals("12:30", call("timeMask", """{"prev":"12:3","raw":"12:30"}""").jsonPrimitive.content)
        assertEquals(TimeOfDayInput.normalize("1230"), call("timeNormalize", """{"value":"1230"}""").jsonPrimitive.content)
    }

    @Test
    fun seriesPassportAndFill() {
        val p = call("passportFor", """{"name":"2ЭС6","user":null}""").jsonObject
        assertEquals(14.0, p["axleLoadTf"]!!.jsonPrimitive.double)
        assertEquals(2000, p["brakeVolumeL"]!!.jsonPrimitive.int)
        val f = call("resolveLoco", """{"routeSeries":"ВЛ80С","routeNumber":"1","curSeries":"","curNumber":"","manualGroup":null,"customVolume":""}""").jsonObject
        assertEquals(DensityLocoGroup.G4.name, f["effectiveGroup"]!!.jsonPrimitive.content)
    }

    @Test
    fun unknownFunctionFails() {
        assertFailsWith<IllegalArgumentException> { call("nope") }
        assertTrue(call("axleCheck", """{"declared":300,"tableSum":280,"locoAxles":0}""") is JsonObject)
    }
}
