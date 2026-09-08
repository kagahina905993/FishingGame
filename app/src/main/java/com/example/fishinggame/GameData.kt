package com.example.fishinggame

private val emphasizedMeaningPattern = Regex("『([^』]+)』")
private val dictionaryLabelPattern = Regex("《[^》]*》|〈[^〉]*〉")
private val japaneseCharacterPattern = Regex("[ぁ-んァ-ヶ一-龯]")
private val dictionaryGrammarMarkers = setOf(
    "名", "動", "形", "副", "代", "前", "接", "冠", "助",
    "単", "複", "過分", "現分"
)

fun extractPrimaryMeaning(dictionaryText: String): String {
    val emphasizedMeaning = emphasizedMeaningPattern
        .findAll(dictionaryText)
        .mapNotNull { match ->
            match.groupValues.getOrNull(1)?.trim()
        }
        .firstOrNull { candidate ->
            candidate.isNotEmpty() &&
                candidate !in dictionaryGrammarMarkers &&
                japaneseCharacterPattern.containsMatchIn(candidate)
        }

    if (!emphasizedMeaning.isNullOrEmpty()) {
        return emphasizedMeaning
    }

    val cleanedMeaning = dictionaryText
        .split(" / ")
        .asSequence()
        .map { section ->
            section
                .replace(dictionaryLabelPattern, "")
                .replace("『", "")
                .replace("』", "")
                .trim()
        }
        .firstOrNull { it.isNotEmpty() }
        .orEmpty()

    if (cleanedMeaning.isEmpty()) return dictionaryText.trim()
    return if (cleanedMeaning.length <= 80) {
        cleanedMeaning
    } else {
        cleanedMeaning.take(80).trimEnd() + "…"
    }
}

data class Word(
    val english: String,
    val japanese: String,
    val ngslRank: Int?,
    val level: Int,
    val schoolGrade: SchoolGrade?,
    val eikenLevel: EikenLevel?,
    val partOfSpeech: String?,
    val sfi: Double?,
    val frequencyPerMillion: Int?,
    val translationSource: String?,
    val wordList: WordList = WordList.NGSL_1_2,
    val sourceRank: Int = ngslRank ?: 0,
    val estimatedCefrLevel: String? = null,
    val eikenClassificationBasis: String? = null,
    val quizMeaning: String? = null,
    val quizPartOfSpeech: String? = null,
    val quizHint: String? = null,
    val quizSource: String? = null
) {
    val wordId: String
        get() = buildString {
            append(wordList.code.lowercase())
            append(':')
            append(sourceRank.toString().padStart(4, '0'))
        }

    val questionMeaning: String
        get() = quizMeaning?.takeIf { it.isNotBlank() }
            ?: extractPrimaryMeaning(japanese)

    val questionPartOfSpeech: String?
        get() = quizPartOfSpeech?.takeIf { it.isNotBlank() }
            ?: partOfSpeech?.takeIf { it.isNotBlank() }

    fun learningItemId(
        direction: LearningDirection = LearningDirection.JAPANESE_TO_ENGLISH
    ): String = "$wordId:${direction.code}"
}

internal fun isFirstAppearance(
    word: Word,
    learningStates: Map<String, LearningState>
): Boolean = learningStates[word.learningItemId()]
    ?.totalAttemptCount
    ?.let { it == 0 }
    ?: true

enum class SchoolGrade(
    val code: Int,
    val displayName: String
) {
    ELEMENTARY_5(5, "小学5年"),
    ELEMENTARY_6(6, "小学6年"),
    JUNIOR_HIGH_1(7, "中学1年"),
    JUNIOR_HIGH_2(8, "中学2年"),
    JUNIOR_HIGH_3(9, "中学3年"),
    HIGH_SCHOOL_1(10, "高校1年"),
    HIGH_SCHOOL_2(11, "高校2年"),
    HIGH_SCHOOL_3(12, "高校3年");

    companion object {
        fun fromCode(code: Int): SchoolGrade? =
            entries.firstOrNull { it.code == code }
    }
}

enum class WordList(val code: String) {
    NGSL_1_2("NGSL_1_2"),
    NAWL_1_2("NAWL_1_2");

    companion object {
        fun fromCode(code: String): WordList? =
            entries.firstOrNull { it.code == code }
    }
}

enum class EikenLevel(
    val code: String,
    val displayName: String
) {
    GRADE_5("5", "英検5級相当"),
    GRADE_4("4", "英検4級相当"),
    GRADE_3("3", "英検3級相当"),
    GRADE_PRE_2("pre2", "英検準2級相当"),
    GRADE_PRE_2_PLUS("pre2plus", "英検準2級プラス相当"),
    GRADE_2("2", "英検2級相当"),
    GRADE_PRE_1("pre1", "英検準1級相当"),
    GRADE_1("1", "英検1級相当");

    companion object {
        fun fromCode(code: String): EikenLevel? =
            entries.firstOrNull { it.code == code }
    }
}


enum class GameResult {
    CAUGHT,
    ESCAPED,
    STUDY_COMPLETE
}

enum class StudyMode(val displayName: String) {
    NORMAL("通常学習"),
    REVIEW("復習"),
    WEAK("苦手単語")
}

enum class GamePhase {
    TITLE,
    LICENSES,
    MAP_SELECTION,
    POINT_SELECTION,
    SETUP,
    HOOK,
    COLLECTION,
    INPUT,
    REELING,
    RESULT
}

data class FishCollectionRecord(
    val caughtCount: Int = 0,
    val largestSizeCm: Float = 0f,
    val firstCaughtAtEpochMillis: Long = 0L,
    val lastCaughtAtEpochMillis: Long = 0L,
    val firstCaughtMapId: String? = null,
    val firstCaughtPointId: String? = null,
    val lastCaughtMapId: String? = null,
    val lastCaughtPointId: String? = null
)

enum class BattleQuestionOutcome {
    FIRST_TRY_CORRECT,
    CORRECTED_AFTER_MISTAKE,
    SKIPPED
}

data class BattleQuestionResult(
    val wordId: String,
    val questionText: String,
    val japaneseText: String? = null,
    val correctAnswer: String,
    val firstAnswer: String? = null,
    val questionFormat: QuestionFormat,
    val outcome: BattleQuestionOutcome,
    val firstResponseTimeMillis: Long,
    val totalTimeMillis: Long
)

data class GameUiState(
    val isLoading: Boolean = false,
    val allWords: List<Word> = emptyList(),
    val sentenceQuestionsByWordId: Map<String, SentenceQuestion> = emptyMap(),
    val words: List<Word> = emptyList(),
    val selectedLevel: Int? = null,
    val selectedSchoolGrade: SchoolGrade? = null,
    val selectedEikenLevel: EikenLevel? = null,
    val studyMode: StudyMode = StudyMode.NORMAL,
    val questionMode: QuestionMode = QuestionMode.WORD_INPUT,
    val currentQuestionPresentation: QuestionPresentation =
        QuestionPresentation.WORD_INPUT,
    val currentQuestionFormat: QuestionFormat = QuestionFormat.FULL_INPUT,
    val currentSentenceQuestion: SentenceQuestion? = null,
    val multipleChoiceOptions: List<Word> = emptyList(),
    val selectedChoiceWordId: String? = null,
    val disabledChoiceWordIds: Set<String> = emptySet(),
    val currentQuestionIndex: Int = 0,
    val typedAnswer: String = "",
    val phase: GamePhase = GamePhase.TITLE,
    val collectionReturnPhase: GamePhase = GamePhase.TITLE,
    val selectedMapId: String? = null,
    val selectedPointId: String? = null,
    val rotations: List<Int> = emptyList(),
    val reelAnimationRunId: Int = 0,
    val isCorrect: Boolean? = null,
    val currentFish: FishState? = null,
    val gameResult: GameResult? = null,
    val isNewFishDiscovery: Boolean = false,
    val isNewLargestSize: Boolean = false,
    val correctAnswerCount: Int = 0,
    val incorrectAnswerCount: Int = 0,
    val caughtFishCount: Int = 0,
    val battleStartedAtEpochMillis: Long = 0L,
    val battleFinishedAtEpochMillis: Long = 0L,
    val battleQuestionResults: List<BattleQuestionResult> = emptyList(),
    val currentQuestionFirstAnswer: String? = null,
    val currentQuestionFirstAnsweredAtEpochMillis: Long? = null,
    val questionStartedAtEpochMillis: Long = 0L,
    val currentLearningAttemptId: String? = null,
    val isCurrentLearningOutcomeRecorded: Boolean = false,
    val wrongAnswersForCurrentQuestion: Set<String> = emptySet(),
    val isDuplicateWrongAnswer: Boolean = false,
    val consecutiveCorrectAnswerCount: Int = 0,
    val bestConsecutiveCorrectAnswerCount: Int = 0,
    val score: Int = 0,
    val lastTimeBonus: Int = 0,
    val skippedQuestionCount: Int = 0,
    val escapedFishCount: Int = 0,
    val lineTension: Int = 0,
    val appliedTimeTensionSteps: Int = 0,
    val fishCollectionRecords: Map<String, FishCollectionRecord> = emptyMap(),
    val learningStates: Map<String, LearningState> = emptyMap(),
    val reviewLearningItemIds: Set<String> = emptySet(),
    val shortTermReviewLearningItemIds: Set<String> = emptySet(),
    val remainingStudyItemIds: Set<String> = emptySet(),
    val isLearningHistoryLoading: Boolean = false,
    val savedAssistedCompletionCount: Int = 0,
    val learningHistoryError: String? = null,
    val errorMessage: String? = null
) {
    val selectedMap: FishingMap?
        get() = findFishingMap(selectedMapId)

    val selectedPoint: FishingPoint?
        get() = findFishingPoint(selectedPointId)

    val word: Word?
        get() = words.getOrNull(currentQuestionIndex)

    val selectedCourseName: String
        get() = studyMode.takeIf { it != StudyMode.NORMAL }?.displayName
            ?: selectedEikenLevel?.displayName
            ?: selectedSchoolGrade?.displayName
            ?: selectedLevel?.let { "レベル$it" }
            ?: "未選択"

    val canSubmit: Boolean
        get() = phase == GamePhase.INPUT && when (currentQuestionFormat) {
            QuestionFormat.MULTIPLE_CHOICE -> selectedChoiceWordId != null
            QuestionFormat.FULL_INPUT,
            QuestionFormat.FILL_IN_THE_BLANK -> typedAnswer.isNotBlank()
        }

    val currentQuestionUsesSentence: Boolean
        get() = currentQuestionPresentation.usesSentence &&
            currentSentenceQuestion != null

    val currentQuestionMistakeCount: Int
        get() = wrongAnswersForCurrentQuestion.size

    val shouldShowQuestionHint: Boolean
        get() = currentQuestionMistakeCount >= 3

    val shouldRevealAnswer: Boolean
        get() = currentQuestionMistakeCount >= 5

    val lineTensionProgress: Float
        get() = (lineTension.toFloat() / MAX_LINE_TENSION)
            .coerceIn(0f, 1f)

    val questionMeaningCollisionCount: Int
        get() {
            val currentWord = word ?: return 0
            return words.count {
                it.questionMeaning == currentWord.questionMeaning &&
                    it.questionPartOfSpeech == currentWord.questionPartOfSpeech
            }
        }

    val totalAnswerCount: Int
        get() = correctAnswerCount + incorrectAnswerCount

    val accuracyPercent: Int
        get() = if (totalAnswerCount == 0) {
            0
        } else {
            correctAnswerCount * 100 / totalAnswerCount
        }

    val battleInitialCorrectCount: Int
        get() = battleQuestionResults.count {
            it.outcome == BattleQuestionOutcome.FIRST_TRY_CORRECT
        }

    val battleCorrectedAfterMistakeCount: Int
        get() = battleQuestionResults.count {
            it.outcome == BattleQuestionOutcome.CORRECTED_AFTER_MISTAKE
        }

    val battleSkippedCount: Int
        get() = battleQuestionResults.count {
            it.outcome == BattleQuestionOutcome.SKIPPED
        }

    val battleInitialAccuracyPercent: Int
        get() = if (battleQuestionResults.isEmpty()) {
            0
        } else {
            battleInitialCorrectCount * 100 / battleQuestionResults.size
        }

    val battleDurationMillis: Long
        get() = if (
            battleStartedAtEpochMillis <= 0L ||
            battleFinishedAtEpochMillis < battleStartedAtEpochMillis
        ) {
            0L
        } else {
            battleFinishedAtEpochMillis - battleStartedAtEpochMillis
        }

    val weakLearningItemIds: Set<String>
        get() = learningStates.values
            .filter { it.isWeak }
            .mapTo(mutableSetOf()) { it.learningItemId }

    val masteredLearningItemCount: Int
        get() = learningStates.values.count {
            it.status == LearningStatus.MASTERED
        }

    val masteryPercent: Int
        get() = if (allWords.isEmpty()) {
            0
        } else {
            masteredLearningItemCount * 100 / allWords.size
        }

    val savedIndependentRecallCount: Int
        get() = learningStates.values.sumOf { it.independentRecallCount }

    val savedAssistedRecallCount: Int
        get() = learningStates.values.sumOf { it.assistedRecallCount }

    val savedFailedRecallCount: Int
        get() = learningStates.values.sumOf { it.failedRecallCount }

    val savedInitialRecallAccuracyPercent: Int
        get() {
            val total = savedIndependentRecallCount +
                savedAssistedRecallCount + savedFailedRecallCount
            return if (total == 0) {
                0
            } else {
                savedIndependentRecallCount * 100 / total
            }
        }

    val isStudySessionFinished: Boolean
        get() = studyMode != StudyMode.NORMAL &&
            remainingStudyItemIds.isEmpty()

    val caughtSpeciesCount: Int
        get() = fishes.count { fish ->
            fishCollectionRecords[fish.name]?.caughtCount?.let {
                it > 0
            } == true
        }

    val availableCaughtSpeciesCount: Int
        get() = currentlyAvailableFishes.count { fish ->
            (fishCollectionRecords[fish.name]?.caughtCount ?: 0) > 0
        }

    val isAvailableFishCollectionComplete: Boolean
        get() = currentlyAvailableFishes.isNotEmpty() &&
            availableCaughtSpeciesCount == currentlyAvailableFishes.size

    fun caughtSpeciesCountFor(rarity: FishRarity): Int =
        fishes.count { fish ->
            fish.rarity == rarity &&
                (fishCollectionRecords[fish.name]?.caughtCount ?: 0) > 0
        }

    val isFishCollectionComplete: Boolean
        get() = fishes.isNotEmpty() && caughtSpeciesCount == fishes.size
}
