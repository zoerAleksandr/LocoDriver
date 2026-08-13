package com.z_company.repository.remote_rest

import com.z_company.domain.entities.announcement.Announcement
import com.z_company.domain.entities.announcement.AnnouncementFeature
import com.z_company.domain.repositories.AnnouncementRepository

/**
 * Сетевая реализация [AnnouncementRepository] (эндпоинт `GET /v1/announcements/latest`).
 * При офлайне/ошибке возвращает null — экран сообщения просто не показывается.
 */
class RemoteAnnouncementRepository(
    private val api: RemoteRestApi,
) : AnnouncementRepository {

    override suspend fun getLatest(platform: String, build: Long): Announcement? {
        return try {
            api.getLatestAnnouncement(platform, build)?.let { dto ->
                Announcement(
                    number = dto.number,
                    title = dto.title,
                    body = dto.body,
                    displayMode = dto.displayMode,
                    type = dto.type,
                    imageUrl = absoluteImageUrl(dto.imageUrl),
                    features = dto.features
                        .sortedBy { it.position }
                        .map { f ->
                            AnnouncementFeature(
                                position = f.position,
                                title = f.title,
                                description = f.description,
                                imageUrl = absoluteImageUrl(f.imageUrl),
                            )
                        },
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Склеивает относительный путь картинки с базовым URL API; null → null. */
    private fun absoluteImageUrl(path: String?): String? {
        if (path.isNullOrBlank()) return null
        if (path.startsWith("http://") || path.startsWith("https://")) return path
        return RemoteRestClient.BASE_URL.trimEnd('/') + "/" + path.trimStart('/')
    }
}
