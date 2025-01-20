package com.z_company.type_converter

import com.google.gson.GsonBuilder
import com.z_company.domain.entities.route.Route

internal object RouteJSONConverter {
    private val gson = GsonBuilder().setDateFormat("MMM dd, yyyy HH:mm:ss").create()
    fun fromString(value: String): Route {
        return gson.fromJson(value, Route::class.java)
    }

    fun toString(route: Route): String {
        return gson.toJson(route)
    }
}