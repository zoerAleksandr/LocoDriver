package com.z_company.repository.remote_rest

import com.z_company.domain.entities.route.Route
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface RemoteRestApi {
    @GET("routes")
    suspend fun getAllRoutes(
//        @Path("id") id: String
    ): Response<List<Route>>

    @POST("routes")
    suspend fun createRoute(
        @Body route: Route
    ): Response<Route>
}