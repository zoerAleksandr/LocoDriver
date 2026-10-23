package com.z_company.repository.remote_rest.tracks

import kotlinx.serialization.Serializable

/*
 * Контракт `/v1/advisor/sections` (чтение) и `POST /v1/advisor/altitude-contrib` (сервер: backend/src/api/v1/advisor.py,
 * api/v1/altitude_contrib.py, schemas/tracks.py). Все поля camelCase; лишние поля игнорируются. Координаты не логируются.
 */

/** Элемент `GET /v1/advisor/sections`: признак геометрии, хэш оси и данные для списка участков. */
@Serializable
data class AdvisorSectionSummaryDto(
    val id: String,
    val hasGeometry: Boolean = false,
    val axisHash: String? = null,
    val name: String = "",
    val fromStation: String = "",
    val toStation: String = "",
    val lengthKm: Double = 0.0,
    val elementsCount: Int = 0,
    val version: Int = 0,
    val updatedAt: Double = 0.0,
    /** Габарит оси `[minLat, minLon, maxLat, maxLon]`; null - у участка нет геометрии или сервер старый (поле не отдаёт). */
    val bbox: List<Double>? = null,
)

/** Элемент профиля: уклон на `[startKm; endKm]` оси. */
@Serializable
data class AdvisorElementDto(
    val startKm: Double,
    val endKm: Double,
    val gradePermille: Double = 0.0,
    val curveRadiusM: Double = 0.0,
    val speedLimitKmh: Int = 0,
    /** Профиль на этом месте неизвестен (данные собираются): рисуется серая зона вместо уклона. */
    val noData: Boolean = false,
)

/** Отметка километража: в месте `km` оси координата `kmAbs`, дальше нумерация `dir` (+1 растёт, −1 убывает). */
@Serializable
data class AdvisorKmMarkDto(
    val km: Double,
    val kmAbs: Double,
    val dir: Int = 1,
    val note: String = "",
)

/** Станция на оси участка. */
@Serializable
data class AdvisorStationDto(
    val km: Double,
    val name: String,
    val lat: Double? = null,
    val lon: Double? = null,
)

/** Точка оси участка: `km` - от начала оси, высота клиенту не нужна. */
@Serializable
data class AdvisorGeoPointDto(val km: Double, val lat: Double, val lon: Double)

/** `GET /v1/advisor/sections/{id}`: ось участка и её хэш. */
@Serializable
data class AdvisorSectionDto(
    val id: String,
    val axisHash: String? = null,
    val geometry: List<AdvisorGeoPointDto> = emptyList(),
    val name: String = "",
    val fromStation: String = "",
    val toStation: String = "",
    val lengthKm: Double = 0.0,
    val version: Int = 0,
    val updatedAt: Double = 0.0,
    val elements: List<AdvisorElementDto> = emptyList(),
    val kmMarks: List<AdvisorKmMarkDto> = emptyList(),
    val stations: List<AdvisorStationDto> = emptyList(),
)

/**
 * Тело вклада. Нет user_id, времени и координат. `data` - base64(zlib(токены высот)), `offsetM` - по одному числу на
 * ячейку `data` (null для ячеек без высоты).
 */
@Serializable
data class AltitudeContribRequest(
    val contribId: String,
    val sectionId: String,
    val axisHash: String,
    val kmFromM: Int,
    val stepM: Int,
    val source: String,
    val meanAccM: Double? = null,
    val data: String,
    val offsetM: List<Double?>,
)

/** `{accepted, status: stored|duplicate|empty, dropped}`. */
@Serializable
data class AltitudeContribResponse(
    val accepted: Boolean = false,
    val status: String = "",
    val dropped: Int = 0,
)
