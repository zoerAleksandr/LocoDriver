package com.z_company.data_local.route.entity_converters

import com.z_company.domain.entities.route.SearchResponse
import com.z_company.data_local.route.entity.SearchResponse as SearchResponseEntity

internal object SearchResponseConverter {
    fun fromData(searchResponse: SearchResponse) = SearchResponseEntity(
        responseText = searchResponse.responseText
    )

    fun toData(entity: SearchResponseEntity) = SearchResponse(
        responseText = entity.responseText
    )
}