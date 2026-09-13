package com.example.fishinggame

import android.media.MediaPlayer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class GameFeedback {
    CORRECT,
    WRONG,
    SKIPPED
}

private const val CATCH_SOUND_LEAD_IN_MILLIS = 1_660L

@Composable
fun FishingGameScreen(
    modifier: Modifier = Modifier,
    state: GameUiState,
    word: Word,
    fishState: FishState,
    onAnswerChanged: (String) -> Unit,
    onSubmitAnswer: () -> Unit,
    onSelectChoice: (String) -> Unit,
    onSkipQuestion: () -> Unit,
    onQuestionTimerTick: (Long, Long) -> Unit,
    onSubmitDebugAnswer: (Boolean) -> Unit,
    onCycleDebugFish: () -> Unit,
    onCatchDebugFish: () -> Unit
) {
    var showFullMeaning by remember(word.english) {
        mutableStateOf(false)
    }
    var showDebugControls by remember {
        mutableStateOf(false)
    }
    var feedback by remember {
        mutableStateOf<GameFeedback?>(null)
    }
    var showResultPanel by remember {
        mutableStateOf(state.gameResult != GameResult.CAUGHT)
    }
    var previousCorrectCount by remember {
        mutableStateOf(state.correctAnswerCount)
    }
    var previousIncorrectCount by remember {
        mutableStateOf(state.incorrectAnswerCount)
    }
    var lastPlayedWrongAnswerCount by remember {
        mutableIntStateOf(state.incorrectAnswerCount)
    }
    var previousSkippedCount by remember {
        mutableStateOf(state.skippedQuestionCount)
    }
    var lastPlayedCatchAtEpochMillis by rememberSaveable {
        mutableLongStateOf(0L)
    }
    val applicationContext = LocalContext.current.applicationContext
    val gameScreenScrollState = rememberScrollState()

    LaunchedEffect(state.incorrectAnswerCount) {
        val shouldPlay =
            state.incorrectAnswerCount > lastPlayedWrongAnswerCount
        lastPlayedWrongAnswerCount = state.incorrectAnswerCount
        if (!shouldPlay) {
            return@LaunchedEffect
        }
        val player = MediaPlayer.create(
            applicationContext,
            R.raw.wrong_answer
        ) ?: return@LaunchedEffect
        try {
            player.start()
            delay((player.duration + 100L).coerceAtLeast(100L))
        } finally {
            player.release()
        }
    }

    LaunchedEffect(
        state.gameResult,
        state.battleFinishedAtEpochMillis
    ) {
        val caughtAt = state.battleFinishedAtEpochMillis
        if (
            state.gameResult != GameResult.CAUGHT ||
            caughtAt <= 0L ||
            caughtAt == lastPlayedCatchAtEpochMillis
        ) {
            return@LaunchedEffect
        }
        lastPlayedCatchAtEpochMillis = caughtAt
        val player = MediaPlayer.create(
            applicationContext,
            R.raw.fish_catch
        ) ?: return@LaunchedEffect
        try {
            player.start()
            delay((player.duration + 100L).coerceAtLeast(100L))
        } finally {
            player.release()
        }
    }

    LaunchedEffect(
        state.gameResult,
        state.battleFinishedAtEpochMillis
    ) {
        if (
            state.gameResult != GameResult.CAUGHT ||
            state.battleFinishedAtEpochMillis <= 0L
        ) {
            return@LaunchedEffect
        }
        delay(CATCH_SOUND_LEAD_IN_MILLIS)
        val player = MediaPlayer.create(
            applicationContext,
            R.raw.fish_flopping_loop
        ) ?: return@LaunchedEffect
        player.isLooping = true
        try {
            player.start()
            awaitCancellation()
        } finally {
            runCatching { player.stop() }
            player.release()
        }
    }

    LaunchedEffect(
        showDebugControls,
        gameScreenScrollState.maxValue
    ) {
        if (showDebugControls && gameScreenScrollState.maxValue > 0) {
            gameScreenScrollState.animateScrollTo(
                gameScreenScrollState.maxValue
            )
        }
    }

    LaunchedEffect(state.phase) {
        if (state.phase != GamePhase.INPUT) {
            showDebugControls = false
        }
    }

    LaunchedEffect(state.gameResult) {
        if (state.gameResult != null) {
            gameScreenScrollState.scrollTo(0)
        }
    }

    LaunchedEffect(state.currentLearningAttemptId) {
        if (state.phase == GamePhase.INPUT) {
            gameScreenScrollState.scrollTo(0)
        }
    }

    LaunchedEffect(
        state.correctAnswerCount,
        state.incorrectAnswerCount,
        state.skippedQuestionCount,
        state.gameResult
    ) {
        val nextFeedback = when {
            state.gameResult != null -> null
            state.correctAnswerCount > previousCorrectCount ->
                GameFeedback.CORRECT
            state.incorrectAnswerCount > previousIncorrectCount ->
                GameFeedback.WRONG
            state.skippedQuestionCount > previousSkippedCount ->
                GameFeedback.SKIPPED
            else -> null
        }

        previousCorrectCount = state.correctAnswerCount
        previousIncorrectCount = state.incorrectAnswerCount
        previousSkippedCount = state.skippedQuestionCount

        if (nextFeedback != null) {
            feedback = nextFeedback
            delay(1000L)
            if (feedback == nextFeedback) {
                feedback = null
            }
        } else if (state.gameResult != null) {
            feedback = null
        }
    }

    LaunchedEffect(state.gameResult) {
        showResultPanel = state.gameResult != GameResult.CAUGHT
        if (state.gameResult == GameResult.CAUGHT) {
            delay(700L)
            showResultPanel = true
        }
    }

    val displayedMeaning = if (showFullMeaning) {
        word.japanese
    } else {
        word.questionMeaning
    }
    val displayedPartOfSpeech = word.questionPartOfSpeech
    val debugControlsAvailable = BuildConfig.DEBUG &&
        state.phase == GamePhase.INPUT
    val shouldScrollGameScreen = debugControlsAvailable ||
        state.gameResult != null

    Column(
        modifier = if (shouldScrollGameScreen) {
            modifier.verticalScroll(gameScreenScrollState)
        } else {
            modifier
        },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GameTopHud(
            state = state,
            fishState = fishState
        )

        FishingScene(
            modifier = if (shouldScrollGameScreen) {
                Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            } else {
                Modifier
                    .weight(1f)
                    .heightIn(min = 96.dp)
            },
            fishState = fishState,
            feedback = feedback,
            timeBonus = state.lastTimeBonus,
            shouldAnimateFish = state.phase == GamePhase.INPUT,
            gameResult = state.gameResult
        )

        if (state.gameResult == null) {
            FishingGauges(
                state = state,
                fishState = fishState
            )

            QuestionPanel(
                state = state,
                word = word,
                displayedMeaning = displayedMeaning,
                displayedPartOfSpeech = displayedPartOfSpeech,
                showFullMeaning = showFullMeaning,
                feedback = feedback,
                onAnswerChanged = onAnswerChanged,
                onSubmitAnswer = onSubmitAnswer,
                onSelectChoice = onSelectChoice,
                onSkipQuestion = onSkipQuestion,
                onQuestionTimerTick = onQuestionTimerTick,
                onToggleFullMeaning = {
                    showFullMeaning = !showFullMeaning
                }
            )
        } else {
            AnimatedVisibility(
                visible = showResultPanel,
                enter = fadeIn() + scaleIn(initialScale = 0.94f)
            ) {
                GameResultPanel(
                    state = state,
                    fishState = fishState
                )
            }
        }

        if (debugControlsAvailable) {
            TextButton(
                modifier = Modifier.align(Alignment.End),
                onClick = { showDebugControls = !showDebugControls }
            ) {
                Text(
                    text = if (showDebugControls) {
                        "デバッグ操作を閉じる"
                    } else {
                        "デバッグ操作"
                    }
                )
            }
            if (showDebugControls) {
                DebugGameControls(
                    state = state,
                    onSubmitDebugAnswer = onSubmitDebugAnswer,
                    onCycleDebugFish = onCycleDebugFish,
                    onCatchDebugFish = onCatchDebugFish
                )
            }
        }
    }
}

@Composable
private fun GameTopHud(state: GameUiState, fishState: FishState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 14.dp,
                vertical = 8.dp
            ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${state.selectedMap?.name ?: "不明なマップ"}" +
                        " ＞ ${state.selectedPoint?.name ?: "不明なポイント"}",
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = if (
                        state.selectedPoint?.lordFishName ==
                        fishState.fish.name
                    ) {
                        "【主】${fishState.fish.name}"
                    } else {
                        fishState.fish.name
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = fishState.fish.rarity.displayText,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = state.selectedCourseName,
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = "${state.score} pt",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FishingScene(
    modifier: Modifier = Modifier,
    fishState: FishState,
    feedback: GameFeedback?,
    timeBonus: Int,
    shouldAnimateFish: Boolean,
    gameResult: GameResult?
) {
    val catchProgress = remember(fishState.fish.name) { Animatable(0f) }
    LaunchedEffect(gameResult) {
        catchProgress.snapTo(0f)
        if (gameResult == GameResult.CAUGHT) {
            catchProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 900)
            )
        }
    }
    val reelProgress by animateFloatAsState(
        targetValue = 1f - fishState.distanceProgress,
        animationSpec = tween(durationMillis = 650),
        label = "fishApproach"
    )
    val seaGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF4FC3F7),
            Color(0xFF0277BD),
            Color(0xFF01579B)
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(seaGradient)
            .padding(12.dp)
    ) {
        Text(
            text = "○   ·       ○\n      ·",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 18.sp,
            modifier = Modifier.align(Alignment.TopStart)
        )
        CatchSplash(
            progress = catchProgress.value,
            modifier = Modifier.fillMaxSize()
        )
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .size(120.dp)
                .graphicsLayer {
                    val approachScale = 0.85f + reelProgress * 0.35f
                    scaleX = approachScale
                    scaleY = approachScale
                    val jumpArc = 4f * catchProgress.value *
                        (1f - catchProgress.value)
                    translationY = -145f * jumpArc
                    rotationZ = -12f * sin(
                        PI.toFloat() * catchProgress.value
                    )
                },
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.14f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                FishArtwork(
                    fish = fishState.fish,
                    modifier = Modifier.size(
                        width = 104.dp,
                        height = 70.dp
                    ),
                    animate = shouldAnimateFish
                )
            }
        }
        Text(
            text = "魚まで %.1f m".format(fishState.currentDistance),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        AnimatedVisibility(
            visible = feedback != null,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 0.9f)
        ) {
            val feedbackColor = when (feedback) {
                GameFeedback.CORRECT -> Color(0xFF2E7D32)
                GameFeedback.WRONG -> Color(0xFFC62828)
                GameFeedback.SKIPPED -> Color(0xFFF57C00)
                null -> Color.Transparent
            }
            val feedbackText = when (feedback) {
                GameFeedback.CORRECT -> if (timeBonus > 0) {
                    "正解！ 巻き上げ成功  +$timeBonus"
                } else {
                    "正解！ 巻き上げ成功"
                }
                GameFeedback.WRONG -> "不正解！ テンション上昇"
                GameFeedback.SKIPPED -> "スキップ  テンション +10"
                null -> ""
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = feedbackColor.copy(alpha = 0.92f)
            ) {
                Text(
                    text = feedbackText,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(
                        horizontal = 14.dp,
                        vertical = 7.dp
                    )
                )
            }
        }
    }
}

@Composable
private fun CatchSplash(
    progress: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (progress <= 0f || progress >= 1f) return@Canvas

        val rippleProgress = (progress / 0.85f).coerceIn(0f, 1f)
        val rippleWidth = 28f + 190f * rippleProgress
        val rippleHeight = 8f + 34f * rippleProgress
        val waterLineY = size.height * 0.53f
        drawOval(
            color = Color.White.copy(
                alpha = 0.75f * (1f - rippleProgress)
            ),
            topLeft = Offset(
                x = center.x - rippleWidth / 2f,
                y = waterLineY - rippleHeight / 2f
            ),
            size = Size(rippleWidth, rippleHeight),
            style = Stroke(width = 4f)
        )

        val dropletProgress = (progress / 0.55f).coerceIn(0f, 1f)
        val dropletArc = sin(PI.toFloat() * dropletProgress)
        listOf(-72f, -38f, 38f, 72f).forEachIndexed { index, x ->
            val height = if (index == 0 || index == 3) 52f else 76f
            drawCircle(
                color = Color.White.copy(
                    alpha = 0.8f * (1f - dropletProgress)
                ),
                radius = if (index % 2 == 0) 6f else 8f,
                center = Offset(
                    x = center.x + x,
                    y = waterLineY - height * dropletArc
                )
            )
        }
    }
}

@Composable
private fun FishingGauges(state: GameUiState, fishState: FishState) {
    val dangerPulseTransition = rememberInfiniteTransition(
        label = "tensionDanger"
    )
    val dangerAlpha by dangerPulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 450),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tensionDangerAlpha"
    )
    val isDangerous = state.lineTension >= 80
    val projectedFishState = state.rotations
        .takeIf { it.isNotEmpty() }
        ?.let { rotations ->
            reelFish(
                state = fishState,
                totalRotation = rotations.sum()
            )
        }
    val projectedReelProgress = projectedFishState?.let {
        1f - it.distanceProgress
    }
    val isReeling = state.phase == GamePhase.REELING &&
        projectedFishState != null
    val reelGaugeAnimationDurationMillis = if (isReeling) {
        reelAnswerAnimationDurationMillis(state.rotations)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
    } else {
        650
    }
    val displayedReelProgress = if (isReeling) {
        projectedReelProgress ?: (1f - fishState.distanceProgress)
    } else {
        1f - fishState.distanceProgress
    }
    val animatedDistance by animateFloatAsState(
        targetValue = if (isReeling) {
            projectedFishState?.currentDistance ?: fishState.currentDistance
        } else {
            fishState.currentDistance
        },
        animationSpec = tween(
            durationMillis = reelGaugeAnimationDurationMillis
        ),
        label = "reelDistance"
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        GaugeRow(
            label = "巻き上げ",
            valueText = when {
                isReeling -> "巻き上げ中 %.1f m".format(animatedDistance)
                projectedFishState != null -> {
                    "%.1f m → %.1f m".format(
                        fishState.currentDistance,
                        projectedFishState.currentDistance
                    )
                }
                else -> "%.1f m".format(fishState.currentDistance)
            },
            progress = displayedReelProgress,
            color = MaterialTheme.colorScheme.primary,
            previewProgress = projectedReelProgress,
            previewColor = Color(0xFFFFB300),
            progressAnimationDurationMillis =
                reelGaugeAnimationDurationMillis
        )
        GaugeRow(
            label = "テンション",
            valueText = "${state.lineTension}%",
            progress = state.lineTensionProgress,
            color = when {
                state.lineTension >= 80 -> MaterialTheme.colorScheme.error
                state.lineTension >= 50 -> Color(0xFFF57C00)
                else -> Color(0xFF2E7D32)
            },
            emphasisAlpha = if (isDangerous) dangerAlpha else 1f
        )
        if (isDangerous) {
            Text(
                text = "糸が切れそう！",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.End)
                    .graphicsLayer { alpha = dangerAlpha }
            )
        }
    }
}

@Composable
private fun GaugeRow(
    label: String,
    valueText: String,
    progress: Float,
    color: Color,
    previewProgress: Float? = null,
    previewColor: Color = Color.Transparent,
    emphasisAlpha: Float = 1f,
    progressAnimationDurationMillis: Int = 650
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(
            durationMillis = progressAnimationDurationMillis
        ),
        label = "gaugeProgress"
    )
    val animatedPreviewProgress by animateFloatAsState(
        targetValue = (previewProgress ?: progress).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 650),
        label = "gaugePreviewProgress"
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = Modifier.graphicsLayer { alpha = emphasisAlpha },
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .progressSemantics(animatedProgress)
        ) {
            val radius = CornerRadius(size.height / 2f)
            drawRoundRect(
                color = trackColor,
                cornerRadius = radius
            )
            if (previewProgress != null) {
                drawRoundRect(
                    color = previewColor,
                    size = Size(
                        width = size.width * animatedPreviewProgress,
                        height = size.height
                    ),
                    cornerRadius = radius
                )
            }
            drawRoundRect(
                color = color,
                size = Size(
                    width = size.width * animatedProgress,
                    height = size.height
                ),
                cornerRadius = radius
            )
        }
    }
}

@Composable
private fun QuestionTimer(
    state: GameUiState,
    onQuestionTimerTick: (Long, Long) -> Unit
) {
    var currentTimeMillis by remember(state.questionStartedAtEpochMillis) {
        mutableLongStateOf(System.currentTimeMillis())
    }
    LaunchedEffect(
        state.questionStartedAtEpochMillis,
        state.phase
    ) {
        while (state.phase == GamePhase.INPUT) {
            currentTimeMillis = System.currentTimeMillis()
            val elapsedSeconds = if (
                state.questionStartedAtEpochMillis <= 0L
            ) {
                0L
            } else {
                ((currentTimeMillis -
                    state.questionStartedAtEpochMillis) / 1000L)
                    .coerceAtLeast(0L)
            }
            onQuestionTimerTick(
                state.questionStartedAtEpochMillis,
                elapsedSeconds
            )
            delay(1000L)
        }
    }
    val elapsedSeconds = if (state.questionStartedAtEpochMillis <= 0L) {
        0L
    } else {
        ((currentTimeMillis - state.questionStartedAtEpochMillis) / 1000L)
            .coerceAtLeast(0L)
    }
    Text(text = "回答時間：${elapsedSeconds}秒")
}

@Composable
private fun QuestionPanel(
    state: GameUiState,
    word: Word,
    displayedMeaning: String,
    displayedPartOfSpeech: String?,
    showFullMeaning: Boolean,
    feedback: GameFeedback?,
    onAnswerChanged: (String) -> Unit,
    onSubmitAnswer: () -> Unit,
    onSelectChoice: (String) -> Unit,
    onSkipQuestion: () -> Unit,
    onQuestionTimerTick: (Long, Long) -> Unit,
    onToggleFullMeaning: () -> Unit
) {
    val fullMeaningScrollState = rememberScrollState()
    val shakeOffset = remember { Animatable(0f) }
    val questionEntrance = remember { Animatable(0f) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isMultipleChoice =
        state.currentQuestionFormat == QuestionFormat.MULTIPLE_CHOICE
    val showFirstAppearanceBadge = remember(
        word.wordId,
        state.currentLearningAttemptId
    ) {
        isFirstAppearance(
            word = word,
            learningStates = state.learningStates
        )
    }
    val cardColor by animateColorAsState(
        targetValue = when (feedback) {
            GameFeedback.CORRECT -> MaterialTheme.colorScheme.surfaceContainer
            GameFeedback.WRONG -> MaterialTheme.colorScheme.errorContainer
            GameFeedback.SKIPPED -> MaterialTheme.colorScheme.tertiaryContainer
            null -> MaterialTheme.colorScheme.surfaceContainer
        },
        animationSpec = tween(durationMillis = 220),
        label = "questionCardColor"
    )

    LaunchedEffect(feedback) {
        if (feedback == GameFeedback.WRONG) {
            listOf(-10f, 10f, -7f, 7f, 0f).forEach { target ->
                shakeOffset.animateTo(
                    targetValue = target,
                    animationSpec = tween(durationMillis = 55)
                )
            }
        } else {
            shakeOffset.snapTo(0f)
        }
    }

    LaunchedEffect(state.currentLearningAttemptId) {
        questionEntrance.snapTo(0f)
        questionEntrance.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.68f,
                stiffness = 480f
            )
        )
    }

    LaunchedEffect(
        state.phase,
        state.questionStartedAtEpochMillis
    ) {
        if (state.phase == GamePhase.INPUT && !isMultipleChoice) {
            delay(100L)
            focusRequester.requestFocus()
            keyboardController?.show()
        } else {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                val visibleProgress = questionEntrance.value
                    .coerceIn(0f, 1f)
                alpha = 0.25f + 0.75f * visibleProgress
                val entranceScale = 0.94f +
                    0.06f * questionEntrance.value
                scaleX = entranceScale
                scaleY = entranceScale
                translationY = (1f - questionEntrance.value) *
                    18.dp.toPx()
            }
            .offset {
                IntOffset(
                    x = shakeOffset.value.roundToInt(),
                    y = 0
                )
            },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "問題 ${state.currentQuestionIndex + 1}" +
                            " / ${state.words.size}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (showFirstAppearanceBadge) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = "初出",
                                modifier = Modifier.padding(
                                    horizontal = 8.dp,
                                    vertical = 2.dp
                                ),
                                color = MaterialTheme.colorScheme
                                    .onTertiaryContainer,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
                QuestionTimer(
                    state = state,
                    onQuestionTimerTick = onQuestionTimerTick
                )
            }

            Text(
                text = when (state.currentQuestionFormat) {
                    QuestionFormat.FULL_INPUT -> "日本語から英単語"
                    QuestionFormat.FILL_IN_THE_BLANK -> "文章穴埋め入力"
                    QuestionFormat.MULTIPLE_CHOICE ->
                        if (state.currentQuestionUsesSentence) {
                            "文章4択"
                        } else {
                            "意味4択"
                        }
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            val sentenceQuestion = state.currentSentenceQuestion
            if (state.currentQuestionUsesSentence && sentenceQuestion != null) {
                Text(
                    text = sentenceQuestion.japaneseSentence,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = sentenceQuestion.sentence,
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Text(
                    text = displayedMeaning,
                    modifier = if (showFullMeaning) {
                        Modifier
                            .heightIn(max = 132.dp)
                            .verticalScroll(fullMeaningScrollState)
                    } else {
                        Modifier
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = if (showFullMeaning) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "${word.english.length}文字",
                    style = MaterialTheme.typography.labelMedium
                )
                if (!showFullMeaning && displayedPartOfSpeech != null) {
                    Text(
                        text = displayedPartOfSpeech,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                if (state.currentQuestionMistakeCount > 0) {
                    Text(
                        text = "ミス ${state.currentQuestionMistakeCount}回",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            if (!state.currentQuestionUsesSentence &&
                !showFullMeaning && word.quizHint != null
            ) {
                Text(
                    text = "補足：${word.quizHint}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (!state.currentQuestionUsesSentence &&
                !showFullMeaning && state.questionMeaningCollisionCount > 1
            ) {
                Text(
                    text = "同じ代表意味の単語が" +
                        "${state.questionMeaningCollisionCount}語あります。" +
                        "文字数や辞書全文も確認してください。",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (state.shouldShowQuestionHint ||
                (isMultipleChoice && state.currentQuestionMistakeCount >= 2)
            ) {
                Text(
                    text = "ヒント：最初の文字は「" +
                        word.english.firstOrNull()?.uppercaseChar() +
                        "」です",
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (state.shouldRevealAnswer ||
                (isMultipleChoice && state.currentQuestionMistakeCount >= 3)
            ) {
                Text(
                    text = "答え：${word.english}",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
                sentenceQuestion?.let { question ->
                    Text(
                        text = question.sentence.replace(
                            "___",
                            formatAnswerForSentence(
                                question.sentence,
                                question.answer
                            )
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "解説：${question.explanation}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (BuildConfig.DEBUG) {
                Text(
                    text = "デバッグ用の答え：${word.english}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (!state.currentQuestionUsesSentence && (
                    word.questionMeaning != word.japanese ||
                        word.japanese.length > 120
                    )
            ) {
                TextButton(onClick = onToggleFullMeaning) {
                    Text(
                        text = if (showFullMeaning) {
                            "折りたたむ"
                        } else if (word.questionMeaning != word.japanese) {
                            "辞書全文を表示"
                        } else {
                            "全文を表示"
                        }
                    )
                }
            }

            if (isMultipleChoice) {
                MultipleChoicePanel(
                    state = state,
                    onSelectChoice = onSelectChoice
                )
                val selectedChoice = state.multipleChoiceOptions.firstOrNull {
                    it.wordId == state.selectedChoiceWordId
                }
                Text(
                    text = if (selectedChoice == null) {
                        "答えを1回押して選択"
                    } else {
                        "強調された答えをもう一度押すと回答"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                AlphabetReelPanel(
                    typedAnswer = selectedChoice?.english.orEmpty(),
                    rotations = state.rotations,
                    animationRunId = state.reelAnimationRunId,
                    correctAnswer = word.english,
                    showCorrectStopFeedback =
                        state.phase == GamePhase.REELING
                )
            } else {
                TextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    value = state.typedAnswer,
                    onValueChange = onAnswerChanged,
                    enabled = state.phase == GamePhase.INPUT,
                    singleLine = true,
                    placeholder = { Text(text = "英単語を入力") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (state.canSubmit) {
                                onSubmitAnswer()
                            }
                        }
                    )
                )

                AlphabetReelPanel(
                    typedAnswer = state.typedAnswer,
                    rotations = state.rotations,
                    animationRunId = state.reelAnimationRunId,
                    correctAnswer = word.english,
                    showCorrectStopFeedback =
                        state.phase == GamePhase.REELING
                )
            }

            if (isMultipleChoice) {
                TextButton(
                    enabled = state.phase == GamePhase.INPUT,
                    onClick = onSkipQuestion
                ) {
                    Text(text = "スキップ +10")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        enabled = state.canSubmit,
                        onClick = onSubmitAnswer
                    ) {
                        Text(text = "回答する")
                    }
                    TextButton(
                        enabled = state.phase == GamePhase.INPUT,
                        onClick = onSkipQuestion
                    ) {
                        Text(text = "スキップ +10")
                    }
                }
            }

            InlineAnswerFeedback(state = state)
        }
    }
}

@Composable
private fun MultipleChoicePanel(
    state: GameUiState,
    onSelectChoice: (String) -> Unit
) {
    val sentence = state.currentSentenceQuestion?.sentence.orEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.multipleChoiceOptions.chunked(2).forEach { choices ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                choices.forEach { choice ->
                    val selected = choice.wordId == state.selectedChoiceWordId
                    val enabled = state.phase == GamePhase.INPUT &&
                        choice.wordId !in state.disabledChoiceWordIds
                    if (selected) {
                        Button(
                            modifier = Modifier.weight(1f),
                            enabled = enabled,
                            onClick = { onSelectChoice(choice.wordId) }
                        ) {
                            Text(
                                text = formatAnswerForSentence(
                                    sentence,
                                    choice.english
                                ),
                                fontWeight = FontWeight.Black
                            )
                        }
                    } else {
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            enabled = enabled,
                            onClick = { onSelectChoice(choice.wordId) }
                        ) {
                            Text(
                                text = formatAnswerForSentence(
                                    sentence,
                                    choice.english
                                )
                            )
                        }
                    }
                }
                if (choices.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DebugGameControls(
    state: GameUiState,
    onSubmitDebugAnswer: (Boolean) -> Unit,
    onCycleDebugFish: () -> Unit,
    onCatchDebugFish: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "問題ID：" + (
                state.currentSentenceQuestion?.questionId
                    ?: state.word?.wordId
                    ?: "不明"
                ),
            style = MaterialTheme.typography.labelSmall
        )
        if (state.rotations.isNotEmpty()) {
            Text(
                text = state.rotations.joinToString(" → ") +
                    "\n合計：${state.rotations.sum()}回転"
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = { onSubmitDebugAnswer(true) }
            ) {
                Text(text = "デバッグ：正解")
            }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = { onSubmitDebugAnswer(false) }
            ) {
                Text(text = "デバッグ：不正解")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = onCycleDebugFish
            ) {
                Text(text = "デバッグ：魚変更")
            }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = onCatchDebugFish
            ) {
                Text(text = "デバッグ：即釣る")
            }
        }
    }
}

@Composable
private fun InlineAnswerFeedback(state: GameUiState) {
    if (state.isDuplicateWrongAnswer) {
        Text(text = "同じ不正解はカウントされません")
    } else if (state.isCorrect == true) {
        Text(text = "正解！ リールを巻いた！")
        if (state.lastTimeBonus > 0) {
            Text(text = "時間ボーナス：+${state.lastTimeBonus}")
        }
    } else if (state.isCorrect == false) {
        Text(text = "不正解")
    }
}

@Composable
private fun GameResultPanel(
    state: GameUiState,
    fishState: FishState
) {
    val wasCaught = state.gameResult == GameResult.CAUGHT
    val studyComplete = state.gameResult == GameResult.STUDY_COMPLETE
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (wasCaught || studyComplete) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (wasCaught || studyComplete) "🎉" else "💥",
                fontSize = 38.sp
            )
            Text(
                text = if (wasCaught) {
                    if (
                        state.selectedPoint?.lordFishName ==
                        fishState.fish.name
                    ) {
                        "主・${fishState.fish.name}を釣り上げた！"
                    } else {
                        "${fishState.fish.name}を釣り上げた！"
                    }
                } else if (studyComplete) {
                    "${state.studyMode.displayName}が完了！"
                } else {
                    "糸が切れた！"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            if (wasCaught) {
                if (state.isNewFishDiscovery || state.isNewLargestSize) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (state.isNewFishDiscovery) {
                            ResultBadge(
                                text = "NEW！ 初発見",
                                color = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        if (state.isNewLargestSize) {
                            ResultBadge(
                                text = "自己ベスト更新",
                                color = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ResultValue(
                        label = "サイズ",
                        value = "%.1f cm".format(fishState.sizeCm)
                    )
                    ResultValue(
                        label = "レア度",
                        value = fishState.fish.rarity.displayText
                    )
                    ResultValue(
                        label = "スコア",
                        value = "${state.score} pt"
                    )
                }

                if (state.lastTimeBonus > 0) {
                    Text(
                        text = "時間ボーナス  +${state.lastTimeBonus}",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                BattleLearningResult(state = state)

                if (fishState.fish.description.isNotBlank()) {
                    Text(
                        text = fishState.fish.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Text(
                    text = "捕獲日時：" + formatGameDate(
                        state.fishCollectionRecords[fishState.fish.name]
                            ?.lastCaughtAtEpochMillis
                            ?: 0L
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "渓流図鑑 ${state.availableCaughtSpeciesCount}" +
                        " / ${currentlyAvailableFishes.size}種類",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            } else if (!studyComplete) {
                Text(
                    text = "${fishState.fish.name}に逃げられた…",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "テンションが100%に達しました",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "時間をかけすぎたり、不正解やスキップが" +
                    "続いたりすると糸が切れます。",
                    style = MaterialTheme.typography.bodyMedium
                )

                BattleLearningResult(state = state)
            } else {
                Text(
                    text = "復習対象の問題をすべて終えました。",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = (
                        "魚はまだ %.1f m先です。" +
                            "今回は捕獲しておらず、図鑑にも登録していません。"
                        ).format(fishState.currentDistance),
                    style = MaterialTheme.typography.bodyMedium
                )
                BattleLearningResult(state = state)
            }
        }
    }
}

@Composable
private fun BattleLearningResult(state: GameUiState) {
    var showQuestionDetails by remember(state.battleStartedAtEpochMillis) {
        mutableStateOf(false)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "今回の学習結果",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ResultValue(
                    label = "初回正答率",
                    value = "${state.battleInitialAccuracyPercent}%"
                )
                ResultValue(
                    label = "問題数",
                    value = "${state.battleQuestionResults.size}問"
                )
                ResultValue(
                    label = "釣り時間",
                    value = formatElapsedTime(state.battleDurationMillis)
                )
            }
            Text(
                text = "一発正解 ${state.battleInitialCorrectCount}問  ・  " +
                    "ミス後正解 ${state.battleCorrectedAfterMistakeCount}問  ・  " +
                    "スキップ ${state.battleSkippedCount}問",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold
            )
            if (state.battleLineBrokenCount > 0) {
                Text(
                    text = "糸切れ ${state.battleLineBrokenCount}問",
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }

            if (state.battleQuestionResults.isNotEmpty()) {
                TextButton(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    onClick = {
                        showQuestionDetails = !showQuestionDetails
                    }
                ) {
                    Text(
                        text = if (showQuestionDetails) {
                            "問題別の結果を閉じる"
                        } else {
                            "問題別の結果を見る"
                        }
                    )
                }
            }

            if (showQuestionDetails) {
                state.battleQuestionResults.forEachIndexed { index, result ->
                    BattleQuestionResultRow(
                        number = index + 1,
                        result = result
                    )
                }
            }
        }
    }
}

@Composable
private fun BattleQuestionResultRow(
    number: Int,
    result: BattleQuestionResult
) {
    val outcomeText = when (result.outcome) {
        BattleQuestionOutcome.FIRST_TRY_CORRECT -> "一発正解"
        BattleQuestionOutcome.CORRECTED_AFTER_MISTAKE -> "ミス後正解"
        BattleQuestionOutcome.SKIPPED -> "スキップ"
        BattleQuestionOutcome.LINE_BROKEN -> "糸切れ"
    }
    val outcomeColor = when (result.outcome) {
        BattleQuestionOutcome.FIRST_TRY_CORRECT -> Color(0xFF2E7D32)
        BattleQuestionOutcome.CORRECTED_AFTER_MISTAKE -> Color(0xFFF57C00)
        BattleQuestionOutcome.SKIPPED -> MaterialTheme.colorScheme.error
        BattleQuestionOutcome.LINE_BROKEN -> MaterialTheme.colorScheme.error
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${number}問目・${questionFormatLabel(result.questionFormat)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = outcomeText,
                    color = outcomeColor,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black
                )
            }
            result.japaneseText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = result.questionText,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "最初の回答：${result.firstAnswer ?: "（未回答）"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "正解：${result.correctAnswer}",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = when {
                    result.outcome == BattleQuestionOutcome.SKIPPED &&
                        result.firstAnswer == null ->
                        "スキップまで ${formatElapsedTime(result.totalTimeMillis)}"
                    result.outcome == BattleQuestionOutcome.SKIPPED ->
                        "初回回答 ${formatElapsedTime(result.firstResponseTimeMillis)}" +
                            "  ・  スキップ ${formatElapsedTime(result.totalTimeMillis)}"
                    result.outcome == BattleQuestionOutcome.LINE_BROKEN &&
                        result.firstAnswer == null ->
                        "糸切れまで ${formatElapsedTime(result.totalTimeMillis)}"
                    result.outcome == BattleQuestionOutcome.LINE_BROKEN ->
                        "初回回答 ${formatElapsedTime(result.firstResponseTimeMillis)}" +
                            "  ・  糸切れ ${formatElapsedTime(result.totalTimeMillis)}"
                    else ->
                        "初回回答 ${formatElapsedTime(result.firstResponseTimeMillis)}" +
                            "  ・  完了 ${formatElapsedTime(result.totalTimeMillis)}"
                },
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

private fun questionFormatLabel(format: QuestionFormat): String = when (format) {
    QuestionFormat.FULL_INPUT -> "単語入力"
    QuestionFormat.FILL_IN_THE_BLANK -> "文章入力"
    QuestionFormat.MULTIPLE_CHOICE -> "4択"
}

internal fun formatElapsedTime(durationMillis: Long): String {
    val safeMillis = durationMillis.coerceAtLeast(0L)
    val totalSeconds = safeMillis / 1_000L
    return if (totalSeconds >= 60L) {
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        "${minutes}分${seconds}秒"
    } else {
        val tenths = (safeMillis % 1_000L) / 100L
        "${totalSeconds}.${tenths}秒"
    }
}

@Composable
private fun ResultBadge(
    text: String,
    color: Color,
    contentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color,
        contentColor = contentColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 5.dp
            )
        )
    }
}

@Composable
private fun ResultValue(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatGameDate(epochMillis: Long): String {
    if (epochMillis <= 0L) return "記録なし"
    return java.text.SimpleDateFormat(
        "yyyy/MM/dd HH:mm",
        java.util.Locale.JAPAN
    ).format(java.util.Date(epochMillis))
}
