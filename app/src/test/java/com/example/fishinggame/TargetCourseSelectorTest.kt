package com.example.fishinggame

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TargetCourseSelectorTest {
    @Test
    fun targetCourse_usesApprovedMixAndExcludesHigherLevels() {
        val words = buildList {
            addAll(wordsFor(EikenLevel.GRADE_2, 75, 1))
            addAll(wordsFor(EikenLevel.GRADE_PRE_2_PLUS, 30, 101))
            addAll(wordsFor(EikenLevel.GRADE_PRE_2, 20, 201))
            addAll(wordsFor(EikenLevel.GRADE_3, 20, 301))
            addAll(wordsFor(EikenLevel.GRADE_4, 20, 401))
            addAll(wordsFor(EikenLevel.GRADE_5, 20, 501))
            addAll(wordsFor(EikenLevel.GRADE_PRE_1, 20, 601))
        }

        val course = buildTargetCourseWords(
            words = words,
            targetLevel = EikenLevel.GRADE_2,
            random = Random(1)
        )

        assertEquals(75, course.count { it.eikenLevel == EikenLevel.GRADE_2 })
        assertEquals(
            15,
            course.count { it.eikenLevel == EikenLevel.GRADE_PRE_2_PLUS }
        )
        assertEquals(
            10,
            course.count {
                it.eikenLevel in setOf(
                    EikenLevel.GRADE_PRE_2,
                    EikenLevel.GRADE_3,
                    EikenLevel.GRADE_4,
                    EikenLevel.GRADE_5
                )
            }
        )
        assertFalse(course.any { it.eikenLevel == EikenLevel.GRADE_PRE_1 })
        assertEquals(course.size, course.map { it.wordId }.distinct().size)
        assertTrue(
            listOf(
                EikenLevel.GRADE_PRE_2,
                EikenLevel.GRADE_3,
                EikenLevel.GRADE_4,
                EikenLevel.GRADE_5
            ).all { level -> course.any { it.eikenLevel == level } }
        )
        course.chunked(20).forEach { block ->
            assertEquals(
                15,
                block.count { it.eikenLevel == EikenLevel.GRADE_2 }
            )
            assertEquals(
                3,
                block.count {
                    it.eikenLevel == EikenLevel.GRADE_PRE_2_PLUS
                }
            )
            assertEquals(
                2,
                block.count {
                    it.eikenLevel != EikenLevel.GRADE_2 &&
                        it.eikenLevel != EikenLevel.GRADE_PRE_2_PLUS
                }
            )
        }
    }

    @Test
    fun lowestTarget_usesOnlyTargetLevel() {
        val words = wordsFor(EikenLevel.GRADE_5, 20, 1) +
            wordsFor(EikenLevel.GRADE_4, 20, 101)

        val course = buildTargetCourseWords(
            words = words,
            targetLevel = EikenLevel.GRADE_5,
            random = Random(2)
        )

        assertEquals(20, course.size)
        assertTrue(course.all { it.eikenLevel == EikenLevel.GRADE_5 })
    }

    @Test
    fun targetWithoutFoundation_usesTargetAndImmediateLowerOnly() {
        val words = wordsFor(EikenLevel.GRADE_4, 15, 1) +
            wordsFor(EikenLevel.GRADE_5, 10, 101)

        val course = buildTargetCourseWords(
            words = words,
            targetLevel = EikenLevel.GRADE_4,
            random = Random(4)
        )

        assertEquals(18, course.size)
        assertEquals(15, course.count { it.eikenLevel == EikenLevel.GRADE_4 })
        assertEquals(3, course.count { it.eikenLevel == EikenLevel.GRADE_5 })
    }

    @Test
    fun targetWithoutCandidates_returnsEmptyCourse() {
        val course = buildTargetCourseWords(
            words = wordsFor(EikenLevel.GRADE_5, 5, 1),
            targetLevel = EikenLevel.GRADE_1,
            random = Random(5)
        )

        assertTrue(course.isEmpty())
    }

    @Test
    fun foundation_prefersUrgentThenUnseenWords() {
        val targetWords = wordsFor(EikenLevel.GRADE_2, 15, 1)
        val immediateWords =
            wordsFor(EikenLevel.GRADE_PRE_2_PLUS, 3, 101)
        val urgent = wordsFor(EikenLevel.GRADE_5, 1, 201).single()
        val unseen = wordsFor(EikenLevel.GRADE_PRE_2, 1, 202).single()
        val alreadyAttempted =
            wordsFor(EikenLevel.GRADE_4, 1, 203).single()

        val course = buildTargetCourseWords(
            words = targetWords + immediateWords +
                listOf(urgent, unseen, alreadyAttempted),
            targetLevel = EikenLevel.GRADE_2,
            urgentLearningItemIds = setOf(urgent.learningItemId()),
            attemptedLearningItemIds = setOf(
                urgent.learningItemId(),
                alreadyAttempted.learningItemId()
            ),
            random = Random(3)
        )

        assertTrue(urgent in course)
        assertTrue(unseen in course)
        assertFalse(alreadyAttempted in course)
    }

    private fun wordsFor(
        level: EikenLevel,
        count: Int,
        firstRank: Int
    ): List<Word> = List(count) { index ->
        val rank = firstRank + index
        Word(
            english = "word" + rank,
            japanese = "意味" + rank,
            ngslRank = rank,
            level = 1,
            schoolGrade = SchoolGrade.ELEMENTARY_5,
            eikenLevel = level,
            partOfSpeech = "名詞",
            sfi = null,
            frequencyPerMillion = null,
            translationSource = "test",
            sourceRank = rank,
            quizMeaning = "意味" + rank,
            quizPartOfSpeech = "名詞",
            quizSource = "test"
        )
    }
}
