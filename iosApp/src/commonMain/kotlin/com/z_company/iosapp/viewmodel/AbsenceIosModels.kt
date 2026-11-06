package com.z_company.iosapp.viewmodel

/**
 * Модели экрана «Новое отвлечение» (SCREEN_SPECS §16.1, §16.4) для SwiftUI.
 */

data class AbsenceIosUi(
    val isLoading: Boolean = true,
    val year: Int = 0,
    val month: Int = 0,          // 0-based
    val monthLabel: String = "", // «Ноябрь 2026»
    val days: List<AbsenceIosDay> = emptyList(),
    /** "none" | "single" | "range" — подзаголовок шапки. */
    val selectionKind: String = "none",
    val summaryRangeText: String = "Не выбрано",
    val summaryDays: Int = 0,
    /** «24:00» / «по рабочим дням» (для «По уходу» — «по всем дням»). */
    val summaryHoursText: String = "0:00",
    val summaryHoursCaption: String = "по рабочим дням",
    val selectedKind: String = "vacation",
    val typePickerOpen: Boolean = false,
    val isSaving: Boolean = false,
    val done: Boolean = false,
    /** Одноразовое сообщение об ошибке (snackbar); пусто — нет. */
    val errorMessage: String = "",
)

data class AbsenceIosDay(
    val day: Int,
    val weekdayIndex: Int,   // 0 = понедельник
    val isWeekend: Boolean,
    /** null | "start" | "end" | "single" | "inside" — выделение выбранного диапазона. */
    val selection: String? = null,
    /** Бейдж уже сохранённого отвлечения («ОТП», …) или null. */
    val existingBadge: String? = null,
)

/** Вариант типа отвлечения для списка выбора. */
data class AbsenceIosTypeOption(
    val kind: String,
    val label: String,
)
