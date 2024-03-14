package com.example.domain.use_cases

import com.example.core.ResultState
import com.example.domain.entities.route.Photo
import com.example.domain.repositories.RouteRepository
import kotlinx.coroutines.flow.Flow

class PhotoUseCase(
    private val repository: RouteRepository
) {
     fun addingPhoto(photo: Photo): Flow<ResultState<Unit>> {
        return repository.savePhoto(photo)
    }

    fun getPhotosByRoute(basicId: String): Flow<ResultState<List<Photo>>> {
        return repository.loadPhotosByRoute(basicId)
    }

    fun getPhotoById(photoId: String): Flow<ResultState<Photo?>> {
        return repository.loadPhoto(photoId)
    }
    fun removePhoto(photo: Photo): Flow<ResultState<Unit>>{
        return repository.removePhoto(photo)
    }
}