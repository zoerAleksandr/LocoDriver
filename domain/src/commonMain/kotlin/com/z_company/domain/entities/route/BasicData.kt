@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.domain.entities.route

import com.z_company.domain.entities.serializers.DateAsLongSerializer
import com.z_company.domain.util.generateId
import kotlin.time.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class BasicData(
    var id: String = generateId(),
    var remoteRouteId: String? = null,
    var isSynchronized: Boolean = false,
    var remoteObjectId: String? = null,
    var isOnePersonOperation: Boolean = false,
    var isDeleted: Boolean = false,
    /** Время перемещения в локальную корзину, epoch millis. */
    @Transient
    var deletedAt: Long? = null,
    /** Машиночитаемая причина перемещения в корзину. */
    @Transient
    var deletionReason: String? = null,
    /** Массовое серверное удаление ожидает явного подтверждения пользователя. */
    @Transient
    var remoteDeletionPending: Boolean = false,
    /** Время подтверждения серверного удаления, epoch millis. */
    @Transient
    var remoteDeletedAt: Long? = null,
    @Serializable(with = DateAsLongSerializer::class)
    var updatedAt: Long = Clock.System.now().toEpochMilliseconds(),
    var number: String? = null,
    var timeStartWork: Long? = null,
    var timeEndWork: Long? = null,
    var restPointOfTurnover: Boolean = false,
    var notes: String? = null,
    var isFavorite: Boolean = false,
    var timeStartBreak: Long? = null,
    var timeEndBreak: Long? = null,
    /**
     * «Явка по прибытию пассажиром» на уровне рейса: id пассажира-источника
     * (или null). Единый с PWA/сервером контракт — источник истины «явки по
     * прибытию». На клиенте фактическое состояние по-прежнему ведётся булевым
     * [Passenger.isWorkStartByArrival]; это поле выводится из него в момент
     * отправки (RoutesManager.saveRouteInRemote) и уходит на сервер. Локально в
     * БД не хранится (нет столбца) — при чтении из БД остаётся null; сервер
     * держит булев флаг пассажира в согласии, поэтому клиент им и пользуется.
     */
    var workStartByArrivalPassengerId: String? = null,
    /**
     * Явка, которая была ДО включения «явки по прибытию пассажиром». Нужна, чтобы
     * при выключении тумблера вернуть timeStartWork к прежнему значению (иначе он
     * остаётся равным времени прибытия). Хранится только локально (@Transient — на
     * сервер не отправляется), поэтому после выгрузки/загрузки с сервера будет null.
     */
    @Transient
    var timeStartWorkBeforeArrival: Long? = null
)
