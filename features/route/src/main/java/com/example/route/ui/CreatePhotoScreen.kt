package com.example.route.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.example.core.ResultState
import com.example.core.util.ConverterUrlBase64
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
    onSelectPhotosInGallery: (photo: Bitmap) -> Unit,
    onPhotoSelected: () -> Unit,
    onCreatePhoto: (photo: String, basicId: String) -> Unit,
    onDismissPermission: () -> Unit,
    basicId: String
) {
    if (savePhotoState is ResultState.Success) {
        LaunchedEffect(savePhotoState) {
            onPhotoSelected()
        }
    } else {
        CreatePhotoScreenContent(
            onSelectPhotosInGallery = onSelectPhotosInGallery,
            onCreatePhoto = onCreatePhoto,
            onDismissPermission = onDismissPermission,
            basicId = basicId
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalCoroutinesApi::class)
@Composable
fun CreatePhotoScreenContent(
    onSelectPhotosInGallery: (photo: Bitmap) -> Unit,
    onCreatePhoto: (photo: String, basicId: String) -> Unit,
    onDismissPermission: () -> Unit,
    basicId: String
) {
    val showGallerySelect = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    if (showGallerySelect.value) {
        GallerySelect(
            onImageUri = { uriList ->
                showGallerySelect.value = false
                uriList.forEach { uri ->
                    val bitmap = ConverterUrlBase64.uriToBitmap(uri, context.contentResolver)
                    onSelectPhotosInGallery(bitmap)
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
                        onCreatePhoto(encodeUri, basicId)
                    }
                },
                onDismissPermission = onDismissPermission
            )
        }
    }
}