package com.example.fishinggame

import androidx.room.withTransaction
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LearningRepository(
    private val database: LearningDatabase,
    private val scheduler: LearningScheduler
) {
    private val dao = database.learningDao()
    private val writeMutex = Mutex()

    suspend fun recordAttempt(attempt: RecallAttempt): LearningState =
        writeMutex.withLock {
            database.withTransaction {
                val previous = dao.getState(attempt.learningItemId)?.toDomain()
                val updated = scheduler.applyAttempt(previous, attempt)
                dao.insertAttempt(attempt.toEntity())
                dao.upsertState(updated.toEntity())
                updated
            }
        }

    suspend fun recordAdditionalSubmission(
        attemptId: String,
        wasCorrect: Boolean,
        usedHint: Boolean,
        occurredAtEpochMillis: Long
    ) = writeMutex.withLock {
        val updatedRows = dao.recordAdditionalSubmission(
            attemptId = attemptId,
            wasCorrect = wasCorrect,
            usedHint = usedHint,
            occurredAtEpochMillis = occurredAtEpochMillis
        )
        require(updatedRows == 1) { "回答対象の履歴が見つかりません" }
    }

    suspend fun getState(learningItemId: String): LearningState? =
        dao.getState(learningItemId)?.toDomain()

    suspend fun getAllStates(): List<LearningState> =
        dao.getAllStates().map { it.toDomain() }

    suspend fun getRecentAttempts(
        learningItemId: String,
        limit: Int = 3
    ): List<RecallAttempt> {
        require(limit > 0) { "履歴件数は1件以上にしてください" }
        return dao.getRecentAttempts(learningItemId, limit).map {
            it.toDomain()
        }
    }

    suspend fun isWeak(learningItemId: String): Boolean =
        isWeakLearningItem(getRecentAttempts(learningItemId))

    suspend fun getReviewCandidates(
        nowEpochMillis: Long
    ): List<LearningState> = getReviewCandidateSets(nowEpochMillis).all

    suspend fun getReviewCandidateSets(
        nowEpochMillis: Long
    ): ReviewCandidateSets {
        val shortTerm = dao.getEligibleShortTermReviewStates(
            nowEpochMillis = nowEpochMillis,
            fallbackDelayMillis =
                scheduler.config.shortTermFallbackDelayMillis,
            minimumOtherItems =
                scheduler.config.shortTermMinimumOtherItems
        )
        val formal = dao.getFormalReviewCandidates(nowEpochMillis)
        val shortTermStates = shortTerm.map { it.toDomain() }
        val formalStates = formal.map { it.toDomain() }
        return ReviewCandidateSets(
            shortTerm = shortTermStates,
            formal = formalStates,
            all = (shortTermStates + formalStates)
            .distinctBy { it.learningItemId }
        )
    }

    suspend fun countAttempts(): Int = dao.countAttempts()

    suspend fun countAssistedCompletions(): Int =
        dao.countAssistedCompletions()
}

private fun RecallAttempt.toEntity(): LearningAttemptEntity =
    LearningAttemptEntity(
        attemptId = attemptId,
        learningItemId = learningItemId,
        wordId = wordId,
        direction = direction.name,
        questionFormat = questionFormat.name,
        outcome = outcome.name,
        occurredAtEpochMillis = occurredAtEpochMillis,
        responseTimeMillis = responseTimeMillis,
        usedHint = usedHint,
        gameMode = gameMode,
        courseId = courseId,
        reviewPurpose = reviewPurpose.name,
        completionOutcome = completionOutcome?.name,
        completedAtEpochMillis = completedAtEpochMillis,
        submissionCount = submissionCount,
        algorithmVersion = algorithmVersion
    )

private fun LearningAttemptEntity.toDomain(): RecallAttempt =
    RecallAttempt(
        attemptId = attemptId,
        learningItemId = learningItemId,
        wordId = wordId,
        direction = enumValueOf(direction),
        questionFormat = enumValueOf(questionFormat),
        outcome = enumValueOf(outcome),
        occurredAtEpochMillis = occurredAtEpochMillis,
        responseTimeMillis = responseTimeMillis,
        usedHint = usedHint,
        gameMode = gameMode,
        courseId = courseId,
        reviewPurpose = enumValueOf(reviewPurpose),
        completionOutcome = completionOutcome?.let {
            enumValueOf<RecallOutcome>(it)
        },
        completedAtEpochMillis = completedAtEpochMillis,
        submissionCount = submissionCount,
        algorithmVersion = algorithmVersion
    )

data class ReviewCandidateSets(
    val shortTerm: List<LearningState>,
    val formal: List<LearningState>,
    val all: List<LearningState>
)

private fun LearningState.toEntity(): LearningStateEntity =
    LearningStateEntity(
        learningItemId = learningItemId,
        wordId = wordId,
        direction = direction.name,
        status = status.name,
        stage = stage.name,
        independentRecallCount = independentRecallCount,
        assistedRecallCount = assistedRecallCount,
        failedRecallCount = failedRecallCount,
        lastAnsweredAtEpochMillis = lastAnsweredAtEpochMillis,
        lastCorrectAtEpochMillis = lastCorrectAtEpochMillis,
        lastIndependentRecallAtEpochMillis =
            lastIndependentRecallAtEpochMillis,
        lastFormalSuccessAtEpochMillis =
            lastFormalSuccessAtEpochMillis,
        lastFailedAtEpochMillis = lastFailedAtEpochMillis,
        intervalAnchorAtEpochMillis = intervalAnchorAtEpochMillis,
        nextFormalReviewAtEpochMillis = nextFormalReviewAtEpochMillis,
        needsShortTermReview = needsShortTermReview,
        recentOutcomeCodes = recentOutcomes.joinToString(",") { it.name },
        algorithmVersion = algorithmVersion
    )

private fun LearningStateEntity.toDomain(): LearningState =
    LearningState(
        learningItemId = learningItemId,
        wordId = wordId,
        direction = enumValueOf(direction),
        status = enumValueOf(status),
        stage = enumValueOf(stage),
        independentRecallCount = independentRecallCount,
        assistedRecallCount = assistedRecallCount,
        failedRecallCount = failedRecallCount,
        lastAnsweredAtEpochMillis = lastAnsweredAtEpochMillis,
        lastCorrectAtEpochMillis = lastCorrectAtEpochMillis,
        lastIndependentRecallAtEpochMillis =
            lastIndependentRecallAtEpochMillis,
        lastFormalSuccessAtEpochMillis =
            lastFormalSuccessAtEpochMillis,
        lastFailedAtEpochMillis = lastFailedAtEpochMillis,
        intervalAnchorAtEpochMillis = intervalAnchorAtEpochMillis,
        nextFormalReviewAtEpochMillis = nextFormalReviewAtEpochMillis,
        needsShortTermReview = needsShortTermReview,
        recentOutcomes = recentOutcomeCodes
            .split(',')
            .filter { it.isNotBlank() }
            .map { enumValueOf<RecallOutcome>(it) },
        algorithmVersion = algorithmVersion
    )
