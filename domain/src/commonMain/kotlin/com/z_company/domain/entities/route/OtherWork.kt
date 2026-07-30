package com.z_company.domain.entities.route

import com.z_company.domain.util.generateId
import kotlinx.serialization.Serializable

/**
 * «Прочая работа» — зонтичный подраздел маршрута наряду с локомотивом,
 * поездом и следованием пассажиром. Каждая запись описывает отдельный вид
 * работы (маневровая, вывозная, при депо или пользовательский тип) с временем
 * начала/окончания, станцией и примечанием.
 *
 * Раздел информационный: данные хранятся и синхронизируются, но НЕ участвуют
 * в расчёте рабочего времени и зарплаты.
 *
 * [workType] — имя типа работы (предопределённое из [PREDEFINED_TYPES] или
 * пользовательское из `UserSettings.otherWorkTypeList`). Хранится строкой —
 * как `stationList`/`locomotiveSeriesList`, чтобы поддержать кастомные типы.
 *
 * Все поля nullable/с дефолтами — контракт full-replace + ignoreUnknownKeys,
 * обратная совместимость со старыми клиентами/сервером.
 */
@Serializable
data class OtherWork(
    var otherWorkId: String = generateId(),
    var basicId: String = "",
    var remoteObjectId: String? = null,
    var workType: String? = null,
    var timeStart: Long? = null,
    var timeEnd: Long? = null,
    var station: String? = null,
    var notes: String? = null,
) {
    companion object {
        const val TYPE_SHUNTING = "Маневровая"
        const val TYPE_HAUL_OUT = "Вывозная"
        const val TYPE_AT_DEPOT = "При депо"

        /** Предопределённые типы прочей работы (в порядке отображения). */
        val PREDEFINED_TYPES: List<String> = listOf(TYPE_SHUNTING, TYPE_HAUL_OUT, TYPE_AT_DEPOT)
    }
}
