package com.z_company.iosapp.viewmodel

/**
 * Модели экрана «Норма по месяцам» / «Своя настройка» (SCREEN_SPECS §16.3, §16.3.1).
 */

data class NormMonthIosUi(
    val isLoading: Boolean = true,
    val year: Int = 0,
    val month: Int = 0,             // 0-based
    val monthLabel: String = "",    // «Ноябрь 2026»
    /** «168:00» — норма за месяц (NormaUseCase). */
    val normaMonthText: String = "0:00",
    /** "standard" | "sixDay" | "custom". */
    val weekMode: String = "standard",
    val standardTitle: String = "Стандартная · 8 ч, Пн–Пт",
    val sixDayTitle: String = "Шестидневная · 7 ч + Сб 5 ч",
    val customTitle: String = "По неделям",
    val isWeekTemplateExpanded: Boolean = false,
    /** 7 ячеек шаблона «По неделям» (Пн…Вс). */
    val weekTemplate: List<NormMonthIosWeekday> = emptyList(),
    val days: List<NormMonthIosDay> = emptyList(),
    /** Одноразовое сообщение (snackbar). */
    val message: String = "",
)

data class NormMonthIosWeekday(
    val index: Int,             // 0 = понедельник
    val label: String,          // «Пн»
    val hours: Int,             // 0..12 (для счётчика шторки)
    val hoursText: String,      // «8 ч» / «ВЫХ»
    val isDayOff: Boolean,
)

data class NormMonthIosDay(
    val day: Int,
    val weekdayIndex: Int,       // 0 = понедельник
    val isWeekend: Boolean,
    val isToday: Boolean,
    val isHoliday: Boolean,
    val holidayName: String?,
    /** «8» / «7» / «0» / «» — норма дня; пусто у ВЫХ, праздника, отвлечения, 0 ч. */
    val normText: String = "",
    /** Норма с начала месяца (mono 10sp) или «». */
    val cumulativeText: String = "",
    /** «ВЫХ» / «ПР» / бейдж отвлечения («ОТП») — метка вместо числа. */
    val label: String? = null,
    val labelIsHoliday: Boolean = false,
    val isDayOff: Boolean = false,
    val isNormReducing: Boolean = false,   // отпуск/больничный и т.п.
    val isCustom: Boolean = false,
    /** «8 ч» — значение по умолчанию для «Вернуть по умолчанию (N ч)». */
    val defaultHoursText: String = "",
    /** Название и период отвлечения для шторки дня («Отпуск, 10–24 ноября (15 дн.)»). */
    val absenceTitle: String? = null,
    val absencePeriodText: String? = null,
    val isEditable: Boolean = true,
)
