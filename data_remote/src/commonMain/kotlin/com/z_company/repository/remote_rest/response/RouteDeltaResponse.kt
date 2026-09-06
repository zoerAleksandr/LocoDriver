package com.z_company.repository.remote_rest.response

import com.z_company.domain.entities.route.Route
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Одна страница ответа `GET /v1/route/delta`.
 *
 * @param routes изменившиеся маршруты — полные объекты, как в `GET /v1/route/`.
 * @param deleted id маршрутов, удалённых на другом устройстве. При дельте
 *   отсутствие маршрута в выгрузке не значит ничего, поэтому удаления сервер
 *   присылает явно.
 * @param cursor граница «клиент видел всё до сюда». Непрозрачная строка,
 *   разбирать её на клиенте нельзя.
 * @param hasMore есть ещё страницы. Пока true, изменения НЕ применяются:
 *   применение и сохранение курсора — одним куском после всего обхода.
 * @param fullResync сервер отказался обслуживать курсор (нет, просрочен, чужой,
 *   битый) и отдаёт полный набор с нуля. Локальный набор в этом случае
 *   заменяется целиком, а не домерживается.
 */
@Serializable
data class RouteDeltaResponse(
    val routes: List<Route> = emptyList(),
    val deleted: List<String> = emptyList(),
    val cursor: String? = null,
    @SerialName("has_more") val hasMore: Boolean = false,
    @SerialName("full_resync") val fullResync: Boolean = false,
)
