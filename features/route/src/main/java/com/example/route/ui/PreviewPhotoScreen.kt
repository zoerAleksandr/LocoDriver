package com.example.route.ui

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
import coil.compose.rememberAsyncImagePainter
import com.example.core.ResultState
import com.example.core.ui.component.AsyncData
import com.example.core.ui.component.GenericError

@Composable
fun PreviewPhotoScreen(
    photoUrl: String,
    basicId: String,
    onSavePhoto: () -> Unit,
    reshoot: () -> Unit,
    onPhotoSaved: (String) -> Unit,
    resetSaveState: () -> Unit,
    photoSaveState: ResultState<Unit>?
) {
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
                                    onSavePhoto()
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
                        painter = rememberAsyncImagePainter(photoUrl),
                        contentScale = ContentScale.FillWidth,
                        contentDescription = null
                    )
                }
            }
        }
    }
}