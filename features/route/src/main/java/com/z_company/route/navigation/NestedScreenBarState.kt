package com.z_company.route.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Вложенные экраны внутри корневой вкладки (например «Плечи» в Настройках) не имеют своего маршрута,
 * поэтому нижнее меню приложения скрывает этот флаг: экран ставит его на время показа.
 */
object NestedScreenBarState {
    var hideBottomMenu by mutableStateOf(false)
}
