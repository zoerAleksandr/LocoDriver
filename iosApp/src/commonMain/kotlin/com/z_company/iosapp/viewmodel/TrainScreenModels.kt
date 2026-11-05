package com.z_company.iosapp.viewmodel

/**
 * Снимки состояния экрана «Поезд» (SCREEN_SPECS §6) для SwiftUI.
 *
 * Всё, что считается (стоянки, перегоны, сводка, секундомер, длина состава, категория),
 * посчитано и отформатировано в Kotlin; Swift только показывает. Времена — мс UTC,
 * в список станций они выводятся в GMT+3 (BUG-22), в шторках — в поясе пользователя
 * ([TrainScreenState.displayOffsetSeconds]).
 */

/** Станция в порядке поезда (исходном, не отображаемом). */
data class TrainStationUi(
    val id: String,
    val name: String,
    val trackNumber: String,
    val arrival: Long?,
    val departure: Long?,
    val isFinal: Boolean,
    val isPassing: Boolean,
    /** Путь и примечание ПЕРЕД этой станцией (между предыдущей и этой). */
    val segmentTrack: String,
    val segmentNotes: String,
    /** Бейдж стоянки: «N'», «Hч», «HчM'»; null — без бейджа. */
    val dwellText: String?,
    /** 0 — без фона, 1 — жёлтый (> 5 мин), 2 — красный (> 30 мин, пассажирский — > 20). */
    val dwellLevel: Int,
)

/**
 * Строка списка в ПОРЯДКЕ ОТОБРАЖЕНИЯ: станция ([isSegment] = false, [stationIndex] — индекс в
 * [TrainScreenState.stations]) либо перегон между станциями [stationIndex] и [stationIndex] + 1
 * в исходном порядке (для «будущего» перегона второй станции может не быть).
 */
data class TrainRowUi(
    val key: String,
    val isSegment: Boolean,
    val stationIndex: Int,
    val isFirst: Boolean,
    val isLast: Boolean,
    /** Только для перегона: время в пути («1 ч 51 мин»); null — посчитать не из чего. */
    val travelText: String?,
    val segmentTrack: String,
    val segmentNotes: String,
    /** «Будущий перегон» — пунктиром, без шеврона, с иконкой локомотива. */
    val isFuture: Boolean,
)

/** Запись истории данных поезда (§6.7). */
data class TrainVersionUi(
    val index: Int,
    val title: String,
    val weight: String,
    val axle: String,
    val conditionalLength: String,
)

/** Раздел вспомогательной тяги (§6.2 п.7). `present` = false — раздела нет («Добавить»). */
data class TrainAssistUi(
    val present: Boolean,
    val series: String,
    val number: String,
    val driver: String,
    val notes: String,
    /** null — направление не выбрано. */
    val isFirst: Boolean?,
)

/** Сводка на экране: заголовок и подробности («серия №номер · ТЧМ имя · примечание»). */
data class TrainSummaryRowUi(val title: String, val details: String)

data class TrainPhaseUi(
    val id: String,
    val from: String,
    val to: String,
    val distance: Int,
    val rate: Double,
    val isSelected: Boolean,
)

data class TrainStopwatchUi(
    /** 0 — не показывать, 1 — «стоянка», 2 — «в пути». */
    val kind: Int,
    val text: String,
    /** Для стоянки: 0 нейтральный, 1 жёлтый (≥ 5 мин), 2 красный (≥ 30 мин). */
    val level: Int,
)

data class TrainScreenState(
    val isLoading: Boolean = true,
    val isNew: Boolean = true,
    val number: String = "",
    val additionalNumbers: List<String> = emptyList(),
    val distance: String = "",
    val weight: String = "",
    val axle: String = "",
    val conditionalLength: String = "",
    /** Строка категории по номеру поезда («i» рядом с номером); null — номер пуст/не распознан. */
    val categoryText: String? = null,
    /** «· N м» рядом с У.Д.; null — не показывать. */
    val lengthMetersText: String? = null,
    val hasPhase: Boolean = false,
    val phaseTitle: String = "",
    val phaseDistanceText: String = "",
    val phases: List<TrainPhaseUi> = emptyList(),
    val pusher: TrainAssistUi = TrainAssistUi(false, "", "", "", "", null),
    val doubleTraction: TrainAssistUi = TrainAssistUi(false, "", "", "", "", null),
    val doubledTrain: TrainAssistUi = TrainAssistUi(false, "", "", "", "", null),
    val assistSummary: List<TrainSummaryRowUi> = emptyList(),
    val hasCarInspector: Boolean = false,
    val carInspectorFullName: String = "",
    val carInspectorTabNumber: String = "",
    val carInspectorCouplingTime: Long? = null,
    /** Сводка вагонника на экране (null — блок не показывать: ни одно поле не заполнено). */
    val carInspectorSummary: TrainSummaryRowUi? = null,
    /** Оранжевые точки на нижней панели: раздел содержит хотя бы одно заполненное поле. */
    val assistHasData: Boolean = false,
    val inspectorHasData: Boolean = false,
    val stations: List<TrainStationUi> = emptyList(),
    val rows: List<TrainRowUi> = emptyList(),
    val isReversed: Boolean = false,
    val showSegments: Boolean = true,
    val versions: List<TrainVersionUi> = emptyList(),
    /** «Данные менялись · N версия/версии/версий»; null — версий ≤ 1. */
    val versionsText: String? = null,
    val summaryTotalTime: String? = null,
    val summarySectionSpeed: String? = null,
    val summaryTechSpeed: String? = null,
    /** Следующее действие «GO» — отправление (▶), иначе прибытие (⏸). */
    val goIsDeparture: Boolean = true,
    val errorMessage: String? = null,
    val saveError: String? = null,
    val stationSuggestions: List<String> = emptyList(),
    val seriesSuggestions: List<String> = emptyList(),
    val displayOffsetSeconds: Int = 3 * 3600,
)
