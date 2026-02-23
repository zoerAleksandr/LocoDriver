package com.z_company

import com.z_company.domain.entities.route.Route
import com.z_company.repository.remote_rest.RemoteRestClient
import kotlinx.serialization.json.Json

/**
 * Утилита для сериализации/десериализации Route в JSON-строку.
 * Заменяет Gson на kotlinx.serialization.
 */
object RouteSerializer {

    fun serialize(route: Route): String = RemoteRestClient.appJson.encodeToString(Route.serializer(), route)

    fun deserialize(json: String): Route = RemoteRestClient.appJson.decodeFromString(Route.serializer(), json)
}
