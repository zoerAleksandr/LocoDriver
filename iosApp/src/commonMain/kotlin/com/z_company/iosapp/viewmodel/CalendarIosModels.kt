package com.z_company.iosapp.viewmodel

/**
 * Модели состояния экрана «Календарь» (SCREEN_SPECS §14) для SwiftUI.
 * Все строки отформатированы в Kotlin — Swift только раскладывает их по экрану.
 */

data class CalendarIosUi(
    val isLoading: Boolean = true,
    /** Текст блокирующей операции («Создаём маршруты…» / «Удаляем маршрут…») или null. */
    val operationMessage: String? = null,
    val year: Int = 0,
    val month: Int = 0,                 // 0-based
    val monthLabelUpper: String = "",   // «МАЙ 2026»
    val monthLabelNormal: String = "",  // «Май 2026»
    val totalWorkedText: String = "00:00",
    val summaryText: String = "",
    val days: List<CalendarIosDay> = emptyList(),
    val today: Int? = null,
    /** Средний час из настроек ЗП (§14.6), 0 — не задан. */
    val averagePaymentHour: Double = 0.0,
    val currencySymbol: String = "₽",
)

/** Ячейка/деталь дня календаря. */
data class CalendarIosDay(
    val day: Int,
    val weekdayIndex: Int,          // 0 = понедельник … 6 = воскресенье
    val isWeekend: Boolean,
    val isToday: Boolean,
    val isHoliday: Boolean,
    val holidayName: String?,       // null — «Праздничный день» без названия
    val trips: List<CalendarIosTrip> = emptyList(),
    val absence: CalendarIosAbsence? = null,
)

/** Маршрут дня (поездка). */
data class CalendarIosTrip(
    val basicId: String,
    val timeText: String,       // «07:45»
    val numberText: String,     // «Поездка №115» / «Поездка»
    val hoursText: String,      // «04:35»
    val isFavorite: Boolean,
    val isSynchronized: Boolean,
    /** Карточка маршрута §4.5 — та же, что на Главном экране. */
    val card: HomeIosRouteCard,
)

/** Отвлечение дня (в составе непрерывного периода). */
data class CalendarIosAbsence(
    /** "vacation" | "sick" | "courses" | "donor" | "childCare" | "dayOff" | "businessTrip" | "technicalStudy" | "other" */
    val kind: String,
    val label: String,          // «Отпуск»
    val badge: String,          // «ОТП»
    val hoursPerDay: Int,       // норма-часы за этот день
    val periodStart: Int,
    val periodEnd: Int,
    val periodDays: Int,
    val periodHours: Int,
    /** Часы техзанятий за день («3» / «3,5») — только для technicalStudy. */
    val customHoursText: String? = null,
    val customPeriodHoursText: String? = null,
    /** «{часы}:00» / «часов» / «всего» — справа в карточке (для DayOff — «×2» / «тариф»). */
    val hoursValueText: String = "",
    val hoursCaptionText: String = "",
    val isDayOff: Boolean = false,
    val isTechnicalStudy: Boolean = false,
)

/** Состояние режима планирования маршрутов (§14.4). */
data class CalendarIosPlanUi(
    val active: Boolean = false,
    val timeOptions: List<CalendarIosTimeOption> = emptyList(),
    val activeTimeText: String? = null,
    /** Отмеченные дни → время явки («07:45»). */
    val plannedDays: List<CalendarIosPlanDay> = emptyList(),
    val initialDay: Int? = null,
    /** Шаг подсказки: 0 — «ШАГ 1 · ВРЕМЯ ЯВКИ», 1 — «ШАГ 2 · ДАТЫ», 2 — «✓ Готово». */
    val hintStep: Int = 0,
    val bottomText: String = "Выберите время и отметьте дни",
    /** Начальное значение шторки продолжительности, мс (12 ч по умолчанию). */
    val suggestedWorkDuration: Long = 43_200_000L,
)

data class CalendarIosTimeOption(val millis: Long, val label: String)

data class CalendarIosPlanDay(val day: Int, val timeText: String)

/** Пачка не помещается в бесплатный лимит (§14.4 SubscriptionLimitDialog). */
data class CalendarIosLimit(
    val requested: Int,
    val remaining: Int,
    /** true — остаток 0 (текст «Бесплатный лимит исчерпан…»). */
    val isExhausted: Boolean,
    val title: String = "Не хватает бесплатных маршрутов",
    val message: String = "",
)

/** Дубль по времени явки (§14 / §14.4): шторка «Маршрут с такой явкой уже сохранён.» */
data class CalendarIosDuplicate(
    /** «dd.MM HH:mm» найденного маршрута. */
    val existingLabel: String = "",
    val message: String = "Маршрут с такой явкой уже сохранён.",
)
