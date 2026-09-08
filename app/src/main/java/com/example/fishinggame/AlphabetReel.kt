package com.example.fishinggame

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.floor
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.delay

private const val ALPHABET_SIZE = 26
private const val REEL_STEP_DURATION_MILLIS = 34
private const val MIN_REEL_DURATION_MILLIS = 135
private const val MAX_REEL_DURATION_MILLIS = 825
private const val FIRST_REEL_DURATION_MILLIS = 625
private const val REEL_FINISH_GAP_MILLIS = 200
private const val CORRECT_REEL_FEEDBACK_MILLIS = 250
private const val WHOLE_ANSWER_FEEDBACK_MILLIS = 700

internal data class ReelAnimationTiming(
    val durationMillis: Int,
    val animationSteps: Int
)

internal fun reelAnimationSchedule(
    rotations: List<Int>
): List<ReelAnimationTiming> {
    var previousEndMillis = 0
    return rotations.mapIndexed { index, rotationSteps ->
        val animationSteps = rotationSteps + ALPHABET_SIZE
        val naturalDuration =
            (animationSteps * REEL_STEP_DURATION_MILLIS)
                .coerceIn(
                    MIN_REEL_DURATION_MILLIS,
                    MAX_REEL_DURATION_MILLIS
                )
        val preferredDuration = if (index == 0) {
            minOf(naturalDuration, FIRST_REEL_DURATION_MILLIS)
        } else {
            naturalDuration
        }
        val durationMillis = maxOf(
            preferredDuration,
            previousEndMillis + REEL_FINISH_GAP_MILLIS
        )
        previousEndMillis = durationMillis
        ReelAnimationTiming(
            durationMillis = durationMillis,
            animationSteps = animationSteps
        )
    }
}

internal fun reelAnimationDurationMillis(rotations: List<Int>): Long =
    reelAnimationSchedule(rotations)
        .lastOrNull()
        ?.durationMillis
        ?.toLong()
        ?: 0L

internal fun reelAnswerAnimationDurationMillis(rotations: List<Int>): Long =
    reelAnimationDurationMillis(rotations) +
        CORRECT_REEL_FEEDBACK_MILLIS +
        WHOLE_ANSWER_FEEDBACK_MILLIS

internal fun reelLetterMatchesAnswer(
    typedAnswer: String,
    correctAnswer: String,
    index: Int
): Boolean = typedAnswer.getOrNull(index)?.uppercaseChar() ==
    correctAnswer.getOrNull(index)?.uppercaseChar()

internal fun alphabetLetterAfterSteps(start: Char, steps: Int): Char {
    require(start in 'A'..'Z') { "リールの開始文字はA〜Zで指定してください" }
    require(steps >= 0) { "回転数は0以上で指定してください" }
    return 'A' + ((start - 'A' + steps) % ALPHABET_SIZE)
}

@Composable
internal fun AlphabetReelPanel(
    typedAnswer: String,
    rotations: List<Int>,
    animationRunId: Int = 0,
    correctAnswer: String = "",
    showCorrectStopFeedback: Boolean = false,
    modifier: Modifier = Modifier
) {
    val letters = typedAnswer.uppercase().toList()
    val reelCount = minOf(letters.size, rotations.size)
    val animationSchedule = reelAnimationSchedule(
        rotations.take(reelCount)
    )
    val wholeAnswerPulse = remember { Animatable(0f) }
    val wholeAnswerIsCorrect = typedAnswer.isNotEmpty() &&
        typedAnswer.equals(correctAnswer, ignoreCase = true)

    LaunchedEffect(
        animationRunId,
        showCorrectStopFeedback,
        wholeAnswerIsCorrect,
        reelCount
    ) {
        wholeAnswerPulse.snapTo(0f)
        if (showCorrectStopFeedback && wholeAnswerIsCorrect) {
            delay(
                reelAnimationDurationMillis(rotations.take(reelCount)) +
                    CORRECT_REEL_FEEDBACK_MILLIS
            )
            wholeAnswerPulse.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 250)
            )
            wholeAnswerPulse.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 450)
            )
        }
    }

    val wholePulseValue = wholeAnswerPulse.value
    val wholePulseScale = 1f + 0.025f * sin(
        PI.toFloat() * wholePulseValue
    )
    val successColor = Color(0xFF43A047)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = wholePulseScale
                scaleY = wholePulseScale
            },
        shape = RoundedCornerShape(14.dp),
        color = lerp(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            successColor,
            0.38f * wholePulseValue
        ),
        border = BorderStroke(
            width = if (wholePulseValue > 0f) 2.dp else 1.dp,
            color = lerp(
                MaterialTheme.colorScheme.outlineVariant,
                successColor,
                wholePulseValue
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "英単語リール",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "合計 ${rotations.take(reelCount).sum()}回転",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (reelCount == 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    AlphabetReel(
                        startLetter = 'A',
                        targetLetter = 'A',
                        rotationSteps = 0,
                        position = null
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clipToBounds()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(reelCount) { index ->
                        AlphabetReel(
                            startLetter = if (index == 0) {
                                'A'
                            } else {
                                letters[index - 1]
                            },
                            targetLetter = letters[index],
                            rotationSteps = rotations[index],
                            position = index + 1,
                            animationRunId = animationRunId,
                            animationTiming = animationSchedule[index],
                            showCorrectStopFeedback =
                                showCorrectStopFeedback &&
                                    reelLetterMatchesAnswer(
                                        typedAnswer = typedAnswer,
                                        correctAnswer = correctAnswer,
                                        index = index
                                    )
                        )
                    }
                }
            }
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = when {
                    wholePulseValue > 0f -> "✓ 全部正解！"
                    reelCount == 0 -> "すべてのリールは A から始まります"
                    else -> "同時に回転し、左から順に止まります"
                },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (wholePulseValue > 0f) {
                    FontWeight.Black
                } else {
                    FontWeight.Normal
                },
                color = if (wholePulseValue > 0f) {
                    successColor
                } else {
                    Color.Unspecified
                },
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AlphabetReel(
    startLetter: Char,
    targetLetter: Char,
    rotationSteps: Int,
    position: Int?,
    animationRunId: Int = 0,
    animationTiming: ReelAnimationTiming? = null,
    showCorrectStopFeedback: Boolean = false
) {
    val animatedSteps = remember { Animatable(0f) }
    val correctPulse = remember { Animatable(0f) }
    LaunchedEffect(
        startLetter,
        targetLetter,
        rotationSteps,
        animationRunId,
        animationTiming,
        showCorrectStopFeedback
    ) {
        animatedSteps.snapTo(0f)
        correctPulse.snapTo(0f)
        if (rotationSteps > 0) {
            animatedSteps.animateTo(
                targetValue = (
                    animationTiming?.animationSteps ?: rotationSteps
                ).toFloat(),
                animationSpec = tween(
                    durationMillis = animationTiming?.durationMillis
                        ?: (rotationSteps * REEL_STEP_DURATION_MILLIS)
                            .coerceIn(
                                MIN_REEL_DURATION_MILLIS,
                                MAX_REEL_DURATION_MILLIS
                            ),
                    easing = LinearEasing
                )
            )
            if (showCorrectStopFeedback) {
                correctPulse.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 90)
                )
                correctPulse.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 160)
                )
            }
        }
    }

    val completedSteps = floor(animatedSteps.value).toInt()
        .coerceAtMost(animationTiming?.animationSteps ?: rotationSteps)
    val stepFraction = animatedSteps.value - floor(animatedSteps.value)
    val currentLetter = alphabetLetterAfterSteps(
        start = startLetter,
        steps = completedSteps
    )
    val previousLetter = alphabetLetterAfterSteps(currentLetter, 25)
    val nextLetter = alphabetLetterAfterSteps(currentLetter, 1)
    val reelShape = RoundedCornerShape(10.dp)

    Column(
        modifier = Modifier.width(58.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 58.dp, height = 72.dp)
                .graphicsLayer {
                    val pulseScale = 1f + 0.08f * sin(
                        PI.toFloat() * correctPulse.value
                    )
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
                .clip(reelShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF15232D),
                            Color(0xFF314653),
                            Color(0xFF15232D)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Color(0xFF43A047).copy(
                            alpha = 0.62f * correctPulse.value
                        )
                    )
            )
            ReelLetter(
                letter = previousLetter,
                yOffset = -30f - stepFraction * 30f,
                color = Color.White.copy(alpha = 0.38f),
                fontSize = 16
            )
            ReelLetter(
                letter = currentLetter,
                yOffset = -stepFraction * 30f,
                color = Color.White,
                fontSize = 28
            )
            ReelLetter(
                letter = nextLetter,
                yOffset = 30f - stepFraction * 30f,
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 16
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .align(Alignment.Center)
                    .background(Color.White.copy(alpha = 0.22f))
            )
            if (correctPulse.value > 0f) {
                Text(
                    text = "✓",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
        Text(
            text = if (position == null) {
                "開始 A"
            } else {
                "$position: +$rotationSteps"
            },
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}

@Composable
private fun ReelLetter(
    letter: Char,
    yOffset: Float,
    color: Color,
    fontSize: Int
) {
    Text(
        text = letter.toString(),
        modifier = Modifier.offset(y = yOffset.dp),
        color = color,
        fontSize = fontSize.sp,
        fontWeight = FontWeight.Black,
        textAlign = TextAlign.Center
    )
}
