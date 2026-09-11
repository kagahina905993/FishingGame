package com.example.fishinggame

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private const val FISH_ANIMATION_FRAME_MILLIS = 160L

@Composable
fun FishArtwork(
    fish: Fish,
    modifier: Modifier = Modifier,
    animate: Boolean = false,
    silhouette: Boolean = false
) {
    var frame by remember(fish.name) { mutableIntStateOf(0) }
    val frameResources = fishArtworkResources(fish)

    LaunchedEffect(fish.name, animate, frameResources) {
        frame = 0
        if (animate && frameResources.size > 1) {
            while (true) {
                delay(fishAnimationFrameMillis(fish, frame))
                frame = (frame + 1) % frameResources.size
            }
        }
    }

    Box(
        modifier = modifier.semantics {
            contentDescription = if (silhouette) {
                "未発見の魚"
            } else {
                fish.name
            }
        },
        contentAlignment = Alignment.Center
    ) {
        val resourceId = frameResources.getOrNull(frame)
        if (resourceId != null) {
            Image(
                bitmap = ImageBitmap.imageResource(id = resourceId),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                colorFilter = if (silhouette) {
                    ColorFilter.tint(
                        color = Color(0xFF17343D),
                        blendMode = BlendMode.SrcIn
                    )
                } else {
                    null
                },
                filterQuality = FilterQuality.None
            )
        } else {
            Text(
                text = if (silhouette) "◆" else "🐟",
                color = if (silhouette) {
                    Color(0xFF17343D)
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                fontSize = if (silhouette) 42.sp else 52.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

internal fun fishArtworkResources(fish: Fish): List<Int> = when (fish.name) {
    "カワムツ" -> listOf(
        R.drawable.fish_kawamutsu_0,
        R.drawable.fish_kawamutsu_1
    )
    "オイカワ" -> listOf(R.drawable.fish_oikawa)
    "タカハヤ" -> listOf(R.drawable.fish_takahaya)
    "鯉" -> listOf(R.drawable.fish_koi)
    "錦鯉" -> listOf(R.drawable.fish_nishikigoi)
    "アジ" -> listOf(R.drawable.fish_aji)
    "イワシ" -> listOf(R.drawable.fish_iwashi)
    "マグロ" -> listOf(R.drawable.fish_maguro)
    else -> emptyList()
}

private fun fishAnimationFrameMillis(fish: Fish, frame: Int): Long =
    when (fish.name) {
        "カワムツ" -> listOf(100L, 130L).getOrElse(frame) {
            FISH_ANIMATION_FRAME_MILLIS
        }
        else -> FISH_ANIMATION_FRAME_MILLIS
    }
