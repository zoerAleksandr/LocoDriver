@file:OptIn(ExperimentalJsExport::class)

package com.z_company.domain.brake

import com.z_company.domain.entities.norma_time.LocomotiveSeries
import com.z_company.domain.entities.route.Station
import com.z_company.domain.util.toFiniteDoubleOrNull
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/**
 * Мост PWA к расчётам справки ВУ-45: те же формулы, что и на Android (общий код domain/brake), без переписывания на JS.
 *
 * Все вызовы - JSON в JSON: `call(name, argsJson)` возвращает JSON-строку. Формат аргументов и ответов - в [dispatch];
 * общие справочники (категории, группы ГР, нормы) - в [catalogJson]. Состояние справки передаётся как
 * [BrakeCertificateData] (тот же JSON, что хранится и уходит на сервер). Время - мс эпохи UTC, часовой пояс - смещение в минутах.
 */
@JsExport
object PwaBrakeBridge {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = true; coerceInputValues = true }

    /** Справочные данные, не зависящие от состояния: категории, ступени уклона, группы ГР, нормы, каталог локомотивов. */
    fun catalogJson(): String = json.encodeToString(JsonElement.serializer(), catalog())

    /** Вызов функции по имени; ошибка аргументов - исключение (PWA ловит и показывает «не удалось посчитать»). */
    fun call(name: String, argsJson: String): String {
        val args = if (argsJson.isBlank()) JsonObject(emptyMap()) else json.parseToJsonElement(argsJson).jsonObject
        return json.encodeToString(JsonElement.serializer(), dispatch(name, args))
    }

    // ───────────────────────── справочники ─────────────────────────

    private fun catalog(): JsonElement = buildJsonObject {
        put("categories", buildJsonArray { BrakeNorms.categories.forEach { add(category(it)) } })
        put("slopeSteps", buildJsonArray {
            SlopeStep.values().forEach {
                add(buildJsonObject {
                    put("name", it.name); put("slopeText", it.slopeText); put("grade", it.grade.name); put("gradeTitle", it.grade.title); put("table6", it.table6)
                })
            }
        })
        put("grades", buildJsonArray { GradeClass.values().forEach { add(buildJsonObject { put("name", it.name); put("title", it.title) }) } })
        put("manualNorms", buildJsonArray {
            BrakeNorms.ManualBrakeNorm.values().forEach { add(buildJsonObject { put("name", it.name); put("perHundred", it.perHundred); put("title", it.title) }) }
        })
        put("axlePresets", buildJsonArray { BrakeNorms.axlePresets.forEach { add(buildJsonObject { put("title", it.title); put("pressure", it.pressure) }) } })
        put("blankAxlePressures", buildJsonArray { BrakeNorms.blankAxlePressures.forEach { add(JsonPrimitive(it)) } })
        put("axleLoadCategoryIds", buildJsonArray { BrakeCategory.axleLoadCategoryIds.sorted().forEach { add(JsonPrimitive(it)) } })
        put("compositeShares", buildJsonArray { CompositeShare.values().forEach { add(JsonPrimitive(it.name)) } })
        put("groups", buildJsonArray {
            DensityLocoGroup.values().forEach { g ->
                add(buildJsonObject {
                    put("name", g.name); put("liters", g.liters); put("title", g.title); put("examplesText", g.examplesText)
                    put("minTimes", buildJsonArray { g.minTimes.forEach { add(JsonPrimitive(it)) } })
                    put("examples", buildJsonArray {
                        g.examples.forEach { e ->
                            add(buildJsonObject {
                                put("series", e.series); put("volume", e.volume); put("note", e.note); put("auto", e.auto); put("show", e.show)
                            })
                        }
                    })
                })
            }
        })
        put("axleBounds", buildJsonArray { BrakeDensity.axleBounds.forEach { add(JsonPrimitive(it)) } })
        put("testKinds", buildJsonArray { TurnaroundEntry.TEST_KINDS.forEach { add(JsonPrimitive(it)) } })
        put("constants", buildJsonObject {
            put("vl10First1500Number", BrakeDensity.VL10_FIRST_1500_NUMBER)
            put("loadedAxleLoadThreshold", BrakeCategory.LOADED_AXLE_LOAD_THRESHOLD)
            put("unifiedManual", BrakeNorms.UNIFIED_MANUAL)
            put("over5Ms", TurnaroundPlanner.OVER_5_MS)
            put("over30Ms", TurnaroundPlanner.OVER_30_MS)
            put("newEntryTarget", TurnaroundTarget.NEW_ENTRY)
            put("defaultTestKind", TurnaroundEntry.DEFAULT_TEST_KIND)
            put("kindFull", TurnaroundEntry.KIND_FULL)
            put("kindShort", TurnaroundEntry.KIND_SHORT)
        })
        put("locoCatalog", buildJsonArray {
            BrakeLocoCatalog.items.forEach {
                add(buildJsonObject {
                    put("series", it.series); put("weight", it.weight); put("axles", it.axles); put("pressurePerAxle", it.pressurePerAxle)
                    put("note", it.note); put("weightAxlesConfirmed", it.weightAxlesConfirmed)
                })
            }
        })
    }

    // ───────────────────────── диспетчер ─────────────────────────

    private fun dispatch(name: String, a: JsonObject): JsonElement = when (name) {
        // Производные величины справки: всё, что рисуют «Бланк» и «Расчёт».
        "evaluate" -> evaluate(data(a), a.long("nowMs"))
        // Каноническая форма справки: все поля со значениями по умолчанию (как хранит и отправляет Android).
        "normalizeData" -> json.encodeToJsonElement(BrakeCertificateData.serializer(), data(a))
        "calculate" -> BrakeCertificateEngine.calculate(data(a))?.let { result(it) } ?: JsonNull
        "locoRow" -> locoRow(data(a))
        "axleCheck" -> axleCheck(a.intOrNull("declared"), a.int("tableSum") ?: 0, a.int("locoAxles") ?: 0)

        // Категории и нормы
        "category" -> BrakeNorms.category(a.str("id") ?: "")?.let { category(it) } ?: JsonNull
        "categoryForAxles" -> {
            val cur = BrakeNorms.category(a.str("currentId") ?: "") ?: return JsonNull
            category(BrakeNorms.categoryForAxles(cur, a.int("axles")))
        }
        "manualInfo" -> manualInfo(slope(a.str("slope")), a.str("override")?.let { runCatching { BrakeNorms.ManualBrakeNorm.valueOf(it) }.getOrNull() })
        "parseTrainNumber" -> BrakeCategory.parseTrainNumber(a.str("raw"))?.let { JsonPrimitive(it) } ?: JsonNull
        "byTrainNumber" -> BrakeCategory.byTrainNumber(a.str("number"), a.int("axles"))?.let { category(it) } ?: JsonNull
        "suggestByTrainNumber" -> BrakeCategory.suggestByTrainNumber(a.str("number"), a.int("axles"), a.str("currentId") ?: "").let {
            buildJsonObject { put("categoryId", it.categoryId); put("note", it.note) }
        }
        "suggestByAxleLoad" -> axleLoadSuggestion(
            BrakeCategory.suggestByAxleLoad(a.str("currentId") ?: "", a.bool("manual"), a.bool("numberDetermines"), a.dbl("weightT"), a.int("axles")),
        )
        "isPassengerNumber" -> JsonPrimitive(BrakeCategory.isPassengerNumber(a.int("number") ?: 0))

        // Плотность и группы ГР
        "groupBySeries" -> group(BrakeDensity.groupBySeries(a.str("series")))
        "groupForLoco" -> group(BrakeDensity.groupForLoco(a.str("series"), a.str("number")))
        "parseLocoNumber" -> BrakeDensity.parseLocoNumber(a.str("raw"))?.let { JsonPrimitive(it) } ?: JsonNull
        "groupByVolume" -> volumeGroup(BrakeDensity.groupByVolume(a.str("text")))
        "volumeCaption" -> JsonPrimitive(BrakeDensity.volumeCaption(a.str("text")))
        "volumeFieldText" -> JsonPrimitive(BrakeDensity.volumeFieldText(a.str("customVolume") ?: "", groupOrNull(a.str("group")), a.bool("manuallySelected")))
        "normLines" -> buildJsonArray {
            BrakeDensity.normLines(groupOrNull(a.str("group")), a.int("axles") ?: 0, a.dbl("chargePressure"), a.int("locoCount") ?: 1).forEach { add(JsonPrimitive(it)) }
        }
        "minTimeSeconds" -> groupOrNull(a.str("group"))?.let { g ->
            BrakeDensity.minTimeSeconds(g, a.int("axles") ?: 0, a.dbl("chargePressure"), a.int("locoCount") ?: 1)?.let { JsonPrimitive(it) }
        } ?: JsonNull
        "normIvSeconds" -> JsonPrimitive(BrakeDensity.normIvSeconds(a.dbl("normII") ?: 0.0))
        "ceilTenth" -> JsonPrimitive(BrakeDensity.ceilTenth(a.dbl("value") ?: 0.0))
        "formatNormSeconds" -> JsonPrimitive(BrakeDensity.formatNormSeconds(a.dbl("value") ?: 0.0))
        "chargeFactor" -> JsonPrimitive(BrakeDensity.chargeFactor(a.dbl("chargePressure")))
        "axleColumn" -> BrakeDensity.axleColumn(a.int("axles") ?: 0)?.let { JsonPrimitive(it) } ?: JsonNull
        "commitDensity" -> when (val c = BrakeDensity.commitDensity(a.str("text") ?: "", a.str("initialText") ?: "", a.bool("cleared"))) {
            BrakeDensity.DensityCommit.Keep -> buildJsonObject { put("kind", "KEEP") }
            BrakeDensity.DensityCommit.Clear -> buildJsonObject { put("kind", "CLEAR") }
            is BrakeDensity.DensityCommit.Set -> buildJsonObject { put("kind", "SET"); put("seconds", c.seconds) }
        }
        "checkPressure" -> BrakeDensity.checkPressure(a.str("kind") ?: "", a.str("start") ?: "", a.str("end") ?: "", a.bool("passenger"))?.let {
            buildJsonObject { put("diff", it.diff); put("ok", it.ok); put("text", it.text) }
        } ?: JsonNull
        "parseManualSeconds" -> BrakeDensity.parseManualSeconds(a.str("text") ?: "")?.let { JsonPrimitive(it) } ?: JsonNull
        "assessFreightII" -> assessment(BrakeDensity.assessFreightII(a.dbl("timeSec") ?: 0.0, a.dbl("minTime"), a.boolOr("groupSelected", true)))
        "assessFreightIV" -> assessment(BrakeDensity.assessFreightIV(a.dbl("timeIVSec") ?: 0.0, a.dbl("timeIISec")))
        "assessRoute" -> assessment(BrakeDensity.assessRoute(a.dbl("timeSec") ?: 0.0, a.dbl("timeIISec")))
        "assessPassenger" -> assessment(BrakeDensity.assessPassenger(a.dbl("timeSec") ?: 0.0, a.dbl("dropKgf")))
        "minTailPressure" -> BrakeDensity.minTailPressure(a.dbl("chargePressure"), a.int("axles") ?: 0)?.let { JsonPrimitive(it) } ?: JsonNull

        // Оборот
        "turnaroundApplies" -> JsonPrimitive(BrakeNorms.category(a.str("categoryId") ?: "")?.let { TurnaroundPlanner.applies(it) } ?: false)
        "turnaroundCandidates" -> buildJsonArray {
            val stations = (a["stations"] as? JsonArray).orEmpty().map { s ->
                val o = s.jsonObject
                Station(
                    stationId = o.str("stationId") ?: "", stationName = o.str("stationName"), timeArrival = o.long("timeArrival"),
                    timeDeparture = o.long("timeDeparture"), orderIndex = o.int("orderIndex") ?: 0,
                    isFinalStation = o.bool("isFinalStation"), isPassingStation = o.bool("isPassingStation"),
                )
            }
            TurnaroundPlanner.candidates(stations, a.long("nowMs") ?: 0L).forEach { c ->
                add(buildJsonObject {
                    put("stationId", c.stationId); put("stationName", c.stationName); put("arrivalMs", c.arrivalMs); put("departureMs", c.departureMs)
                    put("dwellMs", c.dwellMs); put("stopClass", c.stopClass.name); put("ongoing", c.ongoing)
                })
            }
        }
        "classifyDwell" -> TurnaroundPlanner.classify(a.long("dwellMs") ?: 0L)?.let { JsonPrimitive(it.name) } ?: JsonNull
        "turnaroundPick" -> when (val p = TurnaroundTarget.pick(entries(a), a.str("targetId"), a.long("nowMs") ?: 0L, a.boolOr("applies", true))) {
            is TurnaroundPick.Existing -> buildJsonObject { put("kind", "EXISTING"); put("id", p.id) }
            TurnaroundPick.CreateNew -> buildJsonObject { put("kind", "CREATE_NEW") }
            TurnaroundPick.NotApplicable -> buildJsonObject { put("kind", "NOT_APPLICABLE") }
        }
        "turnaroundDensityCheck" -> TurnaroundDensityCheck.check(a.dbl("densitySec") ?: 0.0, a.dbl("referenceIISec")).let {
            buildJsonObject { put("verdict", it.verdict.name); put("deviationPct", it.deviationPct); put("lo", it.lo); put("hi", it.hi) }
        }
        "turnaroundReference" -> {
            val offset = (a.int("tzOffsetMinutes") ?: 0)
            val info = TurnaroundReference.resolve(entries(a), a.dbl("iiSec")) { ms -> (((ms / 60_000L + offset) % 1440 + 1440) % 1440).toInt() }
            buildJsonObject {
                info.forEach { (id, i) ->
                    put(id, buildJsonObject {
                        put("becomesReference", i.becomesReference)
                        put("ref", i.ref?.let { r -> buildJsonObject { put("seconds", r.seconds); put("kind", r.kind); put("time", r.time); put("label", r.label) } } ?: JsonNull)
                    })
                }
            }
        }
        "entryInfo" -> entryInfo(json.decodeFromJsonElement(TurnaroundEntry.serializer(), a["entry"]!!))
        "wagonChangeInfo" -> json.decodeFromJsonElement(WagonChange.serializer(), a["change"]!!).let {
            buildJsonObject { put("pressure", it.pressure); put("axles", it.axles); put("isValid", it.isValid) }
        }
        "compositionCompute" -> compositionResult(compositionOf(data(a), entryOf(a, "entry")))
        "compositionAutoFill" -> {
            val d = data(a)
            val prevEntry = entryOf(a, "prevEntry")!!
            val nextEntry = entryOf(a, "nextEntry")!!
            val filled = TurnaroundComposition.autoFill(prevEntry, nextEntry, compositionOf(d, prevEntry), compositionOf(d, nextEntry))
            json.encodeToJsonElement(TurnaroundEntry.serializer(), filled)
        }
        "compositionIsManual" -> JsonPrimitive(TurnaroundComposition.isManual(a.str("entered") ?: "", a.str("calculated")))
        "fmt1" -> JsonPrimitive(TurnaroundComposition.fmt1(a.dbl("value") ?: 0.0))

        // Ввод времени суток
        "timeFilter" -> JsonPrimitive(TimeOfDayInput.filter(a.str("raw") ?: ""))
        "timeMask" -> JsonPrimitive(TimeOfDayInput.mask(a.str("prev") ?: "", a.str("raw") ?: ""))
        "timeCommittable" -> TimeOfDayInput.committable(a.str("masked") ?: "")?.let { JsonPrimitive(it) } ?: JsonNull
        "timeNormalize" -> TimeOfDayInput.normalize(a.str("value") ?: "")?.let { JsonPrimitive(it) } ?: JsonNull
        "timeDisplay" -> JsonPrimitive(TimeOfDayInput.display(a.str("stored") ?: ""))

        // Подстановка и справочник серий
        "resolveLoco" -> BrakeFillResolver.resolveLoco(
            a.str("routeSeries"), a.str("routeNumber"), a.str("curSeries") ?: "", a.str("curNumber") ?: "",
            groupOrNull(a.str("manualGroup")), a.str("customVolume") ?: "",
        ).let {
            buildJsonObject {
                put("series", it.series); put("number", it.number); put("manualGroup", it.manualGroup?.name); put("customVolume", it.customVolume)
                put("effectiveGroup", it.effectiveGroup?.name); put("groupReset", it.groupReset)
            }
        }
        "passportFor" -> LocoSeriesDefaults.passportFor(a.str("name"), a["user"]?.takeIf { it !is JsonNull }?.let { json.decodeFromJsonElement(LocomotiveSeries.serializer(), it) }).let {
            buildJsonObject {
                put("weightT", it.weightT); put("axleCount", it.axleCount); put("axleLoadTf", it.axleLoadTf); put("brakeVolumeL", it.brakeVolumeL); put("note", it.note)
            }
        }
        "recordWithEdits" -> {
            val existing = a["existing"]?.takeIf { it !is JsonNull }?.let { json.decodeFromJsonElement(LocomotiveSeries.serializer(), it) }
            val e = a["edited"]!!.jsonObject
            val fields = (a["fields"] as? JsonArray).orEmpty().map { SeriesPassportField.valueOf(it.jsonPrimitive.content) }.toSet()
            val edited = LocoPassport(weightT = e.dbl("weightT"), axleCount = e.int("axleCount"), axleLoadTf = e.dbl("axleLoadTf"), brakeVolumeL = e.int("brakeVolumeL"))
            LocoSeriesDefaults.recordWithEdits(a.str("name") ?: "", existing, edited, fields, a.long("now") ?: 0L)
                ?.let { json.encodeToJsonElement(LocomotiveSeries.serializer(), it) } ?: JsonNull
        }
        "seriesDefaults" -> LocoSeriesDefaults.forSeries(a.str("name")).let {
            buildJsonObject {
                put("sectionCount", it.sectionCount); put("brakeVolumeL", it.brakeVolumeL); put("axleCount", it.axleCount)
                put("axleLoadTf", it.axleLoadTf); put("weightT", it.weightT)
            }
        }
        "locoCatalogFind" -> BrakeLocoCatalog.find(a.str("series"))?.let {
            buildJsonObject {
                put("series", it.series); put("weight", it.weight); put("axles", it.axles); put("pressurePerAxle", it.pressurePerAxle)
                put("note", it.note); put("weightAxlesConfirmed", it.weightAxlesConfirmed)
            }
        } ?: JsonNull
        "stationsMerge" -> buildJsonArray { BrakeStations.merge(strings(a, "train"), strings(a, "others")).forEach { add(JsonPrimitive(it)) } }
        "stationsFilter" -> buildJsonArray { BrakeStations.filter(strings(a, "all"), a.str("query") ?: "").forEach { add(JsonPrimitive(it)) } }
        "stationsIsExact" -> JsonPrimitive(BrakeStations.isExact(strings(a, "all"), a.str("query") ?: ""))
        "annotateSource" -> JsonPrimitive(InstructionRef.annotate(a.str("text") ?: ""))
        else -> throw IllegalArgumentException("Unknown brake bridge function: $name")
    }

    // ───────────────────────── evaluate ─────────────────────────

    private fun evaluate(d: BrakeCertificateData, nowMs: Long?): JsonElement {
        val input = BrakeCertificateEngine.input(d)
        val res = input?.let { BrakeCertificateCalculator.calculate(it) }
        val rows = BrakeCertificateEngine.effectiveRows(d)
        val group = BrakeCertificateEngine.effectiveDensityGroup(d)
        val status = BrakeStatusResolver.resolve(res, input != null && input.rows.isNotEmpty())
        val info = BrakeNorms.manualInfo(d.slope, d.manualNorm)
        return buildJsonObject {
            put("hasInput", input != null)
            put("result", res?.let { result(it) } ?: JsonNull)
            put("status", buildJsonObject {
                put("level", status.level.name); put("headline", status.headline)
                put("calculated", BrakeStatusResolver.isCalculated(res)); put("speedKmh", BrakeStatusResolver.speedKmh(res))
            })
            put("effectiveRows", buildJsonArray { rows.forEach { add(buildJsonObject { put("pressure", it.pressure); put("axles", it.axles) }) } })
            put("pressureField", when (val p = BrakeCertificateEngine.pressureField(d)) {
                PressureFieldState.Empty -> buildJsonObject { put("kind", "EMPTY") }
                PressureFieldState.Multiple -> buildJsonObject { put("kind", "MULTIPLE") }
                is PressureFieldState.Single -> buildJsonObject { put("kind", "SINGLE"); put("text", p.text) }
            })
            put("effectiveGroup", group(group))
            put("manualInfo", manualInfo(d.slope, d.manualNorm))
            put("manualFieldText", BrakeNorms.manualFieldText(info))
            put("locoRow", locoRow(d))
            put("category", BrakeNorms.category(d.categoryId)?.let { category(it) } ?: JsonNull)
            put("turnaroundApplies", BrakeNorms.category(d.categoryId)?.let { TurnaroundPlanner.applies(it) } ?: false)
        }
    }

    // Нажатие/оси строки локомотива в таблице бланка (слияние с напечатанной шкалой).
    private fun locoRow(d: BrakeCertificateData): JsonElement {
        val input = BrakeCertificateEngine.input(d)
        val rows = input?.rows ?: emptyList()
        val r = BrakeTable.locoRow(rows, input?.loco, BrakeNorms.blankAxlePressures)
        return buildJsonObject {
            put("mergedPressure", r.mergedPressure); put("separateRow", r.separateRow); put("totalAxles", r.totalAxles); put("totalPressure", r.totalPressure)
        }
    }

    private fun axleCheck(declared: Int?, tableSum: Int, locoAxles: Int): JsonElement {
        val c = AxleCheck.compareWithLoco(declared, tableSum, locoAxles)
        return buildJsonObject {
            put("kind", c::class.simpleName)
            put("message", AxleCheck.message(c))
            when (c) {
                is AxleSumCheck.DeclaredEmpty -> put("tableSum", c.tableSum)
                is AxleSumCheck.Equal -> put("total", c.total)
                is AxleSumCheck.TableLess -> { put("tableSum", c.tableSum); put("declared", c.declared); put("diff", c.diff) }
                is AxleSumCheck.TableMore -> { put("tableSum", c.tableSum); put("declared", c.declared); put("diff", c.diff) }
                AxleSumCheck.NoTable -> Unit
            }
        }
    }

    // ───────────────────────── DTO ─────────────────────────

    private fun category(c: BrakeCategory): JsonElement = buildJsonObject {
        put("id", c.id); put("tableRow", c.tableRow); put("title", c.title); put("group", c.group.name); put("norm", c.norm)
        put("vFlat", c.vFlat); put("vFlatAutoblock", c.vFlatAutoblock); put("vSteep", c.vSteep); put("minNorm", c.minNorm)
        put("minSpeeds", buildJsonArray { c.minSpeeds.forEach { add(it?.let { v -> JsonPrimitive(v) } ?: JsonNull) } })
        put("locoCountedByDefault", c.locoCountedByDefault); put("manualBrakesRequired", c.manualBrakesRequired); put("isLoadedFreight", c.isLoadedFreight)
    }

    private fun manualInfo(slope: SlopeStep, override: BrakeNorms.ManualBrakeNorm?): JsonElement = BrakeNorms.manualInfo(slope, override).let {
        buildJsonObject { put("perHundred", it.perHundred); put("text", it.text); put("auto", it.auto); put("fieldText", BrakeNorms.manualFieldText(it)) }
    }

    private fun axleLoadSuggestion(s: BrakeCategory.Companion.AxleLoadSuggestion?): JsonElement = s?.let {
        buildJsonObject { put("categoryId", it.categoryId); put("loadPerAxle", it.loadPerAxle); put("loaded", it.loaded); put("note", it.note) }
    } ?: JsonNull

    private fun group(g: DensityLocoGroup?): JsonElement = g?.let { buildJsonObject { put("name", it.name); put("liters", it.liters); put("title", it.title) } } ?: JsonNull

    private fun volumeGroup(v: BrakeDensity.VolumeGroup): JsonElement = when (v) {
        is BrakeDensity.VolumeGroup.Found -> buildJsonObject { put("kind", "FOUND"); put("liters", v.liters); put("group", group(v.group)) }
        is BrakeDensity.VolumeGroup.BelowTable -> buildJsonObject { put("kind", "BELOW_TABLE"); put("liters", v.liters) }
        BrakeDensity.VolumeGroup.Invalid -> buildJsonObject { put("kind", "INVALID") }
    }

    private fun assessment(a: BrakeDensity.Assessment): JsonElement = buildJsonObject { put("verdict", a.verdict.name); put("text", a.text) }

    private fun result(r: BrakeCertificateResult): JsonElement = buildJsonObject {
        put("totalWeight", r.totalWeight); put("requiredPressure", r.requiredPressure); put("actualPressure", r.actualPressure)
        put("actualPer100", r.actualPer100); put("enough", r.enough); put("shortagePer100", r.shortagePer100); put("totalAxles", r.totalAxles)
        put("speed", when (val s = r.speed) {
            is SpeedLimit.Allowed -> buildJsonObject {
                put("type", "ALLOWED"); put("kmh", s.kmh); put("note", s.note); put("reducedFrom", s.reducedFrom); put("steps", s.steps)
                put("conditional", s.conditional); put("condition", s.condition)
            }
            is SpeedLimit.NotDetermined -> buildJsonObject { put("type", "NOT_DETERMINED"); put("reason", s.reason); put("kind", s.kind.name) }
        })
        put("requiredManualAxles", r.requiredManualAxles); put("manualAxlesOk", r.manualAxlesOk)
        put("warnings", buildJsonArray { r.warnings.forEach { add(JsonPrimitive(it)) } })
        put("loco", r.loco?.let { l -> buildJsonObject { put("axles", l.axles); put("pressureTc", l.pressureTc); put("weight", l.weight) } } ?: JsonNull)
    }

    private fun entryInfo(e: TurnaroundEntry): JsonElement = buildJsonObject {
        put("compositionChanged", e.compositionChanged); put("compositionSummary", e.compositionSummary); put("testKindShort", e.testKindShort)
    }

    private fun compositionOf(d: BrakeCertificateData, e: TurnaroundEntry?): TurnaroundComposition.Result? =
        e?.let { TurnaroundComposition.compute(BrakeCertificateEngine.input(d), it.newWeight.toFiniteDoubleOrNull(), it.newAxles.trim().toIntOrNull(), it.changes) }

    private fun compositionResult(r: TurnaroundComposition.Result?): JsonElement = r?.let {
        buildJsonObject {
            put("category", category(it.category)); put("normBefore", it.normBefore); put("normAfter", it.normAfter); put("normChanged", it.normChanged)
            put("totalWeight", it.totalWeight); put("required", it.required); put("frontActual", it.frontActual); put("frontAxles", it.frontAxles)
            put("derivedAxles", it.derivedAxles); put("actual", it.actual); put("attachedTc", it.attachedTc); put("detachedTc", it.detachedTc)
            put("requiredManualAxles", it.requiredManualAxles); put("manualAxlesBefore", it.manualAxlesBefore)
            put("warnings", buildJsonArray { it.warnings.forEach { w -> add(JsonPrimitive(w)) } })
            put("requiredText", it.requiredText); put("actualText", it.actualText); put("axlesText", it.axlesText)
        }
    } ?: JsonNull

    // ───────────────────────── разбор аргументов ─────────────────────────

    private fun data(a: JsonObject): BrakeCertificateData = json.decodeFromJsonElement(BrakeCertificateData.serializer(), a["data"]!!)

    private fun entries(a: JsonObject): List<TurnaroundEntry> =
        (a["entries"] as? JsonArray).orEmpty().map { json.decodeFromJsonElement(TurnaroundEntry.serializer(), it) }

    private fun entryOf(a: JsonObject, key: String): TurnaroundEntry? =
        a[key]?.takeIf { it !is JsonNull }?.let { json.decodeFromJsonElement(TurnaroundEntry.serializer(), it) }

    private fun strings(a: JsonObject, key: String): List<String> = (a[key] as? JsonArray).orEmpty().map { it.jsonPrimitive.content }

    private fun slope(name: String?): SlopeStep = SlopeStep.values().firstOrNull { it.name == name } ?: SlopeStep.S006

    private fun groupOrNull(name: String?): DensityLocoGroup? = DensityLocoGroup.values().firstOrNull { it.name == name }

    private fun JsonObject.prim(key: String): JsonPrimitive? = (this[key] as? JsonPrimitive)?.takeIf { it !is JsonNull }
    private fun JsonObject.str(key: String): String? = prim(key)?.contentOrNull
    private fun JsonObject.int(key: String): Int? = prim(key)?.let { it.intOrNull ?: it.doubleOrNull?.toInt() }
    private fun JsonObject.intOrNull(key: String): Int? = int(key)
    private fun JsonObject.long(key: String): Long? = prim(key)?.let { it.longOrNull ?: it.doubleOrNull?.toLong() }
    private fun JsonObject.dbl(key: String): Double? = prim(key)?.doubleOrNull
    private fun JsonObject.bool(key: String): Boolean = prim(key)?.booleanOrNull ?: false
    private fun JsonObject.boolOr(key: String, default: Boolean): Boolean = prim(key)?.booleanOrNull ?: default

    private fun <T> List<T>?.orEmpty(): List<T> = this ?: emptyList()
    private fun JsonArray?.orEmpty(): List<JsonElement> = this?.toList() ?: emptyList()
}
