package com.z_company.core.gps

/**
 * Одна GPS-точка. Координаты не логируются и не сохраняются самим компонентом записи —
 * потребитель решает, что с ними делать.
 *
 * @property timeMs время фикса, мс от Unix epoch (UTC).
 * @property speedMps скорость в м/с или null, если провайдер её не сообщил.
 * @property accuracyM горизонтальная точность в метрах или null.
 * @property altitudeM высота GPS в метрах (над уровнем моря, если провайдер отдаёт) или null.
 * @property baroAltitudeM барометрическая высота в метрах (датчик давления, стандартная атмосфера) или null, если датчика нет.
 */
data class GpsPoint(
    val timeMs: Long,
    val latitude: Double,
    val longitude: Double,
    val speedMps: Float?,
    val accuracyM: Float?,
    val altitudeM: Double? = null,
    val baroAltitudeM: Double? = null,
) {
    val speedKmh: Double? get() = speedMps?.let { it * 3.6 }
}
