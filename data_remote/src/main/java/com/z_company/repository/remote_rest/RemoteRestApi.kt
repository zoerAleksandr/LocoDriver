package com.z_company.repository.remote_rest

import com.z_company.core.ResultState
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.UserSettings
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface RemoteRestApi {
    @Headers("Content-Type: application/json")
    @POST("v1/auth")
    suspend fun authWithEmail(
        @Body authRequest: AuthRequest
    ): Response<AuthResponse>

    @Headers("Content-Type: application/json")
    @POST("v1/auth/create")
    suspend fun registerUserByEmail(
        @Body request: RegisteredRequestByEmail
    ): Response<LoginResponse>

    @Headers("Content-Type: application/json")
    @POST("v1/auth/create")
    suspend fun registerUserByVKID(
        @Body request: RegisteredRequestByVKID
    ): Response<LoginResponse>

    @Headers("Content-Type: application/json")
    @GET("v1/auth")
    suspend fun getUserProfile(
        @Header("Authorization") token: String
    ): Response<UserWithRouteResponse>

    @PATCH("/v1/auth/vkId/remove")
    suspend fun removeVKID(
        @Header("Authorization") token: String,
    ): Response<UserWithRouteResponse>
    @Headers("Content-Type: application/json")
    @POST("v1/route")
    suspend fun saveRoute(
        @Header("Authorization") token: String,
        @Body data: Route,
    ): Response<Route>

    @GET("v1/route")
    suspend fun getRoutes(
        @Header("Authorization") token: String,
    ): Response<List<Route>>



    @Headers("Content-Type: text/plain")
    @PATCH("v1/auth/vkId/add")
    suspend fun attachVKID(
        @Header("Authorization") token: String,
        @Body vkId: String
    ): Response<UserWithRouteResponse>

    @Headers("Content-Type: application/json")
    @POST("v1/user_settings")
    suspend fun saveUserSetting(
        @Header("Authorization") token: String,
        @Body body: UserSettings
    ): Response<SuccessResponse>

    @GET("v1/user_settings")
    suspend fun getUserSetting(
        @Header("Authorization") token: String,
    ): Response<UserSettings>

    @Headers("Content-Type: application/json")
    @POST("v1/salary_settings")
    suspend fun saveSalarySetting(
        @Header("Authorization") token: String,
        @Body body: SalarySetting
    ): Response<SuccessResponse>

    @GET("v1/salary_settings")
    suspend fun getSalarySetting(
        @Header("Authorization") token: String,
    ): Response<SalarySetting>

    @POST("v1/year/")
    suspend fun saveMonthOfYearList(
        @Header("Authorization") token: String,
        @Body body: List<MonthOfYear>
    ): Response<SuccessResponse>

    @GET("v1/year/")
    suspend fun getMonthOfYearList(
        @Header("Authorization") token: String
    ): Response<List<MonthOfYear>>
}