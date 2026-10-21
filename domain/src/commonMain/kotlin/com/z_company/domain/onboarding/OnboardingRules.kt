package com.z_company.domain.onboarding

/**
 * Подсказки на месте (coach marks) и мягкое напоминание про «АВТО» (docs/ONBOARDING_MATERIALS.md §2, §4;
 * SCREEN_SPECS §6.9 «Знакомство с картой и АВТО»). Чистая логика без Android.
 */
enum class HintId(val key: String) {
    /** Кнопка «Карта» в нижней панели «Поезда». */
    TRAIN_MAP("train_map"),

    /** Чип «АВТО» в «Поезде». */
    AUTO_CHIP("auto_chip"),

    /** Метки поверх карты при первом открытии. */
    MAP_FIRST_OPEN("map_first_open"),

    /** Профиль → строка «Пригласить друга» (только для авторизованных). */
    PROFILE_REFERRAL("profile_referral"),
}

object OnboardingHints {
    // Тексты утверждены (ONBOARDING_MATERIALS §2) - дословно.
    const val TEXT_TRAIN_MAP = "Новая карта: ваш путь, скорость и ближайшая станция."
    const val TEXT_AUTO_CHIP =
        "Записывает стоянки и отправления сам. Выключите, когда закончите, или он выключится через 12 часов."
    const val TEXT_PROFILE_REFERRAL =
        "Приглашайте друзей: после первой оплаты друга вы оба получите бесплатные дни."
    const val TEXT_MAP_POSITION = "Это вы. Значок меняется в настройках карты"
    const val TEXT_MAP_LINE = "Цвет линии — скорость. Нажмите на путь, чтобы увидеть время и скорость"
    const val TEXT_MAP_AHEAD = "Нажмите на точку впереди — рассчитаем нужную скорость к плановому времени"
    const val TEXT_MAP_HELP = "Подсказки можно открыть снова"
    const val BUTTON_OK = "Понятно"

    /** Порядок на экране «Поезд»: за раз показывается одна подсказка. */
    private val TRAIN_SCREEN_ORDER = listOf(HintId.TRAIN_MAP, HintId.AUTO_CHIP)

    /**
     * Можно ли показать подсказку [id] сейчас.
     * @param shown ключи уже показанных подсказок (хранятся на устройстве)
     * @param homeReached пользователь уже видел экран «Главная» (первый запуск приложения не перегружаем)
     * @param recording идёт запись «АВТО» - подсказки не показываем
     * @param blocked открыт диалог/шторка/другая подсказка
     * @param authorized пользователь вошёл в аккаунт (нужно только для [HintId.PROFILE_REFERRAL])
     */
    fun canShow(
        id: HintId,
        shown: Set<String>,
        homeReached: Boolean,
        recording: Boolean,
        blocked: Boolean = false,
        authorized: Boolean = true,
    ): Boolean = when {
        id.key in shown -> false
        !homeReached -> false
        recording -> false
        blocked -> false
        id == HintId.PROFILE_REFERRAL && !authorized -> false
        else -> true
    }

    /** Следующая подсказка экрана «Поезд» («Карта», затем «АВТО») или null. */
    fun nextOnTrainScreen(shown: Set<String>, homeReached: Boolean, recording: Boolean, blocked: Boolean): HintId? =
        TRAIN_SCREEN_ORDER.firstOrNull { canShow(it, shown, homeReached, recording, blocked) }
}

/** Мягкое напоминание про «АВТО» (ONBOARDING_MATERIALS §4): не чаще раза в 30 дней, только после недели поездок без «АВТО». */
object AutoReminderRules {
    const val TITLE = "Попробуйте режим «АВТО»"
    const val TEXT = "Записывает стоянки и отправления сам. Включается в «Поезде»."
    const val INTERVAL_MS = 30L * 24 * 60 * 60 * 1000
    const val WINDOW_MS = 7L * 24 * 60 * 60 * 1000
    private const val DAY_MS = 24L * 60 * 60 * 1000

    /** Столько разных дней с поездками за последнюю неделю считаем «неделей поездок». */
    const val MIN_TRIP_DAYS = 3

    /**
     * @param lastRemindedMs когда напоминали в прошлый раз (0 - никогда)
     * @param lastAutoEnabledMs когда пользователь в последний раз включал «АВТО» (0 - никогда)
     * @param tripStartsMs время начала работы по рейсам (мс, UTC) - любые, фильтруются по окну
     * @param autoActive «АВТО» включено сейчас
     * @param zoneOffsetMs смещение пояса пользователя для разбиения на дни (по умолчанию UTC)
     */
    fun shouldRemind(
        nowMs: Long,
        lastRemindedMs: Long,
        lastAutoEnabledMs: Long,
        tripStartsMs: List<Long>,
        autoActive: Boolean,
        zoneOffsetMs: Long = 0L,
    ): Boolean {
        if (autoActive) return false
        if (lastRemindedMs > 0 && nowMs - lastRemindedMs < INTERVAL_MS) return false
        // «Без АВТО»: пользователь не включал режим за последние 30 дней.
        if (lastAutoEnabledMs > 0 && nowMs - lastAutoEnabledMs < INTERVAL_MS) return false
        val days = tripStartsMs
            .filter { it in (nowMs - WINDOW_MS)..nowMs }
            .map { (it + zoneOffsetMs).floorDiv(DAY_MS) }
            .toSet()
        return days.size >= MIN_TRIP_DAYS
    }
}
