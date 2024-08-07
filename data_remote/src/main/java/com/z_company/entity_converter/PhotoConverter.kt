package com.z_company.entity_converter

import com.z_company.domain.entities.route.Photo
import com.z_company.entity.Photo as PhotoRemote

object PhotoConverter {
    fun toRemote(photo: Photo) = PhotoRemote(
        photoId = photo.photoId,
        basicId = photo.basicId,
        remoteObjectId = photo.remoteObjectId,
        url = photo.url,
        dateOfCreate = photo.dateOfCreate
    )

    private fun fromRemote(photoRemote: PhotoRemote) = Photo(
        photoId = photoRemote.photoId,
        basicId = photoRemote.basicId,
        remoteObjectId = photoRemote.remoteObjectId,
        url = photoRemote.url,
        dateOfCreate = photoRemote.dateOfCreate
    )

    fun toRemoteList(photoList: List<Photo>): MutableList<PhotoRemote> {
        return photoList.map {
            toRemote(it)
        }.toMutableList()
    }

    fun fromRemoteList(photoRemoteList: List<PhotoRemote>): MutableList<Photo> {
        return photoRemoteList.map {
            fromRemote(it)
        }.toMutableList()
    }
}