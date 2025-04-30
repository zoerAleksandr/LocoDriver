package com.z_company.route.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.rememberAsyncImagePainter
import com.z_company.core.ResultState
import com.z_company.core.ui.component.AsyncData
import com.z_company.core.ui.component.GenericError
import com.z_company.core.ui.theme.custom.AppTypography
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
    val subTitleTextStyle = AppTypography.getType().titleLarge
        .copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal
        )
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
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                TextButton(onClick = {
                                    reshoot()
                                }) {
                                    Text(
                                        style = subTitleTextStyle,
                                        text = "Переснять"
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        val bitmap = ConverterUrlBase64.uriToBitmap(
                                            uri.toUri(),
                                            context.contentResolver
                                        )
                                        onSavePhoto(bitmap)
                                    }
                                ) {
                                    Text(
                                        text = "Сохранить",
                                        style = subTitleTextStyle
                                    )
                                }
                            }
                        },
                        containerColor = Color.Transparent
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