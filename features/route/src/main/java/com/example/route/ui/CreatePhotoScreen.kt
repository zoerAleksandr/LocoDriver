package com.example.route.ui

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.net.toUri
import com.example.core.ResultState
import com.example.route.component.camera.CameraCapture
import com.example.route.component.camera.GallerySelect
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun CreatePhotoScreen(
    savePhotoState: ResultState<Unit>?,
    onSelectPhotosInGallery: (photo: String) -> Unit,
    onPhotoSelected: () -> Unit,
    onCreatePhoto: (photo: String) -> Unit,
    onDismissPermission: () -> Unit
) {
    if (savePhotoState is ResultState.Success){
        LaunchedEffect(savePhotoState){
            onPhotoSelected()
        }
    } else {
        CreatePhotoScreenContent(
            onSelectPhotosInGallery = onSelectPhotosInGallery,
            onCreatePhoto = onCreatePhoto,
            onDismissPermission = onDismissPermission
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalCoroutinesApi::class)
@Composable
fun CreatePhotoScreenContent(
    onSelectPhotosInGallery: (photo: String) -> Unit,
    onCreatePhoto: (photo: String) -> Unit,
    onDismissPermission: () -> Unit
){
    val showGallerySelect = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (showGallerySelect.value) {
        GallerySelect(
            onImageUri = { uriList ->
                showGallerySelect.value = false
                uriList.forEach { uri ->
                    onSelectPhotosInGallery(uri.toString())
                }
            },
            onDismissPermission = onDismissPermission
        )
    } else {
        Box {
            CameraCapture(
                gallerySelect = showGallerySelect,
                onImageFile = { file ->
                    showGallerySelect.value = false
                    scope.launch {
                        val encodeUri =
                            withContext(Dispatchers.IO) {
                                URLEncoder.encode(
                                    file.toUri().toString(),
                                    StandardCharsets.UTF_8.toString()
                                )
                            }
                        onCreatePhoto(encodeUri)
                        Log.d("ZZZ", "on create $encodeUri")

                    }
                },
                onDismissPermission = onDismissPermission
            )
        }
    }
}