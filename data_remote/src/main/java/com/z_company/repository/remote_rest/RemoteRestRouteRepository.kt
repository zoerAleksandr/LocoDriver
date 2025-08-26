package com.z_company.repository.remote_rest

import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.domain.entities.route.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class RemoteRestRouteRepository(private val api: RemoteRestApi) {
    suspend fun getRoutes(): ResultState<List<Route>> =
        withContext(Dispatchers.IO) {
            try {
                val resp: Response<List<Route>> = api.getAllRoutes()
                if (resp.isSuccessful) {
                    val body = resp.body() ?: emptyList()
                    ResultState.Success(body)
                } else {
                    ResultState.Error(ErrorEntity(message = "${resp.message() ?: "Unknown error"}  ${resp.code()}"))
                }
            } catch (e: Exception) {
                ResultState.Error(ErrorEntity(message = e.localizedMessage ?: "Network error"))
            }
        }

    suspend fun createRoute(route: Route): ResultState<Route> = withContext(Dispatchers.IO) {
        try {
            val resp = api.createRoute(route)
            if (resp.isSuccessful) {
                resp.body()?.let { ResultState.Success(it) } ?: ResultState.Error(
                    ErrorEntity(message = "Empty body ${resp.code()}")
                )
            } else {
                ResultState.Error(ErrorEntity(message = "${resp.message() ?: "Unknown error"}  ${resp.code()}"))
            }
        } catch (e: Exception) {
            ResultState.Error(ErrorEntity(message = e.localizedMessage ?: "Network error"))
        }
    }
}