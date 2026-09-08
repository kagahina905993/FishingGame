package com.example.fishinggame

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LearningRepositoryInstrumentedTest {
    private lateinit var database: LearningDatabase
    private lateinit var repository: LearningRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            LearningDatabase::class.java
        ).build()
        repository = LearningRepository(
            database,
            LearningScheduler(
                LearningAlgorithmConfig(
                    AssistedRecallPolicy.KEEP_SHORT_TERM_REVIEW
                )
            )
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun recordAttempt_persistsEventAndDerivedStateAtomically() = runBlocking {
        val attempt = testAttempt()

        val updated = repository.recordAttempt(attempt)
        val restored = repository.getState(attempt.learningItemId)

        assertEquals(1, repository.countAttempts())
        assertEquals(updated, restored)
        assertEquals(ReviewStage.DAY_1, restored?.stage)
        assertNotNull(restored?.nextFormalReviewAtEpochMillis)
    }

    @Test
    fun duplicateAttemptId_rollsBackStateUpdate() = runBlocking {
        val attempt = testAttempt()
        repository.recordAttempt(attempt)

        runCatching {
            repository.recordAttempt(
                attempt.copy(occurredAtEpochMillis = 60_000L)
            )
        }

        val restored = repository.getState(attempt.learningItemId)
        assertEquals(1, repository.countAttempts())
        assertEquals(1, restored?.independentRecallCount)
    }

    @Test
    fun failedItem_becomesShortTermEligibleAfterTenMinutes() = runBlocking {
        repository.recordAttempt(
            testAttempt(outcome = RecallOutcome.FAILED)
        )

        assertEquals(
            0,
            repository.getReviewCandidates(9 * MINUTE).size
        )
        assertEquals(
            listOf("ngsl_1_2:0001:ja-en"),
            repository.getReviewCandidates(10 * MINUTE)
                .map { it.learningItemId }
        )
    }

    @Test
    fun failedItem_becomesShortTermEligibleAfterFourDifferentItems() =
        runBlocking {
            repository.recordAttempt(
                testAttempt(outcome = RecallOutcome.FAILED)
            )
            (2..5).forEach { rank ->
                repository.recordAttempt(
                    testAttempt(
                        attemptId = "attempt-$rank",
                        rank = rank,
                        occurredAt = rank * 1_000L
                    )
                )
            }

            assertEquals(
                true,
                repository.getReviewCandidates(5_000L).any {
                    it.learningItemId == "ngsl_1_2:0001:ja-en"
                }
            )
        }

    @Test
    fun laterCorrectSubmission_isStoredAsAssistedWithoutSecondTransition() =
        runBlocking {
            val attempt = testAttempt(outcome = RecallOutcome.FAILED)
            repository.recordAttempt(attempt)
            repository.recordAdditionalSubmission(
                attemptId = attempt.attemptId,
                wasCorrect = true,
                usedHint = true,
                occurredAtEpochMillis = 60_000L
            )

            val restoredAttempt = repository.getRecentAttempts(
                attempt.learningItemId,
                limit = 1
            ).single()
            val restoredState = repository.getState(attempt.learningItemId)
            assertEquals(RecallOutcome.FAILED, restoredAttempt.outcome)
            assertEquals(
                RecallOutcome.ASSISTED,
                restoredAttempt.completionOutcome
            )
            assertEquals(2, restoredAttempt.submissionCount)
            assertEquals(true, restoredAttempt.usedHint)
            assertEquals(1, restoredState?.failedRecallCount)
            assertEquals(0, restoredState?.assistedRecallCount)
        }

    private fun testAttempt(
        attemptId: String = "attempt-1",
        rank: Int = 1,
        occurredAt: Long = 0L,
        outcome: RecallOutcome = RecallOutcome.INDEPENDENT
    ): RecallAttempt {
        val wordId = "ngsl_1_2:${rank.toString().padStart(4, '0')}"
        return RecallAttempt(
        attemptId = attemptId,
        learningItemId = "$wordId:ja-en",
        wordId = wordId,
        direction = LearningDirection.JAPANESE_TO_ENGLISH,
        questionFormat = QuestionFormat.FULL_INPUT,
        outcome = outcome,
        occurredAtEpochMillis = occurredAt,
        responseTimeMillis = 1_000L,
        gameMode = "instrumented-test"
    )
    }

    private companion object {
        const val MINUTE = 60L * 1000L
    }
}
