package com.z_company.entity

data class Train(
    var trainId: String = "",
    var remoteObjectId: String = "",
    var basicId: String = "",
    var number: String? = null,
    var weight: String? = null,
    var axle: String? = null,
    var conditionalLength: String? = null,
    var stations: List<Station> = mutableListOf()
)
