package com.z_company

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.z_company.domain.entities.route.Route
import com.z_company.repository.remote_rest.ReleaseTypeAdapter
import com.z_company.domain.entities.ReleaseType
import java.util.Date

object RouteSerializer {
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(ReleaseType::class.java, ReleaseTypeAdapter())
        .create()

    fun serialize(route: Route): String = gson.toJson(route)

    fun deserialize(json: String): Route = gson.fromJson(json, Route::class.java)
}
