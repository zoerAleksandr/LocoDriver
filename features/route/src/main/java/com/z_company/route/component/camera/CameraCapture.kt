package com.z_company.route.component.camera

import android.Manifest
import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.media.AudioManager
import android.media.AudioManager.*
import android.media.MediaPlayer
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import com.z_company.route.R
import com.z_company.route.extention.executor
import com.z_company.route.extention.getCameraProvider
import com.z_company.route.extention.takePicture
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@ExperimentalPermissionsApi
@ExperimentalCoroutinesApi
@Composable
fun CameraCapture(
    modifier: Modifier = Modifier,
    gallerySelect: MutableState<Boolean>,
    onImageFile: (File) -> Unit,
    onDismissPermission: () -> Unit
) {
    val context = LocalContext.current
    val executor = context.executor
    val scope = rememberCoroutineScope()
    val audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager

    val volumeLevel = audioManager.getStreamVolume(STREAM_MUSIC)
    val maxVolumeLevel =
        audioManager.getStreamMaxVolume(STREAM_MUSIC).times(0.7).toInt()

    var shimmerState by remember { mutableStateOf(false) }
    fun cameraShimmerStart() {
        scope.launch {
            shimmerState = true
            delay(100)
            shimmerState = false
        }
    }

    var cameraSelector by remember {
        mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA)
    }

    Permission(
        permission = Manifest.permission.CAMERA,
        onDismissPermission = onDismissPermission
    ) {
        Box(modifier = modifier) {
            val lifecycleOwner = LocalLifecycleOwner.current
            val coroutineScope = rememberCoroutineScope()
            var previewUseCase by remember { mutableStateOf<UseCase>(Preview.Builder().build()) }
            val imageCaptureUseCase by remember {
                mutableStateOf(
                    ImageCapture.Builder().setCaptureMode(CAPTURE_MODE_MAXIMIZE_QUALITY).build()
                )
            }

            Box {
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    onUseCase = {
                        previewUseCase = it
                    }
                )

                AnimatedVisibility(
                    exit = fadeOut(animationSpec = tween(durationMillis = 100)),
                    enter = fadeIn(animationSpec = tween(durationMillis = 100)),
                    visible = shimmerState
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(Color.Black)
                        .align(Alignment.TopCenter)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .padding(start = 36.dp, end = 36.dp, bottom = 52.dp, top = 26.dp)
                        .align(Alignment.BottomCenter),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OpenGalleryButton(
                        modifier = Modifier.size(50.dp),
                    ) {
                        gallerySelect.value = true
                    }

                    CapturePictureButton(
                        modifier = Modifier
                    ) {
                        cameraShimmerStart()
                        audioManager.setStreamVolume(
                            STREAM_MUSIC,
                            maxVolumeLevel,
                            FLAG_REMOVE_SOUND_AND_VIBRATE
                        )
                        val sound: MediaPlayer =
                            MediaPlayer.create(context, R.raw.sound_snapshot)

                        sound.start()
                        coroutineScope.launch {
                            imageCaptureUseCase.takePicture(executor).let { file ->
                                onImageFile(file)
                                audioManager.setStreamVolume(
                                    STREAM_MUSIC,
                                    volumeLevel,
                                    FLAG_REMOVE_SOUND_AND_VIBRATE
                                )
                            }
                        }
                    }

                    ReverseCameraButton(
                        modifier = Modifier.size(45.dp)
                    ) {
                        cameraSelector =
                            if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            } else {
                                CameraSelector.DEFAULT_BACK_CAMERA
                            }

                        scope.launch {
                            startCamera(
                                context,
                                lifecycleOwner,
                                cameraSelector,
                                previewUseCase,
                                imageCaptureUseCase
                            )
                        }
                    }
                }
            }

            LaunchedEffect(previewUseCase) {
                startCamera(
                    context,
                    lifecycleOwner,
                    cameraSelector,
                    previewUseCase,
                    imageCaptureUseCase
                )
            }
        }
    }
}

private suspend fun startCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    cameraSelector: CameraSelector,
    previewUseCase: UseCase,
    imageCaptureUseCase: ImageCapture
) {
    val cameraProvider = context.getCameraProvider()
    try {
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner, cameraSelector, previewUseCase, imageCaptureUseCase
        )
    } catch (ex: Exception) {
        Log.e("CameraCapture", "Failed to bind camera use cases", ex)
    }
}