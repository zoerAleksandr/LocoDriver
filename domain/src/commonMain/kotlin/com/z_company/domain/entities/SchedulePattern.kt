package com.z_company.domain.entities

import com.z_company.domain.util.generateId
import kotlinx.serialization.Serializable

/**
 * Тип смены в цикле графика мастера «Заполнить месяц».
 * DAY — дневная, NIGHT — ночная, OFF — выходной.
 */
@Serializable
enum class WorkShiftType { DAY, NIGHT, OFF }

/**
 * Паттерн графика для мастера «Заполнить месяц».
 *
 * Хранится ЛОКАЛЬНО (SharedPreferences), НЕ синхронизируется с сервером —
 * это личная UI-настройка. Список паттернов редактируемый: стандартные
 * (2/2, 4/4, 2/1) можно удалить, а свой цикл после применения сохраняется
 * новым паттерном.
 *
 * @param cycle последовательность типов смен одного периода цикла;
 *              раскладывается по дням месяца от «первого дня».
 */
@Serializable
data class SchedulePattern(
    val id: String = generateId(),
    val title: String,          // крупная подпись плитки, напр. «2/2»
    val subtitle: String,       // описание, напр. «день · ночь · вых · вых»
    val cycle: List<WorkShiftType>,
) {
    companion object {
        /** Паттерны по умолчанию (сидируются при первом запуске мастера). */
        fun defaults(): List<SchedulePattern> = listOf(
            SchedulePattern(
                id = "default-2-2",
                title = "2/2",
                subtitle = "день · ночь · вых · вых",
                cycle = listOf(WorkShiftType.DAY, WorkShiftType.NIGHT, WorkShiftType.OFF, WorkShiftType.OFF),
            ),
            SchedulePattern(
                id = "default-4-4",
                title = "4/4",
                subtitle = "2 дня · 2 ночи · 4 вых",
                cycle = listOf(
                    WorkShiftType.DAY, WorkShiftType.DAY, WorkShiftType.NIGHT, WorkShiftType.NIGHT,
                    WorkShiftType.OFF, WorkShiftType.OFF, WorkShiftType.OFF, WorkShiftType.OFF,
                ),
            ),
            SchedulePattern(
                id = "default-2-1",
                title = "2/1",
                subtitle = "день · ночь · вых",
                cycle = listOf(WorkShiftType.DAY, WorkShiftType.NIGHT, WorkShiftType.OFF),
            ),
        )
    }
}
