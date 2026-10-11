package com.z_company.route.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.z_company.domain.brake.AxleRow
import com.z_company.domain.brake.BrakeCertificateInput
import com.z_company.domain.brake.BrakeNorms
import com.z_company.domain.brake.CompositeShare
import com.z_company.domain.brake.GradeClass
import com.z_company.domain.brake.LocoBrakeInput
import com.z_company.domain.util.toFiniteDoubleOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Строка таблицы нажатий: [presetIndex] - строка табл. 3 (или null - своё значение), [pressureText] - нажатие на ось. */
data class BrakeRowUi(
    val id: Int,
    val presetIndex: Int? = null,
    val pressureText: String = "",
    val axlesText: String = "",
)

data class BrakeCertificateUiState(
    val categoryId: String = "FREIGHT_LOADED",
    val weightText: String = "",
    val declaredAxlesText: String = "",
    val rows: List<BrakeRowUi> = listOf(BrakeRowUi(id = 0)),
    val locoEnabled: Boolean = false,
    val locoWeightText: String = "",
    val locoAxlesText: String = "",
    val locoPressureText: String = "",
    val grade: GradeClass = GradeClass.UP_TO_006,
    val autoblock3: Boolean = false,
    val compositeShare: CompositeShare = CompositeShare.NONE,
    val manualNorm: BrakeNorms.ManualBrakeNorm = BrakeNorms.ManualBrakeNorm.UNIFIED_06,
    val actualManualText: String = "",
    val nextRowId: Int = 1,
)

/** Экран-калькулятор «Справка ВУ-45»: данные не сохраняются (кроме выбранной нормы ручных тормозов). */
class BrakeCertificateViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(BrakeCertificateUiState(manualNorm = loadManualNorm()))
    val state: StateFlow<BrakeCertificateUiState> = _state.asStateFlow()

    private var prefilled = false

    /** Предзаполнение из поезда маршрута (один раз за жизнь VM). */
    fun prefill(weight: String?, axles: String?) {
        if (prefilled) return
        prefilled = true
        // Нулевые/нечисловые значения поезда (поле не заполнено) за данные не считаем.
        val w = weight?.trim().orEmpty().takeIf { (it.toFiniteDoubleOrNull() ?: 0.0) > 0 }.orEmpty()
        val a = axles?.trim().orEmpty().takeIf { (it.toIntOrNull() ?: 0) > 0 }.orEmpty()
        if (w.isEmpty() && a.isEmpty()) return
        _state.update {
            it.copy(
                weightText = w,
                declaredAxlesText = a,
                rows = listOf(BrakeRowUi(id = 0, axlesText = a)),
            )
        }
    }

    private fun loadManualNorm(): BrakeNorms.ManualBrakeNorm =
        runCatching { BrakeNorms.ManualBrakeNorm.valueOf(prefs.getString(KEY_MANUAL_NORM, null) ?: "") }
            .getOrDefault(BrakeNorms.ManualBrakeNorm.UNIFIED_06)

    fun setCategory(id: String) = _state.update {
        val cat = BrakeNorms.category(id) ?: return@update it
        it.copy(categoryId = id, locoEnabled = cat.locoCountedByDefault, compositeShare = CompositeShare.NONE)
    }

    fun setWeight(v: String) = _state.update { it.copy(weightText = v) }
    fun setDeclaredAxles(v: String) = _state.update { it.copy(declaredAxlesText = v) }
    fun setGrade(g: GradeClass) = _state.update { it.copy(grade = g) }
    fun setAutoblock(v: Boolean) = _state.update { it.copy(autoblock3 = v) }
    fun setCompositeShare(v: CompositeShare) = _state.update { it.copy(compositeShare = v) }
    fun setActualManual(v: String) = _state.update { it.copy(actualManualText = v) }
    fun setLocoEnabled(v: Boolean) = _state.update { it.copy(locoEnabled = v) }
    fun setLocoWeight(v: String) = _state.update { it.copy(locoWeightText = v) }
    fun setLocoAxles(v: String) = _state.update { it.copy(locoAxlesText = v) }
    fun setLocoPressure(v: String) = _state.update { it.copy(locoPressureText = v) }

    fun setManualNorm(norm: BrakeNorms.ManualBrakeNorm) {
        prefs.edit().putString(KEY_MANUAL_NORM, norm.name).apply()
        _state.update { it.copy(manualNorm = norm) }
    }

    fun addRow() = _state.update {
        it.copy(rows = it.rows + BrakeRowUi(id = it.nextRowId), nextRowId = it.nextRowId + 1)
    }

    fun removeRow(id: Int) = _state.update {
        val left = it.rows.filterNot { r -> r.id == id }
        it.copy(rows = left.ifEmpty { listOf(BrakeRowUi(id = it.nextRowId)) }, nextRowId = it.nextRowId + 1)
    }

    fun setRowPreset(id: Int, presetIndex: Int?) = updateRow(id) { r ->
        if (presetIndex == null) r.copy(presetIndex = null)
        else r.copy(
            presetIndex = presetIndex,
            pressureText = formatPressure(BrakeNorms.axlePresets[presetIndex].pressure),
        )
    }

    fun setRowPressure(id: Int, v: String) = updateRow(id) { it.copy(presetIndex = null, pressureText = v) }
    fun setRowAxles(id: Int, v: String) = updateRow(id) { it.copy(axlesText = v) }

    private fun updateRow(id: Int, f: (BrakeRowUi) -> BrakeRowUi) = _state.update {
        it.copy(rows = it.rows.map { r -> if (r.id == id) f(r) else r })
    }

    companion object {
        private const val PREFS = "brake_certificate_prefs"
        private const val KEY_MANUAL_NORM = "manual_norm"

        fun formatPressure(p: Double): String =
            if (p % 1.0 == 0.0) p.toInt().toString() else p.toString().replace('.', ',')
    }
}

/** Собирает вход калькулятора; null - данных недостаточно (нет веса или категории). */
fun BrakeCertificateUiState.toInput(): BrakeCertificateInput? {
    val cat = BrakeNorms.category(categoryId) ?: return null
    val weight = weightText.toFiniteDoubleOrNull()?.takeIf { it > 0 } ?: return null
    val axleRows = rows.mapNotNull { r ->
        val p = r.pressureText.toFiniteDoubleOrNull() ?: return@mapNotNull null
        val n = r.axlesText.trim().toIntOrNull() ?: return@mapNotNull null
        if (p <= 0 || n <= 0) null else AxleRow(p, n)
    }
    val loco = if (locoEnabled) {
        val w = locoWeightText.toFiniteDoubleOrNull()
        val a = locoAxlesText.trim().toIntOrNull()
        val p = locoPressureText.toFiniteDoubleOrNull()
        if (w != null && a != null && p != null && w > 0 && a > 0 && p > 0) LocoBrakeInput(w, a, p) else null
    } else null
    return BrakeCertificateInput(
        category = cat,
        compositionWeight = weight,
        rows = axleRows,
        loco = loco,
        grade = grade,
        autoblock3 = autoblock3,
        compositeShare = compositeShare,
        manualNorm = manualNorm,
        actualManualAxles = actualManualText.trim().toIntOrNull(),
        declaredAxles = declaredAxlesText.trim().toIntOrNull(),
    )
}
