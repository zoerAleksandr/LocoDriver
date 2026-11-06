package com.z_company.iosapp.viewmodel

/**
 * Модели мастера «Заполнить месяц» (SCREEN_SPECS §15) для SwiftUI.
 */

data class WizardIosUi(
    val step: Int = 1,                 // 1 | 2
    val year: Int = 0,
    val month: Int = 0,
    val monthName: String = "",
    val daysInMonth: Int = 0,
    val patterns: List<WizardIosPattern> = emptyList(),
    val selectedId: String = "",
    val isCustom: Boolean = true,
    /** Индекс дня цикла с открытым пикером типа, -1 — закрыт. */
    val pickerIndex: Int = -1,
    /** Дни «своего» цикла: "day" | "night" | "off". */
    val customCycle: List<String> = emptyList(),
    val cycleCaption: String = "",
    val dayStartText: String = "08:00",
    val dayEndText: String = "20:00",
    val nightStartText: String = "20:00",
    val nightEndText: String = "08:00",
    val hasDayShift: Boolean = true,
    val hasNightShift: Boolean = true,
    val firstDay: Int = 1,
    val extendToNextMonth: Boolean = false,
    val canContinuePrevious: Boolean = false,
    val showContinuePreviousSheet: Boolean = false,
    val previousMonthName: String = "",
    val continuePrevious: Boolean = false,
    val phaseOffset: Int = 0,
    /** Предпросмотр раскладки: "day" | "night" | "off" на каждый день месяца. */
    val preview: List<String> = emptyList(),
    val previewShiftCount: Int = 0,
    val isSaving: Boolean = false,
    val done: Boolean = false,
    /** Текст для snackbar после apply() («Создано черновиков маршрутов: N…»). */
    val applyMessage: String = "",
    /** Пачка не помещается в бесплатный лимит (§15 / §14.4). */
    val limitRequested: Int = 0,
    val limitRemaining: Int = 0,
    val showLimitDialog: Boolean = false,
    val limitMessage: String = "",
)

data class WizardIosPattern(
    val id: String,
    val title: String,
    val subtitle: String,
    /** "day" | "night" | "off". */
    val cycle: List<String>,
)
