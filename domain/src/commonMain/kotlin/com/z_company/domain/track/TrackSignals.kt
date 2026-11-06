package com.z_company.domain.track

import kotlin.math.abs

/*
 * Светофоры на оси участка и правила выбора тех, что нужны машинисту (SCREEN_SPECS §6.9, «Светофоры на профиле»).
 * Чистая логика KMP. Единицы: километры оси участка.
 */

/**
 * Литер светофора: [EVEN] - Ч, виден поезду чётного направления; [ODD] - Н, виден поезду нечётного. Литер определяет, чей это
 * светофор, а не где он стоит: на двухпутном перегоне оба ряда стоят рядом, лицом в противоположные стороны.
 */
enum class SignalTrack(val letter: String) {
    EVEN("Ч"), ODD("Н");

    val opposite: SignalTrack get() = if (this == EVEN) ODD else EVEN
}

/** Вид светофора по режимной карте: проходной (номер), станционный (входной, выходной, маршрутный), маневровый, предупредительный. */
enum class SignalKind { PASS, STATION, SHUNT, DISTANT }

/** Направление движения по чётности: чётное (литер Ч) или нечётное (литер Н). */
enum class TravelParity(val title: String) {
    EVEN("Чётное"), ODD("Нечётное");

    val opposite: TravelParity get() = if (this == EVEN) ODD else EVEN
    val track: SignalTrack get() = if (this == EVEN) SignalTrack.EVEN else SignalTrack.ODD
}

/**
 * Светофор на оси участка. [facing] - в какую сторону оси он виден лицом (+1 - по возрастанию км оси, −1 - против): поезд,
 * идущий в эту сторону, видит его огни; идущий навстречу - спину светофора.
 * [label] - как напечатано на режимной карте: у проходного только номер («6»), у остальных с литером («Ч2», «НМ1А»).
 */
data class TrackSignal(
    val km: Double,
    val label: String,
    val track: SignalTrack,
    val kind: SignalKind,
    val facing: Int,
) {
    /** Подпись для машиниста: как на светофоре и на режимной карте. У проходного только номер («6»), литер Ч/Н на нём не пишется. */
    val title: String get() = label
}

/** Светофор впереди по ходу и расстояние до него. */
data class SignalAhead(val signal: TrackSignal, val distanceKm: Double, val backToTrain: Boolean)

object SignalRules {
    /** Дальше этого расстояния светофор другого места не говорит о направлении «чётного» здесь (км оси). */
    private const val LOCAL_KM = 15.0

    /**
     * Куда по оси идёт чётное направление в месте [km]: +1 - к росту км оси, −1 - к убыли; 0 - светофоров нет и узнать не из
     * чего. Берётся ближайший светофор (литер Ч - его [TrackSignal.facing], литер Н - обратный), в пределах 15 км; дальше -
     * большинство по всему участку. Так участок, где ось на середине поворачивается относительно нумерации, считается верно.
     */
    fun evenAxisDir(signals: List<TrackSignal>, km: Double): Int {
        if (signals.isEmpty()) return 0
        fun evenDirOf(s: TrackSignal) = if (s.track == SignalTrack.EVEN) s.facing else -s.facing
        val nearest = signals.filter { it.facing != 0 }.minByOrNull { abs(it.km - km) }
        if (nearest != null && abs(nearest.km - km) <= LOCAL_KM) return evenDirOf(nearest)
        val sum = signals.sumOf { evenDirOf(it) }
        return if (sum > 0) 1 else if (sum < 0) -1 else 0
    }

    /** Чётность движения: [travelAxisDir] - сторона оси, в которую идёт поезд (+1 или −1); null - направление чётности неизвестно. */
    fun parity(signals: List<TrackSignal>, km: Double, travelAxisDir: Int): TravelParity? {
        val even = evenAxisDir(signals, km)
        if (even == 0) return null
        return if (travelAxisDir == even) TravelParity.EVEN else TravelParity.ODD
    }

    /** Сторона оси, в которую идёт поезд заданной чётности; null - неизвестно. */
    fun axisDir(signals: List<TrackSignal>, km: Double, parity: TravelParity): Int? {
        val even = evenAxisDir(signals, km)
        if (even == 0) return null
        return if (parity == TravelParity.EVEN) even else -even
    }

    /**
     * Чьи светофоры показывать. По своему пути - литер движения (чётное - Ч, нечётное - Н). По неправильному пути (чётное
     * направление по нечётному пути и наоборот) машинист ориентируется на светофоры пути, по которому едет, - литер противоположный,
     * и они стоят к нему спиной.
     */
    fun shownTrack(parity: TravelParity, wrongTrack: Boolean): SignalTrack =
        if (wrongTrack) parity.track.opposite else parity.track

    /**
     * Чётность поезда по номеру: чётный номер - чётное направление, нечётный - нечётное; null - номера нет или в нём нет цифр.
     * Берутся цифры первой группы номера (суффикс-буква не мешает: «2704Ч» - чётный).
     */
    fun parityOfTrainNumber(number: String?): TravelParity? {
        val digits = number?.let { Regex("\\d+").find(it)?.value } ?: return null
        return if ((digits.last() - '0') % 2 == 0) TravelParity.EVEN else TravelParity.ODD
    }

    fun visible(signals: List<TrackSignal>, parity: TravelParity, wrongTrack: Boolean): List<TrackSignal> {
        val track = shownTrack(parity, wrongTrack)
        return signals.filter { it.track == track }
    }

    /**
     * Ближайшие впереди по ходу светофоры нужного литера от места [kmAxis], по возрастанию расстояния; [travelAxisDir] - сторона
     * оси, в которую идёт поезд. [backToTrain] - светофор смотрит в другую сторону (проезд по неправильному пути).
     */
    fun ahead(
        signals: List<TrackSignal>,
        kmAxis: Double,
        travelAxisDir: Int,
        parity: TravelParity,
        wrongTrack: Boolean,
        limit: Int = 3,
    ): List<SignalAhead> {
        return aheadOfTrack(signals, kmAxis, travelAxisDir, shownTrack(parity, wrongTrack), limit)
    }

    /** То же по выбранному литеру напрямую (переключатель 1П/2П на экране профиля). */
    fun aheadOfTrack(
        signals: List<TrackSignal>,
        kmAxis: Double,
        travelAxisDir: Int,
        track: SignalTrack,
        limit: Int = 3,
    ): List<SignalAhead> {
        val dir = if (travelAxisDir >= 0) 1 else -1
        return signals.filter { it.track == track }
            .map { it to (it.km - kmAxis) * dir }
            .filter { it.second > 0.02 }
            .sortedBy { it.second }
            .take(limit)
            .map { (s, d) -> SignalAhead(s, d, backToTrain = s.facing != 0 && s.facing != dir) }
    }
}
