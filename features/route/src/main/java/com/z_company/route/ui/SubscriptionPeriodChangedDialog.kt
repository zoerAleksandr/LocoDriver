package com.z_company.route.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.z_company.route.R
import com.z_company.route.viewmodel.SubscriptionPeriodChange
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val SUBSCRIPTION_DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy")

/**
 * Глобальный диалог «Подписка продлена» (рекуррентное списание, оплата на
 * другой платформе, правка на сервере). Только рост срока — см.
 * SubscriptionNoticesViewModel. Тот же [PaymentDialog], что у «Платёж
 * принят!» и «Бонус начислен!».
 */
@Composable
fun SubscriptionPeriodChangedDialog(
    change: SubscriptionPeriodChange,
    onDismiss: () -> Unit,
) {
    val until = Instant.ofEpochMilli(change.current)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(SUBSCRIPTION_DATE_FORMAT)
    PaymentDialog(
        iconRes = R.drawable.check_circle_24px,
        iconTone = MaterialTheme.colorScheme.surfaceTint,
        title = "Подписка продлена",
        body = "Срок подписки продлён до $until. Спасибо за поддержку приложения!",
        onDismiss = onDismiss,
        primaryLabel = "Отлично!",
        onPrimary = onDismiss,
    )
}

/**
 * Глобальный диалог «Платёж принят!» — оплата, начатая в этом приложении,
 * подтверждена сервером (PaymentReturnChecker). Показывается на любом
 * экране, каким бы способом пользователь ни вернулся из оплаты.
 */
@Composable
fun PaymentAcceptedDialog(
    paidUntil: Long,
    onDismiss: () -> Unit,
) {
    val until = Instant.ofEpochMilli(paidUntil)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(SUBSCRIPTION_DATE_FORMAT)
    PaymentDialog(
        iconRes = R.drawable.check_circle_24px,
        iconTone = MaterialTheme.colorScheme.surfaceTint,
        title = "Платёж принят!",
        body = "Подписка активна до $until. Спасибо за поддержку приложения!",
        onDismiss = onDismiss,
        primaryLabel = "Отлично!",
        onPrimary = onDismiss,
    )
}
