package com.example.fishinggame

import kotlin.random.Random

const val BASE_CORRECT_SCORE = 100
const val TIME_BONUS_SECONDS = 30
const val MAX_LINE_TENSION = 100
const val TIME_TENSION_GRACE_SECONDS = 10L
const val TIME_TENSION_INTERVAL_SECONDS = 5L
const val TIME_TENSION_PER_STEP = 5
const val WRONG_ANSWER_TENSION = 15
const val SKIP_QUESTION_TENSION = 10

fun validateWordDataset(words: List<Word>): List<Word> {
    require(words.isNotEmpty()) {
        "words.jsonに単語がありません"
    }
    require(words.map { it.english.lowercase() }.distinct().size == words.size) {
        "英単語が重複しています"
    }
    require(words.map { it.wordId }.distinct().size == words.size) {
        "単語IDが重複しています"
    }
    require(words.none { it.schoolGrade == null }) {
        "学年が未分類の単語があります"
    }
    require(words.none { it.eikenLevel == null }) {
        "英検級が未分類の単語があります"
    }
    require(words.all {
        it.quizMeaning?.isNotBlank() == true &&
            it.questionPartOfSpeech != null &&
            it.quizSource?.isNotBlank() == true
    }) {
        "問題用の意味・品詞・出典が不足している単語があります"
    }
    require(words.all { word ->
        when (word.wordList) {
            WordList.NGSL_1_2 -> word.ngslRank == word.sourceRank
            WordList.NAWL_1_2 -> word.ngslRank == null
        }
    }) {
        "語彙リストと順位の組み合わせが不正です"
    }
    require(words.groupBy { it.wordList }.values.all { sourceWords ->
        sourceWords.map { it.sourceRank }.sorted() ==
            (1..sourceWords.size).toList()
    }) {
        "語彙リスト内の順位に重複または欠番があります"
    }
    fun hasAmbiguousPrompt(courseWords: Collection<List<Word>>): Boolean =
        courseWords.any { groupedWords ->
            groupedWords.groupBy {
                it.questionMeaning to it.questionPartOfSpeech
            }.values.any { it.size > 1 }
        }
    require(!hasAmbiguousPrompt(words.groupBy { it.level }.values)) {
        "ゲームレベル内に同一の意味・品詞の問題があります"
    }
    require(!hasAmbiguousPrompt(words.groupBy { it.schoolGrade }.values)) {
        "学年内に同一の意味・品詞の問題があります"
    }
    require(!hasAmbiguousPrompt(words.groupBy { it.eikenLevel }.values)) {
        "英検級内に同一の意味・品詞の問題があります"
    }
    return words
}

data class QuestionProgress(
    val words: List<Word>,
    val currentIndex: Int
)

fun rotationSteps(from: Char, to: Char): Int {
    val distance = (to - from + 26) % 26
    return if (distance == 0) 26 else distance
}
fun calculateRotation(answer: String): List<Int> {
    var current = 'A'
    return answer.uppercase().map { next ->
        val steps = rotationSteps(current, next)
        current = next
        steps
    }
}

fun isCorrectAnswer(answer: String, word: Word): Boolean {
    val normalizedAnswer = answer.filterNot { it.isWhitespace() }
    val normalizedCorrect = word.english.filterNot { it.isWhitespace() }
    return normalizedAnswer.equals(
        normalizedCorrect,
        ignoreCase = true
    )
}

fun calculateTimeBonus(elapsedSeconds: Long): Int =
    (TIME_BONUS_SECONDS - elapsedSeconds.coerceAtLeast(0L))
        .coerceAtLeast(0L)
        .toInt()

fun calculateTimeTensionSteps(elapsedSeconds: Long): Int {
    if (elapsedSeconds <= TIME_TENSION_GRACE_SECONDS) return 0
    return ((elapsedSeconds - TIME_TENSION_GRACE_SECONDS) /
        TIME_TENSION_INTERVAL_SECONDS).toInt()
}

fun increaseLineTension(current: Int, amount: Int): Int =
    (current + amount).coerceIn(0, MAX_LINE_TENSION)

fun normalizeAnswerForTracking(answer: String): String =
    answer.filterNot { it.isWhitespace() }.lowercase()

data class WrongAnswerUpdate(
    val answers: Set<String>,
    val isDuplicate: Boolean
)

fun registerWrongAnswer(
    existingAnswers: Set<String>,
    answer: String
): WrongAnswerUpdate {
    val normalizedAnswer = normalizeAnswerForTracking(answer)
    val isDuplicate = normalizedAnswer in existingAnswers
    return WrongAnswerUpdate(
        answers = if (isDuplicate) {
            existingAnswers
        } else {
            existingAnswers + normalizedAnswer
        },
        isDuplicate = isDuplicate
    )
}

fun reelFish(
    state: FishState,
    totalRotation: Int
): FishState {
    val reelDamage =
        maxOf(1, totalRotation / 5)

    val newHp = maxOf(
        0,
        state.currentHp - reelDamage
    )

    val hpRate =
        newHp.toFloat() / state.startHp

    val newDistance =
        state.startDistance * hpRate

    return state.copy(
        currentHp = newHp,
        currentDistance = newDistance
    )
}

fun filterWordsByLevel(
    words: List<Word>,
    selectedLevel: Int
): List<Word> {
    return words.filter { word ->
        word.level == selectedLevel
    }
}

fun filterWordsBySchoolGrade(
    words: List<Word>,
    schoolGrade: SchoolGrade
): List<Word> = words.filter { word ->
    word.schoolGrade == schoolGrade
}

fun filterWordsByEikenLevel(
    words: List<Word>,
    eikenLevel: EikenLevel
): List<Word> = words.filter { word ->
    word.eikenLevel == eikenLevel
}

fun advanceQuestion(
    words: List<Word>,
    currentIndex: Int,
    random: Random = Random.Default
): QuestionProgress {
    require(words.isNotEmpty()) {
        "出題する単語がありません"
    }

    if (currentIndex < words.lastIndex) {
        return QuestionProgress(
            words = words,
            currentIndex = currentIndex + 1
        )
    }

    val shuffledWords = words.shuffled(random).toMutableList()
    if (
        shuffledWords.size > 1 &&
        shuffledWords.first() == words[currentIndex]
    ) {
        val firstWord = shuffledWords[0]
        shuffledWords[0] = shuffledWords[1]
        shuffledWords[1] = firstWord
    }

    return QuestionProgress(
        words = shuffledWords,
        currentIndex = 0
    )
}

data class FishCatchUpdate(
    val records: Map<String, FishCollectionRecord>,
    val isNewDiscovery: Boolean,
    val isNewLargestSize: Boolean
)

fun recordFishCatch(
    records: Map<String, FishCollectionRecord>,
    fishState: FishState,
    caughtAtEpochMillis: Long,
    mapId: String? = null,
    pointId: String? = null
): FishCatchUpdate {
    val fishName = fishState.fish.name
    val previous = records[fishName]
    val isNewDiscovery = (previous?.caughtCount ?: 0) == 0
    val isNewLargestSize = fishState.sizeCm >
        (previous?.largestSizeCm ?: 0f)
    val updated = FishCollectionRecord(
        caughtCount = (previous?.caughtCount ?: 0) + 1,
        largestSizeCm = maxOf(
            previous?.largestSizeCm ?: 0f,
            fishState.sizeCm
        ),
        firstCaughtAtEpochMillis = previous
            ?.firstCaughtAtEpochMillis
            ?.takeIf { it > 0L }
            ?: caughtAtEpochMillis,
        lastCaughtAtEpochMillis = caughtAtEpochMillis,
        firstCaughtMapId = previous?.firstCaughtMapId ?: mapId,
        firstCaughtPointId = previous?.firstCaughtPointId ?: pointId,
        lastCaughtMapId = mapId,
        lastCaughtPointId = pointId
    )
    return FishCatchUpdate(
        records = records + (fishName to updated),
        isNewDiscovery = isNewDiscovery,
        isNewLargestSize = isNewLargestSize
    )
}

fun createIncorrectAnswer(correctAnswer: String): String =
    if (correctAnswer.equals("wrong", ignoreCase = true)) {
        "incorrect"
    } else {
        "wrong"
    }
