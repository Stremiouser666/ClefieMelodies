package com.clefie.melodies.ui

import android.graphics.PixelFormat
import android.net.Uri
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    assetPath: String,
    modifier: Modifier = Modifier,
    loop: Boolean = true,
    speed: Float = 1f,
    transparent: Boolean = false
) {
    val context = LocalContext.current

    val exoPlayer = remember(assetPath) {
        ExoPlayer.Builder(context).build().apply {
            val uri = Uri.parse("asset:///$assetPath")
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            volume = 0f
            playWhenReady = true
            prepare()
        }
    }

    LaunchedEffect(speed) {
        exoPlayer.setPlaybackSpeed(speed.coerceIn(0.25f, 3f))
    }

    DisposableEffect(assetPath) {
        onDispose { exoPlayer.release() }
    }

    AndroidView(
        factory = { ctx ->
            TextureView(ctx).apply {
                if (transparent) {
                    // Enable alpha compositing for transparency
                    isOpaque = false
                    surfaceTexture?.let { st ->
                        // Will be set once surface is available
                    }
                }
                exoPlayer.setVideoTextureView(this)
            }
        },
        modifier = modifier
    )
}
