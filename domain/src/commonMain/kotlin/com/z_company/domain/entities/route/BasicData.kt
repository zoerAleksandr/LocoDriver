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
     * Явка, которая была ДО включения «явки по прибытию пассажиром». Нужна, чтобы
     * при выключении тумблера вернуть timeStartWork к прежнему значению (иначе он
     * остаётся равным времени прибытия). Хранится только локально (@Transient — на
     * сервер не отправляется), поэтому после выгрузки/загрузки с сервера будет null.
     */
    @Transient
    var timeStartWorkBeforeArrival: Long? = null
)
