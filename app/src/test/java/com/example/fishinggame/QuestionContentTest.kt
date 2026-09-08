package com.example.fishinggame

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class QuestionContentTest {
    private fun word(english: String, rank: Int) = Word(
        english = english,
        japanese = english,
        ngslRank = rank,
        level = 1,
        schoolGrade = SchoolGrade.ELEMENTARY_5,
        eikenLevel = EikenLevel.GRADE_5,
        partOfSpeech = "動詞",
        sfi = null,
        frequencyPerMillion = null,
        translationSource = "test",
        sourceRank = rank,
        quizMeaning = english,
        quizPartOfSpeech = "動詞",
        quizSource = "test"
    )

    private val words = listOf(
        word("go", 1),
        word("see", 2),
        word("make", 3),
        word("take", 4)
    )
    private val question = SentenceQuestion(
        questionId = "go-sentence-1",
        wordId = words[0].wordId,
        sentence = "We ___ to the river.",
        japaneseSentence = "私たちは川へ行きます。",
        answer = "go",
        distractorWordIds = words.drop(1).map { it.wordId },
        explanation = "go to ～を使います。",
        source = PROJECT_AUTHORED_QUESTION_SOURCE,
        license = PROJECT_ORIGINAL_QUESTION_LICENSE,
        sourceUrl = null
    )

    @Test
    fun validateSentenceQuestions_acceptsCompleteProjectAuthoredQuestion() {
        assertEquals(
            listOf(question),
            validateSentenceQuestions(listOf(question), words)
        )
    }

    @Test
    fun validateSentenceQuestions_requiresSourceUrlForExternalContent() {
        assertThrows(IllegalArgumentException::class.java) {
            validateSentenceQuestions(
                listOf(question.copy(source = "external-source")),
                words
            )
        }
    }

    @Test
    fun multipleChoiceOptions_containOneAnswerAndThreeDistractors() {
        val options = buildMultipleChoiceOptions(
            question = question,
            words = words,
            random = Random(1)
        )

        assertEquals(4, options.size)
        assertEquals(words.map { it.wordId }.toSet(), options.map { it.wordId }.toSet())
    }

    @Test
    fun questionFormat_fallsBackAndPreventsChoiceOnlyReview() {
        assertEquals(
            QuestionFormat.FULL_INPUT,
            resolveQuestionFormat(
                QuestionMode.SENTENCE_INPUT,
                StudyMode.NORMAL,
                words[0].wordId,
                hasSentenceQuestion = false
            )
        )
        assertEquals(
            QuestionFormat.MULTIPLE_CHOICE,
            resolveQuestionFormat(
                QuestionMode.SENTENCE_MULTIPLE_CHOICE,
                StudyMode.NORMAL,
                words[0].wordId,
                hasSentenceQuestion = true
            )
        )
        assertEquals(
            QuestionFormat.FILL_IN_THE_BLANK,
            resolveQuestionFormat(
                QuestionMode.SENTENCE_MULTIPLE_CHOICE,
                StudyMode.REVIEW,
                words[0].wordId,
                hasSentenceQuestion = true
            )
        )
        assertEquals(
            QuestionPresentation.MEANING_MULTIPLE_CHOICE,
            resolveQuestionPresentation(
                QuestionMode.MEANING_MULTIPLE_CHOICE,
                StudyMode.NORMAL,
                words[0].wordId,
                hasSentenceQuestion = false
            )
        )
        assertEquals(
            QuestionPresentation.WORD_INPUT,
            resolveQuestionPresentation(
                QuestionMode.MEANING_MULTIPLE_CHOICE,
                StudyMode.REVIEW,
                words[0].wordId,
                hasSentenceQuestion = true
            )
        )
    }

    @Test
    fun meaningChoiceOptions_preferValidDistinctCourseWords() {
        val options = buildMeaningChoiceOptions(
            target = words[0],
            courseWords = words,
            random = Random(2)
        )

        assertEquals(4, options.size)
        assertEquals(4, options.map { it.wordId }.distinct().size)
        assertTrue(words[0] in options)
        assertTrue(options.none {
            it != words[0] && it.questionMeaning == words[0].questionMeaning
        })
    }

    @Test
    fun multipleChoiceSuccess_isAssistedRatherThanIndependent() {
        assertEquals(
            RecallOutcome.ASSISTED,
            recallOutcomeForSubmission(
                wasCorrect = true,
                questionFormat = QuestionFormat.MULTIPLE_CHOICE
            )
        )
        assertEquals(
            RecallOutcome.INDEPENDENT,
            recallOutcomeForSubmission(
                wasCorrect = true,
                questionFormat = QuestionFormat.FILL_IN_THE_BLANK
            )
        )
        assertTrue(
            recallOutcomeForSubmission(
                wasCorrect = false,
                questionFormat = QuestionFormat.MULTIPLE_CHOICE
            ) == RecallOutcome.FAILED
        )
    }

    @Test
    fun answerDisplay_capitalizesOnlyWhenBlankStartsSentence() {
        assertEquals(
            "go",
            formatAnswerForSentence("We ___ to the river.", "GO")
        )
        assertEquals(
            "Go",
            formatAnswerForSentence("  ___ to the river now.", "go")
        )
    }
}
