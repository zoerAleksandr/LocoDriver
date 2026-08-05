package com.z_company.domain.use_cases

import com.z_company.domain.entities.announcement.Announcement
import com.z_company.domain.repositories.AnnouncementRepository
import com.z_company.domain.repositories.SharedPreferencesRepositories

/**
 * Логика показа сообщений при запуске.
 *
 * Отметка «видел/не видел» хранится на устройстве по номеру сообщения:
 * - при первом запуске/после переустановки (lastSeen == [NOT_SEEN]) записываем
 *   пришедший `number` как базовый и НЕ показываем — старые сообщения не всплывают;
 * - иначе показываем, только если `number` больше сохранённого lastSeen;
 * - после закрытия экрана вызывается [markSeen].
 */
class AnnouncementUseCase(
    private val repository: AnnouncementRepository,
    private val sharedPreferences: SharedPreferencesRepositories,
) {
    /** Сообщение, которое нужно показать сейчас, либо null. */
    suspend fun getAnnouncementToShow(platform: String, build: Long): Announcement? {
        val announcement = repository.getLatest(platform, build) ?: return null
        val lastSeen = sharedPreferences.getLastSeenAnnouncementNumber()
        if (lastSeen == NOT_SEEN) {
            // Первый запуск/переустановка — базовая отметка, ничего не показываем.
            sharedPreferences.setLastSeenAnnouncementNumber(announcement.number)
            return null
        }
        return if (announcement.number > lastSeen) announcement else null
    }

    /** Пометить сообщение показанным (после закрытия полноэкранного экрана). */
    fun markSeen(number: Int) {
        sharedPreferences.setLastSeenAnnouncementNumber(number)
    }

    companion object {
        /** Значение lastSeen, означающее «ещё ничего не видел» (первый запуск). */
        const val NOT_SEEN = -1
    }
}
