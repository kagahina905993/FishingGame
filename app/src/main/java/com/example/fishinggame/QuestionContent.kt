package com.example.fishinggame

import kotlin.random.Random

enum class QuestionMode(val displayName: String) {
    WORD_INPUT("単語入力"),
    SENTENCE_INPUT("文章入力"),
    SENTENCE_MULTIPLE_CHOICE("文章4択"),
    MEANING_MULTIPLE_CHOICE("意味4択"),
    MIXED("ミックス")
}

fun QuestionMode.requiresSentenceQuestion(): Boolean =
    this == QuestionMode.SENTENCE_INPUT ||
        this == QuestionMode.SENTENCE_MULTIPLE_CHOICE

fun filterWordsForQuestionMode(
    words: List<Word>,
    mode: QuestionMode,
    sentenceWordIds: Set<String>
): List<Word> = if (mode.requiresSentenceQuestion()) {
    words.filter { it.wordId in sentenceWordIds }
} else {
    words
}

enum class QuestionPresentation(
    val questionFormat: QuestionFormat,
    val usesSentence: Boolean
) {
    WORD_INPUT(QuestionFormat.FULL_INPUT, false),
    SENTENCE_INPUT(QuestionFormat.FILL_IN_THE_BLANK, true),
    SENTENCE_MULTIPLE_CHOICE(QuestionFormat.MULTIPLE_CHOICE, true),
    MEANING_MULTIPLE_CHOICE(QuestionFormat.MULTIPLE_CHOICE, false)
}

data class SentenceQuestion(
    val questionId: String,
    val wordId: String,
    val sentence: String,
    val japaneseSentence: String,
    val answer: String,
    val distractorWordIds: List<String>,
    val explanation: String,
    val source: String,
    val license: String,
    val sourceUrl: String?
)

fun validateSentenceQuestions(
    questions: List<SentenceQuestion>,
    words: List<Word>
): List<SentenceQuestion> {
    require(questions.isNotEmpty()) {
        "question_content.jsonに文章問題がありません"
    }
    require(questions.map { it.questionId }.distinct().size == questions.size) {
        "文章問題IDが重複しています"
    }
    require(questions.map { it.wordId }.distinct().size == questions.size) {
        "同じ単語に複数の文章問題が登録されています"
    }
    require(questions.map { it.sentence }.distinct().size == questions.size) {
        "文章問題の英文が重複しています"
    }

    val wordsById = words.associateBy { it.wordId }
    questions.forEach { question ->
        val word = requireNotNull(wordsById[question.wordId]) {
            "文章問題が存在しない単語を参照しています: ${question.wordId}"
        }
        require(question.sentence.windowed(3).count { it == "___" } == 1) {
            "英文の空欄は1つにしてください: ${question.questionId}"
        }
        val trimmedSentence = question.sentence.trimStart()
        require(
            trimmedSentence.startsWith("___") ||
                trimmedSentence.firstOrNull()?.isUpperCase() == true
        ) {
            "英文は文頭を大文字にしてください: ${question.questionId}"
        }
        require(question.sentence.lastOrNull() in setOf('.', '?', '!')) {
            "英文の末尾に句読点が必要です: ${question.questionId}"
        }
        require(question.answer == word.english) {
            "文章問題の答えが見出し語と一致しません: ${question.questionId}"
        }
        require(
            !Regex(
                pattern = "\\b${Regex.escape(question.answer)}\\b",
                option = RegexOption.IGNORE_CASE
            ).containsMatchIn(question.sentence)
        ) {
            "英文中に正解が表示されています: ${question.questionId}"
        }
        require(question.japaneseSentence.isNotBlank()) {
            "文章問題の和文がありません: ${question.questionId}"
        }
        require(question.japaneseSentence.lastOrNull() in setOf('。', '？', '！')) {
            "文章問題の和文末尾に句読点が必要です: ${question.questionId}"
        }
        require(question.explanation.isNotBlank()) {
            "文章問題の解説がありません: ${question.questionId}"
        }
        require(question.explanation.lastOrNull() in setOf('。', '？', '！')) {
            "文章問題の解説末尾に句読点が必要です: ${question.questionId}"
        }
        require(question.distractorWordIds.size == 3) {
            "文章4択の誤答は3つ必要です: ${question.questionId}"
        }
        require(question.distractorWordIds.distinct().size == 3) {
            "文章4択の誤答が重複しています: ${question.questionId}"
        }
        require(question.wordId !in question.distractorWordIds) {
            "文章4択の誤答に正解が含まれています: ${question.questionId}"
        }
        require(question.distractorWordIds.all { it in wordsById }) {
            "文章4択が存在しない誤答単語を参照しています: " +
                question.questionId
        }
        val distractorWords = question.distractorWordIds.mapNotNull(wordsById::get)
        require(
            distractorWords.map { it.questionPartOfSpeech }.distinct().size == 1
        ) {
            "文章4択の誤答候補で品詞が混在しています: ${question.questionId}"
        }
        require(
            (listOf(question.answer) + distractorWords.map { it.english.lowercase() })
                .distinct()
                .size == 4
        ) {
            "文章4択に同じ表示の選択肢があります: ${question.questionId}"
        }
        require(question.distractorWordIds.all {
            (wordsById[it]?.level ?: Int.MAX_VALUE) <= word.level
        }) {
            "文章4択の誤答は正解以下のゲームレベルにしてください: " +
                question.questionId
        }
        require(question.source.isNotBlank() && question.license.isNotBlank()) {
            "文章問題の出典・権利情報が不足しています: ${question.questionId}"
        }
        require(
            question.source == PROJECT_AUTHORED_QUESTION_SOURCE ||
                !question.sourceUrl.isNullOrBlank()
        ) {
            "外部由来の文章問題には出典URLが必要です: ${question.questionId}"
        }
        require(
            question.source != PROJECT_AUTHORED_QUESTION_SOURCE ||
                question.license == PROJECT_ORIGINAL_QUESTION_LICENSE
        ) {
            "独自作成問題の権利表記が不正です: ${question.questionId}"
        }
    }
    return questions
}

const val PROJECT_AUTHORED_QUESTION_SOURCE = "project-authored"
const val PROJECT_ORIGINAL_QUESTION_LICENSE = "project-original"

fun resolveQuestionFormat(
    mode: QuestionMode,
    studyMode: StudyMode,
    wordId: String,
    hasSentenceQuestion: Boolean
): QuestionFormat = resolveQuestionPresentation(
    mode = mode,
    studyMode = studyMode,
    wordId = wordId,
    hasSentenceQuestion = hasSentenceQuestion
).questionFormat

fun resolveQuestionPresentation(
    mode: QuestionMode,
    studyMode: StudyMode,
    wordId: String,
    hasSentenceQuestion: Boolean
): QuestionPresentation {
    if (studyMode != StudyMode.NORMAL) {
        return when (mode) {
            QuestionMode.WORD_INPUT,
            QuestionMode.MEANING_MULTIPLE_CHOICE ->
                QuestionPresentation.WORD_INPUT
            QuestionMode.SENTENCE_INPUT,
            QuestionMode.SENTENCE_MULTIPLE_CHOICE -> if (hasSentenceQuestion) {
                QuestionPresentation.SENTENCE_INPUT
            } else {
                QuestionPresentation.WORD_INPUT
            }
            QuestionMode.MIXED -> if (
                hasSentenceQuestion && wordId.hashCode() % 2 != 0
            ) {
                QuestionPresentation.SENTENCE_INPUT
            } else {
                QuestionPresentation.WORD_INPUT
            }
        }
    }
    return when (mode) {
        QuestionMode.WORD_INPUT -> QuestionPresentation.WORD_INPUT
        QuestionMode.SENTENCE_INPUT -> if (hasSentenceQuestion) {
            QuestionPresentation.SENTENCE_INPUT
        } else {
            QuestionPresentation.WORD_INPUT
        }
        QuestionMode.SENTENCE_MULTIPLE_CHOICE -> if (hasSentenceQuestion) {
            QuestionPresentation.SENTENCE_MULTIPLE_CHOICE
        } else {
            QuestionPresentation.WORD_INPUT
        }
        QuestionMode.MEANING_MULTIPLE_CHOICE ->
            QuestionPresentation.MEANING_MULTIPLE_CHOICE
        QuestionMode.MIXED -> when (Math.floorMod(wordId.hashCode(), 4)) {
            0 -> QuestionPresentation.WORD_INPUT
            1 -> QuestionPresentation.MEANING_MULTIPLE_CHOICE
            2 -> if (hasSentenceQuestion) {
                QuestionPresentation.SENTENCE_INPUT
            } else {
                QuestionPresentation.WORD_INPUT
            }
            else -> if (hasSentenceQuestion) {
                QuestionPresentation.SENTENCE_MULTIPLE_CHOICE
            } else {
                QuestionPresentation.MEANING_MULTIPLE_CHOICE
            }
        }
    }
}

fun buildMultipleChoiceOptions(
    question: SentenceQuestion,
    words: List<Word>,
    random: Random = Random.Default
): List<Word> {
    val wordsById = words.associateBy { it.wordId }
    return (listOf(question.wordId) + question.distractorWordIds)
        .mapNotNull(wordsById::get)
        .shuffled(random)
}

fun buildMeaningChoiceOptions(
    target: Word,
    courseWords: List<Word>,
    random: Random = Random.Default
): List<Word> {
    val validCandidates = courseWords.filter {
        it.wordId != target.wordId &&
            it.questionMeaning != target.questionMeaning
    }
    val samePartOfSpeech = validCandidates.filter {
        it.questionPartOfSpeech == target.questionPartOfSpeech
    }.shuffled(random)
    val sameLevel = validCandidates.filter {
        it.level == target.level && it !in samePartOfSpeech
    }.shuffled(random)
    val remaining = validCandidates.filter {
        it !in samePartOfSpeech && it !in sameLevel
    }.shuffled(random)
    val distractors = (samePartOfSpeech + sameLevel + remaining).take(3)
    require(distractors.size == 3) {
        "意味4択の誤答候補が不足しています: ${target.wordId}"
    }
    return (distractors + target).shuffled(random)
}

fun formatAnswerForSentence(
    sentence: String,
    answer: String
): String {
    val lowercaseAnswer = answer.lowercase()
    return if (sentence.trimStart().startsWith("___")) {
        lowercaseAnswer.replaceFirstChar { it.uppercaseChar() }
    } else {
        lowercaseAnswer
    }
}
