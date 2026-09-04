package com.z_company.data_local.route

import com.z_company.core.ResultState
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.FilterSearch
import com.z_company.domain.entities.RouteWithTag
import com.z_company.domain.entities.SearchStateScreen
import com.z_company.domain.entities.SearchTag
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.inTimePeriod
import com.z_company.domain.repositories.RouteRepository
import com.z_company.domain.util.splitBySpaceAndComma
import kotlinx.coroutines.flow.Flow

/**
 * Поиск по маршрутам.
 *
 * Архитектура:
 *  1. [buildIndex] один раз (при изменении списка маршрутов или настроек) собирает
 *     «поисковый корпус» из СЫРЫХ значений полей — без подписей UI («Тепловоз»,
 *     «явка», «• Секция»), которые раньше давали ложные совпадения и мусор в
 *     подсказках. Даты берутся в стабильном формате [DateAndTimeConverter].
 *  2. [search] работает по готовому индексу: чистая CPU-функция без БД и без
 *     пересборки строк на каждое нажатие клавиши.
 *
 * Отличия от прежней реализации (поиск шёл по отображаемой строке `EntityString`):
 *  - учтены все поля модели, а не только те, что попадали в текст для UI
 *    (доп. номера поезда, ФИО напарника и его локомотив, номер пути, нормы,
 *    счётчики, «другой род тока», экипировка в кг и т.д.);
 *  - совпадения ранжируются: точное совпадение идентификатора → префикс →
 *    вхождение, внутри группы — свежие маршруты выше;
 *  - подсказки строятся из словаря идентификаторов, а не из форматированного
 *    текста, поэтому в них не попадают «•», «явка», куски дат.
 */
class SearchRouteUseCase(val repository: RouteRepository) {

    /** Поток маршрутов. VM подписывается один раз и на каждый эмит строит индекс. */
    fun routesFlow(): Flow<ResultState<List<Route>>> = repository.loadRoutesAsStateFlow()

    /**
     * Предрасчитанное представление маршрута для поиска.
     *
     * `*Match` — строки в нижнем регистре для сопоставления. По одной строке на
     * сущность (локомотив/поезд/пассажир): токены запроса должны совпасть в
     * пределах ОДНОЙ сущности (прежняя семантика).
     * [keyValues] — идентификаторы (в нижнем регистре) для ранжирования.
     * [vocabulary] — те же идентификаторы в исходном регистре для подсказок.
     */
    class RouteSearchEntry(
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

    /** Строится один раз при изменении списка маршрутов или настроек. */
    fun buildIndex(routes: List<Route>, converter: DateAndTimeConverter): List<RouteSearchEntry> =
        routes.map { route -> buildEntry(route, converter) }

    private fun buildEntry(route: Route, c: DateAndTimeConverter): RouteSearchEntry {
        // Идентификаторы для ранжирования и подсказок (в исходном регистре, без дублей).
        val keys = LinkedHashSet<String>()
        fun key(v: String?) {
            val t = v?.trim()
            if (!t.isNullOrEmpty()) keys.add(t)
        }

        // ---- Основные данные ----
        val basicParts = ArrayList<String>()
        route.basicData.number?.let { basicParts += it; key(it) }
        route.basicData.timeStartWork?.let { basicParts += c.getDateAndTime(it) }
        route.basicData.timeEndWork?.let { basicParts += c.getDateAndTime(it) }
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
            loco.timeStartOfAcceptance?.let { p += c.getDateAndTime(it) }
            loco.timeEndOfAcceptance?.let { p += c.getDateAndTime(it) }
            loco.timeStartOfDelivery?.let { p += c.getDateAndTime(it) }
            loco.timeEndOfDelivery?.let { p += c.getDateAndTime(it) }
            loco.timeBarrierOut?.let { p += c.getDateAndTime(it) }
            loco.timeBarrierIn?.let { p += c.getDateAndTime(it) }
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
                ci.couplingTime?.let { p += c.getDateAndTime(it) }
            }
            train.stations.forEach { st ->
                key(st.stationName); st.stationName?.let { p += it }
                key(st.trackNumber); st.trackNumber?.let { p += it }
                key(st.segmentTrackNumber); st.segmentTrackNumber?.let { p += it }
                st.segmentNotes?.let { p += it }
                st.timeArrival?.let { p += c.getDateAndTime(it) }
                st.timeDeparture?.let { p += c.getDateAndTime(it) }
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
            pass.timeDeparture?.let { p += c.getDateAndTime(it) }
            pass.timeArrival?.let { p += c.getDateAndTime(it) }
            pass.notes?.let { p += it }
            p.joinToString(" ").lowercase()
        }

        // ---- Прочая работа ----
        val otherWorkMatch = route.otherWorks.map { work ->
            val p = ArrayList<String>()
            p += "прочая работа"
            key(work.workType); work.workType?.let { p += it }
            key(work.station); work.station?.let { p += it }
            work.timeStart?.let { p += c.getDateAndTime(it) }
            work.timeEnd?.let { p += c.getDateAndTime(it) }
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

        // ---- Примечания маршрута ----
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

    private class Scored(
        val tagged: RouteWithTag,
        val entry: RouteSearchEntry,
        val score: Int,
    )

    /**
     * Быстрый поиск по готовому индексу.
     *
     * Семантика совпадения сохранена: маршрут попадает под тег, если хотя бы одна
     * его сущность содержит ВСЕ токены запроса (регистронезависимо). Дедуп O(R²)
     * с глубоким `equals` убран — каждая пара (маршрут, тег) уникальна.
     */
    fun search(
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

    /** Все токены присутствуют в строке (AND-семантика). Строки уже в нижнем регистре. */
    private fun containsAll(haystack: String, tokens: List<String>): Boolean {
        tokens.forEach { token ->
            if (!haystack.contains(token)) return false
        }
        return true
    }

    /**
     * Оценка релевантности по идентификаторам маршрута:
     * 3 — токен точно равен идентификатору, 2 — идентификатор начинается с токена,
     * 1 — идентификатор содержит токен, 0 — совпадение только по неключевым полям.
     */
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

    /**
     * Подсказки-уточнения: слова из словаря идентификаторов совпавших маршрутов,
     * содержащие последний токен запроса (но не равные ему целиком). Без мусора
     * форматирования. Порядок сохраняется, дубликаты убираются.
     */
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
}
