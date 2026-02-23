package com.z_company.domain.entities.route

import kotlinx.serialization.Serializable

@Serializable
enum class LocoType(val text: String) {
    ELECTRIC("Электротяга"), DIESEL("Теплотяга")
}