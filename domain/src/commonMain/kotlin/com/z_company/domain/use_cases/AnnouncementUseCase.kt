package com.z_company.domain.use_cases

import com.z_company.domain.entities.announcement.Announcement
import com.z_company.domain.repositories.AnnouncementRepository
import com.z_company.domain.repositories.SharedPreferencesRepositories

/**
 * Логика показа сообщений при запуске — простая и предсказуемая.
 *
 * Режим показа приходит с сервера в [Announcement.displayMode]:
 * - [Announcement.DISPLAY_MODE_ONCE] («один раз») — показать активное сообщение,
 *   только если его `number` больше локально сохранённого «просмотренного» номера
 *   (`lastSeenAnnouncementNumber`, по умолчанию `-1`). После закрытия экрана номер
 *   сохраняется ([markSeen]), поэтому повторно то же сообщение не показывается.
 * - [Announcement.DISPLAY_MODE_ALWAYS] («каждый запуск») — показывать при каждом
 *   старте, пока сообщение активно на сервере; `lastSeenNumber` игнорируется.
 *   Показ прекращается, когда владелец выключает сообщение или меняет режим в кабинете.
 *
 * Управление с сервера — через `number` (для режима «один раз»):
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
        // «Каждый запуск»: показываем всегда, пока сообщение активно (его вернул сервер).
        if (announcement.displayMode == Announcement.DISPLAY_MODE_ALWAYS) {
            return announcement
        }
        // «Один раз»: показываем, только если сообщение новее уже виденного.
        val lastSeen = sharedPreferences.getLastSeenAnnouncementNumber()
        return if (announcement.number > lastSeen) announcement else null
    }

    /** Пометить сообщение показанным (после закрытия полноэкранного экрана). */
    fun markSeen(number: Int) {
        sharedPreferences.setLastSeenAnnouncementNumber(number)
    }
}
