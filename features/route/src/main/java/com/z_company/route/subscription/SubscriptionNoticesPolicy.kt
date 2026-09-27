package com.z_company.route.subscription

import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.route.viewmodel.SubscriptionPeriodChange

/**
 * «Увиденное» пользователем по аккаунтам (ключ — userId). На Android —
 * SharedPreferences, см. [SharedPreferencesRepositories.getLastSeenSubscriptionPeriod]
 * и соседние методы; отдельный интерфейс — чтобы решение проверялось тестом.
 */
interface SubscriptionSeenStore {
    fun getLastSeenReferralAwardedDays(userId: String): Int
    fun setLastSeenReferralAwardedDays(userId: String, value: Int)
    fun getLastSeenSubscriptionPeriod(userId: String): Long
    fun setLastSeenSubscriptionPeriod(userId: String, value: Long)
    fun getSubscriptionPeriodReferralDays(userId: String): Int
    fun setSubscriptionPeriodReferralDays(userId: String, value: Int)
}

fun SharedPreferencesRepositories.asSubscriptionSeenStore(): SubscriptionSeenStore {
    val prefs = this
    return object : SubscriptionSeenStore {
        override fun getLastSeenReferralAwardedDays(userId: String) =
            prefs.getLastSeenReferralAwardedDays(userId)
        override fun setLastSeenReferralAwardedDays(userId: String, value: Int) =
            prefs.setLastSeenReferralAwardedDays(userId, value)
        override fun getLastSeenSubscriptionPeriod(userId: String) =
            prefs.getLastSeenSubscriptionPeriod(userId)
        override fun setLastSeenSubscriptionPeriod(userId: String, value: Long) =
            prefs.setLastSeenSubscriptionPeriod(userId, value)
        override fun getSubscriptionPeriodReferralDays(userId: String) =
            prefs.getSubscriptionPeriodReferralDays(userId)
        override fun setSubscriptionPeriodReferralDays(userId: String, value: Int) =
            prefs.setSubscriptionPeriodReferralDays(userId, value)
    }
}

/**
 * Решение глобальных диалогов «Бонус начислен!» / «Подписка продлена»
 * (SCREEN_SPECS §32.1a) для одного аккаунта. Всё «увиденное» читается и
 * пишется строго под переданным [userId].
 */
class SubscriptionNoticesPolicy(
    private val store: SubscriptionSeenStore,
    private val now: () -> Long = System::currentTimeMillis,
) {
    data class Notices(
        /** Новые бонусные дни с прошлого увиденного, null — диалога нет. */
        val bonusDays: Int?,
        val periodChange: SubscriptionPeriodChange?,
    )

    /**
     * [includePeriod] = false — срок сейчас не оцениваем и «увиденное» по
     * нему не трогаем: ждём подтверждения оплаты, начатой в приложении
     * (PaymentReturnChecker покажет «Платёж принят!» и сам отметит срок).
     */
    fun check(userId: String, period: Long, awardedDays: Int, includePeriod: Boolean = true): Notices = Notices(
        bonusDays = checkReferralBonus(userId, awardedDays),
        periodChange = if (includePeriod) checkPeriod(userId, period, awardedDays) else null,
    )

    /**
     * Сравнивает свежий awardedDays с последним увиденным ДЛЯ ЭТОГО userId
     * (см. checkNewReferralBonus в PWA). -1 — первый запуск трекинга: только
     * запоминаем базу, без диалога.
     */
    private fun checkReferralBonus(userId: String, awardedDays: Int): Int? {
        val lastSeen = store.getLastSeenReferralAwardedDays(userId)
        store.setLastSeenReferralAwardedDays(userId, awardedDays)
        return if (lastSeen in 0 until awardedDays) awardedDays - lastSeen else null
    }

    /**
     * Не сообщаем: при `period <= 0` (пустые настройки до загрузки — не
     * запоминаем); при первом отслеживании аккаунта на устройстве (только
     * база — иначе вход с оплаченной подпиской выглядел бы «продлением»);
     * при уменьшении срока; если новый срок уже истёк (это не продление —
     * так приходит, например, прошлый срок другого аккаунта из синхронизации);
     * если рост целиком объясняется бонусом рефералки.
     */
    private fun checkPeriod(userId: String, period: Long, awardedDays: Int): SubscriptionPeriodChange? {
        if (period <= 0L) return null
        val lastSeen = store.getLastSeenSubscriptionPeriod(userId)
        val referralDaysInLastSeen = store.getSubscriptionPeriodReferralDays(userId)
        if (period == lastSeen) {
            // Срок подтверждён экраном Подписки — бонус, пришедший вместе с
            // этой оплатой, уже внутри него.
            if (referralDaysInLastSeen < 0) {
                store.setSubscriptionPeriodReferralDays(userId, awardedDays)
            }
            return null
        }
        store.setLastSeenSubscriptionPeriod(userId, period)
        store.setSubscriptionPeriodReferralDays(userId, awardedDays)
        if (lastSeen <= 0L) return null

        val bonusDays = if (referralDaysInLastSeen >= 0) {
            (awardedDays - referralDaysInLastSeen).coerceAtLeast(0)
        } else {
            0
        }
        if (!isExtendedBeyondBonus(lastSeen, period, bonusDays)) return null
        return SubscriptionPeriodChange(lastSeen, period)
    }

    /**
     * Сервер начисляет бонус как `max(срок, сейчас) + бонус`. Без бонуса —
     * любой рост до будущей даты. С бонусом — только если срок ушёл дальше,
     * чем мог увести один бонус (запас [TOLERANCE_MS] на округление
     * awardedDays вниз).
     */
    private fun isExtendedBeyondBonus(lastSeen: Long, period: Long, bonusDays: Int): Boolean {
        val now = now()
        if (period <= lastSeen || period <= now) return false
        if (bonusDays == 0) return true
        val bonusBase = maxOf(lastSeen, now)
        return period - bonusDays * DAY_MS > bonusBase + TOLERANCE_MS
    }

    private companion object {
        const val DAY_MS = 86_400_000L
        const val TOLERANCE_MS = DAY_MS
    }
}
