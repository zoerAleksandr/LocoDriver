package com.z_company.repository.remote_rest.tracks

import kotlinx.serialization.Serializable

/*
 * Контракт `/v1/tracks` (сервер: backend/src/api/v1/tracks.py, schemas/tracks.py). Все поля camelCase.
 * Координаты и точки нигде не логируются; блобы — `{"enc":"ld-track-v1","data":"<base64>"}`.
 */

@Serializable
data class TrackConsentRequest(val storageGranted: Boolean, val policyVersion: String? = null)

/** Ответ GET (storageGranted, grantedAt, policyVersion) и PUT (storageGranted, deletedTracks) `/tracks/consent`. */
@Serializable
data class TrackConsentResponse(
    val storageGranted: Boolean = false,
    val grantedAt: Long? = null,
    val policyVersion: String? = null,
    val deletedTracks: Int = 0,
)

@Serializable
data class TrackEncPayload(val enc: String, val data: String)

@Serializable
data class TrackMetaRequest(
    val routeId: String? = null,
    val title: String = "",
    val startedAt: Long,
    val endedAt: Long,
    val distanceKm: Double = 0.0,
    val maxSpeedKmh: Double = 0.0,
    val avgSpeedKmh: Double = 0.0,
    val pointsCount: Int = 0,
    val chunksExpected: Int? = null,
    val preview: TrackEncPayload? = null,
)

/** Карточка трека (ответ `PUT/PATCH /tracks/{id}`, элемент списка, основа `GET /tracks/{id}`). */
@Serializable
data class TrackCardResponse(
    val id: String,
    val title: String = "",
    val routeId: String? = null,
    val startedAt: Long = 0L,
    val endedAt: Long = 0L,
    val distanceKm: Double = 0.0,
    val durationS: Long = 0L,
    val avgSpeedKmh: Double = 0.0,
    val maxSpeedKmh: Double = 0.0,
    val pointsCount: Int = 0,
    val isFavorite: Boolean = false,
    val status: String = "",
    val expiresAt: Long? = null,
    val preview: TrackEncPayload? = null,
    /** Только в `GET /tracks/{id}`. */
    val chunks: List<TrackChunkInfo> = emptyList(),
)

@Serializable
data class TrackChunkInfo(val n: Int, val pointsCount: Int = 0, val tFrom: Long = 0L, val tTo: Long = 0L)

@Serializable
data class TrackChunkRequest(val enc: String, val data: String, val sha256: String)

/** `status`: `stored` | `duplicate`. */
@Serializable
data class TrackChunkResponse(val chunkNo: Int, val status: String)

@Serializable
data class TrackCompleteRequest(val chunks: Int)

/** `status`: `complete` | `uploading` (тогда в [missing] номера недостающих чанков). */
@Serializable
data class TrackCompleteResponse(val status: String, val missing: List<Int> = emptyList())

@Serializable
data class TrackListResponse(val tracks: List<TrackCardResponse> = emptyList(), val nextCursor: String? = null)

@Serializable
data class TrackChunkPayload(val enc: String, val data: String, val sha256: String? = null)

@Serializable
data class TrackPatchRequest(val isFavorite: Boolean? = null, val title: String? = null)

@Serializable
data class TrackDeleteAllRequest(val confirm: Boolean)

@Serializable
data class TrackDeleteAllResponse(val deleted: Int = 0)
