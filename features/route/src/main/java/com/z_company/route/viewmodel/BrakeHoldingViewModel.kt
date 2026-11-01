package com.z_company.route.viewmodel

import androidx.lifecycle.ViewModel
import com.z_company.domain.brake.BrakeHolding
import com.z_company.domain.brake.BrakeLocoCatalog
import com.z_company.domain.brake.BrakePrefill
import com.z_company.domain.brake.HoldingInput
import com.z_company.domain.brake.HoldingLoad
import com.z_company.domain.util.toFiniteDoubleOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Состояние инструмента «Закрепление поезда» (SCREEN_SPECS §27). Поля - текст, как ввёл пользователь. */
data class BrakeHoldingUiState(
    val weightText: String = "",
    val locoWeightText: String = "",
    val axlesText: String = "",
    val slopeText: String = "",
    val shoesText: String = "",
    /** Груженый/порожний, выбранный вручную; null - определяется по нагрузке на ось. */
    val loadManual: HoldingLoad? = null,
    val locoSeries: String = "",
    /** Поезд подставлен из раздела «Поезд» (для подписи). */
    val fromTrain: Boolean = false,
)

/** Груженый/порожний по нагрузке на ось (вес без локомотива / оси); null - вычислить нельзя. */
fun BrakeHoldingUiState.autoLoad(): HoldingLoad? = BrakeHolding.loadByAxleLoad(weightText.toFiniteDoubleOrNull(), axlesText.trim().toIntOrNull())

/** Ручной выбор, иначе автоопределение; null - ещё не определено (пользователь выберет сам). */
fun BrakeHoldingUiState.load(): HoldingLoad? = loadManual ?: autoLoad()

fun BrakeHoldingUiState.toInput(): HoldingInput? {
    val weight = weightText.toFiniteDoubleOrNull()?.takeIf { it > 0.0 } ?: return null
    val slope = slopeText.toFiniteDoubleOrNull()?.takeIf { it >= 0.0 } ?: return null
    val load = load() ?: return null
    return HoldingInput(
        trainWeight = weight,
        locoWeight = locoWeightText.toFiniteDoubleOrNull()?.takeIf { it > 0.0 } ?: 0.0,
        load = load,
        slopePermille = slope,
        shoesAvailable = shoesText.trim().toIntOrNull()?.coerceAtLeast(0) ?: 0,
        trainAxles = axlesText.trim().toIntOrNull()?.takeIf { it > 0 },
    )
}

class BrakeHoldingViewModel : ViewModel() {
    private val _state = MutableStateFlow(BrakeHoldingUiState())
    val state: StateFlow<BrakeHoldingUiState> = _state.asStateFlow()
    private var prefilled = false

    /** Подстановка данных поезда (один раз за открытие экрана): вес, оси, локомотив (вес - из справочника серий, если серия есть). */
    fun prefill(p: BrakePrefill?) {
        if (prefilled) return
        prefilled = true
        val s = p?.sanitized() ?: return
        val loco = BrakeLocoCatalog.find(s.locoSeries)
        _state.update {
            it.copy(
                weightText = s.weight ?: it.weightText,
                axlesText = s.axles ?: it.axlesText,
                locoSeries = s.locoSeries.orEmpty(),
                locoWeightText = loco?.weight?.let(::fmt) ?: it.locoWeightText,
                fromTrain = s.weight != null || s.axles != null,
            )
        }
    }

    fun setWeight(v: String) = _state.update { it.copy(weightText = v) }
    fun setLocoWeight(v: String) = _state.update { it.copy(locoWeightText = v) }
    fun setAxles(v: String) = _state.update { it.copy(axlesText = v) }
    fun setSlope(v: String) = _state.update { it.copy(slopeText = v) }
    fun setShoes(v: String) = _state.update { it.copy(shoesText = v) }
    fun setLoad(v: HoldingLoad?) = _state.update { it.copy(loadManual = v) }

    private fun fmt(v: Double): String = if (v % 1.0 == 0.0) v.toLong().toString() else v.toString().replace('.', ',')
}
