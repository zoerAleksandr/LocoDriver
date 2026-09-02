package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.z_company.domain.navigation.Router
import com.z_company.route.component.AppAlertDialog
import com.z_company.route.viewmodel.RouteActionsHelper
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Единая точка входа на экран покупок.
 *
 * Подписка живёт на сервере и привязана к аккаунту: срок (`subscriptionPeriod`)
 * приезжает в приложение только при синхронизации авторизованного пользователя.
 * Если оплатить, не войдя в аккаунт, деньги спишутся, а срок в приложении
 * не обновится — синхронизировать его будет некому. Поэтому перед переходом
 * проверяем авторизацию (`RouteActionsHelper.isAuthorized()`) и без неё
 * показываем диалог и уводим в «Профиль», где форма входа/регистрации.
 *
 * Все переходы на `PurchasesRoute` (главный экран, форма маршрута, «Все
 * маршруты», мастер графика, профиль, диалоги подписки в `LocoDriverApp`)
 * идут через этот хелпер, а не через `router.showPurchasesScreen()` напрямую.
 */
@Composable
fun rememberShowPurchasesScreen(router: Router): () -> Unit {
    val scope = rememberCoroutineScope()
    val routeHelper: RouteActionsHelper = koinInject()

    var showAuthRequiredDialog by remember { mutableStateOf(false) }

    if (showAuthRequiredDialog) {
        AppAlertDialog(
            onDismissRequest = { showAuthRequiredDialog = false },
            title = "Нужен вход в аккаунт",
            text = "Подписка привязывается к аккаунту. Войдите или " +
                    "зарегистрируйтесь в «Профиле» — это займёт минуту.",
            confirmText = "Войти",
            onConfirm = {
                showAuthRequiredDialog = false
                router.showProfile()
            },
            dismissText = "Отмена",
            onDismiss = { showAuthRequiredDialog = false }
        )
    }

    return remember(router, scope, routeHelper) {
        {
            scope.launch {
                if (routeHelper.isAuthorized()) {
                    router.showPurchasesScreen()
                } else {
                    showAuthRequiredDialog = true
                }
            }
            Unit
        }
    }
}
