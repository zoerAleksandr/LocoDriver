package com.z_company.route.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.z_company.domain.brake.BrakeHolding
import com.z_company.domain.brake.BrakeLocoCatalog
import com.z_company.domain.brake.BrakePrefill
import com.z_company.domain.brake.HoldingInput
import com.z_company.domain.brake.HoldingRadioData
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
    /** Момент остановки (мс, «Остановились» на вкладке «Порядок действий»); null - таймер не запущен. Хранится в SharedPreferences. */
    val stopStartMs: Long? = null,
    /** Номера отмеченных шагов порядка действий. */
    val doneSteps: Set<Int> = emptySet(),
    /** Поля радиосообщений (вкладка «Порядок действий»). */
    val radio: HoldingRadioData = HoldingRadioData(),
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

class BrakeHoldingViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("brake_holding_prefs", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(
        BrakeHoldingUiState(
            stopStartMs = prefs.getLong(KEY_STOP, 0L).takeIf { it > 0L },
            doneSteps = prefs.getStringSet(KEY_DONE, emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.toSet(),
            // место остановки (километр/пикет по GPS) хранится вместе с остановкой; остальное в сообщениях - из поезда и локомотива
            radio = HoldingRadioData(
                km = prefs.getString(KEY_KM, "").orEmpty(),
                picket = prefs.getString(KEY_PICKET, "").orEmpty(),
                // поезд и локомотив запомнены только на время идущей остановки (иначе подставляются из «Поезда»)
                trainNumber = if (prefs.getLong(KEY_STOP, 0L) > 0L) prefs.getString(KEY_TRAIN, "").orEmpty() else "",
                locoSeries = if (prefs.getLong(KEY_STOP, 0L) > 0L) prefs.getString(KEY_SERIES, "").orEmpty() else "",
                locoNumber = if (prefs.getLong(KEY_STOP, 0L) > 0L) prefs.getString(KEY_NUMBER, "").orEmpty() else "",
            ),
        ),
    )
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
                // реквизиты радиосообщений: из поезда и локомотива маршрута (при запущенной остановке не затираем)
                radio = if (it.stopStartMs != null) it.radio else it.radio.copy(
                    trainNumber = s.trainNumber ?: it.radio.trainNumber,
                    locoSeries = s.locoSeries ?: it.radio.locoSeries,
                    locoNumber = s.locoNumber ?: it.radio.locoNumber,
                ),
            )
        }
    }

    fun setWeight(v: String) = _state.update { it.copy(weightText = v) }
    fun setLocoWeight(v: String) = _state.update { it.copy(locoWeightText = v) }
    fun setAxles(v: String) = _state.update { it.copy(axlesText = v) }
    fun setSlope(v: String) = _state.update { it.copy(slopeText = v) }
    fun setShoes(v: String) = _state.update { it.copy(shoesText = v) }
    fun setLoad(v: HoldingLoad?) = _state.update { it.copy(loadManual = v) }

    /** «Остановились»: запуск таймера от текущего момента (повторное нажатие не перезапускает). */
    fun startStop(nowMs: Long = System.currentTimeMillis()) {
        if (_state.value.stopStartMs != null) return
        _state.update { it.copy(stopStartMs = nowMs) }
        prefs.edit().putLong(KEY_STOP, nowMs)
            .putString(KEY_TRAIN, _state.value.radio.trainNumber).putString(KEY_SERIES, _state.value.radio.locoSeries).putString(KEY_NUMBER, _state.value.radio.locoNumber)
            .apply()
    }

    /** Сброс остановки: таймер, отметки шагов и место остановки. */
    fun resetStop() {
        _state.update { it.copy(stopStartMs = null, doneSteps = emptySet(), radio = it.radio.copy(km = "", picket = "")) }
        prefs.edit().remove(KEY_STOP).remove(KEY_DONE).remove(KEY_KM).remove(KEY_PICKET).apply()
    }

    fun toggleStep(i: Int) {
        val next = _state.value.doneSteps.let { if (i in it) it - i else it + i }
        _state.update { it.copy(doneSteps = next) }
        prefs.edit().putStringSet(KEY_DONE, next.map(Int::toString).toSet()).apply()
    }

    /** Место остановки по GPS: километр и пикет для радиосообщений. */
    fun setPlace(km: String, picket: String) {
        _state.update { it.copy(radio = it.radio.copy(km = km, picket = picket)) }
        prefs.edit().putString(KEY_KM, km).putString(KEY_PICKET, picket).apply()
    }

    private companion object {
        const val KEY_STOP = "stop_start_ms"; const val KEY_DONE = "done_steps"
        const val KEY_KM = "r_km"; const val KEY_PICKET = "r_picket"
        const val KEY_TRAIN = "r_train"; const val KEY_SERIES = "r_series"; const val KEY_NUMBER = "r_number"
    }

    private fun fmt(v: Double): String = if (v % 1.0 == 0.0) v.toLong().toString() else v.toString().replace('.', ',')
}
