package com.z_company.domain.track

/**
 * Состояние записи трека «АВТО» на момент открытия графика скорости (SCREEN_SPECS §6.9). Без координат.
 *
 * @property consent согласие №1 на сохранение треков на ЭТОМ устройстве (SharedPreferences, после переустановки сбрасывается).
 * @property autoEnabled «АВТО» сейчас включено.
 * @property targetTrainId поезд, который пишет «АВТО» (`target_train_id`); null - цель ещё не определена.
 * @property thisTrainId поезд, на экране которого открыта карта (трек читается по нему); null - неизвестен.
 * @property otherTrainNumber номер целевого поезда, если он другой (для текста).
 */
data class TrackRecordingState(
    val consent: Boolean,
    val autoEnabled: Boolean,
    val targetTrainId: String?,
    val thisTrainId: String?,
    val otherTrainNumber: String? = null,
)

/** Почему на графике скорости нет данных. */
enum class TrackEmptyReason {
    /** Состояние записи неизвестно (экран просмотра/нет данных) - общий текст. */
    UNKNOWN,

    /** Согласие №1 выключено на этом устройстве. */
    CONSENT_OFF,

    /** «АВТО» выключено (или не писало). */
    AUTO_OFF,

    /** «АВТО» пишет другой поезд, а трек читается по этому. */
    OTHER_TRAIN,

    /** «АВТО» включено, но поезд для записи не определён (включали с главного/виджета без поездов). */
    NO_TARGET,

    /** Всё в порядке, но точек ещё нет (сеанс только начался; прореживание: движение раз в 5 с, стоянка раз в 60 с). */
    NO_POINTS_YET,

    /** Точки есть, но GPS не отдаёт скорость. */
    NO_SPEED,
}

/** Действие кнопки пустого графика скорости. */
enum class TrackEmptyAction {
    NONE,

    /** «Включить АВТО» (для текущего поезда, как кнопка в «Поезде»). */
    ENABLE_AUTO,

    /** «Открыть согласие» - экран «Данные и конфиденциальность». */
    OPEN_CONSENT,

    /** «Записывать этот поезд» - сделать этот поезд целью записи. */
    MAKE_TARGET,
}

/** Выбор причины пустого графика и отладочная строка состояния. */
object TrackEmptyReasons {

    /**
     * @param pointsInTrack точек в треке поезда на экране.
     * @param pointsWithSpeed из них со скоростью.
     * Порядок проверок - от самой «железной» причины к самой мягкой.
     */
    fun determine(state: TrackRecordingState?, pointsInTrack: Int, pointsWithSpeed: Int): TrackEmptyReason {
        if (pointsInTrack >= 2 && pointsWithSpeed == 0) return TrackEmptyReason.NO_SPEED
        if (state == null) return TrackEmptyReason.UNKNOWN
        return when {
            !state.consent -> TrackEmptyReason.CONSENT_OFF
            !state.autoEnabled -> TrackEmptyReason.AUTO_OFF
            state.targetTrainId == null -> TrackEmptyReason.NO_TARGET
            state.thisTrainId != null && state.targetTrainId != state.thisTrainId -> TrackEmptyReason.OTHER_TRAIN
            else -> TrackEmptyReason.NO_POINTS_YET
        }
    }

    /** «согласие: вкл · автозапись: выкл · цель: этот · точек: 3» - только флаги и счётчик, без координат. */
    fun debugLine(state: TrackRecordingState?, pointsInTrack: Int): String {
        if (state == null) return "состояние записи неизвестно · точек: $pointsInTrack"
        val target = when {
            state.targetTrainId == null -> "нет"
            state.thisTrainId == null || state.targetTrainId == state.thisTrainId -> "этот"
            else -> "другой"
        }
        fun onOff(v: Boolean) = if (v) "вкл" else "выкл"
        return "согласие: ${onOff(state.consent)} · автозапись: ${onOff(state.autoEnabled)} · цель: $target · точек: $pointsInTrack"
    }

    /** Текст пустого графика по причине (по центру экрана). */
    fun message(reason: TrackEmptyReason, otherTrainNumber: String? = null): String = when (reason) {
        TrackEmptyReason.UNKNOWN -> "Нет записи скорости. Включите автозапись и согласие на сохранение трека"
        TrackEmptyReason.CONSENT_OFF -> "Для записи скорости дайте согласие на сохранение трека"
        TrackEmptyReason.AUTO_OFF -> "Для записи скорости включите автозапись"
        TrackEmptyReason.OTHER_TRAIN ->
            "Автозапись ведётся для другого поезда" + otherTrainNumber?.takeIf { it.isNotBlank() }?.let { " (№$it)" }.orEmpty()
        TrackEmptyReason.NO_TARGET -> "Автозапись включена, но поезд для записи не выбран"
        TrackEmptyReason.NO_POINTS_YET -> "Запись идёт. Для построения графика скорости нужно начать движение"
        TrackEmptyReason.NO_SPEED -> "Нет данных о скорости от GPS"
    }

    /** Какая кнопка нужна под текстом. [canEnableAuto] - есть ли у экрана возможность включить АВТО (не просмотр трека). */
    fun action(reason: TrackEmptyReason, canEnableAuto: Boolean = true): TrackEmptyAction = when (reason) {
        TrackEmptyReason.CONSENT_OFF, TrackEmptyReason.UNKNOWN -> TrackEmptyAction.OPEN_CONSENT
        TrackEmptyReason.AUTO_OFF -> if (canEnableAuto) TrackEmptyAction.ENABLE_AUTO else TrackEmptyAction.NONE
        TrackEmptyReason.OTHER_TRAIN, TrackEmptyReason.NO_TARGET -> TrackEmptyAction.MAKE_TARGET
        else -> TrackEmptyAction.NONE
    }

    /** Подпись кнопки действия. */
    fun actionLabel(action: TrackEmptyAction, reason: TrackEmptyReason): String = when (action) {
        TrackEmptyAction.ENABLE_AUTO -> "Включить автозапись"
        TrackEmptyAction.OPEN_CONSENT -> if (reason == TrackEmptyReason.CONSENT_OFF) "Открыть согласие" else "Настройки согласия"
        TrackEmptyAction.MAKE_TARGET -> "Записывать этот поезд"
        TrackEmptyAction.NONE -> ""
    }
}
