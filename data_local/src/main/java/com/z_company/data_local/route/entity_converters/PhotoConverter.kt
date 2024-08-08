package com.z_company.data_local.route.entity_converters

import com.z_company.domain.entities.route.Photo
import com.z_company.data_local.route.entity.Photo as PhotoEntity

internal object PhotoConverter {
    fun fromData(photo: Photo) = PhotoEntity(
        photoId = photo.photoId,
        basicId = photo.basicId,
        remoteObjectId = photo.remoteObjectId,
        url = photo.url,
        dateOfCreate = photo.dateOfCreate
    )

    fun toData(entity: PhotoEntity) = Photo(
        photoId = entity.photoId,
        basicId = entity.basicId,
        remoteObjectId = entity.remoteObjectId,
        url = entity.url,
        dateOfCreate = entity.dateOfCreate
    )

    fun fromDataList(list: List<Photo>): MutableList<PhotoEntity> {
        return list.map {
            fromData(it)
        }.toMutableList()
    }

    fun toDataList(entityList: List<PhotoEntity>): MutableList<Photo> {
        return entityList.map {
            toData(it)
        }.toMutableList()
    }
}