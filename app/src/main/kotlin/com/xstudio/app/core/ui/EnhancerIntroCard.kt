package com.xstudio.app.core.ui

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.xstudio.app.core.designsystem.XBlack
import com.xstudio.app.core.designsystem.XGlassBorder
import com.xstudio.app.core.designsystem.XStudioLogo
import com.xstudio.app.core.designsystem.XSurfaceElevated
import com.xstudio.app.core.designsystem.XWhite
import com.xstudio.app.core.designsystem.XWhiteMuted
import com.xstudio.app.core.designsystem.liquidGlass

/**
 * Native port of source/src/intro.tsx:
 * Plays assets/hero.mp4 (1280x720 @ 29.97fps monochrome After Effects hero loop)
 * muted & looping at the top of the Enhancer / Video Enhancer / Batch Enhancer intro screen,
 * with Upload + Select asset buttons below.
 */
@OptIn(UnstableApi::class)
@Composable
fun EnhancerIntroCard(
    title: String,
    description: String,
    note: String,
    onPickClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var videoFailed by remember { mutableStateOf(false) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0f
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    videoFailed = true
                }
            })
            setMediaItem(MediaItem.fromUri(Uri.parse("asset:///hero.mp4")))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(RoundedCornerShape(28.dp))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(20.dp))
                .background(XSurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            if (videoFailed) {
                XStudioLogo(iconSize = 56.dp, showWordmark = false)
            } else {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            XStudioLogo(iconSize = 32.dp, showWordmark = false)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                color = XWhite,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Text(
            text = description,
            color = XWhiteMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onPickClick,
                colors = ButtonDefaults.buttonColors(containerColor = XWhite, contentColor = XBlack),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = XBlack, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upload", fontWeight = FontWeight.ExtraBold)
            }

            Button(
                onClick = onPickClick,
                colors = ButtonDefaults.buttonColors(containerColor = XBlack, contentColor = XWhite),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .border(1.dp, XGlassBorder, RoundedCornerShape(16.dp))
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = XWhite, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select asset", fontWeight = FontWeight.Bold)
            }
        }

        Text(
            text = note,
            color = XWhiteMuted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}
