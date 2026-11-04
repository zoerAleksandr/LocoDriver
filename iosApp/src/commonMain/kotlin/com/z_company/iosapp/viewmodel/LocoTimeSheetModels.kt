package com.z_company.iosapp.viewmodel

/**
 * Снимки состояния шторки установки времени (SCREEN_SPECS §2) для SwiftUI.
 * Расчёты по нормам, валидация и предупреждения — в [LocoTimeSheetIosViewModel].
 */

/** Строка времени шторки (§2.3). `id`: routeStart | start | end | barrierOut | barrierIn | workEnd. */
data class TimeRowUi(
    val id: String,
    val label: String,
    /** `ЧЧ:ММ` в поясе отображения или null → «—:—». */
    val timeText: String?,
    val timeMs: Long?,
    /** «+N мин» / «−N мин» и пояснение нормы; пусто — не показывать. */
    val deltaText: String,
    val deltaNote: String,
    /** 0 — приглушённый, 1 — больше нормы (красный), 2 — меньше нормы (зелёный). */
    val deltaTone: Int,
    val isLocked: Boolean,
    val hasError: Boolean,
    /** Режим ASKING: строка пригодна как якорь и мигает. */
    val isHighlighted: Boolean,
    /** Предложение «Обновить ЧЧ:ММ» вычисленного окончания работы. */
    val pendingText: String?,
    val canTap: Boolean,
    val canLongPress: Boolean,
)

/** Оранжевое предупреждение (WarnItem, §2.7). */
data class TimeWarnUi(
    val text: String,
    val hint: String?,
    val cta: String?,
    val isSeries: Boolean,
    val dismissible: Boolean,
)

/** Кнопка «Сохранить норму …» (§2.8). */
data class SaveNormUi(
    val title: String,
    val subtitle: String,
)

data class TimeSheetState(
    val ready: Boolean = false,
    val isAcceptance: Boolean = true,
    val title: String = "",
    val seriesName: String? = null,
    val seriesId: String? = null,
    val stationName: String? = null,
    val stationId: String? = null,
    val stationLabel: String = "",
    /** Сегмент «После отстоя | Из рук в руки» — только при выбранной серии (§2.4). */
    val showNormMode: Boolean = false,
    val handToHand: Boolean = false,
    val warnings: List<TimeWarnUi> = emptyList(),
    val isAsking: Boolean = false,
    val rows: List<TimeRowUi> = emptyList(),
    val saveSeries: SaveNormUi? = null,
    val saveStation: SaveNormUi? = null,
    val canCalculate: Boolean = false,
    val hasSequenceError: Boolean = false,
    /** Диалог «Другая норма» при смене варианта (§2.4). */
    val showRecalcDialog: Boolean = false,
    val recalcVariantText: String = "",
    /** Растёт при каждом изменении справочников — пикеры перечитывают списки. */
    val catalogVersion: Int = 0,
)
