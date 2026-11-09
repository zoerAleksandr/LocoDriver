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

/** `GET /v1/advisor/graph`: узел графа участков (стык, развилка или тупик). */
@Serializable
data class AdvisorGraphNodeDto(
    val id: String,
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val name: String = "",
)

/** Ребро графа: отрезок оси участка-владельца [sectionId] между узлами, км — по его оси. */
@Serializable
data class AdvisorGraphEdgeDto(
    val id: String,
    val sectionId: String,
    val fromKm: Double = 0.0,
    val toKm: Double = 0.0,
    val fromNode: String = "",
    val toNode: String = "",
    val lengthKm: Double = 0.0,
)

/** Граф оцифрованных участков; `version` меняется при правке участка или кабинета «Стыки». */
@Serializable
data class AdvisorGraphDto(
    val version: String = "",
    val nodes: List<AdvisorGraphNodeDto> = emptyList(),
    val edges: List<AdvisorGraphEdgeDto> = emptyList(),
)

/** Проезд ребра по ходу поезда (`GET /v1/advisor/graph/path`): диапазон км реального участка-владельца. */
@Serializable
data class AdvisorGraphTraversalDto(
    val edge: AdvisorGraphEdgeDto,
    val startKm: Double = 0.0,
    val endKm: Double = 0.0,
)

/** Путь плеча по графу стыков; сервер ищет по всем опубликованным участкам. */
@Serializable
data class AdvisorGraphPathDto(
    val version: String = "",
    val lengthKm: Double = 0.0,
    val traversals: List<AdvisorGraphTraversalDto> = emptyList(),
)

/**
 * Светофор на оси участка. `track` - литер: "E" (Ч, чётное направление) или "O" (Н, нечётное); `kind` - "pass" | "station" |
 * "shunt" | "distant"; `facing` - в какую сторону оси виден лицом (+1 по возрастанию км, −1 против). Поле необязательное:
 * старый сервер его не отдаёт, участок без светофоров тоже.
 */
@Serializable
data class AdvisorSignalDto(
    val km: Double,
    val label: String,
    val track: String = "E",
    val kind: String = "pass",
    val facing: Int = 0,
)

@Serializable
data class AdvisorCrossingDto(
    val km: Double,
    val label: String = "",
    val kind: String = "crossing",
)

@Serializable
data class AdvisorBrakeTestDto(
    val km: Double,
    val label: String = "",
    val kind: String = "brake_test",
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
    val signals: List<AdvisorSignalDto> = emptyList(),
    val crossings: List<AdvisorCrossingDto> = emptyList(),
    val brakeTests: List<AdvisorBrakeTestDto> = emptyList(),
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
