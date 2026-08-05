package com.z_company.domain.repositories

import com.z_company.domain.entities.announcement.Announcement

/**
 * Источник широковещательных сообщений (эндпоинт `GET /v1/announcements/latest`).
 */
interface AnnouncementRepository {
    /**
     * Актуальное активное сообщение для данной платформы/версии, либо null,
     * если показывать нечего (нет активного, 204 от сервера, или ошибка сети —
     * реализация не должна падать при офлайне).
     *
     * @param platform "android" | "ios"
     * @param build номер сборки клиента (versionCode) — для фильтра min/max версии
     */
    suspend fun getLatest(platform: String, build: Long): Announcement?
}
