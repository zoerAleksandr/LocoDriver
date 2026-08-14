package com.z_company.loco_driver.ui.theme

import com.z_company.core.theme.ThemeManager
import com.z_company.core.theme.ThemeMode
import com.z_company.domain.repositories.SharedPreferencesRepositories
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Хранит выбранную пользователем тему оформления в [SharedPreferencesRepositories]
 * (локально, без синхронизации с сервером) и отдаёт её реактивно через [themeMode],
 * чтобы корневой [LocoDriverTheme] пересобирался при переключении.
 *
 * По умолчанию — [ThemeMode.MODE_SYSTEM] (как в системе).
 */
class ThemeManagerImpl(
    private val prefs: SharedPreferencesRepositories,
) : ThemeManager {

    private val _themeMode = MutableStateFlow(readSavedTheme())
    override val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    override fun setTheme(theme: ThemeMode) {
        prefs.setThemeMode(theme.name)
        _themeMode.value = theme
    }

    /**
     * null — тема «как в системе» (решает вызывающий по `isSystemInDarkTheme()`),
     * true — тёмная, false — светлая.
     */
    override fun isDark(theme: ThemeMode): Boolean? = when (theme) {
        ThemeMode.MODE_SYSTEM -> null
        ThemeMode.MODE_DARK -> true
        ThemeMode.MODE_LIGHT -> false
    }

    private fun readSavedTheme(): ThemeMode =
        prefs.getThemeMode()?.let { name ->
            runCatching { ThemeMode.valueOf(name) }.getOrNull()
        } ?: ThemeMode.MODE_SYSTEM
}
