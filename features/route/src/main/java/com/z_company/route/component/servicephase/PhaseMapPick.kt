package com.z_company.route.component.servicephase

import com.z_company.domain.util.PointStation
import com.z_company.domain.util.PointStationResult

/** Кнопка карты редактора плеча: какую станцию она указывает и как подписана. Активна всегда. */
internal data class MapPickButton(val isStart: Boolean, val label: String, val enabled: Boolean = true)

/** Что показать в диалоге после нажатия на карте. */
internal sealed interface PickConfirm {
    val title: String
    val text: String
    val confirmText: String
    val dismissText: String

    /** Найдена станция: «Применить» записывает её в поле. */
    data class Found(
        val station: PointStation,
        override val title: String,
        override val text: String,
    ) : PickConfirm {
        override val confirmText get() = "Применить"
        override val dismissText get() = "Отмена"
    }

    /** Станций рядом нет, но в поле уже есть название: «Применить» ставит на эту точку [name]. */
    data class UseTypedName(val name: String, override val text: String) : PickConfirm {
        override val title get() = "Станция не найдена"
        override val confirmText get() = "Применить"
        override val dismissText get() = "Отмена"
    }

    /** Станций рядом нет, названия нет: можно только выбрать другое место. */
    data class NothingNearby(override val text: String) : PickConfirm {
        override val title get() = "Станция не найдена"
        override val confirmText get() = "Выбрать другую"
        override val dismissText get() = "Отмена"
    }
}

/** Цель камеры карты редактора плеча. */
internal data class MapStart(val latitude: Double, val longitude: Double, val zoom: Double)

/**
 * Тексты и состояния карты редактора плеча (SCREEN_SPECS §6.6): кнопки «Указать/Уточнить», диалог подтверждения,
 * камера без станций. Чистая логика - покрыта юнит-тестами.
 */
internal object PhaseMapPick {
    /** Россия целиком - когда нет ни станций, ни местоположения, ни последней станции справочника. */
    private val RUSSIA = MapStart(61.5, 96.0, 2.8)
    private const val LAST_STATION_ZOOM = 9.0
    private const val MY_LOCATION_ZOOM = 12.0

    fun buttons(departure: String, arrival: String): List<MapPickButton> {
        val dep = departure.isBlank()
        val arr = arrival.isBlank()
        return listOf(
            MapPickButton(
                isStart = true,
                label = when {
                    dep && arr -> "Указать первую станцию"
                    dep -> "Указать станцию отправления"
                    else -> "Уточнить на карте отправление"
                },
            ),
            MapPickButton(
                isStart = false,
                label = when {
                    dep && arr -> "Указать конечную станцию"
                    arr -> "Указать станцию прибытия"
                    else -> "Уточнить на карте прибытие"
                },
            ),
        )
    }

    /** «Измерить по карте» на карте: обе станции названы и имеют координаты. */
    fun canMeasure(departure: String, arrival: String, departureKnown: Boolean, arrivalKnown: Boolean): Boolean =
        departure.isNotBlank() && arrival.isNotBlank() && departureKnown && arrivalKnown

    /** Подсказка в режиме выбора (пользователь нажимает точку на карте). */
    fun prompt(isStart: Boolean, departure: String, arrival: String): String {
        val both = departure.isBlank() && arrival.isBlank()
        return "Нажмите на карте место " + when {
            both && isStart -> "первой станции"
            both -> "конечной станции"
            isStart -> "станции отправления"
            else -> "станции прибытия"
        }
    }

    /** [currentName] - название, уже стоящее в поле этой станции. */
    fun confirm(result: PointStationResult, currentName: String): PickConfirm {
        val current = currentName.trim()
        val st = result.station
        val offlineNote = if (result.offline) " Нет связи с сервером — искали среди сохранённых станций." else ""
        if (st == null) {
            return if (current.isNotEmpty()) {
                PickConfirm.UseTypedName(
                    name = current,
                    text = "Рядом (в радиусе 1 км) станций нет. Указать эту точку как «$current»?$offlineNote",
                )
            } else {
                PickConfirm.NothingNearby(
                    text = "Рядом (в радиусе 1 км) станций нет. Нажмите ближе к станции или сначала введите название в поле.$offlineNote",
                )
            }
        }
        val replaces = current.isNotEmpty() && !current.equals(st.name.trim(), ignoreCase = true)
        return PickConfirm.Found(
            station = st,
            title = "Станция «${st.name}»?",
            text = "Расстояние до точки ${st.distanceM} м" +
                (if (replaces) ". Название в поле «$current» будет заменено." else "") +
                offlineNote,
        )
    }

    /** Куда направить камеру, когда станций с координатами нет: местоположение, иначе последняя станция справочника, иначе Россия. */
    fun startWithoutStops(myLocation: Pair<Double, Double>?, lastStation: Pair<Double, Double>?): MapStart = when {
        myLocation != null -> MapStart(myLocation.first, myLocation.second, MY_LOCATION_ZOOM)
        lastStation != null -> MapStart(lastStation.first, lastStation.second, LAST_STATION_ZOOM)
        else -> RUSSIA
    }
}
