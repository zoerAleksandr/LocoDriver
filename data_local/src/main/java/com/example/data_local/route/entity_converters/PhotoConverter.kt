package com.example.data_local.route.entity_converters

import com.example.domain.entities.route.Photo
import com.example.data_local.route.entity.Photo as PhotoEntity

internal object PhotoConverter {
    fun fromData(photo: Photo) = PhotoEntity(
        photoId = photo.photoId,
        basicId = photo.basicId,
        urlPhoto = photo.urlPhoto
    )

    fun toData(entity: PhotoEntity) = Photo(
        photoId = entity.photoId,
        basicId = entity.basicId,
        urlPhoto = entity.urlPhoto
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