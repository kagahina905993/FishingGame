package com.example.fishinggame

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningSchedulerTest {
    private val scheduler = LearningScheduler(
        LearningAlgorithmConfig(
            assistedRecallPolicy =
                AssistedRecallPolicy.KEEP_SHORT_TERM_REVIEW
        )
    )

    @Test
    fun firstIndependentRecall_startsOneDayStage() {
        val state = scheduler.applyAttempt(null, attempt(at = 0L))

        assertEquals(LearningStatus.LEARNING, state.status)
        assertEquals(ReviewStage.DAY_1, state.stage)
        assertEquals(1, state.independentRecallCount)
        assertEquals(DAY, state.nextFormalReviewAtEpochMillis)
        assertEquals(0L, state.lastFormalSuccessAtEpochMillis)
    }

    @Test
    fun shortIntervalSuccesses_doNotAdvanceOrResetFormalClock() {
        val first = scheduler.applyAttempt(null, attempt(at = 0L))
        val second = scheduler.applyAttempt(
            first,
            attempt(id = "2", at = 5 * MINUTE)
        )
        val third = scheduler.applyAttempt(
            second,
            attempt(id = "3", at = 10 * MINUTE)
        )

        assertEquals(3, third.independentRecallCount)
        assertEquals(ReviewStage.DAY_1, third.stage)
        assertEquals(0L, third.lastFormalSuccessAtEpochMillis)
        assertEquals(DAY, third.nextFormalReviewAtEpochMillis)
    }

    @Test
    fun fullOneThreeSevenDaySequence_becomesMastered() {
        val day1 = scheduler.applyAttempt(null, attempt(at = 0L))
        val day3 = scheduler.applyAttempt(
            day1,
            attempt(id = "2", at = DAY)
        )
        val day7 = scheduler.applyAttempt(
            day3,
            attempt(id = "3", at = DAY + 3 * DAY)
        )
        val mastered = scheduler.applyAttempt(
            day7,
            attempt(id = "4", at = DAY + 3 * DAY + 7 * DAY)
        )

        assertEquals(ReviewStage.DAY_3, day3.stage)
        assertEquals(ReviewStage.DAY_7, day7.stage)
        assertEquals(LearningStatus.MASTERED, mastered.status)
        assertEquals(ReviewStage.MASTERED_DAY_14, mastered.stage)
    }

    @Test
    fun overdueSuccess_advancesOnlyOneStageWithoutPenalty() {
        val first = scheduler.applyAttempt(null, attempt(at = 0L))
        val late = scheduler.applyAttempt(
            first,
            attempt(id = "2", at = 100 * DAY)
        )

        assertEquals(LearningStatus.LEARNING, late.status)
        assertEquals(ReviewStage.DAY_3, late.stage)
        assertEquals(103 * DAY, late.nextFormalReviewAtEpochMillis)
    }

    @Test
    fun clockMovingBackward_doesNotLoseAnswerOrCreateTimeProgress() {
        val first = scheduler.applyAttempt(null, attempt(at = DAY))
        val clockMovedBackward = scheduler.applyAttempt(
            first,
            attempt(id = "2", at = 0L)
        )

        assertEquals(2, clockMovedBackward.independentRecallCount)
        assertEquals(ReviewStage.DAY_1, clockMovedBackward.stage)
        assertEquals(DAY, clockMovedBackward.lastAnsweredAtEpochMillis)
        assertEquals(2 * DAY,
            clockMovedBackward.nextFormalReviewAtEpochMillis)
    }

    @Test
    fun failure_movesBackOneStageAndUsesFailureAsIntervalAnchor() {
        val day7 = stateAtDay7()
        val failedAt = 5 * DAY
        val failed = scheduler.applyAttempt(
            day7,
            attempt(
                id = "failed",
                at = failedAt,
                outcome = RecallOutcome.FAILED
            )
        )

        assertEquals(ReviewStage.DAY_3, failed.stage)
        assertEquals(failedAt, failed.intervalAnchorAtEpochMillis)
        assertEquals(failedAt + 3 * DAY,
            failed.nextFormalReviewAtEpochMillis)
        assertEquals(day7.lastFormalSuccessAtEpochMillis,
            failed.lastFormalSuccessAtEpochMillis)
        assertTrue(failed.needsShortTermReview)
    }

    @Test
    fun successSoonAfterFailure_isShortTermOnly() {
        val failed = scheduler.applyAttempt(
            stateAtDay7(),
            attempt(
                id = "failed",
                at = 5 * DAY,
                outcome = RecallOutcome.FAILED
            )
        )
        val relearned = scheduler.applyAttempt(
            failed,
            attempt(id = "relearned", at = 5 * DAY + 5 * MINUTE)
        )

        assertEquals(ReviewStage.DAY_3, relearned.stage)
        assertFalse(relearned.needsShortTermReview)
        assertEquals(8 * DAY, relearned.nextFormalReviewAtEpochMillis)
        assertEquals(failed.lastFormalSuccessAtEpochMillis,
            relearned.lastFormalSuccessAtEpochMillis)
    }

    @Test
    fun shortTermPurpose_neverAdvancesFormalStageEvenWhenFormalDue() {
        val failed = scheduler.applyAttempt(
            null,
            attempt(at = 0L, outcome = RecallOutcome.FAILED)
        )
        val shortTermSuccess = scheduler.applyAttempt(
            failed,
            attempt(id = "short-term", at = DAY).copy(
                reviewPurpose = ReviewPurpose.SHORT_TERM
            )
        )

        assertFalse(shortTermSuccess.needsShortTermReview)
        assertEquals(ReviewStage.DAY_1, shortTermSuccess.stage)
        assertEquals(DAY, shortTermSuccess.nextFormalReviewAtEpochMillis)
        assertNull(shortTermSuccess.lastFormalSuccessAtEpochMillis)
    }

    @Test
    fun firstFailure_startsLearningWithoutInventingSuccess() {
        val failed = scheduler.applyAttempt(
            null,
            attempt(at = 0L, outcome = RecallOutcome.FAILED)
        )

        assertEquals(LearningStatus.LEARNING, failed.status)
        assertEquals(ReviewStage.DAY_1, failed.stage)
        assertEquals(DAY, failed.nextFormalReviewAtEpochMillis)
        assertNull(failed.lastFormalSuccessAtEpochMillis)
    }

    @Test
    fun masteredFailure_returnsToLearningAndCanBeMasteredAgain() {
        val mastered = stateAtMastered()
        val failedAt = 12 * DAY
        val failed = scheduler.applyAttempt(
            mastered,
            attempt(
                id = "failed",
                at = failedAt,
                outcome = RecallOutcome.FAILED
            )
        )
        val day7 = scheduler.applyAttempt(
            failed,
            attempt(id = "recover-1", at = failedAt + 3 * DAY)
        )
        val masteredAgain = scheduler.applyAttempt(
            day7,
            attempt(id = "recover-2", at = failedAt + 10 * DAY)
        )

        assertEquals(LearningStatus.LEARNING, failed.status)
        assertEquals(ReviewStage.DAY_3, failed.stage)
        assertEquals(ReviewStage.DAY_7, day7.stage)
        assertEquals(LearningStatus.MASTERED, masteredAgain.status)
    }

    @Test
    fun masteredReview_movesFromFourteenToRepeatingThirtyDays() {
        val mastered = stateAtMastered()
        val fourteenDayReviewAt =
            requireNotNull(mastered.nextFormalReviewAtEpochMillis)
        val thirtyDay = scheduler.applyAttempt(
            mastered,
            attempt(id = "14-day", at = fourteenDayReviewAt)
        )
        val thirtyDayReviewAt =
            requireNotNull(thirtyDay.nextFormalReviewAtEpochMillis)
        val repeated = scheduler.applyAttempt(
            thirtyDay,
            attempt(id = "30-day", at = thirtyDayReviewAt)
        )

        assertEquals(ReviewStage.MASTERED_DAY_30, thirtyDay.stage)
        assertEquals(ReviewStage.MASTERED_DAY_30, repeated.stage)
        assertEquals(thirtyDayReviewAt + 30 * DAY,
            repeated.nextFormalReviewAtEpochMillis)
    }

    @Test
    fun assistedRecall_neverAdvancesFormalStage() {
        val first = scheduler.applyAttempt(null, attempt(at = 0L))
        val assisted = scheduler.applyAttempt(
            first,
            attempt(
                id = "assisted",
                at = DAY,
                outcome = RecallOutcome.ASSISTED,
                usedHint = true
            )
        )

        assertEquals(ReviewStage.DAY_1, assisted.stage)
        assertEquals(1, assisted.assistedRecallCount)
        assertEquals(0L, assisted.lastFormalSuccessAtEpochMillis)
    }

    @Test
    fun approvedAssistedPolicy_clearsOnlyShortTermReview() {
        val clearScheduler = LearningScheduler(
            LearningAlgorithmConfig(
                AssistedRecallPolicy.CLEAR_SHORT_TERM_REVIEW
            )
        )
        val failed = clearScheduler.applyAttempt(
            null,
            attempt(at = 0L, outcome = RecallOutcome.FAILED)
        )
        val assisted = clearScheduler.applyAttempt(
            failed,
            attempt(
                id = "assisted",
                at = 5 * MINUTE,
                outcome = RecallOutcome.ASSISTED,
                usedHint = true
            )
        )

        assertFalse(assisted.needsShortTermReview)
        assertEquals(ReviewStage.DAY_1, assisted.stage)
        assertEquals(DAY, assisted.nextFormalReviewAtEpochMillis)
        assertEquals(failed.lastFormalSuccessAtEpochMillis,
            assisted.lastFormalSuccessAtEpochMillis)
    }

    @Test
    fun stateWeakFlag_usesThreeMostRecentOutcomes() {
        val firstFailure = scheduler.applyAttempt(
            null,
            attempt(at = 0L, outcome = RecallOutcome.FAILED)
        )
        val success = scheduler.applyAttempt(
            firstFailure,
            attempt(id = "2", at = MINUTE)
        )
        val secondFailure = scheduler.applyAttempt(
            success,
            attempt(
                id = "3",
                at = 2 * MINUTE,
                outcome = RecallOutcome.FAILED
            )
        )

        assertTrue(secondFailure.isWeak)
    }

    @Test
    fun weakRule_usesThreeMostRecentCompletedAttempts() {
        val weak = listOf(
            attempt(id = "1", at = 1L),
            attempt(id = "2", at = 2L, outcome = RecallOutcome.FAILED),
            attempt(id = "3", at = 3L, outcome = RecallOutcome.FAILED)
        )
        val improving = listOf(
            attempt(id = "1", at = 1L, outcome = RecallOutcome.FAILED),
            attempt(id = "2", at = 2L),
            attempt(id = "3", at = 3L)
        )

        assertTrue(isWeakLearningItem(weak))
        assertFalse(isWeakLearningItem(improving))
        assertFalse(isWeakLearningItem(weak.take(2)))
    }

    @Test
    fun masteryRate_usesOnlyRequestedLearningItems() {
        val mastered = stateAtMastered()
        val learning = LearningState(
            learningItemId = "nawl_1_2:0001:ja-en",
            wordId = "nawl_1_2:0001",
            direction = LearningDirection.JAPANESE_TO_ENGLISH,
            status = LearningStatus.LEARNING
        )

        assertEquals(
            50,
            masteryRatePercent(
                listOf(mastered, learning),
                setOf(mastered.learningItemId, learning.learningItemId)
            )
        )
        assertEquals(100, masteryRatePercent(
            listOf(mastered, learning),
            setOf(mastered.learningItemId)
        ))
    }

    private fun stateAtDay7(): LearningState {
        val first = scheduler.applyAttempt(null, attempt(at = 0L))
        val second = scheduler.applyAttempt(
            first,
            attempt(id = "day-1", at = DAY)
        )
        return scheduler.applyAttempt(
            second,
            attempt(id = "day-3", at = 4 * DAY)
        )
    }

    private fun stateAtMastered(): LearningState = scheduler.applyAttempt(
        stateAtDay7(),
        attempt(id = "day-7", at = 11 * DAY)
    )

    private fun attempt(
        id: String = "1",
        at: Long,
        outcome: RecallOutcome = RecallOutcome.INDEPENDENT,
        usedHint: Boolean = false
    ) = RecallAttempt(
        attemptId = id,
        learningItemId = "ngsl_1_2:0001:ja-en",
        wordId = "ngsl_1_2:0001",
        direction = LearningDirection.JAPANESE_TO_ENGLISH,
        questionFormat = QuestionFormat.FULL_INPUT,
        outcome = outcome,
        occurredAtEpochMillis = at,
        responseTimeMillis = 1_000L,
        usedHint = usedHint,
        gameMode = "test"
    )

    private companion object {
        const val MINUTE = 60L * 1000L
        const val DAY = 24L * 60L * MINUTE
    }
}
