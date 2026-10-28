package com.z_company.domain.track

import kotlinx.serialization.Serializable

/*
 * Журнал проследования ограничений (SCREEN_SPECS §6.9, «Журнал проследования»). Запись на каждое предупреждение,
 * которое машинист привязал к поезду: время и скорость захода головой, время и скорость выхода всем составом
 * (хвостом), максимальная скорость внутри зоны и флаг превышения. Координат в записи НЕТ: только время, скорость и км/пикет.
 * Запись - снимок предупреждения (его можно потом править и удалять), ключ - (trainId, warningId).
 */

/** Тип записи журнала; значения совпадают с серверным контрактом (`type`). */
object RestrictionKind {
    const val SPEED = "speed"
    const val PANTOGRAPH = "pantograph"
    const val POWER_OFF = "power_off"
    const val OTHER = "other"

    fun of(type: WarningType): String = when (type) {
        WarningType.SPEED -> SPEED
        WarningType.PANTOGRAPH -> PANTOGRAPH
        WarningType.POWER_OFF -> POWER_OFF
        WarningType.OTHER -> OTHER
    }
}

/** Состояние записи журнала. */
enum class RestrictionStatus {
    /** Привязано к поезду, голова ещё не зашла в зону. */
    PLANNED,

    /** Голова зашла, хвост ещё не вышел (только ограничение скорости). */
    IN_ZONE,

    /** Проследовано: у ограничения скорости - вышел весь состав, у остальных видов - зашла голова. */
    DONE,
}

@Serializable
data class RestrictionPass(
    val trainId: String,
    val warningId: String,
    /** Рейс поезда (`basicId`): по нему сервер проверяет владельца и каскадно удаляет журнал вместе с рейсом. */
    val routeId: String,
    val sectionId: String,
    /** [RestrictionKind]. */
    val kind: String,
    /** Снимок: «ограничение 40 км/ч», «опустить токоприёмники», текст примечания. */
    val title: String,
    /** Снимок: станция или перегон, где предупреждение добавлено. */
    val place: String = "",
    val note: String = "",
    /** Ограничение скорости, км/ч (только у `speed`). */
    val limitKmh: Int? = null,
    /** Границы зоны в железнодорожных км (142.4 = 142 км 5 пк); null - у участка нет километровой привязки. */
    val startAbsKm: Double? = null,
    val endAbsKm: Double? = null,
    /** Границы зоны в км оси участка (запас для участка без километровой привязки). */
    val startRelKm: Double = 0.0,
    val endRelKm: Double = 0.0,
    /** Заход головой: время (мс, UTC) и скорость (км/ч). */
    val enterAt: Long? = null,
    val enterSpeedKmh: Double? = null,
    /** Заход зафиксирован по первому фиксу внутри зоны (реальный момент входа неизвестен). */
    val enterApprox: Boolean = false,
    /** Выход всем составом: время и скорость в момент, когда хвост вышел за дальний край. */
    val exitAt: Long? = null,
    val exitSpeedKmh: Double? = null,
    /** Выход посчитан по голове (длина поезда не указана). */
    val exitByHead: Boolean = false,
    /** Длина поезда, м, использованная для выхода хвостом; null - не указана. */
    val trainLengthM: Int? = null,
    /** Сторона оси при проследовании: +1 к росту км оси, -1 против, 0 - не определена. */
    val dir: Int = 0,
    /** Максимальная скорость, пока поезд (хотя бы частью) в зоне. */
    val maxSpeedKmh: Double? = null,
    val exceeded: Boolean = false,
    /** Метка последнего изменения (мс, UTC): LWW при синхронизации. */
    val updatedAt: Long,
    /** Запись снята с поезда (надгробие для синхронизации). */
    val isDeleted: Boolean = false,
) {
    val status: RestrictionStatus
        get() = when {
            enterAt == null -> RestrictionStatus.PLANNED
            kind == RestrictionKind.SPEED && exitAt == null -> RestrictionStatus.IN_ZONE
            else -> RestrictionStatus.DONE
        }

    /** Есть записанные данные проследования (удалять такую запись из окна предупреждений нельзя, только из журнала). */
    val hasData: Boolean get() = enterAt != null

    /** Подпись места по км: «138 км 1 пк 0 м — 139 км 5 пк 0 м»; null - привязки нет. */
    fun kmText(): String? {
        val a = startAbsKm ?: return null
        val b = endAbsKm ?: return null
        return KmMarks.position(a).text() + " — " + KmMarks.position(b).text()
    }

    companion object {
        /** Допуск превышения: GPS-скорость шумит на 1-2 км/ч, меньшее превышение не считается нарушением. */
        const val EXCEED_TOLERANCE_KMH = 1.0

        fun exceeded(maxSpeedKmh: Double?, limitKmh: Int?): Boolean =
            maxSpeedKmh != null && limitKmh != null && maxSpeedKmh > limitKmh + EXCEED_TOLERANCE_KMH

        /** Снимок предупреждения поезда [trainId] рейса [routeId]. */
        fun planned(trainId: String, routeId: String, w: TrackWarning, nowMs: Long, trainLengthM: Int? = null) = RestrictionPass(
            trainId = trainId,
            warningId = w.id,
            routeId = routeId,
            sectionId = w.sectionId,
            kind = RestrictionKind.of(w.type),
            title = w.label.take(MAX_TEXT),
            place = w.place.take(MAX_TEXT),
            note = w.note.take(MAX_NOTE),
            limitKmh = if (w.type == WarningType.SPEED) w.speedKmh else null,
            startAbsKm = w.startAbsKm,
            endAbsKm = w.endAbsKm,
            startRelKm = w.startRelKm,
            endRelKm = w.endRelKm,
            trainLengthM = trainLengthM,
            updatedAt = nowMs,
        )

        const val MAX_TEXT = 200
        const val MAX_NOTE = 500
    }
}

/** Локальное хранилище журнала. Вызовы блокирующие и быстрые; звать с `Dispatchers.Default`. Реализации: Track.db и память (тесты). */
interface RestrictionJournalStore {
    /** Все не удалённые записи поезда. */
    fun forTrain(trainId: String): List<RestrictionPass>

    /** Не удалённые записи поезда, которые ещё ждут данных проследования (не [RestrictionStatus.DONE]). */
    fun openForTrain(trainId: String): List<RestrictionPass>

    fun get(trainId: String, warningId: String): RestrictionPass?

    /** Записать (вставка или замена) с пометкой «не отправлено на сервер». */
    fun upsert(pass: RestrictionPass)

    /** Снять предупреждение с поезда: запись остаётся надгробием до отправки на сервер. */
    fun remove(trainId: String, warningId: String, nowMs: Long)

    /** Удалить журнал поезда совсем (поезд или рейс удалён; сервер удалит каскадом через рейс). Возвращает id рейса для очереди, если нужен. */
    fun deleteByTrain(trainId: String)

    /** Записи, ещё не доставленные на сервер (включая надгробия). */
    fun pendingUpload(limit: Int): List<RestrictionPass>

    /** Отметить доставленными записи, которые не менялись после отправки (по [RestrictionPass.updatedAt]). */
    fun markUploaded(sent: List<RestrictionPass>)

    /** Слияние с серверной записью по LWW: берётся запись с большим `updatedAt`; пришедшее с сервера считается доставленным. */
    fun mergeRemote(remote: List<RestrictionPass>)

    /** Забыть всё (выход из аккаунта / отзыв согласия на хранение не трогает локальное). */
    fun deleteAll()
}
