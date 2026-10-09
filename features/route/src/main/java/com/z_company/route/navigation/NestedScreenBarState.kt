package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/**
 * Правило нижнего меню приложения (SCREEN_SPECS §3.1): оно видно только на главных экранах корневых вкладок
 * (Главная, Зарплата, хаб Настроек, хаб Профиля). Любой экран «глубже» — отдельный маршрут (Мои треки, Данные и
 * конфиденциальность, Все маршруты…) или вложенный экран внутри вкладки без своего маршрута (подэкраны Настроек,
 * Корзина и Рефералы в Профиле) — меню скрывает. Для вложенных экранов экран объявляет [HideBottomMenu].
 *
 * Счётчик, а не флаг: при переходе между вкладками (fade) старый экран убирается позже нового,
 * и его `onDispose` не должен вернуть меню, которое новый экран уже просит скрыть.
 */
object NestedScreenBarState {
    private var hiders by mutableIntStateOf(0)

    val hideBottomMenu: Boolean get() = hiders > 0

    internal fun acquire() {
        hiders++
    }

    internal fun release() {
        if (hiders > 0) hiders--
    }
}

/** Скрывает нижнее меню приложения, пока [hide] истинно и экран в композиции. */
@Composable
fun HideBottomMenu(hide: Boolean = true) {
    DisposableEffect(hide) {
        if (hide) NestedScreenBarState.acquire()
        onDispose { if (hide) NestedScreenBarState.release() }
    }
}
