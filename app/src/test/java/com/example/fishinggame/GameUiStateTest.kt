package com.example.fishinggame

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameUiStateTest {
    private val word = Word(
        english = "fish",
        japanese = "魚の辞書本文",
        ngslRank = 1,
        level = 1,
        schoolGrade = null,
        eikenLevel = null,
        partOfSpeech = null,
        sfi = 1.0,
        frequencyPerMillion = 1,
        translationSource = "test"
    )

    @Test
    fun canSubmit_requiresInputPhaseAndNonBlankAnswer() {
        assertFalse(GameUiState().canSubmit)
        assertFalse(
            GameUiState(
                phase = GamePhase.INPUT,
                typedAnswer = ""
            ).canSubmit
        )
        assertTrue(
            GameUiState(
                phase = GamePhase.INPUT,
                typedAnswer = "fish"
            ).canSubmit
        )
        assertFalse(
            GameUiState(
                phase = GamePhase.RESULT,
                typedAnswer = "fish"
            ).canSubmit
        )
        assertFalse(
            GameUiState(
                phase = GamePhase.REELING,
                typedAnswer = "fish"
            ).canSubmit
        )
        assertTrue(
            GameUiState(
                phase = GamePhase.INPUT,
                currentQuestionFormat = QuestionFormat.MULTIPLE_CHOICE,
                selectedChoiceWordId = "ngsl_1_2:0001"
            ).canSubmit
        )
        assertFalse(
            GameUiState(
                phase = GamePhase.INPUT,
                currentQuestionFormat = QuestionFormat.MULTIPLE_CHOICE
            ).canSubmit
        )
    }

    @Test
    fun mistakeThresholds_showHintAtThreeAndAnswerAtFive() {
        val twoMistakes = GameUiState(
            wrongAnswersForCurrentQuestion = setOf("a", "b")
        )
        val threeMistakes = GameUiState(
            wrongAnswersForCurrentQuestion = setOf("a", "b", "c")
        )
        val fiveMistakes = GameUiState(
            wrongAnswersForCurrentQuestion =
                setOf("a", "b", "c", "d", "e")
        )

        assertFalse(twoMistakes.shouldShowQuestionHint)
        assertTrue(threeMistakes.shouldShowQuestionHint)
        assertFalse(threeMistakes.shouldRevealAnswer)
        assertTrue(fiveMistakes.shouldRevealAnswer)
    }

    @Test
    fun lineTensionProgress_isClampedBetweenZeroAndOne() {
        assertTrue(GameUiState(lineTension = -10).lineTensionProgress == 0f)
        assertTrue(GameUiState(lineTension = 50).lineTensionProgress == 0.5f)
        assertTrue(GameUiState(lineTension = 150).lineTensionProgress == 1f)
    }

    @Test
    fun questionMeaning_prefersQuizMeaningAndFallsBackToDictionary() {
        assertTrue(word.questionMeaning == "魚の辞書本文")
        assertTrue(
            word.copy(quizMeaning = "魚").questionMeaning == "魚"
        )
    }

    @Test
    fun extractPrimaryMeaning_prefersEmphasizedDictionaryMeaning() {
        val result = extractPrimaryMeaning(
            "〈C〉《一般に》『仕事』,作業 / 別の意味"
        )

        assertTrue(result == "仕事")
    }

    @Test
    fun extractPrimaryMeaning_usesCleanedFirstSectionWithoutEmphasis() {
        val result = extractPrimaryMeaning(
            "《前置詞》～の中に、～で / 別の意味"
        )

        assertTrue(result == "～の中に、～で")
    }

    @Test
    fun extractPrimaryMeaning_skipsDictionaryGrammarMarkers() {
        val result = extractPrimaryMeaning(
            "〈U〉投資すること《+『of』+『名』》 / 『投資金』"
        )

        assertTrue(result == "投資金")
    }

    @Test
    fun extractPrimaryMeaning_searchesLaterSectionsAfterLabelOnlySection() {
        val result = extractPrimaryMeaning(
            "《疑問代名詞》 / 《情報を求めて》『何』,どんなもの"
        )

        assertTrue(result == "何")
    }

    @Test
    fun questionMeaningCollisionCount_countsMatchingPrompts() {
        val sameMeaningWord = word.copy(
            english = "fishing",
            japanese = "魚の辞書本文"
        )
        val state = GameUiState(
            words = listOf(word, sameMeaningWord),
            currentQuestionIndex = 0
        )

        assertTrue(state.questionMeaningCollisionCount == 2)
    }

    @Test
    fun questionMeaningCollisionCount_distinguishesPartOfSpeech() {
        val sameMeaningDifferentPartOfSpeech = word.copy(
            english = "fish",
            japanese = "魚の辞書本文",
            quizPartOfSpeech = "動詞"
        )
        val state = GameUiState(
            words = listOf(word, sameMeaningDifferentPartOfSpeech),
            currentQuestionIndex = 0
        )

        assertTrue(state.questionMeaningCollisionCount == 1)
    }

    @Test
    fun answerStatistics_calculateTotalAndAccuracy() {
        val state = GameUiState(
            correctAnswerCount = 3,
            incorrectAnswerCount = 1
        )

        assertTrue(state.totalAnswerCount == 4)
        assertTrue(state.accuracyPercent == 75)
        assertTrue(GameUiState().accuracyPercent == 0)
    }

    @Test
    fun battleResults_useFirstAnswersForAccuracyAndDuration() {
        fun result(outcome: BattleQuestionOutcome) = BattleQuestionResult(
            wordId = word.wordId,
            questionText = word.questionMeaning,
            correctAnswer = word.english,
            questionFormat = QuestionFormat.FULL_INPUT,
            outcome = outcome,
            firstResponseTimeMillis = 1_000L,
            totalTimeMillis = 2_000L
        )
        val state = GameUiState(
            battleStartedAtEpochMillis = 10_000L,
            battleFinishedAtEpochMillis = 80_000L,
            battleQuestionResults = listOf(
                result(BattleQuestionOutcome.FIRST_TRY_CORRECT),
                result(BattleQuestionOutcome.CORRECTED_AFTER_MISTAKE),
                result(BattleQuestionOutcome.SKIPPED),
                result(BattleQuestionOutcome.LINE_BROKEN)
            )
        )

        assertTrue(state.battleInitialCorrectCount == 1)
        assertTrue(state.battleCorrectedAfterMistakeCount == 1)
        assertTrue(state.battleSkippedCount == 1)
        assertTrue(state.battleLineBrokenCount == 1)
        assertTrue(state.battleInitialAccuracyPercent == 25)
        assertTrue(state.battleDurationMillis == 70_000L)
    }

    @Test
    fun elapsedTime_formatsShortAndMinuteDurations() {
        assertTrue(formatElapsedTime(4_560L) == "4.5秒")
        assertTrue(formatElapsedTime(70_000L) == "1分10秒")
    }

    @Test
    fun learningDashboard_calculatesMasteryWeaknessAndSavedAccuracy() {
        val secondWord = word.copy(english = "boat", sourceRank = 2)
        val mastered = LearningState(
            learningItemId = word.learningItemId(),
            wordId = word.wordId,
            direction = LearningDirection.JAPANESE_TO_ENGLISH,
            status = LearningStatus.MASTERED,
            independentRecallCount = 3,
            failedRecallCount = 1,
            recentOutcomes = listOf(
                RecallOutcome.INDEPENDENT,
                RecallOutcome.FAILED,
                RecallOutcome.FAILED
            )
        )
        val state = GameUiState(
            allWords = listOf(word, secondWord),
            learningStates = mapOf(mastered.learningItemId to mastered)
        )

        assertTrue(state.masteredLearningItemCount == 1)
        assertTrue(state.masteryPercent == 50)
        assertTrue(state.savedInitialRecallAccuracyPercent == 75)
        assertTrue(mastered.learningItemId in state.weakLearningItemIds)
    }

    @Test
    fun firstAppearance_requiresNoSavedAttempts() {
        assertTrue(isFirstAppearance(word, emptyMap()))

        val answered = LearningState(
            learningItemId = word.learningItemId(),
            wordId = word.wordId,
            direction = LearningDirection.JAPANESE_TO_ENGLISH,
            independentRecallCount = 1
        )
        assertFalse(
            isFirstAppearance(
                word,
                mapOf(answered.learningItemId to answered)
            )
        )
    }

    @Test
    fun studySessionFinishesOnlyWhenNonNormalQueueIsEmpty() {
        assertFalse(GameUiState().isStudySessionFinished)
        assertTrue(
            GameUiState(
                studyMode = StudyMode.REVIEW,
                remainingStudyItemIds = emptySet()
            ).isStudySessionFinished
        )
    }

    @Test
    fun sentenceReviewCountExcludesWordsWithoutSentenceQuestions() {
        val withSentence = word.copy(ngslRank = 1, sourceRank = 1)
        val withoutSentence = word.copy(
            english = "without",
            ngslRank = 2,
            sourceRank = 2
        )
        val reviewIds = setOf(
            withSentence.learningItemId(),
            withoutSentence.learningItemId()
        )
        val state = GameUiState(
            allWords = listOf(withSentence, withoutSentence),
            sentenceQuestionsByWordId = mapOf(
                withSentence.wordId to SentenceQuestion(
                    questionId = "with-sentence",
                    wordId = withSentence.wordId,
                    sentence = "We ___ home.",
                    japaneseSentence = "私たちは家へ行きます。",
                    answer = withSentence.english,
                    distractorWordIds = emptyList(),
                    explanation = "テスト。",
                    source = PROJECT_AUTHORED_QUESTION_SOURCE,
                    license = PROJECT_ORIGINAL_QUESTION_LICENSE,
                    sourceUrl = null
                )
            ),
            questionMode = QuestionMode.SENTENCE_INPUT,
            reviewLearningItemIds = reviewIds
        )

        assertEquals(1, state.availableReviewWordCount)
    }

    @Test
    fun fishCollection_countsCaughtSpeciesAndCompletion() {
        val partial = GameUiState(
            fishCollectionRecords = mapOf(
                "アジ" to FishCollectionRecord(caughtCount = 2)
            )
        )
        val complete = GameUiState(
            fishCollectionRecords = fishes.associate {
                it.name to FishCollectionRecord(caughtCount = 1)
            }
        )

        assertTrue(partial.caughtSpeciesCount == 1)
        assertFalse(partial.isFishCollectionComplete)
        assertTrue(complete.isFishCollectionComplete)
    }

    @Test
    fun fishCollection_countsCaughtSpeciesByRarity() {
        val commonFish = fishes.first {
            it.rarity == FishRarity.COMMON
        }
        val rareFish = fishes.first {
            it.rarity == FishRarity.RARE
        }
        val state = GameUiState(
            fishCollectionRecords = mapOf(
                commonFish.name to FishCollectionRecord(caughtCount = 1),
                rareFish.name to FishCollectionRecord(caughtCount = 1)
            )
        )

        assertTrue(state.caughtSpeciesCountFor(FishRarity.COMMON) == 1)
        assertTrue(state.caughtSpeciesCountFor(FishRarity.RARE) == 1)
        assertTrue(state.caughtSpeciesCountFor(FishRarity.EPIC) == 0)
    }

    @Test
    fun availableCollection_countsOnlyFishInImplementedPoints() {
        val availableFishes = currentlyAvailableFishes
        val unavailableFish = fishes.first { fish ->
            availableFishes.none { it.name == fish.name }
        }
        val state = GameUiState(
            fishCollectionRecords = availableFishes.associate { fish ->
                fish.name to FishCollectionRecord(caughtCount = 1)
            } + (unavailableFish.name to FishCollectionRecord(caughtCount = 1))
        )

        assertTrue(state.caughtSpeciesCount == availableFishes.size + 1)
        assertTrue(state.availableCaughtSpeciesCount == availableFishes.size)
        assertTrue(state.isAvailableFishCollectionComplete)
    }
}
