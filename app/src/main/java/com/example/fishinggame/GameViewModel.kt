package com.example.fishinggame

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

private data class LearningDashboardData(
    val states: List<LearningState>,
    val review: ReviewCandidateSets,
    val assistedCompletionCount: Int
)

private data class InitialAppData(
    val words: Result<List<Word>>,
    val sentenceQuestions: Result<List<SentenceQuestion>>,
    val fishCollection: Map<String, FishCollectionRecord>,
    val learning: Result<LearningDashboardData>
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val fishCollectionStorage =
        FishCollectionStorage(application)
    private val learningTimeProvider: LearningTimeProvider =
        SystemLearningTimeProvider
    private val learningRepository = LearningRepository(
        database = LearningDatabase.getInstance(application),
        scheduler = LearningScheduler(
            LearningAlgorithmConfig(
                assistedRecallPolicy =
                    AssistedRecallPolicy.CLEAR_SHORT_TERM_REVIEW,
                shortTermMinimumOtherItems = 4,
                shortTermFallbackDelayMillis = 10L * 60L * 1000L
            )
        )
    )
    private val _uiState = mutableStateOf(GameUiState(isLoading = true))
    val uiState: State<GameUiState> = _uiState

    init {
        viewModelScope.launch {
            val initialData =
                withContext(Dispatchers.IO) {
                    val learningResult = runCatching {
                        val states = learningRepository.getAllStates()
                        val review = learningRepository.getReviewCandidateSets(
                            learningTimeProvider.nowEpochMillis()
                        )
                        LearningDashboardData(
                            states = states,
                            review = review,
                            assistedCompletionCount = learningRepository
                                .countAssistedCompletions()
                        )
                    }
                    val wordsResult = loadWords(application)
                    val questionResult = wordsResult.fold(
                        onSuccess = { words ->
                            loadSentenceQuestions(application, words)
                        },
                        onFailure = { Result.failure(it) }
                    )
                    InitialAppData(
                        words = wordsResult,
                        sentenceQuestions = questionResult,
                        fishCollection =
                            fishCollectionStorage.loadFishCollection(),
                        learning = learningResult
                    )
                }

            _uiState.value = initialData.words.fold(
                onSuccess = { words ->
                    val sentenceQuestions = initialData.sentenceQuestions
                        .getOrElse { error ->
                            _uiState.value = GameUiState(
                                fishCollectionRecords =
                                    initialData.fishCollection,
                                errorMessage = error.message
                                    ?: "文章問題を読み込めませんでした"
                            )
                            return@fold _uiState.value
                        }
                    val learningData = initialData.learning.getOrNull()
                    GameUiState(
                        allWords = words,
                        sentenceQuestionsByWordId = sentenceQuestions
                            .associateBy { it.wordId },
                        phase = GamePhase.TITLE,
                        fishCollectionRecords = initialData.fishCollection,
                        learningStates = learningData
                            ?.states
                            .orEmpty()
                            .associateBy { it.learningItemId },
                        reviewLearningItemIds = learningData
                            ?.review
                            ?.all
                            .orEmpty()
                            .mapTo(mutableSetOf()) { it.learningItemId },
                        shortTermReviewLearningItemIds = learningData
                            ?.review
                            ?.shortTerm
                            .orEmpty()
                            .mapTo(mutableSetOf()) { it.learningItemId },
                        savedAssistedCompletionCount = learningData
                            ?.assistedCompletionCount
                            ?: 0,
                        learningHistoryError = initialData.learning
                            .exceptionOrNull()
                            ?.message
                    )
                },
                onFailure = { error ->
                    GameUiState(
                        fishCollectionRecords = initialData.fishCollection,
                        errorMessage = error.message
                            ?: "単語データを読み込めませんでした"
                    )
                }
            )
        }
    }

    private fun saveFishCollection(
        records: Map<String, FishCollectionRecord>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            fishCollectionStorage.saveFishCollection(records)
        }
    }

    private fun refreshLearningDashboard() {
        _uiState.value = _uiState.value.copy(
            isLearningHistoryLoading = true
        )
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                val states = learningRepository.getAllStates()
                val review = learningRepository.getReviewCandidateSets(
                    learningTimeProvider.nowEpochMillis()
                )
                LearningDashboardData(
                    states = states,
                    review = review,
                    assistedCompletionCount = learningRepository
                        .countAssistedCompletions()
                )
            }
            withContext(Dispatchers.Main) {
                val current = _uiState.value
                _uiState.value = result.fold(
                    onSuccess = { dashboard ->
                        current.copy(
                            learningStates = dashboard.states.associateBy {
                                it.learningItemId
                            },
                            reviewLearningItemIds = dashboard.review.all.mapTo(
                                mutableSetOf()
                            ) { it.learningItemId },
                            shortTermReviewLearningItemIds =
                                dashboard.review.shortTerm.mapTo(mutableSetOf()) {
                                    it.learningItemId
                                },
                            savedAssistedCompletionCount =
                                dashboard.assistedCompletionCount,
                            isLearningHistoryLoading = false,
                            learningHistoryError = null
                        )
                    },
                    onFailure = { error ->
                        current.copy(
                            isLearningHistoryLoading = false,
                            learningHistoryError = error.message
                                ?: "学習履歴を読み込めませんでした"
                        )
                    }
                )
            }
        }
    }

    private fun recordLearningSubmission(
        state: GameUiState,
        word: Word,
        wasCorrect: Boolean,
        questionFormat: QuestionFormat = state.currentQuestionFormat
    ): GameUiState {
        val occurredAt = learningTimeProvider.nowEpochMillis()
        val attemptId = state.currentLearningAttemptId
            ?: UUID.randomUUID().toString()

        if (state.isCurrentLearningOutcomeRecorded) {
            viewModelScope.launch(Dispatchers.IO) {
                val result = runCatching {
                    learningRepository.recordAdditionalSubmission(
                        attemptId = attemptId,
                        wasCorrect = wasCorrect,
                        usedHint = state.shouldShowQuestionHint ||
                            state.shouldRevealAnswer,
                        occurredAtEpochMillis = occurredAt
                    )
                    learningRepository.countAssistedCompletions()
                }
                withContext(Dispatchers.Main) {
                    _uiState.value = result.fold(
                        onSuccess = { count ->
                            _uiState.value.copy(
                                savedAssistedCompletionCount = count,
                                learningHistoryError = null
                            )
                        },
                        onFailure = { error ->
                            _uiState.value.copy(
                                learningHistoryError = error.message
                                    ?: "追加回答を保存できませんでした"
                            )
                        }
                    )
                }
            }
            return state
        }

        val outcome = recallOutcomeForSubmission(wasCorrect, questionFormat)
        val attempt = RecallAttempt(
            attemptId = attemptId,
            learningItemId = word.learningItemId(),
            wordId = word.wordId,
            direction = LearningDirection.JAPANESE_TO_ENGLISH,
            questionFormat = questionFormat,
            outcome = outcome,
            occurredAtEpochMillis = occurredAt,
            responseTimeMillis = state.questionStartedAtEpochMillis
                .takeIf { it > 0L }
                ?.let { (occurredAt - it).coerceAtLeast(0L) },
            usedHint = false,
            gameMode = state.studyMode.name.lowercase(),
            courseId = state.learningCourseId(),
            reviewPurpose = when {
                state.studyMode != StudyMode.REVIEW -> ReviewPurpose.NONE
                word.learningItemId() in
                    state.shortTermReviewLearningItemIds ->
                    ReviewPurpose.SHORT_TERM
                else -> ReviewPurpose.FORMAL
            }
        )
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                val updated = learningRepository.recordAttempt(attempt)
                val review = learningRepository.getReviewCandidateSets(
                    learningTimeProvider.nowEpochMillis()
                )
                updated to review
            }
            withContext(Dispatchers.Main) {
                val current = _uiState.value
                _uiState.value = result.fold(
                    onSuccess = { (updated, review) ->
                        current.copy(
                            learningStates = current.learningStates +
                                (updated.learningItemId to updated),
                            reviewLearningItemIds = review.all.mapTo(
                                mutableSetOf()
                            ) { it.learningItemId },
                            shortTermReviewLearningItemIds =
                                review.shortTerm.mapTo(mutableSetOf()) {
                                    it.learningItemId
                                },
                            learningHistoryError = null
                        )
                    },
                    onFailure = { error ->
                        current.copy(
                            learningHistoryError = error.message
                                ?: "回答履歴を保存できませんでした"
                        )
                    }
                )
            }
        }
        return state.copy(
            currentLearningAttemptId = attemptId,
            isCurrentLearningOutcomeRecorded = true
        )
    }

    private fun GameUiState.learningCourseId(): String? = when {
        studyMode != StudyMode.NORMAL -> studyMode.name.lowercase()
        selectedEikenLevel != null -> "eiken:${selectedEikenLevel.code}"
        selectedSchoolGrade != null -> "school:${selectedSchoolGrade.code}"
        selectedLevel != null -> "level:$selectedLevel"
        else -> null
    }

    private fun prepareCurrentQuestion(state: GameUiState): GameUiState {
        val word = state.word ?: return state.copy(
            currentQuestionPresentation = QuestionPresentation.WORD_INPUT,
            currentQuestionFormat = QuestionFormat.FULL_INPUT,
            currentSentenceQuestion = null,
            multipleChoiceOptions = emptyList(),
            selectedChoiceWordId = null,
            disabledChoiceWordIds = emptySet()
        )
        val sentenceQuestion = state.sentenceQuestionsByWordId[word.wordId]
        val presentation = resolveQuestionPresentation(
            mode = state.questionMode,
            studyMode = state.studyMode,
            wordId = word.wordId,
            hasSentenceQuestion = sentenceQuestion != null
        )
        val options = when (presentation) {
            QuestionPresentation.SENTENCE_MULTIPLE_CHOICE ->
                buildMultipleChoiceOptions(
                    requireNotNull(sentenceQuestion),
                    state.allWords
                )
            QuestionPresentation.MEANING_MULTIPLE_CHOICE ->
                buildMeaningChoiceOptions(word, state.words)
            QuestionPresentation.WORD_INPUT,
            QuestionPresentation.SENTENCE_INPUT -> emptyList()
        }
        return state.copy(
            currentQuestionPresentation = presentation,
            currentQuestionFormat = presentation.questionFormat,
            currentSentenceQuestion = sentenceQuestion
                .takeIf { presentation.usesSentence },
            multipleChoiceOptions = options,
            selectedChoiceWordId = null,
            disabledChoiceWordIds = emptySet()
        )
    }

    private fun GameUiState.completeCurrentStudyItem(): GameUiState {
        if (studyMode == StudyMode.NORMAL) return this
        val learningItemId = word?.learningItemId() ?: return this
        return copy(
            remainingStudyItemIds = remainingStudyItemIds - learningItemId
        )
    }

    fun resetFishCollection() {
        val state = _uiState.value
        if (
            state.phase != GamePhase.SETUP &&
            state.phase != GamePhase.COLLECTION
        ) return

        _uiState.value = state.copy(fishCollectionRecords = emptyMap())
        viewModelScope.launch(Dispatchers.IO) {
            fishCollectionStorage.clearFishCollection()
        }
    }

    fun openFishCollection() {
        val state = _uiState.value
        if (
            state.phase != GamePhase.TITLE &&
            state.phase != GamePhase.SETUP
        ) return

        _uiState.value = state.copy(
            phase = GamePhase.COLLECTION,
            collectionReturnPhase = state.phase
        )
    }

    fun closeFishCollection() {
        val state = _uiState.value
        if (state.phase != GamePhase.COLLECTION) return

        _uiState.value = state.copy(phase = state.collectionReturnPhase)
    }

    fun startGame() {
        val state = _uiState.value
        if (state.phase != GamePhase.TITLE) return
        _uiState.value = state.copy(phase = GamePhase.MAP_SELECTION)
    }

    fun openLicenses() {
        val state = _uiState.value
        if (state.phase != GamePhase.TITLE) return
        _uiState.value = state.copy(phase = GamePhase.LICENSES)
    }

    fun selectFishingMap(mapId: String) {
        val state = _uiState.value
        if (state.phase != GamePhase.MAP_SELECTION) return
        if (findFishingMap(mapId) == null) return

        _uiState.value = state.copy(
            selectedMapId = mapId,
            selectedPointId = null,
            phase = GamePhase.POINT_SELECTION
        )
    }

    fun selectFishingPoint(pointId: String) {
        val state = _uiState.value
        if (state.phase != GamePhase.POINT_SELECTION) return
        val point = findFishingPoint(pointId) ?: return
        if (point.mapId != state.selectedMapId) return

        _uiState.value = state.copy(
            selectedPointId = pointId,
            phase = GamePhase.SETUP
        )
        refreshLearningDashboard()
    }

    fun selectQuestionMode(questionMode: QuestionMode) {
        val state = _uiState.value
        if (state.phase != GamePhase.SETUP) return
        _uiState.value = state.copy(questionMode = questionMode)
    }

    fun selectDebugSentenceQuestions() {
        if (!BuildConfig.DEBUG) return
        val state = _uiState.value
        if (
            state.phase != GamePhase.SETUP ||
            state.questionMode !in setOf(
                QuestionMode.SENTENCE_INPUT,
                QuestionMode.SENTENCE_MULTIPLE_CHOICE,
                QuestionMode.MIXED
            )
        ) return
        val sentenceWords = state.allWords.filter {
            it.wordId in state.sentenceQuestionsByWordId
        }
        startWithWords(
            state = state,
            words = sentenceWords.shuffled(),
            selectedLevel = null,
            selectedSchoolGrade = null,
            selectedEikenLevel = null,
            studyMode = StudyMode.NORMAL
        )
    }

    fun navigateBack() {
        val state = _uiState.value
        _uiState.value = when (state.phase) {
            GamePhase.LICENSES -> state.copy(phase = GamePhase.TITLE)
            GamePhase.COLLECTION -> state.copy(
                phase = state.collectionReturnPhase
            )
            GamePhase.MAP_SELECTION -> state.copy(
                phase = GamePhase.TITLE
            )
            GamePhase.POINT_SELECTION -> state.copy(
                phase = GamePhase.MAP_SELECTION,
                selectedPointId = null
            )
            GamePhase.SETUP -> state.copy(
                phase = GamePhase.POINT_SELECTION
            )
            GamePhase.HOOK -> state.copy(
                phase = GamePhase.SETUP,
                currentFish = null,
                words = emptyList(),
                selectedLevel = null,
                selectedSchoolGrade = null,
                selectedEikenLevel = null,
                studyMode = StudyMode.NORMAL,
                currentLearningAttemptId = null,
                isCurrentLearningOutcomeRecorded = false
            )
            else -> state
        }
    }

    fun selectLevel(level: Int) {
        val state = _uiState.value
        if (state.phase != GamePhase.SETUP) return
        val selectedWords = filterWordsByLevel(
            words = state.allWords,
            selectedLevel = level
        )
        startWithWords(
            state = state,
            words = selectedWords.shuffled(),
            selectedLevel = level,
            selectedSchoolGrade = null,
            selectedEikenLevel = null,
            studyMode = StudyMode.NORMAL
        )
    }

    fun selectSchoolGrade(schoolGrade: SchoolGrade) {
        val state = _uiState.value
        if (state.phase != GamePhase.SETUP) return

        val selectedWords = filterWordsBySchoolGrade(
            words = state.allWords,
            schoolGrade = schoolGrade
        )
        startWithWords(
            state = state,
            words = selectedWords.shuffled(),
            selectedLevel = null,
            selectedSchoolGrade = schoolGrade,
            selectedEikenLevel = null,
            studyMode = StudyMode.NORMAL
        )
    }

    fun selectEikenLevel(eikenLevel: EikenLevel) {
        val state = _uiState.value
        if (state.phase != GamePhase.SETUP) return

        val selectedWords = filterWordsByEikenLevel(
            words = state.allWords,
            eikenLevel = eikenLevel
        )
        startWithWords(
            state = state,
            words = selectedWords.shuffled(),
            selectedLevel = null,
            selectedSchoolGrade = null,
            selectedEikenLevel = eikenLevel,
            studyMode = StudyMode.NORMAL
        )
    }

    fun selectReviewMode() {
        val state = _uiState.value
        if (state.phase != GamePhase.SETUP) return
        val words = state.allWords.filter {
            it.learningItemId() in state.reviewLearningItemIds
        }
        startWithWords(
            state = state,
            words = words.shuffled(),
            selectedLevel = null,
            selectedSchoolGrade = null,
            selectedEikenLevel = null,
            studyMode = StudyMode.REVIEW
        )
    }

    fun selectWeakMode() {
        val state = _uiState.value
        if (state.phase != GamePhase.SETUP) return
        val words = state.allWords.filter {
            it.learningItemId() in state.weakLearningItemIds
        }
        startWithWords(
            state = state,
            words = words.shuffled(),
            selectedLevel = null,
            selectedSchoolGrade = null,
            selectedEikenLevel = null,
            studyMode = StudyMode.WEAK
        )
    }

    private fun startWithWords(
        state: GameUiState,
        words: List<Word>,
        selectedLevel: Int?,
        selectedSchoolGrade: SchoolGrade?,
        selectedEikenLevel: EikenLevel?,
        studyMode: StudyMode
    ) {
        if (words.isEmpty()) return
        val pointId = state.selectedPointId ?: return

        _uiState.value = prepareCurrentQuestion(state.copy(
            words = words,
            selectedLevel = selectedLevel,
            selectedSchoolGrade = selectedSchoolGrade,
            selectedEikenLevel = selectedEikenLevel,
            studyMode = studyMode,
            remainingStudyItemIds = if (studyMode == StudyMode.NORMAL) {
                emptySet()
            } else {
                words.mapTo(mutableSetOf()) { it.learningItemId() }
            },
            currentQuestionIndex = 0,
            typedAnswer = "",
            rotations = emptyList(),
            isCorrect = null,
            currentFish = createRandomFishStateForPoint(pointId),
            phase = GamePhase.HOOK,
            gameResult = null,
            isNewFishDiscovery = false,
            isNewLargestSize = false,
            correctAnswerCount = 0,
            incorrectAnswerCount = 0,
            caughtFishCount = 0,
            battleStartedAtEpochMillis = 0L,
            battleFinishedAtEpochMillis = 0L,
            battleQuestionResults = emptyList(),
            currentQuestionFirstAnswer = null,
            currentQuestionFirstAnsweredAtEpochMillis = null,
            questionStartedAtEpochMillis = 0L,
            currentLearningAttemptId = null,
            isCurrentLearningOutcomeRecorded = false,
            wrongAnswersForCurrentQuestion = emptySet(),
            isDuplicateWrongAnswer = false,
            consecutiveCorrectAnswerCount = 0,
            bestConsecutiveCorrectAnswerCount = 0,
            score = 0,
            lastTimeBonus = 0,
            skippedQuestionCount = 0,
            escapedFishCount = 0,
            lineTension = 0,
            appliedTimeTensionSteps = 0
        ))
    }

    fun startReelBattle() {
        val state = _uiState.value
        if (
            state.phase != GamePhase.HOOK ||
            state.word == null ||
            state.currentFish == null
        ) return

        val battleStartedAt = learningTimeProvider.nowEpochMillis()
        _uiState.value = state.copy(
            phase = GamePhase.INPUT,
            battleStartedAtEpochMillis = battleStartedAt,
            battleFinishedAtEpochMillis = 0L,
            battleQuestionResults = emptyList(),
            currentQuestionFirstAnswer = null,
            currentQuestionFirstAnsweredAtEpochMillis = null,
            questionStartedAtEpochMillis = battleStartedAt,
            currentLearningAttemptId = UUID.randomUUID().toString(),
            isCurrentLearningOutcomeRecorded = false
        )
    }

    fun returnToLevelSelection() {
        val state = _uiState.value
        if (state.isLoading || state.errorMessage != null) return

        _uiState.value = state.copy(
            words = emptyList(),
            selectedLevel = null,
            selectedSchoolGrade = null,
            selectedEikenLevel = null,
            studyMode = StudyMode.NORMAL,
            remainingStudyItemIds = emptySet(),
            currentQuestionIndex = 0,
            typedAnswer = "",
            rotations = emptyList(),
            isCorrect = null,
            currentFish = null,
            phase = GamePhase.SETUP,
            gameResult = null,
            isNewFishDiscovery = false,
            isNewLargestSize = false,
            correctAnswerCount = 0,
            incorrectAnswerCount = 0,
            caughtFishCount = 0,
            battleStartedAtEpochMillis = 0L,
            battleFinishedAtEpochMillis = 0L,
            battleQuestionResults = emptyList(),
            currentQuestionFirstAnswer = null,
            currentQuestionFirstAnsweredAtEpochMillis = null,
            questionStartedAtEpochMillis = 0L,
            currentLearningAttemptId = null,
            isCurrentLearningOutcomeRecorded = false,
            wrongAnswersForCurrentQuestion = emptySet(),
            isDuplicateWrongAnswer = false,
            consecutiveCorrectAnswerCount = 0,
            bestConsecutiveCorrectAnswerCount = 0,
            score = 0,
            lastTimeBonus = 0,
            skippedQuestionCount = 0,
            escapedFishCount = 0,
            lineTension = 0,
            appliedTimeTensionSteps = 0
        )
    }

    fun onAnswerChanged(newText: String) {
        val state = _uiState.value
        if (state.phase != GamePhase.INPUT || state.word == null) return
        if (state.currentQuestionFormat == QuestionFormat.MULTIPLE_CHOICE) return

        val filteredText = newText
            .filter { it in 'a'..'z' || it in 'A'..'Z' }
            .lowercase()

        _uiState.value = state.copy(
            typedAnswer = filteredText,
            rotations = calculateRotation(filteredText),
            isCorrect = null,
            isDuplicateWrongAnswer = false
        )
    }

    fun submitAnswer() = submitAnswer(recordLearning = true)

    fun selectChoice(wordId: String) {
        val state = _uiState.value
        if (
            state.phase != GamePhase.INPUT ||
            state.currentQuestionFormat != QuestionFormat.MULTIPLE_CHOICE ||
            wordId in state.disabledChoiceWordIds
        ) return
        val choice = state.multipleChoiceOptions.firstOrNull {
            it.wordId == wordId
        } ?: return
        if (state.selectedChoiceWordId == wordId) {
            submitAnswer()
            return
        }
        _uiState.value = state.copy(
            selectedChoiceWordId = wordId,
            rotations = calculateRotation(choice.english),
            isCorrect = null,
            isDuplicateWrongAnswer = false
        )
    }

    private fun recordFirstBattleAnswer(
        state: GameUiState,
        submittedAnswer: String,
        answeredAtEpochMillis: Long
    ): GameUiState = if (state.currentQuestionFirstAnswer == null) {
        state.copy(
            currentQuestionFirstAnswer = submittedAnswer,
            currentQuestionFirstAnsweredAtEpochMillis =
                answeredAtEpochMillis
        )
    } else {
        state
    }

    private fun completeBattleQuestion(
        state: GameUiState,
        word: Word,
        finalAnswer: String?,
        outcome: BattleQuestionOutcome,
        completedAtEpochMillis: Long
    ): GameUiState {
        val firstAnswer = state.currentQuestionFirstAnswer ?: finalAnswer
        val firstAnsweredAt =
            state.currentQuestionFirstAnsweredAtEpochMillis
                ?: completedAtEpochMillis
        val questionStartedAt = state.questionStartedAtEpochMillis
            .takeIf { it > 0L }
            ?: firstAnsweredAt
        val sentenceQuestion = state.currentSentenceQuestion
            .takeIf { state.currentQuestionUsesSentence }
        val result = BattleQuestionResult(
            wordId = word.wordId,
            questionText = sentenceQuestion?.sentence
                ?: word.questionMeaning,
            japaneseText = sentenceQuestion?.japaneseSentence,
            correctAnswer = word.english,
            firstAnswer = firstAnswer,
            questionFormat = state.currentQuestionFormat,
            outcome = outcome,
            firstResponseTimeMillis =
                (firstAnsweredAt - questionStartedAt).coerceAtLeast(0L),
            totalTimeMillis =
                (completedAtEpochMillis - questionStartedAt)
                    .coerceAtLeast(0L)
        )
        return state.copy(
            battleQuestionResults = state.battleQuestionResults + result,
            currentQuestionFirstAnswer = null,
            currentQuestionFirstAnsweredAtEpochMillis = null
        )
    }

    private fun submitAnswer(recordLearning: Boolean) {
        var state = _uiState.value
        val word = state.word ?: return
        val fishState = state.currentFish ?: return
        if (!state.canSubmit) return

        val selectedChoice = if (
            state.currentQuestionFormat == QuestionFormat.MULTIPLE_CHOICE
        ) {
            state.multipleChoiceOptions.firstOrNull {
                it.wordId == state.selectedChoiceWordId
            } ?: return
        } else {
            null
        }
        val submittedAnswer = selectedChoice?.english ?: state.typedAnswer
        val submittedAtEpochMillis =
            learningTimeProvider.nowEpochMillis()

        val correct = isCorrectAnswer(submittedAnswer, word)
        if (!correct) {
            val wrongAnswerUpdate = registerWrongAnswer(
                existingAnswers = state.wrongAnswersForCurrentQuestion,
                answer = submittedAnswer
            )
            if (wrongAnswerUpdate.isDuplicate) {
                _uiState.value = state.copy(
                    isCorrect = false,
                    isDuplicateWrongAnswer = true
                )
                return
            }

            if (recordLearning) {
                state = recordLearningSubmission(
                    state = state,
                    word = word,
                    wasCorrect = false
                )
            }
            state = recordFirstBattleAnswer(
                state = state,
                submittedAnswer = submittedAnswer,
                answeredAtEpochMillis = submittedAtEpochMillis
            )

            val stateAfterWrongAnswer = state.copy(
                isCorrect = false,
                incorrectAnswerCount = state.incorrectAnswerCount + 1,
                wrongAnswersForCurrentQuestion =
                    wrongAnswerUpdate.answers,
                isDuplicateWrongAnswer = false,
                consecutiveCorrectAnswerCount = 0,
                lastTimeBonus = 0,
                selectedChoiceWordId = null,
                disabledChoiceWordIds = if (selectedChoice == null) {
                    state.disabledChoiceWordIds
                } else {
                    state.disabledChoiceWordIds + selectedChoice.wordId
                },
                rotations = if (selectedChoice == null) {
                    state.rotations
                } else {
                    emptyList()
                },
                lineTension = increaseLineTension(
                    state.lineTension,
                    WRONG_ANSWER_TENSION
                )
            )
            updateStateOrEscape(
                state = stateAfterWrongAnswer,
                recordLearning = recordLearning
            )
            return
        }

        if (recordLearning) {
            state = recordLearningSubmission(
                state = state,
                word = word,
                wasCorrect = true
            )
        }
        state = state.completeCurrentStudyItem()
        val battleOutcome = if (state.currentQuestionFirstAnswer == null) {
            BattleQuestionOutcome.FIRST_TRY_CORRECT
        } else {
            BattleQuestionOutcome.CORRECTED_AFTER_MISTAKE
        }
        state = completeBattleQuestion(
            state = state,
            word = word,
            finalAnswer = submittedAnswer,
            outcome = battleOutcome,
            completedAtEpochMillis = submittedAtEpochMillis
        )

        val elapsedSeconds = questionElapsedSeconds(state)
        val timeBonus = calculateTimeBonus(elapsedSeconds)
        val newStreak = state.consecutiveCorrectAnswerCount + 1

        val newFishState = reelFish(
            state = fishState,
            totalRotation = state.rotations.sum()
        )

        val answeredAttemptId = state.currentLearningAttemptId
        _uiState.value = state.copy(
            phase = GamePhase.REELING,
            isCorrect = true,
            reelAnimationRunId = state.reelAnimationRunId + 1
        )
        viewModelScope.launch {
            delay(reelAnswerAnimationDurationMillis(state.rotations))
            val activeState = _uiState.value
            if (
                activeState.phase != GamePhase.REELING ||
                activeState.currentLearningAttemptId != answeredAttemptId
            ) return@launch

            if (newFishState.currentDistance <= 0f) {
                completeCatch(
                    state = state,
                    caughtFishState = newFishState,
                    countAsCorrectAnswer = true,
                    timeBonus = timeBonus,
                    newStreak = newStreak
                )
            } else {
                if (state.isStudySessionFinished) {
                    _uiState.value = state.copy(
                        typedAnswer = "",
                        rotations = emptyList(),
                        isCorrect = true,
                        currentFish = newFishState,
                        correctAnswerCount = state.correctAnswerCount + 1,
                        phase = GamePhase.RESULT,
                        gameResult = GameResult.STUDY_COMPLETE,
                        consecutiveCorrectAnswerCount = newStreak,
                        bestConsecutiveCorrectAnswerCount = maxOf(
                            state.bestConsecutiveCorrectAnswerCount,
                            newStreak
                        ),
                        score = state.score + BASE_CORRECT_SCORE + timeBonus,
                        lastTimeBonus = timeBonus
                    )
                    return@launch
                }
                val nextQuestion = advanceQuestion(
                    words = state.words,
                    currentIndex = state.currentQuestionIndex
                )

                _uiState.value = prepareCurrentQuestion(state.copy(
                    words = nextQuestion.words,
                    currentQuestionIndex = nextQuestion.currentIndex,
                    typedAnswer = "",
                    rotations = emptyList(),
                    isCorrect = true,
                    currentFish = newFishState,
                    correctAnswerCount = state.correctAnswerCount + 1,
                    questionStartedAtEpochMillis =
                        learningTimeProvider.nowEpochMillis(),
                    currentLearningAttemptId = UUID.randomUUID().toString(),
                    isCurrentLearningOutcomeRecorded = false,
                    wrongAnswersForCurrentQuestion = emptySet(),
                    isDuplicateWrongAnswer = false,
                    consecutiveCorrectAnswerCount = newStreak,
                    bestConsecutiveCorrectAnswerCount = maxOf(
                        state.bestConsecutiveCorrectAnswerCount,
                        newStreak
                    ),
                    score = state.score + BASE_CORRECT_SCORE + timeBonus,
                    lastTimeBonus = timeBonus,
                    appliedTimeTensionSteps = 0
                ))
            }
        }
    }

    fun skipCurrentQuestion() {
        var state = _uiState.value
        if (
            state.phase != GamePhase.INPUT ||
            state.words.isEmpty()
        ) return

        val skippedWord = state.word
        val skippedAtEpochMillis = learningTimeProvider.nowEpochMillis()
        if (
            skippedWord != null &&
            !state.isCurrentLearningOutcomeRecorded
        ) {
            state = recordLearningSubmission(
                state = state,
                word = skippedWord,
                wasCorrect = false
            )
        }
        state = state.completeCurrentStudyItem()
        if (skippedWord != null) {
            state = completeBattleQuestion(
                state = state,
                word = skippedWord,
                finalAnswer = null,
                outcome = BattleQuestionOutcome.SKIPPED,
                completedAtEpochMillis = skippedAtEpochMillis
            )
        }

        if (state.isStudySessionFinished) {
            _uiState.value = state.copy(
                typedAnswer = "",
                rotations = emptyList(),
                isCorrect = null,
                phase = GamePhase.RESULT,
                gameResult = GameResult.STUDY_COMPLETE,
                skippedQuestionCount = state.skippedQuestionCount + 1,
                consecutiveCorrectAnswerCount = 0
            )
            return
        }

        val tensionAfterSkip = increaseLineTension(
            state.lineTension,
            SKIP_QUESTION_TENSION
        )
        if (tensionAfterSkip >= MAX_LINE_TENSION) {
            escapeCurrentFish(
                state.copy(
                    lineTension = tensionAfterSkip,
                    skippedQuestionCount = state.skippedQuestionCount + 1,
                    consecutiveCorrectAnswerCount = 0
                )
            )
            return
        }

        val nextQuestion = advanceQuestion(
            words = state.words,
            currentIndex = state.currentQuestionIndex
        )
        _uiState.value = prepareCurrentQuestion(state.copy(
            words = nextQuestion.words,
            currentQuestionIndex = nextQuestion.currentIndex,
            typedAnswer = "",
            rotations = emptyList(),
            isCorrect = null,
            questionStartedAtEpochMillis =
                learningTimeProvider.nowEpochMillis(),
            currentLearningAttemptId = UUID.randomUUID().toString(),
            isCurrentLearningOutcomeRecorded = false,
            wrongAnswersForCurrentQuestion = emptySet(),
            isDuplicateWrongAnswer = false,
            consecutiveCorrectAnswerCount = 0,
            lastTimeBonus = 0,
            skippedQuestionCount = state.skippedQuestionCount + 1,
            lineTension = tensionAfterSkip,
            appliedTimeTensionSteps = 0
        ))
    }

    fun onQuestionTimerTick(
        questionStartedAtEpochMillis: Long,
        elapsedSeconds: Long
    ) {
        val state = _uiState.value
        if (
            state.phase != GamePhase.INPUT ||
            state.questionStartedAtEpochMillis !=
            questionStartedAtEpochMillis
        ) return

        val totalSteps = calculateTimeTensionSteps(elapsedSeconds)
        val newSteps = totalSteps - state.appliedTimeTensionSteps
        if (newSteps <= 0) return

        val updatedState = state.copy(
            lineTension = increaseLineTension(
                state.lineTension,
                newSteps * TIME_TENSION_PER_STEP
            ),
            appliedTimeTensionSteps = totalSteps
        )
        updateStateOrEscape(updatedState)
    }

    fun submitDebugAnswer(correct: Boolean) {
        if (!BuildConfig.DEBUG) return

        val state = _uiState.value
        val word = state.word ?: return
        if (state.currentQuestionFormat == QuestionFormat.MULTIPLE_CHOICE) {
            val choice = if (correct) {
                state.multipleChoiceOptions.firstOrNull {
                    it.wordId == word.wordId
                }
            } else {
                state.multipleChoiceOptions.firstOrNull {
                    it.wordId != word.wordId &&
                        it.wordId !in state.disabledChoiceWordIds
                }
            } ?: return
            selectChoice(choice.wordId)
            submitAnswer(recordLearning = false)
            return
        }
        val debugAnswer = if (correct) {
            word.english
        } else {
            createIncorrectAnswer(word.english)
        }

        onAnswerChanged(debugAnswer)
        submitAnswer(recordLearning = false)
    }

    fun cycleDebugFish() {
        if (!BuildConfig.DEBUG) return

        val state = _uiState.value
        val currentFish = state.currentFish ?: return
        if (state.phase != GamePhase.INPUT) return

        val pointFishes = state.selectedPointId
            ?.let(::fishesForPoint)
            .orEmpty()
            .ifEmpty { fishes }
        val currentIndex = pointFishes.indexOfFirst {
            it.name == currentFish.fish.name
        }
        val nextFish = pointFishes[(currentIndex + 1) % pointFishes.size]
        _uiState.value = state.copy(
            currentFish = createFishState(nextFish),
            typedAnswer = "",
            rotations = emptyList(),
            isCorrect = null
        )
    }

    fun catchCurrentFishForDebug() {
        if (!BuildConfig.DEBUG) return

        val state = _uiState.value
        val fishState = state.currentFish ?: return
        if (state.phase != GamePhase.INPUT) return

        completeCatch(
            state = state.copy(
                typedAnswer = "",
                rotations = emptyList()
            ),
            caughtFishState = fishState.copy(
                currentHp = 0,
                currentDistance = 0f
            ),
            countAsCorrectAnswer = false,
            timeBonus = 0,
            newStreak = state.consecutiveCorrectAnswerCount
        )
    }

    private fun completeCatch(
        state: GameUiState,
        caughtFishState: FishState,
        countAsCorrectAnswer: Boolean,
        timeBonus: Int,
        newStreak: Int
    ) {
        val caughtAtEpochMillis = learningTimeProvider.nowEpochMillis()
        val catchUpdate = recordFishCatch(
            records = state.fishCollectionRecords,
            fishState = caughtFishState,
            caughtAtEpochMillis = caughtAtEpochMillis,
            mapId = state.selectedMapId,
            pointId = state.selectedPointId
        )

        _uiState.value = state.copy(
            currentFish = caughtFishState,
            battleFinishedAtEpochMillis = caughtAtEpochMillis,
            isCorrect = true,
            phase = GamePhase.RESULT,
            gameResult = GameResult.CAUGHT,
            isNewFishDiscovery = catchUpdate.isNewDiscovery,
            isNewLargestSize = catchUpdate.isNewLargestSize,
            correctAnswerCount = state.correctAnswerCount +
                (if (countAsCorrectAnswer) 1 else 0),
            caughtFishCount = state.caughtFishCount + 1,
            consecutiveCorrectAnswerCount = newStreak,
            bestConsecutiveCorrectAnswerCount = maxOf(
                state.bestConsecutiveCorrectAnswerCount,
                newStreak
            ),
            score = state.score +
                (if (countAsCorrectAnswer) {
                    BASE_CORRECT_SCORE + timeBonus
                } else {
                    0
                }),
            lastTimeBonus = if (countAsCorrectAnswer) timeBonus else 0,
            fishCollectionRecords = catchUpdate.records
        )
        saveFishCollection(catchUpdate.records)
    }

    fun startNextFish() {
        val state = _uiState.value
        if (state.phase != GamePhase.RESULT || state.words.isEmpty()) return
        if (state.isStudySessionFinished) {
            returnToLevelSelection()
            refreshLearningDashboard()
            return
        }

        val nextQuestion = advanceQuestion(
            words = state.words,
            currentIndex = state.currentQuestionIndex
        )
        val pointId = state.selectedPointId ?: return

        _uiState.value = prepareCurrentQuestion(state.copy(
            words = nextQuestion.words,
            currentQuestionIndex = nextQuestion.currentIndex,
            typedAnswer = "",
            rotations = emptyList(),
            isCorrect = null,
            currentFish = createRandomFishStateForPoint(pointId),
            phase = GamePhase.HOOK,
            gameResult = null,
            isNewFishDiscovery = false,
            isNewLargestSize = false,
            battleStartedAtEpochMillis = 0L,
            battleFinishedAtEpochMillis = 0L,
            battleQuestionResults = emptyList(),
            currentQuestionFirstAnswer = null,
            currentQuestionFirstAnsweredAtEpochMillis = null,
            questionStartedAtEpochMillis = 0L,
            currentLearningAttemptId = null,
            isCurrentLearningOutcomeRecorded = false,
            wrongAnswersForCurrentQuestion = emptySet(),
            isDuplicateWrongAnswer = false,
            lastTimeBonus = 0,
            lineTension = 0,
            appliedTimeTensionSteps = 0
        ))
    }

    private fun updateStateOrEscape(
        state: GameUiState,
        recordLearning: Boolean = true
    ) {
        if (state.lineTension >= MAX_LINE_TENSION) {
            escapeCurrentFish(state, recordLearning)
        } else {
            _uiState.value = state
        }
    }

    private fun escapeCurrentFish(
        state: GameUiState,
        recordLearning: Boolean = true
    ) {
        if (state.phase != GamePhase.INPUT) return
        val currentWord = state.word
        val recordedState = if (
            recordLearning &&
            !state.isCurrentLearningOutcomeRecorded &&
            currentWord != null
        ) {
            recordLearningSubmission(
                state = state,
                word = currentWord,
                wasCorrect = false
            )
        } else {
            state
        }
        val escapedState = recordedState.completeCurrentStudyItem()
        _uiState.value = escapedState.copy(
            phase = GamePhase.RESULT,
            gameResult = GameResult.ESCAPED,
            lineTension = MAX_LINE_TENSION,
            consecutiveCorrectAnswerCount = 0,
            escapedFishCount = state.escapedFishCount + 1,
            lastTimeBonus = 0
        )
    }

    private fun questionElapsedSeconds(state: GameUiState): Long {
        if (state.questionStartedAtEpochMillis <= 0L) return 0L
        return ((System.currentTimeMillis() -
            state.questionStartedAtEpochMillis) / 1000L).coerceAtLeast(0L)
    }

}
