package com.z_company.domain.use_cases

import com.z_company.domain.entities.announcement.Announcement
import com.z_company.domain.repositories.AnnouncementRepository
import com.z_company.domain.repositories.SharedPreferencesRepositories

/**
 * Логика показа сообщений при запуске — простая и предсказуемая:
 * показать активное сообщение, если его `number` больше локально сохранённого
 * «просмотренного» номера (`lastSeenAnnouncementNumber`, по умолчанию `-1` —
 * пользователь ещё ничего не видел). После закрытия экрана номер сохраняется
 * ([markSeen]), поэтому одно и то же сообщение повторно не показывается.
 *
 * Управление с сервера — через `number`:
 * - чтобы разослать новое сообщение всем, задайте `number` **больше** предыдущего;
 * - новые установки (lastSeen = -1) увидят текущее активное сообщение один раз;
 * - повторный POST с тем же/меньшим `number` никому не покажется (уже видели).
 */
class AnnouncementUseCase(
    private val repository: AnnouncementRepository,
    private val sharedPreferences: SharedPreferencesRepositories,
) {
    /** Сообщение, которое нужно показать сейчас, либо null. */
    suspend fun getAnnouncementToShow(platform: String, build: Long): Announcement? {
        val announcement = repository.getLatest(platform, build) ?: return null
        val lastSeen = sharedPreferences.getLastSeenAnnouncementNumber()
        return if (announcement.number > lastSeen) announcement else null
    }

    /** Пометить сообщение показанным (после закрытия полноэкранного экрана). */
    fun markSeen(number: Int) {
        sharedPreferences.setLastSeenAnnouncementNumber(number)
    }
}
