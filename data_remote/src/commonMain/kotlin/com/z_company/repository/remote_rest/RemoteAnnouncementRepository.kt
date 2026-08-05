package com.z_company.repository.remote_rest

import com.z_company.domain.entities.announcement.Announcement
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
                )
            }
        } catch (e: Exception) {
            null
        }
    }
}
