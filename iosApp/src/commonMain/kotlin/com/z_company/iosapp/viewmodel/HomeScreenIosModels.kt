package com.z_company.iosapp.viewmodel

/*
 * Модели состояния Главного экрана iOS (SCREEN_SPECS §4) для SwiftUI.
 *
 * Все строки уже отформатированы в Kotlin (пояс отображения §0.1, формат
 * длительностей ConverterLongToTime) — Swift только раскладывает их по экрану.
 */

/** Полное состояние Главного экрана (кроме живых блоков — см. [HomeIosLive]). */
data class HomeIosScreenUi(
    /** false — скелетон первой загрузки (§4.1: пока не загружены обе настройки). */
    val isReady: Boolean = false,
    /** Ошибка загрузки маршрутов — полноэкранная ошибка «Что-то пошло не так…». */
    val isError: Boolean = false,
    // ── Строка месяца (§4.4 п.2) ──
    val monthTitle: String = "",
    val yearTitle: String = "",
    val selectedMonth: Int = 0,
    val selectedYear: Int = 0,
    val hasPrevMonth: Boolean = false,
    val hasNextMonth: Boolean = false,
    /** Доступные месяцы (0-based) и годы для шторки выбора месяца. */
    val monthOptions: List<Int> = emptyList(),
    val yearOptions: List<Int> = emptyList(),
    // ── Блок «ОТРАБОТАНО» (§4.4 п.3) ──
    /** «К выдаче»: готовая строка, «считаем деньги» или «—». */
    val moneyText: String = "считаем деньги",
    /** 0 — считается, 1 — готово, 2 — ошибка. */
    val moneyState: Int = 0,
    /** null — ещё считается (спиннер). */
    val totalTimeText: String? = null,
    val isTotalTimeError: Boolean = false,
    /** « (без_праздн + праздн)» или null. */
    val breakdownText: String? = null,
    /** «еще ЧЧ:ММ» / «сверх ЧЧ:ММ» или null (норма 0). */
    val normaChipText: String? = null,
    // ── Карусель метрик (§4.4 п.4): 3 страницы по 3 строки ──
    val metricPages: List<List<HomeIosMetricRow>> = emptyList(),
    // ── Карточки-уведомления (§4.4 п.5) ──
    val notices: List<HomeIosNotice> = emptyList(),
    // ── «ПОСЛЕДНИЕ МАРШРУТЫ» (§4.4 п.7) ──
    val lastRoutes: List<HomeIosRouteCard> = emptyList(),
    /** N в «Все (N)» — число маршрутов месяца. */
    val monthRoutesCount: Int = 0,
    // ── Прочее ──
    /** Фоновая синхронизация при открытии экрана (§4.1) — тонкая полоса сверху. */
    val isBackgroundSyncing: Boolean = false,
    /** Авторизован ли пользователь (непустой bearer-токен) — для гейта покупок. */
    val isAuthorized: Boolean = false,
)

/** Строка карусели метрик: подпись … значение + полоса прогресса. */
data class HomeIosMetricRow(
    val label: String,
    /** null — значение ещё считается (маленький спиннер). */
    val value: String?,
    val isError: Boolean,
    /** 0…1. */
    val progress: Float,
)

/** Карточка-уведомление (подписка / бесплатный период / не синхронизировано). */
data class HomeIosNotice(
    /** Ключ скрытия: меняется при смене состояния — карточка появляется снова. */
    val dismissKey: String,
    /** "neutral" | "warning" | "danger". */
    val tone: String,
    /** "crown" | "schedule" | "alert" | "none". */
    val icon: String,
    val title: String,
    val message: String,
    val hint: String,
    val buttonText: String,
    /** "purchases" | "sync". */
    val action: String,
    /** Полоса прогресса бесплатного лимита (0…1) или -1 — нет полосы. */
    val progress: Float,
)

/** Карточка маршрута `ItemHomeScreen` в свёрнутом виде (§4.5). */
data class HomeIosRouteCard(
    val basicId: String,
    /** Явка `dd.MM HH:mm`. */
    val startText: String,
    /** Сдача: `HH:mm` в тот же день, иначе `dd.MM HH:mm`; пусто — нет сдачи. */
    val endText: String,
    /** Сдача всегда с датой — для раскладки столбцом. */
    val endTextWithDate: String,
    /** Продолжительность в месяце (§0.1). */
    val durationText: String,
    /** «№номер Первая — Последняя» или «Тип — Станция»; пусто — строки нет. */
    val summary: String,
    /** «#N». */
    val numberText: String,
    val isFuture: Boolean,
    val isTransition: Boolean,
    /** Ключи значков в порядке §4.5 (без статуса синхронизации). */
    val icons: List<String>,
    val isSynchronized: Boolean,
    val isFavorite: Boolean,
)

/** Живые блоки (§4.3). kind: "none" | "current" | "next" | "restTurnover" | "restHome". */
data class HomeIosLive(
    val kind: String = "none",
    val basicId: String = "",
    // ── Текущий маршрут ──
    /** «ЧЧ:ММ» на работе. */
    val workText: String = "",
    /** часы/12, 0…1. */
    val workProgress: Float = 0f,
    val isOver12: Boolean = false,
    val loco: HomeIosUnitTile? = null,
    val train: HomeIosUnitTile? = null,
    val passenger: HomeIosUnitTile? = null,
    /** Порядок по умолчанию: work + заполненные + пустые (§4.3). */
    val defaultOrder: List<String> = emptyList(),
    // ── Следующий маршрут ──
    val countdownText: String = "",
    val appearanceText: String = "",
    // ── Отдых ──
    val rest: HomeIosRest? = null,
)

/** Плитка единицы текущего маршрута. type: "loco" | "train" | "passenger". */
data class HomeIosUnitTile(
    val type: String,
    val count: Int,
    /** Имя последней единицы (null — пустая плитка). */
    val title: String?,
    val subtitle: String?,
    /** Единицы для шторки «Локомотивы · N» и т.п. */
    val items: List<HomeIosUnitItem>,
)

data class HomeIosUnitItem(
    val id: String,
    val name: String,
)

/** Блок отдыха: все подписи готовы, прогресс — на текущую секунду. */
data class HomeIosRest(
    val title: String,
    /** «ЧЧ:ММ» — сколько длится отдых. */
    val elapsedText: String,
    /** «начало отдыха dd.MM HH:mm». */
    val startedText: String,
    val progress: Float,
    /** Позиция промежуточной точки (короткий/минимальный) 0…1 или -1. */
    val markerFraction: Float,
    /** true — точка зелёная (короткий отдых в ПО), false — оранжевая (минимальный домашний). */
    val markerIsSuccess: Boolean,
    val startLabel: String,
    /** Подпись под промежуточной точкой (пусто для домашнего отдыха). */
    val markerLabel: String,
    val endLabel: String,
    val rows: List<HomeIosRestRow>,
)

data class HomeIosRestRow(
    val title: String,
    /** «до dd.MM HH:mm». */
    val untilText: String,
    /** «ЧЧ:ММ». */
    val leftText: String,
)

/** Диалог ручной выгрузки `SyncProgressDialog` в режиме процентов (§4.6). */
data class HomeIosSyncDialog(
    val isVisible: Boolean = false,
    val stepTitle: String = "Подготовка",
    val progress: Float = 0f,
    val percentText: String = "0%",
    val errorText: String? = null,
    val isComplete: Boolean = false,
    val isSuccess: Boolean = false,
    val isNetworkError: Boolean = false,
    val isSessionExpired: Boolean = false,
    /** Кнопка «Отправить отчет об ошибке» (только не-сетевые ошибки). */
    val canSendReport: Boolean = false,
    val reportText: String = "",
)

/**
 * Решение по кнопке «+» (§3.4). type: "open" | "limit" | "trial" | "error".
 * [freeRoutesLeft] — для «Пробный период»; [isAuthorized] — для гейта покупок.
 */
data class NewRouteIosDecision(
    val type: String,
    val freeRoutesLeft: Int,
    val isAuthorized: Boolean,
)
