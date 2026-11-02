package com.z_company.iosapp.viewmodel

/**
 * Снимки состояния формы маршрута (SCREEN_SPECS §5) для SwiftUI.
 *
 * Всё уже посчитано и отформатировано в Kotlin (строки, суммы, HH:MM), чтобы Swift
 * ничего не пересчитывал и не расходился с Android. Времена в миллисекундах UTC
 * передаются как есть — Swift показывает их в поясе [RouteFormState.displayOffsetSeconds].
 */

/** Строка дочерней сущности маршрута (локомотив, поезд, пассажир, прочая работа). */
data class RouteChildRow(
    val id: String,
    /** Основной текст строки (§5.3 п.9). */
    val title: String,
    /** Номер/серия — набирается моноширинным шрифтом. */
    val isMono: Boolean,
    /** Хвост после mono-части (названия станций) — sans. Может быть пустым. */
    val tail: String,
)

/** Напарник маршрута (§5.3 п.7). */
data class RoutePartnerRow(
    val id: String,
    val sourcePartnerId: String?,
    val shortName: String,
    val initials: String,
    val tabNumber: String?,
)

/** Строка предупреждения «Вторая ночь подряд» (§5.5). */
data class NightWarnRowUi(
    val label: String,
    val rangeText: String,
    val nightText: String,
    val start: Long,
    val end: Long,
    val nightIntervals: List<NightIntervalUi>,
)

data class NightIntervalUi(val start: Long, val end: Long)

/** Шапка «Время работы» (§5.3 п.8). Строки с null/0 не показываются. */
data class WorkSummaryUi(
    val passengerBeforeStart: String?,
    val waitingBeforeStart: String?,
    val worked: String,
    val night: String?,
    val passengerInside: String?,
)

/** Дубль по явке (§5.6). */
data class DuplicateUi(
    val existingStartText: String,
    val exitAfterSave: Boolean,
)

/** Полное состояние формы маршрута. */
data class RouteFormState(
    val isLoading: Boolean = true,
    val basicId: String = "",
    val title: String = "Новый маршрут",
    val number: String = "",
    val notes: String = "",
    val isOnePerson: Boolean = false,
    val isFavorite: Boolean = false,
    val restPointOfTurnover: Boolean = false,
    val timeStartWork: Long? = null,
    val timeEndWork: Long? = null,
    val timeStartBreak: Long? = null,
    val timeEndBreak: Long? = null,
    val startWorkByArrival: Boolean = false,
    /** Баннер ошибки валидации (§5.5); блокирующая — «Окончание работы раньше начала…». */
    val errorMessage: String? = null,
    val isBusinessTrip: Boolean = false,
    val currencySymbol: String = "₽",
    val displayOffsetSeconds: Int = 3 * 3600,

    // Настройки видимости (UserSettings)
    val showOnePersonSwitch: Boolean = true,
    val showPartner: Boolean = true,
    val showBreak: Boolean = true,
    val showLocomotive: Boolean = true,
    val showTrain: Boolean = true,
    val showOtherWork: Boolean = true,
    val showPassenger: Boolean = true,
    val usingDefaultWorkTime: Boolean = false,

    // Шапка «Время работы»
    val summary: WorkSummaryUi? = null,

    // Плитка «Расчёт» (значение уже отформатировано, см. [SalaryUi])
    val salaryTileText: String = "0 ₽",
    val salaryTileIsCalculated: Boolean = false,

    val partners: List<RoutePartnerRow> = emptyList(),
    val locomotives: List<RouteChildRow> = emptyList(),
    val trains: List<RouteChildRow> = emptyList(),
    val otherWorks: List<RouteChildRow> = emptyList(),
    val passengers: List<RouteChildRow> = emptyList(),
    val nightWarn: List<NightWarnRowUi> = emptyList(),

    // Модальные состояния
    val isSharedPreview: Boolean = false,
    val duplicate: DuplicateUi? = null,
    val showPassenger12h: Boolean = false,
    /** Предзаполнение шторки Passenger12h (§5.5). */
    val p12Departure: Long = 0L,
    val p12Arrival: Long = 0L,
    val p12StationDeparture: String = "",
)

/** Строка шторки «Расчёт за смену» (§5.4, §11.9). */
data class SalaryRowUi(val label: String, val hint: String, val amount: String)

data class SalaryUi(
    val subtitle: String? = null,
    val isCalculated: Boolean = false,
    val total: String = "",
    val rows: List<SalaryRowUi> = emptyList(),
    /** Командировка без среднего часа — кликабельная карточка (§5.4). */
    val averageHourMissing: Boolean = false,
    /** Тарифная ставка не задана — ссылка «Установите значение тарифной ставки…». */
    val tariffMissing: Boolean = false,
)

/** Строка шторки «Отдых» (§5.7). */
data class RestItemUi(val title: String, val duration: String, val until: String)

data class RestUi(
    val isPointOfTurnover: Boolean = false,
    val isSecondTurnaround: Boolean = false,
    /** Не удалось рассчитать — показать карточку «Невозможно рассчитать…» с этим текстом. */
    val unavailableMessage: String? = null,
    val items: List<RestItemUi> = emptyList(),
    /** Подпись под карточкой (формула домашнего отдыха). */
    val footnote: String? = null,
)

/** Одноразовое событие экрана. */
data class RouteFormEvent(
    /** saved | exit | snackbar | share */
    val kind: String,
    val text: String? = null,
    val subject: String? = null,
)
