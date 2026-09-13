package com.example.fishinggame

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun FishingGameApp(gameViewModel: GameViewModel) {
    val state by gameViewModel.uiState
    val word = state.word
    val fishState = state.currentFish

    val supportsBackNavigation = state.phase in setOf(
        GamePhase.COLLECTION,
        GamePhase.LICENSES,
        GamePhase.TARGET_LEVEL_SELECTION,
        GamePhase.MAP_SELECTION,
        GamePhase.POINT_SELECTION,
        GamePhase.SETUP,
        GamePhase.HOOK
    )
    BackHandler(enabled = supportsBackNavigation) {
        gameViewModel.navigateBack()
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        bottomBar = {
            FishingGameBottomBar(
                state = state,
                onCloseCollection = gameViewModel::closeFishCollection,
                onStartNextFish = gameViewModel::startNextFish,
                onNavigateBack = gameViewModel::navigateBack,
                onReturnToLevelSelection =
                    gameViewModel::returnToLevelSelection
            )
        }
    ) { contentPadding ->
        val baseScreenModifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)

        val scrollableScreenModifier = baseScreenModifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)

        val gameScreenModifier = baseScreenModifier
            .padding(
                horizontal = 12.dp,
                vertical = 8.dp
            )

        when {
            state.isLoading -> {
                MessageScreen(
                    modifier = scrollableScreenModifier,
                    message = "単語データを読み込んでいます…"
                )
            }

            state.errorMessage != null -> {
                MessageScreen(
                    modifier = scrollableScreenModifier,
                    message = "単語データの読み込みに失敗しました\n" +
                        state.errorMessage
                )
            }

            state.phase == GamePhase.TITLE -> {
                TitleScreen(
                    modifier = scrollableScreenModifier,
                    state = state,
                    onStart = gameViewModel::startGame,
                    onOpenCollection =
                        gameViewModel::openFishCollection,
                    onOpenLicenses = gameViewModel::openLicenses
                )
            }

            state.phase == GamePhase.LICENSES -> {
                LicensesScreen(
                    modifier = scrollableScreenModifier,
                    onClose = gameViewModel::navigateBack
                )
            }

            state.phase == GamePhase.TARGET_LEVEL_SELECTION -> {
                TargetLevelSelectionScreen(
                    modifier = scrollableScreenModifier,
                    state = state,
                    onSelectTargetLevel =
                        gameViewModel::selectTargetEikenLevel
                )
            }

            state.phase == GamePhase.MAP_SELECTION -> {
                MapSelectionScreen(
                    modifier = scrollableScreenModifier,
                    onSelectMap = gameViewModel::selectFishingMap
                )
            }

            state.phase == GamePhase.POINT_SELECTION -> {
                val map = state.selectedMap
                if (map == null) {
                    MessageScreen(
                        modifier = scrollableScreenModifier,
                        message = "マップを準備できませんでした"
                    )
                } else {
                    FishingPointScreen(
                        modifier = scrollableScreenModifier,
                        map = map,
                        fishCollectionRecords =
                            state.fishCollectionRecords,
                        onSelectPoint =
                            gameViewModel::selectFishingPoint
                    )
                }
            }

            state.phase == GamePhase.SETUP -> {
                LevelSelectionScreen(
                    modifier = scrollableScreenModifier,
                    state = state,
                    onStartTargetLevel =
                        gameViewModel::startTargetLevel,
                    onChangeTargetLevel =
                        gameViewModel::openTargetLevelSelection,
                    onSelectReviewMode =
                        gameViewModel::selectReviewMode,
                    onSelectWeakMode =
                        gameViewModel::selectWeakMode,
                    onSelectQuestionMode =
                        gameViewModel::selectQuestionMode,
                    onSelectDebugSentenceQuestions =
                        gameViewModel::selectDebugSentenceQuestions,
                    onOpenFishCollection =
                        gameViewModel::openFishCollection
                )
            }

            state.phase == GamePhase.COLLECTION -> {
                FishCollectionScreen(
                    modifier = scrollableScreenModifier,
                    state = state,
                    onResetCollection =
                        gameViewModel::resetFishCollection
                )
            }

            state.phase == GamePhase.HOOK -> {
                HookScreen(
                    modifier = scrollableScreenModifier,
                    state = state,
                    onStartBattle = gameViewModel::startReelBattle
                )
            }

            word != null && fishState != null -> {
                FishingGameScreen(
                    modifier = gameScreenModifier,
                    state = state,
                    word = word,
                    fishState = fishState,
                    onAnswerChanged = gameViewModel::onAnswerChanged,
                    onSubmitAnswer = gameViewModel::submitAnswer,
                    onSelectChoice = gameViewModel::selectChoice,
                    onSkipQuestion = gameViewModel::skipCurrentQuestion,
                    onQuestionTimerTick =
                        gameViewModel::onQuestionTimerTick,
                    onSubmitDebugAnswer =
                        gameViewModel::submitDebugAnswer,
                    onCycleDebugFish = gameViewModel::cycleDebugFish,
                    onCatchDebugFish =
                        gameViewModel::catchCurrentFishForDebug
                )
            }

            else -> {
                MessageScreen(
                    modifier = scrollableScreenModifier,
                    message = "ゲームデータを準備できませんでした"
                )
            }
        }
    }
}

@Composable
private fun FishingGameBottomBar(
    state: GameUiState,
    onCloseCollection: () -> Unit,
    onStartNextFish: () -> Unit,
    onNavigateBack: () -> Unit,
    onReturnToLevelSelection: () -> Unit
) {
    if (state.isLoading || state.errorMessage != null) return
    val density = LocalDensity.current
    val isKeyboardVisible = WindowInsets.ime.getBottom(density) > 0

    when (state.phase) {
        GamePhase.COLLECTION -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onCloseCollection
                ) {
                    Text(text = "戻る")
                }
            }
        }

        GamePhase.TARGET_LEVEL_SELECTION,
        GamePhase.MAP_SELECTION,
        GamePhase.POINT_SELECTION,
        GamePhase.SETUP,
        GamePhase.HOOK -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNavigateBack
                ) {
                    Text(
                        text = when (state.phase) {
                            GamePhase.TARGET_LEVEL_SELECTION ->
                                if (state.selectedPointId == null) {
                                    "タイトルへ戻る"
                                } else {
                                    "問題設定へ戻る"
                                }
                            GamePhase.SETUP -> "釣りポイントへ戻る"
                            GamePhase.HOOK -> "問題設定へ戻る"
                            else -> "戻る"
                        }
                    )
                }
            }
        }

        GamePhase.INPUT -> {
            if (shouldShowInputBottomBar(isKeyboardVisible)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onReturnToLevelSelection
                    ) {
                        Text(text = "問題設定へ戻る")
                    }
                }
            }
        }

        GamePhase.RESULT -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onStartNextFish
                ) {
                    Text(
                        text = if (state.isStudySessionFinished) {
                            "学習結果を閉じる"
                        } else {
                            "次の魚へ"
                        }
                    )
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onReturnToLevelSelection
                ) {
                    Text(text = "問題設定へ戻る")
                }
            }
        }

        GamePhase.TITLE,
        GamePhase.REELING,
        GamePhase.LICENSES -> Unit
    }
}

internal fun shouldShowInputBottomBar(isKeyboardVisible: Boolean): Boolean =
    !isKeyboardVisible

@Composable
private fun MessageScreen(
    modifier: Modifier,
    message: String
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = message)
    }
}
