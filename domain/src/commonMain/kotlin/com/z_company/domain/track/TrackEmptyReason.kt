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

    /** «согласие: вкл · АВТО: выкл · цель: этот · точек: 3» - только флаги и счётчик, без координат. */
    fun debugLine(state: TrackRecordingState?, pointsInTrack: Int): String {
        if (state == null) return "состояние записи неизвестно · точек: $pointsInTrack"
        val target = when {
            state.targetTrainId == null -> "нет"
            state.thisTrainId == null || state.targetTrainId == state.thisTrainId -> "этот"
            else -> "другой"
        }
        fun onOff(v: Boolean) = if (v) "вкл" else "выкл"
        return "согласие: ${onOff(state.consent)} · АВТО: ${onOff(state.autoEnabled)} · цель: $target · точек: $pointsInTrack"
    }
}
