package com.z_company.domain.autorecord

import com.z_company.domain.entities.route.Train
import com.z_company.domain.track.NO_TRAIN_ID

/**
 * Правила «единственного активного АВТО» (SCREEN_SPECS §6.2 п.6а): режим пишет трек одного поезда
 * (`target_train_id`); на экранах остальных поездов он показывается выключенным, а включение там
 * сначала предупреждает о прерывании текущей записи.
 */
object AutoTargetRules {
    const val CONFLICT_TITLE = "Запись уже идёт"
    const val CONFLICT_CONFIRM = "Продолжить"
    const val CONFLICT_DISMISS = "Отмена"

    /**
     * Показывать ли «АВТО» включённым на экране поезда [thisTrainId]. Без контекста поезда ([thisTrainId] == null:
     * Главный, виджет) - общее состояние; цель ещё не определена ([targetId] == null) - тоже включено (переходное).
     */
    fun isRecordingThis(enabled: Boolean, targetId: String?, thisTrainId: String?): Boolean =
        enabled && (thisTrainId == null || targetId == null || targetId == thisTrainId)

    /** Идёт запись для ДРУГОГО поезда: включение здесь должно предупредить и прервать текущую запись. */
    fun conflictsWith(enabled: Boolean, targetId: String?, thisTrainId: String?): Boolean =
        enabled && thisTrainId != null && targetId != null && targetId != thisTrainId

    /** «№ 1234» / ««Лужская — Гатчина»» / null (нет ни номера, ни станций). */
    fun trainLabel(number: String?, stationNames: List<String?>): String? {
        number?.trim()?.takeIf { it.isNotEmpty() }?.let { return "№ $it" }
        val names = stationNames.mapNotNull { it?.trim()?.takeIf { s -> s.isNotEmpty() } }
        if (names.isEmpty()) return null
        val first = names.first()
        val last = names.last()
        return if (names.size == 1 || first == last) "«$first»" else "«$first — $last»"
    }

    fun trainLabel(train: Train): String? = trainLabel(train.number, train.stations.map { it.stationName })

    /** Текст диалога; [train] == null и [targetId] == [NO_TRAIN_ID] - запись трека без поезда. */
    fun conflictText(train: Train?, targetId: String?): String {
        val who = when {
            targetId == NO_TRAIN_ID -> "трека без поезда"
            train == null -> "трека поезда без номера"
            else -> "трека поезда " + (trainLabel(train) ?: "без номера")
        }
        return "Идёт запись $who. Если продолжите, она будет прервана."
    }
}
