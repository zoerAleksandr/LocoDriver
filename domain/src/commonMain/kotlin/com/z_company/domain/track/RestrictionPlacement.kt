package com.z_company.domain.track

import com.z_company.domain.util.TrainMapStop

/** Нанесётся ли ограничение ДУ-61 на карту и профиль. */
enum class Placement {
    /** Ограничение уже привязано к участку: рисуется на карте и профиле, данные проследования пишутся. */
    ON_MAP,

    /** Участок с профилем в этом месте есть, но ограничение ещё не привязано: встанет, когда профиль участка откроется на карте. */
    PENDING,

    /** В этом месте нет участка с профилем: на карту не нанесётся. */
    NO_PROFILE,

    /** Участок есть, но километраж здесь неизвестен (серая зона профиля): на карту не нанесётся. */
    GRAY_ZONE,

    /** У ограничения нет километров: на ось не поставить. */
    NO_KM,

    /** Определить нельзя (нет списка участков, место не из станций плеча). Ничего не показываем. */
    UNKNOWN,
}

/**
 * Определение [Placement] без сети: по списку участков (габариты) и уже сохранёнными на устройстве участкам. Ничего не загружает, поэтому
 * лимиты сервера на загрузки не тратятся. Габарит - прямоугольник вокруг оси, поэтому «есть участок» - оценка с запасом [NEAR_M].
 */
object RestrictionPlacement {
    const val NEAR_M = 1_000.0

    /** Участки с профилем, чей габарит накрывает место ограничения; null - место не удалось сопоставить станциям плеча. */
    fun candidates(e: RestrictionPass, stops: List<TrainMapStop>, summaries: List<SectionSummary>): List<SectionSummary>? {
        val names = e.place.split(" — ").map { it.trim() }.filter { it.isNotEmpty() }
        val points = names.mapNotNull { n -> stops.firstOrNull { SectionPicker.sameStation(it.name, n) } }
        if (names.isEmpty() || points.size != names.size) return null
        return summaries.filter { it.hasProfile }
            .filter { s -> s.box != null && points.all { p -> s.box.distanceM(p.latitude, p.longitude) <= NEAR_M } }
    }

    /**
     * [cached] - участки, сохранённые на устройстве (id -> участок). Если часть подходящих участков не сохранена, окончательно сказать
     * про серую зону нельзя - [Placement.PENDING].
     */
    fun classify(
        e: RestrictionPass,
        stops: List<TrainMapStop>,
        summaries: List<SectionSummary>,
        cached: Map<String, SectionProfile>,
    ): Placement {
        if (e.tracked) return Placement.ON_MAP
        if (e.startAbsKm == null || e.endAbsKm == null) return Placement.NO_KM
        if (summaries.isEmpty()) return Placement.UNKNOWN
        val candidates = candidates(e, stops, summaries) ?: return Placement.UNKNOWN
        val withProfile = summaries.filter { it.hasProfile }
        if (candidates.isEmpty()) {
            // Участки без габарита (старый сервер) отсеять нечем - честно «не знаем».
            return if (withProfile.any { it.box == null }) Placement.UNKNOWN else Placement.NO_PROFILE
        }
        val loaded = candidates.mapNotNull { cached[it.id] }
        if (loaded.size < candidates.size) return Placement.PENDING
        // Запись ещё не привязана к участку: профиль, попавший в кэш рядом со станцией,
        // может относиться к другой ветке. Серую зону подтверждает только профиль выбранного плеча.
        return Placement.PENDING
    }
}
