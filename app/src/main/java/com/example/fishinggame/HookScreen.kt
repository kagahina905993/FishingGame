package com.example.fishinggame

import android.media.MediaPlayer
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private enum class HookAnimationStage {
    WAITING,
    APPROACHING,
    NIBBLING,
    BITING,
    HOOKED
}

@Composable
fun HookScreen(
    modifier: Modifier = Modifier,
    state: GameUiState,
    onStartBattle: () -> Unit
) {
    val fish = state.currentFish?.fish
    val applicationContext = LocalContext.current.applicationContext
    val approachProgress = remember(fish?.name) { Animatable(0f) }
    val nibbleProgress = remember(fish?.name) { Animatable(0f) }
    val hookedProgress = remember(fish?.name) { Animatable(0f) }
    var animationStage by remember(fish?.name) {
        mutableStateOf(HookAnimationStage.WAITING)
    }

    LaunchedEffect(fish?.name) {
        approachProgress.snapTo(0f)
        nibbleProgress.snapTo(0f)
        hookedProgress.snapTo(0f)
        animationStage = HookAnimationStage.WAITING
        if (fish == null) return@LaunchedEffect

        val castSoundPlayer = MediaPlayer.create(
            applicationContext,
            R.raw.reel_cast
        )
        var splashSoundPlayer: MediaPlayer? = null
        try {
            castSoundPlayer?.start()
            delay(350L)
            splashSoundPlayer = MediaPlayer.create(
                applicationContext,
                R.raw.lure_splash
            )
            splashSoundPlayer?.start()
            animationStage = HookAnimationStage.APPROACHING
            approachProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 700)
            )
            animationStage = HookAnimationStage.NIBBLING
            nibbleProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 450,
                    easing = LinearEasing
                )
            )
            animationStage = HookAnimationStage.BITING
            delay(160L)
            hookedProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 440)
            )
            animationStage = HookAnimationStage.HOOKED
        } finally {
            splashSoundPlayer?.release()
            castSoundPlayer?.release()
        }
    }

    val transition = rememberInfiniteTransition(label = "hookPulse")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hookScale"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "${state.selectedMap?.name} ＞ ${state.selectedPoint?.name}",
            style = MaterialTheme.typography.labelLarge
        )
        StreamDiorama(
            environment = state.selectedMap?.environment
                ?: FishingEnvironment.STREAM,
            underwater = true,
            showFish = true,
            showHook = true,
            fish = fish,
            fishApproachProgress = approachProgress.value,
            fishNibbleProgress = nibbleProgress.value,
            fishHookedProgress = hookedProgress.value
        )
        if (animationStage == HookAnimationStage.HOOKED) {
            Text(
                text = "HIT！",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
            )
            Text(
                text = "魚が針にかかった！\n英単語リールで巻き上げよう。",
                textAlign = TextAlign.Center
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onStartBattle
            ) {
                Text(text = "リールバトル開始")
            }
        } else {
            Text(
                text = when (animationStage) {
                    HookAnimationStage.WAITING -> "水中の様子を見ています…"
                    HookAnimationStage.APPROACHING ->
                        "魚が針に近づいている…"
                    HookAnimationStage.NIBBLING,
                    HookAnimationStage.BITING ->
                        "魚が餌をつついている…"
                    HookAnimationStage.HOOKED -> ""
                },
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
