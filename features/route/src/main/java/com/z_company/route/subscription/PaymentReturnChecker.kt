package com.z_company.route.subscription

import com.z_company.domain.repositories.SharedPreferencesRepositories
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Ожидаемая оплата: кто платит, когда ушёл в оплату, какой срок был до неё и
 * на сколько дней тариф. Хранится строкой, чтобы пережить выгрузку приложения,
 * пока пользователь в банке.
 */
data class PendingPayment(
    val userId: String,
    val startedAt: Long,
    val periodBefore: Long,
    /** Срок тарифа в днях; 0 — неизвестен. */
    val periodDays: Int,
) {
    fun encode(): String = listOf(userId, startedAt, periodBefore, periodDays).joinToString(SEPARATOR)

    companion object {
        private const val SEPARATOR = "|"

        fun decode(raw: String?): PendingPayment? {
            val parts = raw?.split(SEPARATOR) ?: return null
            if (parts.size != 4 || parts[0].isBlank()) return null
            return PendingPayment(
                userId = parts[0],
                startedAt = parts[1].toLongOrNull() ?: return null,
                periodBefore = parts[2].toLongOrNull() ?: return null,
                periodDays = parts[3].toIntOrNull() ?: return null,
            )
        }
    }
}

interface PendingPaymentStore {
    fun get(): String?
    fun set(value: String?)
}

fun SharedPreferencesRepositories.asPendingPaymentStore(): PendingPaymentStore {
    val prefs = this
    return object : PendingPaymentStore {
        override fun get(): String? = prefs.getPendingPayment()
        override fun set(value: String?) = prefs.setPendingPayment(value)
    }
}

/**
 * Гарантирует диалог «Платёж принят!» после возврата из оплаты — каким бы
 * способом пользователь ни вернулся (кнопка «Назад», недавние, иконка,
 * холодный старт после выгрузки приложения), на любом экране.
 *
 * - [markStarted] — при переходе в оплату (Robokassa SDK / ссылка CKassa).
 * - [onAppResumed] — на каждом возврате приложения на передний план.
 * - [checkNow] — экран Покупок после результата SDK / возврата со страницы
 *   CKassa; присоединяется к уже идущей проверке, а не запускает вторую.
 *
 * Пока запись есть, срок запрашивается с сервера (в обход гейта подписки —
 * у бесплатного пользователя автосинхронизации нет). Подтверждение — рост
 * срока минимум на длину тарифа: бонус рефералки платежом не считается.
 * Если не подтвердилось — молчим (пользователь мог просто посмотреть оплату
 * или передумать) и повторяем на следующих возвратах, пока не истечёт [TTL_MS].
 */
class PaymentReturnChecker(
    private val store: PendingPaymentStore,
    private val currentUserId: suspend () -> String?,
    /** Обновить срок с сервера в локальные настройки; вернуть локальный срок или null при ошибке. */
    private val refreshPeriodFromServer: suspend () -> Long?,
    private val tracker: SubscriptionPeriodTracker,
    private val scope: CoroutineScope,
    private val now: () -> Long = System::currentTimeMillis,
) {
    enum class Result { PAID, NOT_CONFIRMED, NO_PENDING }

    private val _hasPending = MutableStateFlow(load() != null)
    /** Есть неподтверждённая оплата — «Подписка продлена» ждёт, решение за этим классом. */
    val hasPending: StateFlow<Boolean> = _hasPending.asStateFlow()

    private val _paidUntil = MutableStateFlow<Long?>(null)
    /** Срок после подтверждённой оплаты — показать «Платёж принят!»; null — диалога нет. */
    val paidUntil: StateFlow<Long?> = _paidUntil.asStateFlow()

    private val mutex = Mutex()
    private var running: Deferred<Result>? = null
    private var lastCheckAt = 0L

    fun markStarted(userId: String, periodBefore: Long, periodDays: Int) {
        save(PendingPayment(userId, now(), periodBefore.coerceAtLeast(0L), periodDays.coerceAtLeast(0)))
    }

    fun onAppResumed() {
        if (!_hasPending.value) return
        if (now() - lastCheckAt < RESUME_THROTTLE_MS) return
        scope.launch { check(forcedAttempts = null) }
    }

    suspend fun checkNow(attempts: Int): Result = check(forcedAttempts = attempts)

    fun dismissPaidDialog() {
        _paidUntil.value = null
    }

    private suspend fun check(forcedAttempts: Int?): Result {
        val job = mutex.withLock {
            running?.takeIf { it.isActive }
                ?: scope.async { poll(forcedAttempts) }.also { running = it }
        }
        return job.await()
    }

    private suspend fun poll(forcedAttempts: Int?): Result {
        val pending = load() ?: return Result.NO_PENDING
        val nowMs = now()
        if (nowMs - pending.startedAt > TTL_MS) {
            save(null)
            return Result.NO_PENDING
        }
        if (currentUserId()?.takeIf { it.isNotBlank() } != pending.userId) {
            // Вышел или сменил аккаунт — оплата была не этого пользователя.
            save(null)
            return Result.NO_PENDING
        }
        // Сразу после оплаты вебхук может задержаться — ждём дольше; спустя
        // время хватает одного запроса на каждом возврате.
        val attempts = forcedAttempts
            ?: if (nowMs - pending.startedAt < FRESH_PAYMENT_MS) FRESH_ATTEMPTS else 1
        lastCheckAt = nowMs
        return tracker.runPeriodUpdate {
            repeat(attempts) { attempt ->
                val period = refreshPeriodFromServer()
                if (period != null && isPaid(pending, period)) {
                    save(null)
                    tracker.acknowledge(period)
                    _paidUntil.value = period
                    return@runPeriodUpdate Result.PAID
                }
                if (attempt < attempts - 1) delay(RETRY_DELAY_MS)
            }
            lastCheckAt = now()
            Result.NOT_CONFIRMED
        }
    }

    /**
     * Сервер продлевает от `max(прежний срок, момент оплаты)`. Бонус
     * рефералки (обычно короче тарифа) оплатой не считается; запас сутки.
     */
    private fun isPaid(pending: PendingPayment, period: Long): Boolean {
        if (period <= pending.periodBefore || period <= now()) return false
        if (pending.periodDays <= 0) return true
        val base = maxOf(pending.periodBefore, pending.startedAt)
        return period - base >= pending.periodDays * DAY_MS - DAY_MS
    }

    private fun load(): PendingPayment? = PendingPayment.decode(store.get())

    private fun save(pending: PendingPayment?) {
        store.set(pending?.encode())
        _hasPending.value = pending != null
    }

    companion object {
        const val DAY_MS = 86_400_000L
        const val TTL_MS = DAY_MS
        const val FRESH_PAYMENT_MS = 10 * 60_000L
        const val FRESH_ATTEMPTS = 10
        const val RETRY_DELAY_MS = 3_000L
        const val RESUME_THROTTLE_MS = 30_000L
    }
}
