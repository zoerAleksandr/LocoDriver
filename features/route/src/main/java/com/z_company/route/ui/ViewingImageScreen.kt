package com.z_company.route.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.z_company.core.ResultState
import com.z_company.core.ui.component.AsyncData
import com.z_company.core.util.ConverterUrlBase64
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.route.Photo

@Composable
fun ViewingImageScreen(
    imageState: ResultState<Photo?>,
    removeRouteState: ResultState<Unit>?,
    deletePhoto: (photo: Photo) -> Unit,
    onPhotoDeleting: () -> Unit,
    onBack: () -> Unit
) {
    AsyncData(resultState = imageState) { photo ->
        photo?.let {
            AsyncData(resultState = removeRouteState) {
                if (removeRouteState is ResultState.Success) {
                    LaunchedEffect(removeRouteState) {
                        onPhotoDeleting()
                    }
                } else {
                    ViewingPhotoContent(
                        photo = photo,
                        deletePhoto = deletePhoto,
                        onBack = onBack
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewingPhotoContent(
    photo: Photo,
    deletePhoto: (photo: Photo) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = ""
//                            DateAndTimeConverter.getDateAndTime(photo.dateOfCreate),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                actions = {
                    IconButton(
                        modifier = Modifier
                            .padding(4.dp),
                        onClick = { deletePhoto(photo) }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
    ) { padding ->
        val decodedImage = ConverterUrlBase64.base64toBitmap(photo.url)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Image(
                modifier = Modifier.fillMaxSize(),
                painter = rememberAsyncImagePainter(decodedImage),
                contentScale = ContentScale.FillWidth,
                contentDescription = null
            )
        }
    }
}