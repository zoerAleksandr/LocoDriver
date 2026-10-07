@file:OptIn(ExperimentalEncodingApi::class)

package com.z_company.repository.remote_rest.tracks

import com.z_company.domain.track.TrackBlobTools
import com.z_company.domain.track.TrackCodecException
import com.z_company.domain.track.TrackPoint
import com.z_company.domain.track.TrackWireCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** Чтение истории с сервера: список карточек, скачивание трека целиком, превью. */
class TrackHistoryLoader(
    private val api: TracksApi,
    private val tools: TrackBlobTools,
) {
    data class Page(val tracks: List<TrackCardResponse>, val nextCursor: String?)

    suspend fun page(token: String, cursor: String?, limit: Int = PAGE_SIZE): Page {
        val r = api.list(token, limit, cursor, favorite = null)
        return Page(r.tracks, r.nextCursor)
    }

    /**
     * Скачать трек: карточка со списком чанков -> каждый чанк -> проверка sha256 -> декодирование.
     * Бросает [TrackCodecException], если данные повреждены, и [TracksApiException] при HTTP-ошибке.
     */
    suspend fun download(token: String, trackId: String): List<TrackPoint> {
        val detail = api.getTrack(token, trackId)
        val out = ArrayList<TrackPoint>(detail.pointsCount)
        for (chunk in detail.chunks.sortedBy { it.n }) {
            val payload = api.getChunk(token, trackId, chunk.n)
            if (payload.enc != TrackWireCodec.ENC_NAME) throw TrackCodecException("неизвестный формат чанка")
            val blob = Base64.decode(payload.data)
            val expected = payload.sha256
            if (expected != null && !expected.equals(tools.sha256Hex(blob), ignoreCase = true)) {
                throw TrackCodecException("контрольная сумма чанка не совпала")
            }
            out += withContext(Dispatchers.Default) { TrackWireCodec.decodeChunk(blob, tools) }
        }
        return out.sortedBy { it.timeMs }
    }

    /** Ломаная мини-карты карточки; пустая, если превью нет или оно не читается. */
    fun previewPoints(card: TrackCardResponse): List<TrackPoint> {
        val p = card.preview ?: return emptyList()
        if (p.enc != TrackWireCodec.ENC_NAME) return emptyList()
        return try {
            TrackWireCodec.decodeChunk(Base64.decode(p.data), tools)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Звезда серверного трека (для треков, которых нет на устройстве). */
    suspend fun setFavorite(token: String, trackId: String, favorite: Boolean) {
        api.patch(token, trackId, TrackPatchRequest(isFavorite = favorite))
    }

    /** Удалить серверный трек (идемпотентно). */
    suspend fun delete(token: String, trackId: String) = api.delete(token, trackId)

    /** `DELETE /tracks {confirm:true}`: удалить ВСЕ серверные треки пользователя. */
    suspend fun deleteAllRemote(token: String): Int = api.deleteAll(token).deleted

    companion object {
        const val PAGE_SIZE = 50
    }
}
