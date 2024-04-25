package com.z_company.route.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import coil.compose.rememberAsyncImagePainter
import com.z_company.core.ResultState
import com.z_company.core.ui.component.AsyncData
import com.z_company.core.ui.component.GenericError
import com.z_company.core.util.ConverterUrlBase64

@Composable
fun PreviewPhotoScreen(
    uri: String,
    basicId: String,
    onSavePhoto: (bitmap: Bitmap) -> Unit,
    reshoot: () -> Unit,
    onPhotoSaved: (String) -> Unit,
    resetSaveState: () -> Unit,
    photoSaveState: ResultState<Unit>?
) {
    val context = LocalContext.current
    AsyncData(
        resultState = photoSaveState,
        errorContent = { GenericError(onDismissAction = resetSaveState) }
    ) {
        if (photoSaveState is ResultState.Success) {
            LaunchedEffect(photoSaveState) {
                onPhotoSaved(basicId)
            }
        } else {
            Scaffold(
                bottomBar = {
                    BottomAppBar(
                        actions = {
                            IconButton(
                                onClick = {
                                    val bitmap = ConverterUrlBase64.uriToBitmap(uri.toUri(), context.contentResolver)
                                    onSavePhoto(bitmap)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Сохранить"
                                )
                            }
                            IconButton(onClick = {
                                reshoot()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Переснять"
                                )
                            }
                        }
                    )
                }
            )
            { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        modifier = Modifier.fillMaxSize(),
                        painter = rememberAsyncImagePainter(uri),
                        contentScale = ContentScale.FillWidth,
                        contentDescription = null
                    )
                }
            }
        }
    }
}