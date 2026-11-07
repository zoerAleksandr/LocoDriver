@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.domain.entities.FilterNames
import com.z_company.domain.entities.FilterSearch
import com.z_company.domain.entities.RouteWithTag
import com.z_company.domain.entities.SearchStateScreen
import com.z_company.domain.entities.SearchTag
import com.z_company.domain.entities.TimePeriod
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.SearchResponse
import com.z_company.domain.entities.route.UtilsForEntities.inTimePeriod
import com.z_company.domain.repositories.HistoryResponseRepository
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.util.displayTimeZone
import com.z_company.domain.util.safetySubList
import com.z_company.domain.util.splitBySpaceAndComma
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// ═════════════════════════════════════════════════════════════════════════════
// Модели поиска (§10).
// ═════════════════════════════════════════════════════════════════════════════

data class SearchChipUi(
    val key: String,
    val label: String,
    val isEnabled: Boolean,
)

/** Диапазон подсветки токена в [SearchResultUi.text] (UTF-16, для Swift NSRange). */
data class MatchRangeUi(
    val start: Int,
    val length: Int,
)

data class SearchResultUi(
    val basicId: String,
    /** «№123» или «Маршрут». */
    val routeNumberText: String,
    /** Дата явки `dd.MM.yy`. */
    val dateText: String,
    /** «ОСНОВНЫЕ ДАННЫЕ», «ЛОКОМОТИВ», … */
    val tagLabel: String,
    /** Текст всех сущностей раздела (формат EntityString), через пустую строку. */
    val text: String,
    val matchRanges: List<MatchRangeUi>,
)

data class SearchIosUi(
    val query: String = "",
    val isPreliminary: Boolean = false,
    val isVisibleHistory: Boolean = true,
    val isVisibleHints: Boolean = false,
    val isVisibleResult: Boolean = false,
    val isLoading: Boolean = false,
    val suggestions: List<String> = emptyList(),
    val results: List<SearchResultUi> = emptyList(),
    /** «НЕДАВНИЕ ЗАПРОСЫ», новые сверху. */
    val history: List<String> = emptyList(),
    val chips: List<SearchChipUi> = emptyList(),
    /** «с dd.MM.yy» / «по dd.MM.yy»; пусто — без даты. */
    val periodStartText: String = "",
    val periodEndText: String = "",
)

// ═════════════════════════════════════════════════════════════════════════════
// Поисковый индекс — порт `data_local/SearchRouteUseCase` (androidMain,
// в iOS-таргет не входит). Семантика совпадения/ранжирования/подсказок — 1:1.
// ═════════════════════════════════════════════════════════════════════════════

private class RouteSearchEntry(
    val route: Route,
    val timeStartWork: Long?,
    val basicMatch: String?,
    val locoMatch: List<String>,
    val trainMatch: List<String>,
    val passengerMatch: List<String>,
    val otherWorkMatch: List<String>,
    val partnerMatch: List<String>,
    val notesMatch: String?,
    val keyValues: Set<String>,
    val vocabulary: List<String>,
)

private class Scored(
    val tagged: RouteWithTag,
    val entry: RouteSearchEntry,
    val score: Int,
)

private fun buildSearchIndex(routes: List<Route>, tz: TimeZone): List<RouteSearchEntry> =
    routes.map { route -> buildSearchEntry(route, tz) }

private fun searchDateAndTime(value: Long?, tz: TimeZone): String {
    if (value == null) return ""
    if (value == 0L) return "Нет данных"
    return Fmt.dateTimeShort(value, tz)
}

private fun buildSearchEntry(route: Route, tz: TimeZone): RouteSearchEntry {
    val keys = LinkedHashSet<String>()
    fun key(v: String?) {
        val t = v?.trim()
        if (!t.isNullOrEmpty()) keys.add(t)
    }

    // ---- Основные данные ----
    val basicParts = ArrayList<String>()
    route.basicData.number?.let { basicParts += it; key(it) }
    route.basicData.timeStartWork?.let { basicParts += searchDateAndTime(it, tz) }
    route.basicData.timeEndWork?.let { basicParts += searchDateAndTime(it, tz) }
    val basicMatch = basicParts.takeIf { it.isNotEmpty() }?.joinToString(" ")?.lowercase()

    // ---- Локомотивы ----
    val locoMatch = route.locomotives.map { loco ->
        val p = ArrayList<String>()
        p += locoTypeWord(loco.type)
        key(loco.series); loco.series?.let { p += it }
        key(loco.number); loco.number?.let { p += it }
        loco.normaDiesel?.let { p += it }
        loco.normaElectricCurrent1?.let { p += it.toString() }
        loco.normaElectricCurrent2?.let { p += it.toString() }
        loco.heatingCounterAccepted?.let { p += it.toString() }
        loco.heatingCounterDelivery?.let { p += it.toString() }
        loco.auxiliaryCounterAccepted?.let { p += it.toString() }
        loco.auxiliaryCounterDelivery?.let { p += it.toString() }
        loco.timeStartOfAcceptance?.let { p += searchDateAndTime(it, tz) }
        loco.timeEndOfAcceptance?.let { p += searchDateAndTime(it, tz) }
        loco.timeStartOfDelivery?.let { p += searchDateAndTime(it, tz) }
        loco.timeEndOfDelivery?.let { p += searchDateAndTime(it, tz) }
        loco.timeBarrierOut?.let { p += searchDateAndTime(it, tz) }
        loco.timeBarrierIn?.let { p += searchDateAndTime(it, tz) }
        loco.dieselSectionList.forEach { s ->
            s.acceptedFuel?.let { p += it.toString() }
            s.deliveryFuel?.let { p += it.toString() }
            s.coefficient?.let { p += it.toString() }
            s.fuelSupply?.let { p += it.toString() }
            s.fuelSupplyInKilo?.let { p += it.toString() }
            s.coefficientSupply?.let { p += it.toString() }
        }
        loco.electricSectionList.forEach { s ->
            s.acceptedEnergy?.let { p += it.toString() }
            s.deliveryEnergy?.let { p += it.toString() }
            s.acceptedRecovery?.let { p += it.toString() }
            s.deliveryRecovery?.let { p += it.toString() }
            s.acceptedEnergyOtherCurrent?.let { p += it.toString() }
            s.deliveryEnergyOtherCurrent?.let { p += it.toString() }
            s.acceptedRecoveryOtherCurrent?.let { p += it.toString() }
            s.deliveryRecoveryOtherCurrent?.let { p += it.toString() }
        }
        p.joinToString(" ").lowercase()
    }

    // ---- Поезда ----
    val trainMatch = route.trains.map { train ->
        val p = ArrayList<String>()
        p += "поезд"
        key(train.number); train.number?.let { p += it }
        train.additionalNumbers.forEach { num -> key(num); p += num }
        train.distance?.let { p += it }
        train.weight?.let { p += it }
        train.axle?.let { p += it }
        train.conditionalLength?.let { p += it }
        train.servicePhase?.let { sp ->
            key(sp.departureStation); p += sp.departureStation
            key(sp.arrivalStation); p += sp.arrivalStation
        }
        listOfNotNull(train.pusher, train.doubleTraction, train.doubledTrain).forEach { a ->
            key(a.locomotiveSeries); a.locomotiveSeries?.let { p += it }
            key(a.locomotiveNumber); a.locomotiveNumber?.let { p += it }
            key(a.driverName); a.driverName?.let { p += it }
            a.notes?.let { p += it }
        }
        train.carInspector?.let { ci ->
            key(ci.fullName); ci.fullName?.let { p += it }
            key(ci.tabNumber); ci.tabNumber?.let { p += it }
            ci.couplingTime?.let { p += searchDateAndTime(it, tz) }
        }
        train.stations.forEach { st ->
            key(st.stationName); st.stationName?.let { p += it }
            key(st.trackNumber); st.trackNumber?.let { p += it }
            key(st.segmentTrackNumber); st.segmentTrackNumber?.let { p += it }
            st.segmentNotes?.let { p += it }
            st.timeArrival?.let { p += searchDateAndTime(it, tz) }
            st.timeDeparture?.let { p += searchDateAndTime(it, tz) }
        }
        p.joinToString(" ").lowercase()
    }

    // ---- Следование пассажиром ----
    val passengerMatch = route.passengers.map { pass ->
        val p = ArrayList<String>()
        p += "пассажир"
        key(pass.trainNumber); pass.trainNumber?.let { p += it }
        key(pass.stationDeparture); pass.stationDeparture?.let { p += it }
        key(pass.stationArrival); pass.stationArrival?.let { p += it }
        pass.timeDeparture?.let { p += searchDateAndTime(it, tz) }
        pass.timeArrival?.let { p += searchDateAndTime(it, tz) }
        pass.notes?.let { p += it }
        p.joinToString(" ").lowercase()
    }

    // ---- Прочая работа ----
    val otherWorkMatch = route.otherWorks.map { work ->
        val p = ArrayList<String>()
        p += "прочая работа"
        key(work.workType); work.workType?.let { p += it }
        key(work.station); work.station?.let { p += it }
        work.timeStart?.let { p += searchDateAndTime(it, tz) }
        work.timeEnd?.let { p += searchDateAndTime(it, tz) }
        work.notes?.let { p += it }
        p.joinToString(" ").lowercase()
    }

    // ---- Напарники ----
    val partnerMatch = route.partners.map { partner ->
        val p = ArrayList<String>()
        p += "напарник"
        key(partner.fullName); partner.fullName?.let { p += it }
        key(partner.tabNumber); partner.tabNumber?.let { p += it }
        partner.notes?.let { p += it }
        p.joinToString(" ").lowercase()
    }

    val notesMatch = route.basicData.notes?.takeIf { it.isNotBlank() }?.lowercase()

    return RouteSearchEntry(
        route = route,
        timeStartWork = route.basicData.timeStartWork,
        basicMatch = basicMatch,
        locoMatch = locoMatch,
        trainMatch = trainMatch,
        passengerMatch = passengerMatch,
        otherWorkMatch = otherWorkMatch,
        partnerMatch = partnerMatch,
        notesMatch = notesMatch,
        keyValues = keys.mapTo(HashSet()) { it.lowercase() },
        vocabulary = keys.toList(),
    )
}

private fun locoTypeWord(type: LocoType): String = when (type) {
    LocoType.DIESEL -> "тепловоз"
    LocoType.ELECTRIC -> "электровоз"
}

private fun searchIndex(
    index: List<RouteSearchEntry>,
    value: String,
    filter: FilterSearch,
    preliminarySearch: Boolean,
): SearchStateScreen<List<RouteWithTag>> {
    val tokens = value.splitBySpaceAndComma()
        .map { it.trim().lowercase() }
        .filter { it.isNotEmpty() }

    if (tokens.isEmpty()) return SearchStateScreen.Success(emptyList())

    val matches = ArrayList<Scored>()

    index.forEach { entry ->
        if (!entry.route.inTimePeriod(filter.timePeriod)) return@forEach
        val score = rankOf(entry.keyValues, tokens)

        if (filter.generalData.second &&
            entry.basicMatch != null && containsAll(entry.basicMatch, tokens)
        ) {
            matches += Scored(RouteWithTag(SearchTag.BASIC_DATA, entry.route), entry, score)
        }
        if (filter.locoData.second && entry.locoMatch.any { containsAll(it, tokens) }) {
            matches += Scored(RouteWithTag(SearchTag.LOCO, entry.route), entry, score)
        }
        if (filter.trainData.second && entry.trainMatch.any { containsAll(it, tokens) }) {
            matches += Scored(RouteWithTag(SearchTag.TRAIN, entry.route), entry, score)
        }
        if (filter.passengerData.second && entry.passengerMatch.any { containsAll(it, tokens) }) {
            matches += Scored(RouteWithTag(SearchTag.PASSENGER, entry.route), entry, score)
        }
        if (filter.otherWorkData.second && entry.otherWorkMatch.any { containsAll(it, tokens) }) {
            matches += Scored(RouteWithTag(SearchTag.OTHER_WORK, entry.route), entry, score)
        }
        if (filter.partnerData.second && entry.partnerMatch.any { containsAll(it, tokens) }) {
            matches += Scored(RouteWithTag(SearchTag.PARTNER, entry.route), entry, score)
        }
        if (filter.notesData.second &&
            entry.notesMatch != null && containsAll(entry.notesMatch, tokens)
        ) {
            matches += Scored(RouteWithTag(SearchTag.NOTES, entry.route), entry, score)
        }
    }

    return if (preliminarySearch) {
        SearchStateScreen.Input(buildHints(matches, tokens))
    } else {
        val sorted = matches
            .sortedWith(
                compareByDescending<Scored> { it.score }
                    .thenByDescending { it.entry.timeStartWork ?: Long.MIN_VALUE }
            )
            .map { it.tagged }
        SearchStateScreen.Success(sorted)
    }
}

private fun containsAll(haystack: String, tokens: List<String>): Boolean {
    tokens.forEach { token ->
        if (!haystack.contains(token)) return false
    }
    return true
}

/** 3 — токен равен идентификатору, 2 — префикс, 1 — вхождение, 0 — только неключевые. */
private fun rankOf(keyValues: Set<String>, tokens: List<String>): Int {
    var best = 0
    tokens.forEach { t ->
        keyValues.forEach { k ->
            val s = when {
                k == t -> 3
                k.startsWith(t) -> 2
                k.contains(t) -> 1
                else -> 0
            }
            if (s > best) best = s
        }
    }
    return best
}

/** Подсказки: слова словаря, содержащие последний токен (не равные ему), без дублей. */
private fun buildHints(matches: List<Scored>, tokens: List<String>): List<String> {
    val lastToken = tokens.last()
    val hints = LinkedHashSet<String>()
    matches.forEach { m ->
        m.entry.vocabulary.forEach { word ->
            val lower = word.lowercase()
            if (lower != lastToken && lower.contains(lastToken)) {
                hints.add(word)
            }
        }
    }
    return hints.toList()
}

// ═════════════════════════════════════════════════════════════════════════════
// Текст сущностей результата — порт `core_android/EntityString` (Android-only).
// BUG-34 (паритет с Android): заголовок пассажира набран с латинскими «cc».
// ═════════════════════════════════════════════════════════════════════════════

private fun entityTexts(route: Route, tag: SearchTag, tz: TimeZone): List<String> =
    when (tag) {
        SearchTag.BASIC_DATA -> listOf(basicDataText(route, tz))
        SearchTag.LOCO -> route.locomotives.map { locomotiveText(it, tz) }
        SearchTag.TRAIN -> route.trains.map { trainText(it, tz) }
        SearchTag.PASSENGER -> route.passengers.map { passengerText(it, tz) }
        SearchTag.OTHER_WORK -> route.otherWorks.map { otherWorkText(it, tz) }
        SearchTag.PARTNER -> route.partners.map { partnerText(it) }
        SearchTag.NOTES -> listOf(route.basicData.notes.orEmpty())
    }

private fun basicDataText(route: Route, tz: TimeZone): String {
    val numberText = route.basicData.number?.let { "Маршрут №$it," } ?: ""
    val startWorkText = route.basicData.timeStartWork?.let {
        "явка ${searchDateAndTime(it, tz)},"
    } ?: ""
    val endWorkText = route.basicData.timeEndWork?.let {
        "сдача ${searchDateAndTime(it, tz)},"
    } ?: ""
    return "$numberText $startWorkText $endWorkText"
}

private fun locomotiveText(loco: com.z_company.domain.entities.route.Locomotive, tz: TimeZone): String {
    val text = StringBuilder()
    when (loco.type) {
        LocoType.DIESEL -> text.append("Тепловоз")
        LocoType.ELECTRIC -> text.append("Электровоз")
    }
    loco.series?.let { text.append(" $it") }
    loco.number?.let { text.append(" №$it") }
    if (loco.timeStartOfAcceptance != null || loco.timeEndOfAcceptance != null) {
        text.append("\nприемка")
    }
    loco.timeStartOfAcceptance?.let {
        text.append("\n${searchDateAndTime(it, tz)}")
    }
    loco.timeEndOfAcceptance?.let {
        text.append(" - ${searchDateAndTime(it, tz)}")
    }
    if (loco.timeStartOfDelivery != null || loco.timeEndOfDelivery != null) {
        text.append("\nсдача")
    }
    loco.timeStartOfDelivery?.let {
        text.append("\n${searchDateAndTime(it, tz)}")
    }
    loco.timeEndOfDelivery?.let {
        text.append(" - ${searchDateAndTime(it, tz)}")
    }
    loco.dieselSectionList.forEachIndexed { index, section ->
        text.append("\n • Cекция ${index + 1} ")
        text.append("\n${dieselSectionText(section)}")
    }
    loco.electricSectionList.forEachIndexed { index, section ->
        text.append("\n • Cекция ${index + 1} ")
        text.append("\n${electricSectionText(section)}")
    }
    return text.toString()
}

private fun dieselSectionText(section: com.z_company.domain.entities.route.SectionDiesel): String {
    val text = StringBuilder()
    if (section.acceptedFuel != null || section.deliveryFuel != null) {
        text.append("Топливо ")
        section.acceptedFuel?.let { text.append("\n$it") }
        section.deliveryFuel?.let { text.append(" - $it ") }
        section.coefficient?.let { text.append("\n k = $it ") }
    }
    section.fuelSupply?.let { supply ->
        text.append("\nЭкипировка")
        text.append("\n$supply л")
        section.coefficientSupply?.let { text.append(" k = $it") }
    }
    return text.toString()
}

private fun electricSectionText(section: com.z_company.domain.entities.route.SectionElectric): String {
    val text = StringBuilder()
    if (section.acceptedEnergy != null || section.deliveryEnergy != null) {
        text.append("Расход электроэнергии")
        section.acceptedEnergy?.let { text.append("\n$it") }
        section.deliveryEnergy?.let { text.append(" - $it ") }
    }
    if (section.acceptedRecovery != null || section.deliveryRecovery != null) {
        text.append("Рекуперация")
        section.acceptedRecovery?.let { text.append("\n$it") }
        section.deliveryRecovery?.let { text.append(" - $it ") }
    }
    return text.toString()
}

private fun trainText(train: com.z_company.domain.entities.route.Train, tz: TimeZone): String {
    val text = StringBuilder("Поезд")
    train.number?.let { text.append(" № $it,") }
    train.weight?.let { text.append(" вес $it,") }
    train.axle?.let { text.append(" оси $it,") }
    train.conditionalLength?.let { text.append(" уд $it,") }
    train.stations.forEach { station ->
        text.append(stationText(station, tz))
    }
    return text.toString()
}

private fun stationText(
    station: com.z_company.domain.entities.route.Station,
    tz: TimeZone,
): String {
    val text = StringBuilder()
    station.stationName?.let { name ->
        val timeArrival = station.timeArrival?.let { searchDateAndTime(it, tz) } ?: ""
        val timeDeparture = station.timeDeparture?.let {
            " - ${searchDateAndTime(it, tz)}"
        } ?: ""
        text.append("\n   • $name $timeArrival$timeDeparture")
    }
    return text.toString()
}

private fun otherWorkText(
    work: com.z_company.domain.entities.route.OtherWork,
    tz: TimeZone,
): String {
    val text = StringBuilder("Прочая работа")
    work.workType?.let { type -> if (type.isNotBlank()) text.append(". $type") }
    work.station?.let { station -> if (station.isNotBlank()) text.append("\n$station") }
    if (work.timeStart != null || work.timeEnd != null) {
        val start = work.timeStart?.let { searchDateAndTime(it, tz) } ?: ""
        val end = work.timeEnd?.let { " - ${searchDateAndTime(it, tz)}" } ?: ""
        text.append("\n$start$end")
    }
    work.notes?.let { notes -> if (notes.isNotBlank()) text.append("\n$notes") }
    return text.toString()
}

private fun passengerText(
    passenger: com.z_company.domain.entities.route.Passenger,
    tz: TimeZone,
): String {
    // BUG-34 (паритет с Android): «паccажиром» с латинскими «cc» — переносим как есть.
    val text = StringBuilder("Следование паccажиром.")
    passenger.trainNumber?.let { text.append("\nПоезд №$it ") }
    passenger.stationDeparture?.let { departure ->
        text.append("\n$departure")
        passenger.stationArrival?.let { arrival -> text.append(" - $arrival ") }
    }
    passenger.timeDeparture?.let { departure ->
        text.append("\n${searchDateAndTime(departure, tz)}")
        passenger.timeArrival?.let { arrival ->
            text.append(" - ${searchDateAndTime(arrival, tz)}")
        }
    }
    passenger.notes?.let { text.append("\n$it") }
    return text.toString()
}

private fun partnerText(partner: com.z_company.domain.entities.route.RoutePartner): String {
    val text = StringBuilder("Напарник")
    partner.fullName?.let { name -> if (name.isNotBlank()) text.append(". $name") }
    partner.tabNumber?.let { tab -> if (tab.isNotBlank()) text.append("\nТаб. № $tab") }
    partner.notes?.let { text.append("\n$it") }
    return text.toString()
}

private fun tagLabel(tag: SearchTag): String = when (tag) {
    SearchTag.BASIC_DATA -> "ОСНОВНЫЕ ДАННЫЕ"
    SearchTag.LOCO -> "ЛОКОМОТИВ"
    SearchTag.TRAIN -> "ПОЕЗД"
    SearchTag.PASSENGER -> "СЛЕДОВАНИЕ ПАССАЖИРОМ"
    SearchTag.OTHER_WORK -> "ПРОЧАЯ РАБОТА"
    SearchTag.PARTNER -> "НАПАРНИК"
    SearchTag.NOTES -> "ПРИМЕЧАНИЯ"
}

/** Диапазоны вхождений токенов (без пересечений, по порядку) для подсветки. */
private fun matchRanges(text: String, tokens: List<String>): List<MatchRangeUi> {
    val lower = text.lowercase()
    val ranges = mutableListOf<MatchRangeUi>()
    tokens.forEach { token ->
        if (token.isEmpty()) return@forEach
        var from = 0
        while (true) {
            val idx = lower.indexOf(token, from)
            if (idx < 0) break
            ranges.add(MatchRangeUi(start = idx, length = token.length))
            from = idx + token.length
        }
    }
    return ranges
        .sortedBy { it.start }
        .fold(mutableListOf<MatchRangeUi>()) { acc, r ->
            val last = acc.lastOrNull()
            if (last != null && r.start < last.start + last.length) acc else acc.apply { add(r) }
        }
}

// ═════════════════════════════════════════════════════════════════════════════
// ViewModel поиска — SCREEN_SPECS §10 (порт Android `SearchViewModel`).
// ═════════════════════════════════════════════════════════════════════════════

class SearchIosViewModel(
    private val routeUseCase: RouteUseCase,
    private val settingsUseCase: SettingsUseCase,
    private val historyRepository: HistoryResponseRepository,
) : ViewModel() {

    private var indexJob: Job? = null
    private var searchJob: Job? = null

    /** Готовый индекс: пересобирается при изменении маршрутов/настроек. */
    private var searchIndex: List<RouteSearchEntry> = emptyList()

    private val _ui = MutableStateFlow(SearchIosUi())

    /** Параметры (§10.3): in-memory, сбрасываются при каждом входе на экран. */
    private var searchFilter = FilterSearch()
    private var queryValue: String = ""
    private var indexTz: TimeZone = TimeZone.of("GMT+3")

    init {
        observeIndex()
        observeHistory()
        publishChips()
    }

    fun watchUi(callback: (SearchIosUi) -> Unit) {
        viewModelScope.launch { _ui.collect { callback(it) } }
    }

    // ── Ввод (§10.2) ───────────────────────────────────────────────────────

    /** Предварительный поиск с дебаунсом 200 мс → подсказки. */
    fun setQueryValue(value: String) {
        queryValue = value
        _ui.update { it.copy(query = value, isPreliminary = true) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            runSearch(queryValue)
        }
    }

    /** Немедленный полный поиск («Поиск», тап по подсказке/истории). */
    fun onSearch() {
        _ui.update { it.copy(isPreliminary = false) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch { runSearch(queryValue) }
        addHistory(queryValue)
    }

    /** Тап по подсказке (§10.2): замена/дополнение текста + отправка. */
    fun applySuggestion(suggestion: String) {
        val current = queryValue
        queryValue = if (suggestion.contains(current) && current.isNotBlank()) {
            suggestion
        } else {
            (current.trim() + " " + suggestion).trim()
        }
        _ui.update { it.copy(query = queryValue, isPreliminary = false) }
        onSearch()
    }

    /** Тап по записи истории: подставить и отправить. */
    fun searchHistoryItem(text: String) {
        queryValue = text
        _ui.update { it.copy(query = text, isPreliminary = false) }
        onSearch()
    }

    fun removeHistoryItem(text: String) {
        viewModelScope.launch {
            historyRepository.removeResponse(SearchResponse(text)).collect { }
        }
    }

    private fun addHistory(response: String) {
        val correct = response.trim()
        if (correct.isEmpty()) return
        viewModelScope.launch {
            historyRepository.addResponse(SearchResponse(responseText = correct)).collect { }
        }
    }

    // ── Параметры (§10.3) ──────────────────────────────────────────────────

    fun setFilterEnabled(key: String, enabled: Boolean) {
        searchFilter = when (key) {
            FilterNames.GENERAL_DATA.value ->
                searchFilter.copy(generalData = key to enabled)

            FilterNames.LOCO_DATA.value ->
                searchFilter.copy(locoData = key to enabled)

            FilterNames.TRAIN_DATA.value ->
                searchFilter.copy(trainData = key to enabled)

            FilterNames.PASSENGER_DATA.value ->
                searchFilter.copy(passengerData = key to enabled)

            FilterNames.OTHER_WORK_DATA.value ->
                searchFilter.copy(otherWorkData = key to enabled)

            FilterNames.PARTNER_DATA.value ->
                searchFilter.copy(partnerData = key to enabled)

            FilterNames.NOTES_DATA.value ->
                searchFilter.copy(notesData = key to enabled)

            else -> searchFilter
        }
        publishChips()
    }

    /** Начало периода (epoch ms) или null — очистить. */
    fun setPeriodStart(ms: Long?) {
        searchFilter = searchFilter.copy(
            timePeriod = searchFilter.timePeriod.copy(startDate = ms)
        )
        publishChips()
    }

    /** Конец периода (epoch ms) или null — очистить. */
    fun setPeriodEnd(ms: Long?) {
        searchFilter = searchFilter.copy(
            timePeriod = searchFilter.timePeriod.copy(endDate = ms)
        )
        publishChips()
    }

    /** «Сбросить»: все разделы включены, период очищен, запрос перезапускается. */
    fun clearFilter() {
        searchFilter = FilterSearch()
        publishChips()
        rerunCurrentQuery()
    }

    /** Закрытие шторки параметров — применить к текущему запросу (§10.3). */
    fun closeSettings() {
        rerunCurrentQuery()
    }

    private fun publishChips() {
        val f = searchFilter
        val chips = listOf(
            SearchChipUi(FilterNames.GENERAL_DATA.value, FilterNames.GENERAL_DATA.value, f.generalData.second),
            SearchChipUi(FilterNames.LOCO_DATA.value, FilterNames.LOCO_DATA.value, f.locoData.second),
            SearchChipUi(FilterNames.TRAIN_DATA.value, FilterNames.TRAIN_DATA.value, f.trainData.second),
            SearchChipUi(FilterNames.PASSENGER_DATA.value, FilterNames.PASSENGER_DATA.value, f.passengerData.second),
            SearchChipUi(FilterNames.OTHER_WORK_DATA.value, FilterNames.OTHER_WORK_DATA.value, f.otherWorkData.second),
            SearchChipUi(FilterNames.PARTNER_DATA.value, FilterNames.PARTNER_DATA.value, f.partnerData.second),
            SearchChipUi(FilterNames.NOTES_DATA.value, FilterNames.NOTES_DATA.value, f.notesData.second),
        )
        _ui.update {
            it.copy(
                chips = chips,
                periodStartText = f.timePeriod.startDate?.let { ms ->
                    "с ${Fmt.date(ms, indexTz)}"
                } ?: "",
                periodEndText = f.timePeriod.endDate?.let { ms ->
                    "по ${Fmt.date(ms, indexTz)}"
                } ?: "",
            )
        }
    }

    // ── Поиск ──────────────────────────────────────────────────────────────

    private fun rerunCurrentQuery() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch { runSearch(queryValue) }
    }

    private suspend fun runSearch(value: String) {
        try {
            val correctValue = value.trim()
            if (correctValue.isEmpty()) {
                _ui.update {
                    it.copy(
                        isVisibleHistory = true,
                        isVisibleHints = false,
                        isVisibleResult = false,
                        suggestions = emptyList(),
                        results = emptyList(),
                        isLoading = false,
                    )
                }
                return
            }
            val preliminary = _ui.value.isPreliminary
            val result = withContext(Dispatchers.Default) {
                searchIndex(searchIndex, correctValue, searchFilter, preliminary)
            }
            applySearchResult(result, correctValue)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Ошибка поиска по индексу — оставляем предыдущее состояние (§10.2).
        }
    }

    private fun applySearchResult(result: SearchStateScreen<List<RouteWithTag>>, query: String) {
        when (result) {
            is SearchStateScreen.Loading -> {
                _ui.update { it.copy(isLoading = true) }
            }

            is SearchStateScreen.Input -> {
                val hints = result.hints
                    .filter { it.isNotBlank() }
                    .safetySubList(0, COUNT_HINTS)
                _ui.update {
                    it.copy(
                        isVisibleHistory = true,
                        isVisibleHints = hints.isNotEmpty(),
                        isVisibleResult = false,
                        suggestions = hints,
                        isLoading = false,
                    )
                }
            }

            is SearchStateScreen.Success -> {
                val tokens = query.splitBySpaceAndComma()
                    .map { t -> t.trim().lowercase() }
                    .filter { it.isNotEmpty() }
                // BUG-32 (паритет с Android): дата карточки результата — в поясе
                // устройства (Android `SimpleDateFormat`), не в поясе отображения.
                val deviceTz = TimeZone.currentSystemDefault()
                val items = (result.data ?: emptyList()).map { tagged ->
                    val texts = entityTexts(tagged.route, tagged.tag, indexTz)
                    val text = texts.joinToString("\n\n")
                    SearchResultUi(
                        basicId = tagged.route.basicData.id,
                        routeNumberText = tagged.route.basicData.number
                            ?.takeIf { it.isNotBlank() }?.let { "№$it" } ?: "Маршрут",
                        dateText = tagged.route.basicData.timeStartWork
                            ?.let { Fmt.date(it, deviceTz) } ?: "",
                        tagLabel = tagLabel(tagged.tag),
                        text = text,
                        matchRanges = matchRanges(text, tokens),
                    )
                }
                _ui.update {
                    it.copy(
                        isVisibleHistory = false,
                        isVisibleHints = false,
                        isVisibleResult = true,
                        results = items,
                        suggestions = emptyList(),
                        isLoading = false,
                    )
                }
            }

            is SearchStateScreen.Failure -> {
                _ui.update { it.copy(isLoading = false) }
            }
        }
    }

    // ── Индекс и история ───────────────────────────────────────────────────

    private fun observeIndex() {
        indexJob?.cancel()
        indexJob = viewModelScope.launch {
            try {
                combine(
                    settingsUseCase.getFlowCurrentSettingsState(),
                    routeUseCase.getListRoutesAsStateFlow(),
                ) { settingState, routesState -> settingState to routesState }
                    .collect { (settingState, routesState) ->
                        if (settingState is ResultState.Success) {
                            val us = settingState.data
                            indexTz = runCatching { TimeZone.of(us.displayTimeZone()) }
                                .getOrElse { TimeZone.of("GMT+3") }
                            publishChips()
                        }
                        when (routesState) {
                            is ResultState.Loading -> {
                                if (queryValue.isNotBlank()) {
                                    _ui.update { it.copy(isLoading = true) }
                                }
                            }

                            is ResultState.Error -> {
                                _ui.update { it.copy(isLoading = false) }
                            }

                            is ResultState.Success -> {
                                val tz = indexTz
                                searchIndex = withContext(Dispatchers.Default) {
                                    buildSearchIndex(routesState.data, tz)
                                }
                                if (queryValue.isNotBlank()) {
                                    rerunCurrentQuery()
                                }
                            }
                        }
                    }
            } catch (_: Exception) {
                // Ошибка подписки на индекс — экран остаётся в текущем состоянии.
            }
        }
    }

    private fun observeHistory() {
        viewModelScope.launch {
            historyRepository.getAllResponse().collect { result ->
                if (result is ResultState.Success) {
                    // Новые сверху; повторный запрос не поднимается (INSERT OR IGNORE).
                    _ui.update { it.copy(history = result.data.asReversed().map { r -> r.responseText }) }
                }
            }
        }
    }

    fun dispose() {
        indexJob?.cancel()
        searchJob?.cancel()
    }

    private companion object {
        const val COUNT_HINTS = 5
        const val SEARCH_DEBOUNCE_MS = 200L
    }
}
