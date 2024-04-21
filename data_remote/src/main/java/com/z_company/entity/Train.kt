package com.z_company.entity

data class Train(
    var trainId: String,
    var remoteObjectId: String = "",
    var basicId: String,
    var number: String?,
    var weight: String?,
    var axle: String?,
    var conditionalLength: String?,
    var stations: List<Station> = mutableListOf()
)
