package com.z_company.route.ui

import androidx.compose.runtime.Composable
import com.z_company.route.component.AppAlertDialog
import com.z_company.route.viewmodel.RouteActionsHelper
import com.z_company.route.viewmodel.SubscriptionLimitState

/**
 * Диалог «пачка маршрутов не помещается в бесплатный лимит».
 *
 * Показывается там, где маршруты создаются сразу пачкой — в «Календаре»
 * (режим планирования) и в мастере «Заполнить месяц». Раньше в этих местах
 * был только snackbar, а своего `SnackbarHost` у экранов не было, поэтому
 * для пользователя маршруты «просто не создавались».
 */
@Composable
fun SubscriptionLimitDialog(
    limit: SubscriptionLimitState,
    onDismiss: () -> Unit,
    onPurchases: () -> Unit,
) {
    val total = RouteActionsHelper.FREE_ROUTES_LIMIT
    val text = if (limit.remaining == 0) {
        "Бесплатный лимит исчерпан: использовано $total из $total. " +
            "Сейчас создаётся ${limit.requested} ${routesWord(limit.requested)}. " +
            "Оформите подписку — лимит снимется и включится синхронизация."
    } else {
        "В бесплатном периоде доступно $total маршрутов, у вас осталось " +
            "${limit.remaining}. Сейчас создаётся ${limit.requested} " +
            "${routesWord(limit.requested)}. Оформите подписку — лимит снимется " +
            "и включится синхронизация."
    }

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = "Не хватает бесплатных маршрутов",
        text = text,
        confirmText = "Оформить подписку",
        onConfirm = onPurchases,
        dismissText = "Отмена",
        onDismiss = onDismiss,
    )
}

private fun routesWord(n: Int): String {
    val nn = n % 100
    val n1 = n % 10
    return when {
        nn in 11..14 -> "маршрутов"
        n1 == 1 -> "маршрут"
        n1 in 2..4 -> "маршрута"
        else -> "маршрутов"
    }
}
