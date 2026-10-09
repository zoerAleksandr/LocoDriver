package com.z_company.repository.remote_rest.tracks

import kotlinx.serialization.Serializable

/*
 * Контракт `/v1/advisor/sections` (чтение) и `POST /v1/advisor/altitude-contrib` (сервер: backend/src/api/v1/advisor.py,
 * api/v1/altitude_contrib.py, schemas/tracks.py). Все поля camelCase; лишние поля игнорируются. Координаты не логируются.
 */

/** Элемент `GET /v1/advisor/sections`: нужны только признак геометрии и хэш оси. */
@Serializable
data class AdvisorSectionSummaryDto(
    val id: String,
    val hasGeometry: Boolean = false,
    val axisHash: String? = null,
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
