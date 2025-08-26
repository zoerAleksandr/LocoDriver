package com.z_company.route.viewmodel

data class RouteRemote(
    val objectId: String,
    // data - здесь содержаться все данные пользователя в JSON
    val data: String,
    // userId - id пользователя
    val userId: String,
    // createdAt - дата создания
    val createdAt: Long,
    // updateAt - дата обновления
    val updateAt: Long,
)