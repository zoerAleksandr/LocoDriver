package com.z_company.domain.entities.route

import kotlinx.serialization.Serializable

@Serializable
data class Route(
    var basicData: BasicData = BasicData(),
    var locomotives: MutableList<Locomotive> = mutableListOf(),
    var trains: MutableList<Train> = mutableListOf(),
    var passengers: MutableList<Passenger> = mutableListOf(),
    var otherWorks: MutableList<OtherWork> = mutableListOf(),
    var partners: MutableList<RoutePartner> = mutableListOf(),
    var photos: MutableList<Photo> = mutableListOf(),
    /**
     * Справки ВУ-45 рейса для синхронизации (контракт сервера: необязательное поле). null - не трогать (по умолчанию и пока
     * сервер не знает поле); в БД Route.db не хранится (отдельный файл BrakeCertificate.db).
     */
    var brakeCertificates: List<com.z_company.domain.brake.storage.BrakeCertificateSync>? = null,
)
