package com.example.fishinggame

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.example.fishinggame.ui.theme.FishinggameTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FishingGameResponsiveUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val word = Word(
        english = "bee",
        japanese = "はち",
        ngslRank = null,
        level = 1,
        schoolGrade = SchoolGrade.ELEMENTARY_5,
        eikenLevel = EikenLevel.GRADE_5,
        partOfSpeech = "名詞",
        sfi = null,
        frequencyPerMillion = null,
        translationSource = "test",
        wordList = WordList.OPEN_VOCAB_EXTENDED_V1,
        sourceRank = 2958,
        quizMeaning = "はち",
        quizPartOfSpeech = "名詞",
        quizSource = "test"
    )
    private val fishState = FishState(
        fish = fishes.first(),
        startHp = 50,
        currentHp = 50,
        startDistance = 5f,
        currentDistance = 5f,
        sizeCm = 12f
    )
    private val state = GameUiState(
        allWords = listOf(word),
        words = listOf(word),
        phase = GamePhase.INPUT,
        currentFish = fishState,
        currentLearningAttemptId = "responsive-test",
        questionStartedAtEpochMillis = System.currentTimeMillis()
    )

    @Test
    fun debugControlsRemainReachableOnSmallScreen() {
        setSmallInputScreen()

        composeRule.onNodeWithText("デバッグ操作")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithText("デバッグ：即釣る")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun answerFieldAcceptsKeyboardInputOnSmallScreen() {
        var enteredText = ""
        setSmallInputScreen(onAnswerChanged = { enteredText = it })

        composeRule.onNode(hasSetTextAction())
            .performScrollTo()
            .assertIsDisplayed()
            .performTextInput("bee")

        assertEquals("bee", enteredText)
    }

    private fun setSmallInputScreen(
        onAnswerChanged: (String) -> Unit = {}
    ) {
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                FishinggameTheme(dynamicColor = false) {
                    Box(Modifier.requiredSize(width = 320.dp, height = 480.dp)) {
                        FishingGameScreen(
                            modifier = Modifier.matchParentSize(),
                            state = state,
                            word = word,
                            fishState = fishState,
                            onAnswerChanged = onAnswerChanged,
                            onSubmitAnswer = {},
                            onSelectChoice = {},
                            onSkipQuestion = {},
                            onQuestionTimerTick = { _, _ -> },
                            onSubmitDebugAnswer = {},
                            onCycleDebugFish = {},
                            onCatchDebugFish = {}
                        )
                    }
                }
            }
        }
    }
}
