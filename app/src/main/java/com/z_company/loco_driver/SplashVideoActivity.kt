package com.z_company.loco_driver

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.z_company.loco_driver.ui.theme.LocoDriverTheme
import androidx.media3.ui.AspectRatioFrameLayout

@SuppressLint("CustomSplashScreen")
class SplashVideoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Полноэкранный режим без статус-бара и навигации
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                scrim = Color.Transparent.toArgb(),
            ),
            navigationBarStyle = SystemBarStyle.light(
                scrim = Color.Transparent.toArgb(),
                darkScrim = Color.Transparent.toArgb()
            )
        )

        setContent {
            LocoDriverTheme {
                VideoSplashScreen(
                    onVideoEnded = {
                        startActivity(Intent(this@SplashVideoActivity, MainActivity::class.java))
                        finish()
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    }
                )
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoSplashScreen(onVideoEnded: () -> Unit) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.Builder()
                .setUri("android.resource://${context.packageName}/${R.raw.splash_video}")
                .build()

            setMediaItem(mediaItem)
            repeatMode = Player.REPEAT_MODE_OFF
            prepare()
            playWhenReady = true

            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        onVideoEnded()
                    }
                }
            })
        }
    }

    AndroidView(
        factory = {
            PlayerView(it).apply {
                player = exoPlayer
                useController = false
                setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                setBackgroundColor(android.graphics.Color.BLACK)
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM   // ← вот это centerCrop
                // Если видео искажается — попробуйте RESIZE_MODE_FILL (полное растяжение)
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    )

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
}