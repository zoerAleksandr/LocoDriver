package com.z_company.route.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.lifecycle.flowWithLifecycle
import com.robokassa.library.pay.RobokassaPayLauncher
import com.z_company.core.ui.component.CustomSnackBar
import com.z_company.core.ui.snackbar.ISnackbarManager
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTheme
import com.z_company.domain.entities.Product
import com.z_company.route.R
import com.z_company.route.viewmodel.BillingEvent
import com.z_company.route.viewmodel.BillingState
import com.z_company.route.viewmodel.PurchasesViewModel
import com.z_company.route.component.PullToSyncContainer
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.roundToInt

// ── Состояние экрана покупки по времени окончания подписки ──────────────────
private sealed interface PurchaseUi {
    /** Подписки никогда не было — витрина тарифов. */
    data object Paywall : PurchaseUi

    /** Подписка активна. daysLeft — сколько дней осталось (для баннера «скоро истечёт»). */
    data class Active(val endTime: Long, val daysLeft: Long) : PurchaseUi

    /** Подписка была, но истекла — возобновление. */
    data class Expired(val endTime: Long) : PurchaseUi
}

private const val EXPIRING_SOON_DAYS = 7L
private const val DAY_MS = 24L * 60 * 60 * 1000

/** Количество месяцев в тарифе по его названию ("1 месяц" / "3 месяца" / "1 год"). */
private fun Product.periodMonths(): Int = when {
    name.contains("год", ignoreCase = true) -> 12
    name.trimStart().startsWith("3") -> 3
    else -> 1
}

/** Суффикс к цене в CTA: «/мес», «/год» или пусто. */
private fun Product.priceSuffix(): String = when (periodMonths()) {
    12 -> "/год"
    1 -> "/мес"
    else -> ""
}

/** Метка прибавляемого периода для листа продления: «+ 1 месяц», «+ 3 месяца», «+ 1 год». */
private fun Product.addLabel(): String = when (periodMonths()) {
    12 -> "+ 1 год"
    3 -> "+ 3 месяца"
    else -> "+ 1 месяц"
}

private val PROMO_DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy")

/** ms Unix epoch → «dd.MM.yyyy» в локальной зоне (для «Акция до …»). */
private fun formatPromoUntil(ms: Long): String =
    Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate().format(PROMO_DATE_FORMAT)

private fun daysWord(n: Long): String {
    val nn = n % 100
    val n1 = n % 10
    return when {
        nn in 11..14 -> "дней"
        n1 == 1L -> "день"
        n1 in 2..4 -> "дня"
        else -> "дней"
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchasesScreen(
    viewModel: PurchasesViewModel,
    billingState: BillingState,
    onProductClick: (Product) -> Unit,
    onBack: () -> Unit,
    eventSharedFlow: SharedFlow<BillingEvent>,
    isPullRefreshing: Boolean = false,
    onPullRefresh: () -> Unit = {},
    pullSyncMessage: String? = null,
    onPullSyncMessageShown: () -> Unit = {},
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }

    val showPaymentLoadingDialog by viewModel.showPaymentLoadingDialog.collectAsState()
    val showPaymentFailedDialog by viewModel.showPaymentFailedDialog.collectAsState()
    val context = LocalContext.current

    if (showPaymentLoadingDialog) {
        PaymentDialog(
            title = "Получаем данные…",
            body = "Проверяем оплату на сервере — это займёт несколько секунд.",
            dismissible = false,
            onDismiss = {},
            showSpinner = true,
        )
    }

    if (showPaymentFailedDialog) {
        PaymentDialog(
            iconRes = R.drawable.ic_pro_alert,
            iconTone = MaterialTheme.colorScheme.error,
            title = "Оплата не завершена",
            body = "Данные об оплате не получены. Если у вас есть вопросы, напишите в поддержку.",
            onDismiss = { viewModel.dismissPaymentFailedDialog() },
            primaryLabel = "Написать в поддержку",
            onPrimary = {
                viewModel.dismissPaymentFailedDialog()
                val email = "locodriver.app@yandex.ru"
                val subject = "Вопрос по оплате"
                val mailIntent = Intent(Intent.ACTION_SENDTO).apply {
                    data = "mailto:$email?subject=$subject".toUri()
                }
                try {
                    context.startActivity(mailIntent)
                } catch (_: Exception) {
                    context.startActivity(
                        Intent(Intent.ACTION_SENDTO).apply { data = "mailto:".toUri() }
                    )
                }
            },
            secondaryLabel = "Закрыть",
            onSecondary = { viewModel.dismissPaymentFailedDialog() },
        )
    }

    val showPaymentSuccessDialog by viewModel.showPaymentSuccessDialog.collectAsState()

    if (showPaymentSuccessDialog) {
        PaymentDialog(
            iconRes = R.drawable.check_circle_24px,
            iconTone = MaterialTheme.colorScheme.surfaceTint,
            title = "Платёж принят!",
            body = "Спасибо за поддержку приложения!",
            onDismiss = { viewModel.dismissPaymentSuccessDialog() },
            primaryLabel = "Отлично!",
            onPrimary = { viewModel.dismissPaymentSuccessDialog() },
        )
    }

    val showPaymentProcessingDialog by viewModel.showPaymentProcessingDialog.collectAsState()

    if (showPaymentProcessingDialog) {
        PaymentDialog(
            iconRes = R.drawable.ic_pro_schedule,
            iconTone = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = "Платёж обрабатывается",
            body = "Robokassa подтвердила платёж, но сервер ещё не обновил подписку. Нажмите «Восстановить» через несколько минут.",
            onDismiss = { viewModel.dismissPaymentProcessingDialog() },
            primaryLabel = "Понятно",
            onPrimary = { viewModel.dismissPaymentProcessingDialog() },
        )
    }

    val snackbarManager: ISnackbarManager = koinInject()

    LaunchedEffect(Unit) {
        snackbarManager.events
            .flowWithLifecycle(lifecycle)
            .collect { event ->
                val result = snackbarHostState.showSnackbar(
                    message = event.message,
                    actionLabel = event.actionLabel,
                    duration = event.duration
                )
                if (result == SnackbarResult.ActionPerformed) {
                    event.onAction?.let { onAction ->
                        launch {
                            try {
                                onAction()
                            } catch (_: Exception) { /* optional logging */
                            }
                        }
                    }
                }
            }
    }

    val payLauncher = rememberLauncherForActivityResult(RobokassaPayLauncher.Contract) { result ->
        when (result) {
            is RobokassaPayLauncher.Success -> {
                Log.d("zzz", "RobokassaPayLauncher.Success")
                viewModel.checkPaymentOnServer(sdkConfirmed = true)
            }

            is RobokassaPayLauncher.Error -> {
                Log.d("zzz", "RobokassaPayLauncher.Error: ${result.desc}")
                viewModel.checkPaymentOnServer(sdkConfirmed = false)
            }

            is RobokassaPayLauncher.Canceled -> {
                Log.d("zzz", "RobokassaPayLauncher.Canceled")
                viewModel.checkPaymentOnServer(sdkConfirmed = false)
            }
        }
    }

    LaunchedEffect(Unit) {
        scope.launch {
            eventSharedFlow.flowWithLifecycle(lifecycle).collect { event ->
                when (event) {
                    is BillingEvent.ShowError -> {
                        snackbarHostState.showSnackbar(message = "Ошибка: ${event.error.message.orEmpty()}")
                    }

                    is BillingEvent.StartPayment -> {
                        payLauncher.launch(
                            RobokassaPayLauncher.StartPay(
                                paymentParams = event.params,
                                onlyCheck = event.onlyChek,
                                testMode = false
                            )
                        )
                    }
                }
            }
        }
    }

    // ── Данные состояния ───────────────────────────────────────────────────
    val purchasesEndTimeInLong = viewModel.purchasesEndTime.collectAsState()
    val currentState by viewModel.state.collectAsState()
    val converter = currentState.dateAndTimeConverter

    val endTime = purchasesEndTimeInLong.value
    val now = System.currentTimeMillis()

    val purchaseState: PurchaseUi = when {
        endTime == 0L -> PurchaseUi.Paywall
        endTime > now -> PurchaseUi.Active(
            endTime = endTime,
            daysLeft = ceil((endTime - now).toDouble() / DAY_MS).toLong()
        )

        else -> PurchaseUi.Expired(endTime)
    }

    // Тарифы сверху вниз: год (лучшая цена) → 3 месяца → месяц. Год выбран по умолчанию.
    val plans = remember(billingState.products) {
        billingState.products.sortedByDescending { it.sum }
    }
    val monthlyBase = remember(plans) {
        plans.minByOrNull { it.periodMonths() }?.let { it.sum / it.periodMonths() }
    }
    // Есть ли активная акция хоть в одном тарифе — тогда зелёные бейджи «выгоды»
    // не показываем, вместо них оранжевый «Акция −N%» у цены.
    val anyPromo = remember(plans) { plans.any { it.discountActive && it.discountPercent > 0 } }
    var selectedProduct by remember(plans) { mutableStateOf(plans.firstOrNull()) }

    // Статус подписки известен (загружен из локальных настроек). Пока нет —
    // держим нейтральный лоадинг, не показываем «неактивную» шапку.
    val isSubscriptionLoaded by viewModel.isSubscriptionLoaded.collectAsState()
    val tariffsLoading = billingState.isLoading

    val showRestore = isSubscriptionLoaded &&
        (purchaseState is PurchaseUi.Paywall || purchaseState is PurchaseUi.Expired)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(
                            tint = MaterialTheme.colorScheme.primary,
                            painter = painterResource(com.z_company.core.R.drawable.ic_clear),
                            contentDescription = "Закрыть"
                        )
                    }
                },
                actions = {
                    if (showRestore) {
                        TextButton(onClick = viewModel::restoreSubscribe) {
                            Text(
                                text = "Восстановить",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { snackBarData ->
                CustomSnackBar(snackBarData = snackBarData)
            }
        },
    ) { padding ->
        PullToSyncContainer(
            isRefreshing = isPullRefreshing,
            onRefresh = onPullRefresh,
            message = pullSyncMessage,
            onMessageShown = onPullSyncMessageShown,
            modifier = Modifier.padding(padding),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .testTag("purchases_scroll_column")
                    .padding(horizontal = 16.dp),
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                if (!isSubscriptionLoaded) {
                    // Статус подписки ещё грузится — нейтральная шапка + лоадер
                    // тарифов, без вида «неактивной подписки».
                    ProHero()
                    TariffsLoading()
                } else when (purchaseState) {
                    is PurchaseUi.Paywall -> {
                        ProHeroWithFeatures()
                        SectionLabel("ВЫБЕРИТЕ ТАРИФ")
                        if (tariffsLoading) TariffsLoading() else PlanList(
                            plans = plans,
                            selected = selectedProduct,
                            monthlyBase = monthlyBase,
                            anyPromo = anyPromo,
                            onSelect = { selectedProduct = it },
                        )
                    }

                    is PurchaseUi.Active -> {
                        ProHero()
                        Spacer(modifier = Modifier.height(16.dp))
                        StatusBanner(
                            iconRes = R.drawable.ic_pro_check,
                            tone = MaterialTheme.colorScheme.surfaceTint, // success
                            title = "Подписка активна",
                            subtitle = {
                                // Один Text: при крупном шрифте перенос идёт по пробелу,
                                // а дата «17.02.27» — один токен и остаётся целой (не «17.02.2/7»).
                                val dateStr = converter?.getDate(purchaseState.endTime) ?: ""
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                                            append("Действует до ")
                                        }
                                        withStyle(
                                            SpanStyle(
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        ) {
                                            append(dateStr)
                                        }
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            },
                        )

                        if (purchaseState.daysLeft in 0..EXPIRING_SOON_DAYS) {
                            Spacer(modifier = Modifier.height(10.dp))
                            WarningBanner(daysLeft = purchaseState.daysLeft)
                        }

                        SectionLabel("ПЕРИОД ПРОДЛЕНИЯ")
                        if (tariffsLoading) {
                            TariffsLoading()
                        } else {
                            DateTransitionCard(
                                currentEndTime = purchaseState.endTime,
                                selected = selectedProduct,
                                converter = converter,
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            PlanList(
                                plans = plans,
                                selected = selectedProduct,
                                monthlyBase = monthlyBase,
                                anyPromo = anyPromo,
                                onSelect = { selectedProduct = it },
                            )
                        }
                    }

                    is PurchaseUi.Expired -> {
                        ProHero()
                        Spacer(modifier = Modifier.height(16.dp))
                        StatusBanner(
                            iconRes = R.drawable.ic_pro_alert,
                            tone = MaterialTheme.colorScheme.error,
                            title = "Подписка истекла",
                            subtitle = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Закончилась ",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = converter?.getDate(purchaseState.endTime) ?: "",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                    )
                                }
                            },
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(Shapes.medium)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.05f))
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "Маршруты и история сохранены. Но добавлять новые и пользоваться синхронизацией нельзя, пока подписка не возобновлена.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        SectionLabel("ВЫБЕРИТЕ ТАРИФ")
                        if (tariffsLoading) TariffsLoading() else PlanList(
                            plans = plans,
                            selected = selectedProduct,
                            monthlyBase = monthlyBase,
                            anyPromo = anyPromo,
                            onSelect = { selectedProduct = it },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // ── Закреплённая снизу CTA + подпись ────────────────────────────
            // Пока грузятся статус/тарифы (нет выбранного тарифа) — CTA прячем.
            if (isSubscriptionLoaded && !tariffsLoading && selectedProduct != null) {
            val ctaLabel: String
            val ctaNote: String
            val onCta: () -> Unit
            when (purchaseState) {
                is PurchaseUi.Paywall -> {
                    ctaLabel = selectedProduct?.let {
                        "Оформить за ${it.sum.toInt()} ₽${it.priceSuffix()}"
                    } ?: "Оформить"
                    ctaNote =
                        "Первые 20 маршрутов — бесплатно. Дальше добавление маршрутов — только по подписке."
                    onCta = { selectedProduct?.let(onProductClick) }
                }

                is PurchaseUi.Active -> {
                    ctaLabel = selectedProduct?.let {
                        "Продлить · ${it.sum.toInt()} ₽"
                    } ?: "Продлить"
                    ctaNote = "Новый период прибавится к текущему сроку."
                    onCta = { selectedProduct?.let(onProductClick) }
                }

                is PurchaseUi.Expired -> {
                    ctaLabel = "Возобновить подписку"
                    ctaNote = "Возобновление начнётся с сегодняшнего дня."
                    onCta = { selectedProduct?.let(onProductClick) }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(
                    onClick = onCta,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(
                        text = ctaLabel,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = ctaNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            }
        }
    }

}
}

// ── Единый диалог возврата с оплаты (стиль ConfirmDeleteDialog) ─────────────
@Composable
private fun PaymentDialog(
    title: String,
    onDismiss: () -> Unit,
    body: String? = null,
    iconRes: Int? = null,
    iconTone: Color? = null,
    dismissible: Boolean = true,
    primaryLabel: String? = null,
    onPrimary: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    showSpinner: Boolean = false,
) {
    val cs = MaterialTheme.colorScheme
    Dialog(
        onDismissRequest = { if (dismissible) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = dismissible,
            dismissOnClickOutside = dismissible,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(cs.surface)
                .border(1.dp, cs.outlineVariant, RoundedCornerShape(22.dp))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (iconRes != null && iconTone != null) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(iconTone.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        modifier = Modifier.size(26.dp),
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = iconTone,
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = cs.primary,
                textAlign = TextAlign.Center,
            )

            if (!body.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            if (showSpinner) {
                Spacer(modifier = Modifier.height(20.dp))
                CircularProgressIndicator(color = cs.tertiary)
            }

            if (primaryLabel != null && onPrimary != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(cs.tertiary)
                        .clickable(onClick = onPrimary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = primaryLabel,
                        color = cs.surface,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }

            if (secondaryLabel != null && onSecondary != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, cs.outline, RoundedCornerShape(14.dp))
                        .clickable(onClick = onSecondary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = secondaryLabel,
                        color = cs.primary,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    )
                }
            }
            }
        }
    }

// ── Hero «Машинист Pro» ────────────────────────────────────────────────────
@Composable
private fun ProHero(modifier: Modifier = Modifier, flush: Boolean = false) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (flush) Modifier else Modifier.clip(Shapes.medium))
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 20.dp, vertical = 22.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(R.drawable.ic_pro_crown),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Column {
                Text(
                    text = "Машинист Pro",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Text(
                    text = "ПОЛНАЯ ВЕРСИЯ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.55f),
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Облако, экспорт и безлимит истории. Все поездки под рукой и в безопасности.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
        )
    }
}

/** Hero + список преимуществ в единой карточке (витрина). */
@Composable
private fun ProHeroWithFeatures() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 1.dp, shape = Shapes.medium)
            .clip(Shapes.medium)
    ) {
        ProHero(flush = true)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 18.dp, vertical = 6.dp)
        ) {
            FeatureRow(
                iconRes = com.z_company.core.R.drawable.rounded_cloud_upload_24,
                label = "Облачная копия и синхронизация",
            )
            FeatureRow(
                iconRes = R.drawable.picture_as_pdf_24px,
                label = "Экспорт в PDF",
            )
            FeatureRow(
                iconRes = R.drawable.notes_24px,
                label = "Безлимит маршрутов и истории",
            )
        }
    }
}

@Composable
private fun FeatureRow(iconRes: Int, label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(17.dp),
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Spacer(modifier = Modifier.height(20.dp))
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 10.dp),
    )
}

/** Лоадер на месте списка тарифов, пока они тянутся с сервера. */
@Composable
private fun TariffsLoading() {
    Spacer(modifier = Modifier.height(20.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.tertiary,
            strokeWidth = 3.dp,
        )
    }
}

// ── Список тарифов ─────────────────────────────────────────────────────────
@Composable
private fun PlanList(
    plans: List<Product>,
    selected: Product?,
    monthlyBase: Double?,
    anyPromo: Boolean,
    onSelect: (Product) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        plans.forEach { product ->
            PlanCard(
                product = product,
                selected = selected?.name == product.name,
                monthlyBase = monthlyBase,
                anyPromo = anyPromo,
                onClick = { onSelect(product) },
            )
        }
    }
}

@Composable
private fun PlanCard(
    product: Product,
    selected: Boolean,
    monthlyBase: Double?,
    anyPromo: Boolean,
    onClick: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.tertiary
    val orange = AppTheme.colors.warnOrange
    val perMonth = (product.sum / product.periodMonths()).roundToInt()
    // Промо-скидка (акция из кабинета).
    val promo = product.discountActive && product.discountPercent > 0
    // «Выгода тарифа» (зелёный бейдж у цены месяца) — показываем ТОЛЬКО когда ни
    // у одного тарифа нет активной акции. Иначе визуал остаётся за оранжевой акцией.
    val tieredDiscount = if (anyPromo) null else monthlyBase
        ?.takeIf { it > 0 }
        ?.let { (100 * (1 - perMonth / it)).roundToInt() }
        ?.takeIf { it > 0 }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Shapes.medium)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .border(
                width = 1.5.dp,
                color = if (selected) accent else MaterialTheme.colorScheme.outlineVariant,
                shape = Shapes.medium,
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Кастомный radio (кружок 22dp)
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(11.dp))
                .border(
                    width = 2.dp,
                    color = if (selected) accent else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(11.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(accent)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
            // Цена за месяц + зелёный бейдж «выгоды тарифа» (только когда нет акции).
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 3.dp),
            ) {
                Text(
                    text = "$perMonth ₽/мес",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (tieredDiscount != null) {
                    DiscountPill(text = "−$tieredDiscount%")
                }
            }
            // Срок действия акции — только если задан.
            if (promo && product.discountUntil != null) {
                Text(
                    text = "Действует до ${formatPromoUntil(product.discountUntil!!)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = orange,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        // Цена справа: при акции — старая цена зачёркнута над новой + оранжевый
        // бейдж «Акция −N%» прижат к стоимости подписки.
        if (promo) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${product.basePrice.toInt()} ₽",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textDecoration = TextDecoration.LineThrough,
                )
                Text(
                    text = "${product.sum.toInt()} ₽",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(3.dp))
                PromoPill(text = "Акция −${product.discountPercent}%")
            }
        } else {
            Text(
                text = "${product.sum.toInt()} ₽",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun DiscountPill(text: String) {
    val success = MaterialTheme.colorScheme.surfaceTint
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(success.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = success,
            maxLines = 1,
            softWrap = false,
        )
    }
}

/** Оранжевый бейдж акции «Акция −N%» (промо из кабинета админа). */
@Composable
private fun PromoPill(text: String) {
    val orange = AppTheme.colors.warnOrange
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(orange.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = orange,
            maxLines = 1,
            softWrap = false,
        )
    }
}

// ── Баннеры статуса ────────────────────────────────────────────────────────
@Composable
private fun StatusBanner(
    iconRes: Int,
    tone: Color,
    title: String,
    subtitle: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Shapes.medium)
            .background(tone.copy(alpha = 0.10f))
            .border(1.dp, tone.copy(alpha = 0.28f), Shapes.medium)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tone.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(22.dp),
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = tone,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            subtitle()
        }
    }
}

@Composable
private fun WarningBanner(daysLeft: Long) {
    val warning = MaterialTheme.colorScheme.surfaceContainerHigh
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Shapes.medium)
            .background(warning.copy(alpha = 0.12f))
            .border(1.dp, warning.copy(alpha = 0.32f), Shapes.medium)
            .padding(horizontal = 15.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(warning),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(21.dp),
                painter = painterResource(R.drawable.ic_pro_schedule),
                contentDescription = null,
                tint = Color(0xFF1B1300),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Подписка истекает через $daysLeft ${daysWord(daysLeft)}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Продлите заранее — новый срок прибавится к текущему, дни не сгорят.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Переход дат для продления: Сейчас до → +период → Станет до ──────────────
@Composable
private fun DateTransitionCard(
    currentEndTime: Long,
    selected: Product?,
    converter: com.z_company.core.util.DateAndTimeConverter?,
) {
    val accent = MaterialTheme.colorScheme.tertiary
    val months = selected?.periodMonths() ?: 1
    val zone = remember(converter) {
        runCatching { ZoneId.of(converter?.timeZoneText ?: "") }.getOrNull() ?: ZoneId.systemDefault()
    }
    val newEndTime = remember(currentEndTime, months, zone) {
        Instant.ofEpochMilli(currentEndTime).atZone(zone).plusMonths(months.toLong())
            .toInstant().toEpochMilli()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 1.dp, shape = Shapes.medium)
            .clip(Shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 10.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        DateChunk(
            caption = "СЕЙЧАС ДО",
            date = converter?.getDate(currentEndTime) ?: "",
            accent = false,
            modifier = Modifier.weight(1f),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = selected?.addLabel() ?: "",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = accent,
            )
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(com.z_company.core.R.drawable.keyboard_arrow_right_24px),
                contentDescription = null,
                tint = accent,
            )
        }
        DateChunk(
            caption = "СТАНЕТ ДО",
            date = converter?.getDate(newEndTime) ?: "",
            accent = true,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DateChunk(
    caption: String,
    date: String,
    accent: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = caption,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = date,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = if (accent) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
        )
    }
}
