package com.z_company.repository.remote_rest

import com.z_company.repository.remote_rest.SettingsSyncAction.NOOP
import com.z_company.repository.remote_rest.SettingsSyncAction.PULL
import com.z_company.repository.remote_rest.SettingsSyncAction.PUSH
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Сценарии из задачи «LWW по дате для настроек». Само сравнение — [SettingsLww];
 * здесь фиксируем, что флаг settingsPending на решение не влияет и что строка,
 * созданная сервером при оплате (updateAt = 0), никогда не побеждает локальную.
 */
class SettingsLwwTest {

    @Test
    fun `пользователь без подписки правил станции — два синка подряд не затирают локальное`() {
        // Сервер строки не отдаёт (404) → в SyncManager remote == null → noop; сама
        // синхронизация без подписки вообще не стартует. Здесь — что даже при
        // равных метках после первого прохода второй ничего не переносит.
        val localAt = 1_700_000_000_100L
        assertEquals(NOOP, SettingsLww.decide(localAt, localAt))
        assertEquals(NOOP, SettingsLww.decide(localAt, localAt))
    }

    @Test
    fun `после оплаты сервер создал дефолты с updateAt = 0 — локальные настройки побеждают`() {
        assertEquals(PUSH, SettingsLww.decide(localUpdatedAt = 1_700_000_000_100L, remoteUpdatedAt = 0L))
        // Даже давняя локальная правка новее серверного «нуля».
        assertEquals(PUSH, SettingsLww.decide(localUpdatedAt = 1L, remoteUpdatedAt = 0L))
    }

    @Test
    fun `настройки изменены на другом устройстве — серверные заменяют локальные`() {
        assertEquals(PULL, SettingsLww.decide(localUpdatedAt = 1_700_000_000_100L, remoteUpdatedAt = 1_700_000_000_200L))
        // Свежая установка / старая БД без метки (0) — всегда pull.
        assertEquals(PULL, SettingsLww.decide(localUpdatedAt = 0L, remoteUpdatedAt = 1_700_000_000_200L))
    }

    @Test
    fun `зарплатные настройки — та же схема`() {
        assertEquals(PUSH, SettingsLww.decide(200L, 100L))
        assertEquals(PULL, SettingsLww.decide(100L, 200L))
        assertEquals(NOOP, SettingsLww.decide(200L, 200L))
        // Пустая серверная строка (нет updated_at → 0) и локальная без правок (0): ничего не делаем.
        assertEquals(NOOP, SettingsLww.decide(0L, 0L))
    }
}
