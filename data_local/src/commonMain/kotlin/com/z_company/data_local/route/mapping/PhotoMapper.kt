package com.z_company.data_local.route.mapping

import com.z_company.data_local.route.db.Photo as PhotoRow
import com.z_company.domain.entities.route.Photo

internal object PhotoMapper {
    fun toData(row: PhotoRow): Photo = Photo(
        photoId = row.photoId,
        basicId = row.basicId,
        remoteObjectId = row.remoteObjectId,
        url = row.url,
        dateOfCreate = row.dateOfCreate
    )
}
