package com.z_company.domain.brake

import com.z_company.domain.entities.norma_time.LocomotiveSeries
import com.z_company.domain.entities.route.LocoType

/** Предзаполнение полей серии справочника: объём ГР, оси, нажатие, вес, число секций. */
data class SeriesBrakeDefaults(
    val sectionCount: Int,
    val brakeVolumeL: Int? = null,
    val axleCount: Int? = null,
    val axleLoadTf: Double? = null,
    val weightT: Double? = null,
)

/** Поля данных локомотива серии (редактор справочника и справка ВУ-45). */
enum class SeriesPassportField { SECTIONS, WEIGHT, AXLES, AXLE_LOAD, VOLUME }

/** Данные локомотива для справки ВУ-45: вес, оси, нажатие, объём ГР. */
data class LocoPassport(
    val weightT: Double? = null,
    val axleCount: Int? = null,
    val axleLoadTf: Double? = null,
    val brakeVolumeL: Int? = null,
    /** Пометка источника справочных значений каталога (показывается в справке). */
    val note: String? = null,
)

/**
 * Что подставляется в справочник серий пользователя. Правило достоверности: только значения, подтверждённые
 * документами (табл. 4-5 Инструкции 277 / проект прил. 2 - вес, оси, нажатие; файл 1 - объём ГР), и значения
 * владельца для 2ЭС4К/3ЭС4К/2ЭС5К/3ЭС5К/2ЭС6 (нажатие 14,0; объём ГР 2000/3000 л). Справочный вес этих
 * ЭС-серий, выведенные по допущениям объёмы ГР и ВЛ10 (объём зависит от номера) не предзаполняются.
 */
object LocoSeriesDefaults {
    /**
     * Секций по названию серии: серия начинается с цифры - это цифра (2ЭС6 -> 2, 3ЭС4К -> 3, 4ТЭ10С -> 4);
     * иначе 1 (ЭП1М, ВЛ80, ВЛ10, ЧС7, ТЭМ2, ЧМЭ3: цифра внутри названия - часть индекса, а не число секций).
     * Ноль в начале и пустое название дают 1.
     */
    fun sectionCountFor(name: String?): Int {
        val first = name?.trim()?.firstOrNull() ?: return 1
        return if (first in '1'..'9') first - '0' else 1
    }

    fun forSeries(name: String?): SeriesBrakeDefaults {
        val loco = BrakeLocoCatalog.find(name)
        return SeriesBrakeDefaults(
            sectionCount = sectionCountFor(name),
            brakeVolumeL = BrakeDensity.confirmedVolumeLiters(name),
            axleCount = loco?.axles, // оси ЭС-серий (8 у двухсекционных, 12 у трёхсекционных) - открытая информация, подтверждены владельцем
            axleLoadTf = loco?.pressurePerAxle,
            weightT = loco?.takeIf { it.weightAxlesConfirmed }?.weight,
        )
    }

    /** Справочные значения каталога для справки ВУ-45 (вес и оси - включая «справочно»): последний уровень подстановки. */
    fun catalogFor(name: String?): LocoBrakeData? = BrakeLocoCatalog.find(name)

    /** Пустые поля серии заполняются значениями по умолчанию; заполненные не трогаются. */
    fun LocomotiveSeries.withDefaults(): LocomotiveSeries {
        val d = forSeries(name)
        return copy(
            brakeVolumeL = brakeVolumeL ?: d.brakeVolumeL,
            axleCount = axleCount ?: d.axleCount,
            axleLoadTf = axleLoadTf ?: d.axleLoadTf,
            weightT = weightT ?: d.weightT,
            sectionCount = sectionCount ?: d.sectionCount,
        )
    }

    /** Нужна ли серии разовая запись значений по умолчанию (число секций не задано - ещё не заполнялась). */
    fun needsDefaults(series: LocomotiveSeries): Boolean = series.sectionCount == null

    /**
     * Данные локомотива для справки ВУ-45. Приоритет по каждому полю: запись справочника пользователя ->
     * каталог (в справке - и справочные вес и оси ЭС-серий, с пометкой источника; объём ГР - только подтверждённый) -> пусто.
     */
    fun passportFor(name: String?, user: LocomotiveSeries?): LocoPassport {
        val catalog = BrakeLocoCatalog.find(name)
        return LocoPassport(
            weightT = user?.weightT ?: catalog?.weight,
            axleCount = user?.axleCount ?: catalog?.axles,
            axleLoadTf = user?.axleLoadTf ?: catalog?.pressurePerAxle,
            brakeVolumeL = user?.brakeVolumeL ?: BrakeDensity.confirmedVolumeLiters(name),
            // пометка относится к значениям каталога: если вес, оси и нажатие целиком пользовательские - пометки нет
            note = catalog?.note?.takeIf { user == null || user.weightT == null || user.axleCount == null || user.axleLoadTf == null },
        )
    }

    /** Тип тяги по названию серии, когда он неизвестен (ТЭ, ТЭМ, ТЭП, ТГ, ЧМЭ, М62 - тепловозы, остальное - электровозы). */
    fun guessType(name: String?): LocoType {
        val n = name?.trim()?.uppercase().orEmpty()
        return if (listOf("ТЭ", "ТГ", "МЭ", "М62").any { n.contains(it) }) LocoType.DIESEL else LocoType.ELECTRIC
    }

    /**
     * Запись серии после того, как пользователь изменил в справке ВУ-45 поля [fields] (значения - в [edited]).
     * Записи нет - создаётся (остальные поля - по умолчанию); есть - перезаписываются только изменённые поля.
     * null - писать нечего: значения совпали с тем, что и так подставляется (запись не создаётся и updatedAt не растёт).
     */
    fun recordWithEdits(
        name: String,
        existing: LocomotiveSeries?,
        edited: LocoPassport,
        fields: Set<SeriesPassportField>,
        now: Long,
    ): LocomotiveSeries? {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || fields.isEmpty()) return null
        val base = existing
            ?: LocomotiveSeries(name = trimmed, type = guessType(trimmed), updatedAt = now).withDefaults()
        var r = base
        for (f in fields) {
            r = when (f) {
                SeriesPassportField.WEIGHT -> r.copy(weightT = edited.weightT)
                SeriesPassportField.AXLES -> r.copy(axleCount = edited.axleCount)
                SeriesPassportField.AXLE_LOAD -> r.copy(axleLoadTf = edited.axleLoadTf)
                SeriesPassportField.VOLUME -> r.copy(brakeVolumeL = edited.brakeVolumeL)
                SeriesPassportField.SECTIONS -> r
            }
        }
        val unchanged = if (existing == null) {
            val shown = passportFor(trimmed, null)
            fields.all {
                when (it) {
                    SeriesPassportField.WEIGHT -> shown.weightT == edited.weightT
                    SeriesPassportField.AXLES -> shown.axleCount == edited.axleCount
                    SeriesPassportField.AXLE_LOAD -> shown.axleLoadTf == edited.axleLoadTf
                    SeriesPassportField.VOLUME -> shown.brakeVolumeL == edited.brakeVolumeL
                    SeriesPassportField.SECTIONS -> true
                }
            }
        } else r == existing
        return if (unchanged) null else r.copy(updatedAt = now)
    }
}
