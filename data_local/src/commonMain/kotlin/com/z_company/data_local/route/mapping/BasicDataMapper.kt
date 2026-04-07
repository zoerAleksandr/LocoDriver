@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.data_local.route.mapping

import com.zcompany.datalocal.route.db.BasicData as BasicDataRow
import com.z_company.domain.entities.route.BasicData
import kotlin.time.Clock

internal object BasicDataMapper {

    /**
     * Store epoch millis as a string. Old Room data had GSON date format like
     * "Jun 21, 2024 12:00:00" — on first read we fall back to current time.
     */
    fun encodeUpdatedAt(millis: Long): String = millis.toString()

    private fun decodeUpdatedAt(value: String): Long =
        value.toLongOrNull() ?: Clock.System.now().toEpochMilliseconds()

    fun toData(row: BasicDataRow): BasicData = BasicData(
        id = row.id,
        isSynchronizedRoute = row.isSynchronizedRoute != 0L,
        remoteRouteId = row.remoteRouteId,
        isSynchronized = row.isSynchronized != 0L,
        remoteObjectId = row.remoteObjectId,
        isOnePersonOperation = row.isOnePersonOperation != 0L,
        isDeleted = row.isDeleted != 0L,
        updatedAt = decodeUpdatedAt(row.updatedAt),
        number = row.number,
        timeStartWork = row.timeStartWork,
        timeEndWork = row.timeEndWork,
        restPointOfTurnover = row.restPointOfTurnover != 0L,
        notes = row.notes,
        isFavorite = row.isFavorite != 0L,
        // 0L сохранялось в старом коде вместо null — нормализуем здесь
        timeStartBreak = row.timeStartBreak?.takeIf { it > 0L },
        timeEndBreak = row.timeEndBreak?.takeIf { it > 0L }
    )
}
