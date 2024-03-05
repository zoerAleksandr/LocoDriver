package com.example.route.component.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.route.R

@Composable
fun CapturePictureButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = { },
) {
    IconButton(
        modifier = modifier,
        onClick = onClick,
        content = {
            Icon(
                painter = painterResource(id = R.drawable.lens_24px),
                contentDescription = "Сделать фото",
                tint = Color.White,
                modifier = Modifier
                    .size(70.dp)
                    .padding(1.dp)
                    .border(1.dp, Color.White, CircleShape)
            )
        }
    )
}

@Composable
fun OpenGalleryButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(modifier = Modifier.background(Color.Transparent)) {
        IconButton(
            modifier = modifier,
            onClick = onClick
        ) {
            Icon(
                modifier = Modifier.fillMaxSize(),
                painter = painterResource(id = R.drawable.photo_library_24px),
                tint = Color.White,
                contentDescription = "Открыть галлерею"
            )
        }
    }
}

@Composable
fun ReverseCameraButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier = Modifier.background(Color.Transparent)) {
        IconButton(
            modifier = modifier,
            onClick = onClick,
            content = {
                Icon(
                    painter = painterResource(id = R.drawable.cameraswitch_24px),
                    contentDescription = "Развернуть камеру",
                    tint = Color.White,
                    modifier = Modifier.fillMaxSize()
                )
            }
        )
    }
}


