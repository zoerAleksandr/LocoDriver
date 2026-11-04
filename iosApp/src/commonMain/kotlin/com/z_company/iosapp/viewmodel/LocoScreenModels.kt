package com.z_company.iosapp.viewmodel

/**
 * Снимки состояния экрана «Локомотив» (SCREEN_SPECS §1) для SwiftUI.
 *
 * Все числа посчитаны и отформатированы в Kotlin (`CalculationEnergy`, `str()`), Swift только
 * показывает строки. Времена в мс UTC показываются в поясе [LocoScreenState.displayOffsetSeconds].
 */

/** Плашка «↓ ПЕРЕРАСХОД −X (−P%)» / «↑ ЭКОНОМИЯ +X (+P%)» (§1.3 п.6). */
data class LocoResultUi(
    val isOver: Boolean,
    val magnitude: String,
    val percent: Int,
)

/** Секция тепловоза (`DieselSectionItem`). Тексты полей уже в выбранных единицах Л/КГ. */
data class LocoDieselSectionUi(
    val id: String,
    val label: String,
    val acceptedText: String,
    val deliveryText: String,
    /** Подсказка-конвертация во вторую единицу («12.5 кг» / «15 л»), пусто — не показывать. */
    val acceptedHint: String,
    val deliveryHint: String,
    val coefficientText: String,
    /** Значение пилюли «k секции» (пусто → «1.0»). */
    val coefficientPill: String,
    val refuelText: String,
    val refuelKiloText: String,
    val refuelCoefficientText: String,
    val refuelCoefficientPill: String,
    val hasSupplyData: Boolean,
    /** «<кг> кг / <л> л». */
    val resultText: String,
    val isEmpty: Boolean,
)

/** Секция электровоза (`ElectricSectionItem`): сырые строки полей и итоги секции. */
data class LocoElectricSectionUi(
    val id: String,
    val label: String,
    val accepted: String,
    val delivery: String,
    val recoveryAccepted: String,
    val recoveryDelivery: String,
    val accepted2: String,
    val delivery2: String,
    val recoveryAccepted2: String,
    val recoveryDelivery2: String,
    /** «Расход» — всегда (0 без пары). */
    val consumption: String,
    val recovery: String?,
    val consumption2: String?,
    val recovery2: String?,
    val hasRecoveryData: Boolean,
    val isEmpty: Boolean,
)

/** ИТОГО электровоза для одного рода тока. */
data class LocoElectricTotalUi(
    /** «ТОК 1» / «ТОК 2» — только когда токов два. */
    val label: String?,
    val consumed: String,
    val recovery: String?,
    val net: String?,
    val result: LocoResultUi?,
)

/** ИТОГО тепловоза: крупно кг (если есть k) с пилюлей «N л», иначе крупно литры. */
data class LocoDieselTotalUi(
    val fuelBig: String,
    val fuelUnit: String,
    val fuelPill: String?,
    val hasSupply: Boolean,
    val supplyBig: String,
    val supplyUnit: String,
    val supplyPill: String?,
    val result: LocoResultUi?,
)

data class LocoScreenState(
    val isLoading: Boolean = true,
    val isNew: Boolean = true,
    val series: String = "",
    val number: String = "",
    val isElectric: Boolean = true,

    // Время (мс UTC), §1.3 п.3
    val timeStartOfAcceptance: Long? = null,
    val timeEndOfAcceptance: Long? = null,
    val timeBarrierOut: Long? = null,
    val timeBarrierIn: Long? = null,
    val timeStartOfDelivery: Long? = null,
    val timeEndOfDelivery: Long? = null,
    val acceptanceStationId: String? = null,
    val deliveryStationId: String? = null,
    val displayOffsetSeconds: Int = 3 * 3600,
    /** Явка/окончание работы маршрута — нужны шторке времени (§1.1). */
    val routeStartWork: Long? = null,
    val routeEndWork: Long? = null,

    // Настройки видимости (§1.3 п.5, п.4)
    val showHeating: Boolean = false,
    val showAuxiliary: Boolean = false,
    val showOtherCurrent: Boolean = false,
    val isKiloMode: Boolean = false,
    val settingsSeriesName: String = "",

    // ДРУГОЕ (§1.3 п.5)
    val heatingAccepted: String = "",
    val heatingDelivery: String = "",
    val heatingResult: String? = null,
    val auxiliaryAccepted: String = "",
    val auxiliaryDelivery: String = "",
    val auxiliaryResult: String? = null,

    // Секции (§1.3 п.4)
    val sectionCount: Int = 0,
    val dieselSections: List<LocoDieselSectionUi> = emptyList(),
    val electricSections: List<LocoElectricSectionUi> = emptyList(),

    // ИТОГО (§1.3 п.6)
    val electricTotals: List<LocoElectricTotalUi> = emptyList(),
    val dieselTotal: LocoDieselTotalUi? = null,
    val norma1: String = "",
    val norma2: String = "",
    val normaDiesel: String = "",

    /** «Ошибка: <message>» после неудачного сохранения (snackbar). */
    val saveError: String? = null,
)

/** Запись списка серий в пикере (§2.10). */
data class SeriesRowUi(
    val id: String,
    val name: String,
    val isElectric: Boolean,
    val normsText: String,
    val isSelected: Boolean,
)

/** Запись списка станций в пикере (§2.10). */
data class StationRowUi(
    val id: String,
    val name: String,
    val normsText: String,
    val hasNorms: Boolean,
    val isSelected: Boolean,
)
