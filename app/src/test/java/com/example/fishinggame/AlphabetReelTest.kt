package com.example.fishinggame

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AlphabetReelTest {
    @Test
    fun alphabetLetterAfterSteps_movesClockwiseAndWraps() {
        assertEquals('A', alphabetLetterAfterSteps('A', 0))
        assertEquals('B', alphabetLetterAfterSteps('A', 1))
        assertEquals('A', alphabetLetterAfterSteps('Z', 1))
        assertEquals('A', alphabetLetterAfterSteps('A', 26))
        assertEquals('F', alphabetLetterAfterSteps('C', 29))
    }

    @Test
    fun alphabetLetterAfterSteps_rejectsInvalidInputs() {
        assertThrows(IllegalArgumentException::class.java) {
            alphabetLetterAfterSteps('a', 1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            alphabetLetterAfterSteps('A', -1)
        }
    }

    @Test
    fun reelAnimationSchedule_spinsTogetherAndStopsFromLeftToRight() {
        assertEquals(0L, reelAnimationDurationMillis(emptyList()))
        assertEquals(625L, reelAnimationDurationMillis(listOf(1)))
        assertEquals(1_025L, reelAnimationDurationMillis(listOf(4, 10, 3)))
        assertEquals(625L, reelAnimationDurationMillis(listOf(26)))

        val schedule = reelAnimationSchedule(listOf(1, 8, 26, 3))
        schedule.zipWithNext().forEach { (left, right) ->
            assertEquals(
                200,
                right.durationMillis - left.durationMillis
            )
        }
        assertEquals(27, schedule.first().animationSteps)
        assertEquals(29, schedule.last().animationSteps)
        assertEquals(
            1_975L,
            reelAnswerAnimationDurationMillis(listOf(4, 10, 3))
        )
    }

    @Test
    fun reelLetterMatch_checksOnlyTheStoppedLetter() {
        assertEquals(true, reelLetterMatchesAnswer("fish", "FISH", 0))
        assertEquals(true, reelLetterMatchesAnswer("fish", "FISH", 3))
        assertEquals(false, reelLetterMatchesAnswer("dish", "fish", 0))
        assertEquals(false, reelLetterMatchesAnswer("fi", "fish", 2))
    }
}
