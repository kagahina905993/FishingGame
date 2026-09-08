package com.example.fishinggame

class LearningScheduler(
    val config: LearningAlgorithmConfig
) {
    fun applyAttempt(
        previous: LearningState?,
        attempt: RecallAttempt
    ): LearningState {
        require(attempt.algorithmVersion == LEARNING_ALGORITHM_VERSION) {
            "未対応の学習アルゴリズムです: ${attempt.algorithmVersion}"
        }
        require(previous == null ||
            previous.learningItemId == attempt.learningItemId) {
            "LearningItemが一致しません"
        }
        require(previous == null || previous.wordId == attempt.wordId) {
            "Word IDが一致しません"
        }
        val initialState = previous ?: LearningState(
            learningItemId = attempt.learningItemId,
            wordId = attempt.wordId,
            direction = attempt.direction
        )
        val state = initialState.copy(
            recentOutcomes = (
                listOf(attempt.outcome) + initialState.recentOutcomes
            ).take(3)
        )
        val effectiveOccurredAt = maxOf(
            attempt.occurredAtEpochMillis,
            initialState.lastAnsweredAtEpochMillis ?: Long.MIN_VALUE
        )

        return when (attempt.outcome) {
            RecallOutcome.INDEPENDENT ->
                applyIndependentRecall(
                    state,
                    effectiveOccurredAt,
                    attempt.reviewPurpose
                )
            RecallOutcome.ASSISTED ->
                applyAssistedRecall(state, effectiveOccurredAt)
            RecallOutcome.FAILED ->
                applyFailedRecall(state, effectiveOccurredAt)
        }
    }

    private fun applyIndependentRecall(
        state: LearningState,
        occurredAt: Long,
        reviewPurpose: ReviewPurpose
    ): LearningState {
        val common = state.copy(
            independentRecallCount = state.independentRecallCount + 1,
            lastAnsweredAtEpochMillis = occurredAt,
            lastCorrectAtEpochMillis = occurredAt,
            lastIndependentRecallAtEpochMillis = occurredAt,
            needsShortTermReview = false
        )

        if (state.status == LearningStatus.UNLEARNED) {
            return common.advanceTo(
                status = LearningStatus.LEARNING,
                stage = ReviewStage.DAY_1,
                occurredAt = occurredAt
            )
        }


        if (reviewPurpose == ReviewPurpose.SHORT_TERM) {
            return common
        }

        if (!state.isFormalReviewAvailable(occurredAt)) {
            return common
        }

        val (nextStatus, nextStage) = when (state.stage) {
            ReviewStage.NOT_STARTED ->
                LearningStatus.LEARNING to ReviewStage.DAY_1
            ReviewStage.DAY_1 ->
                LearningStatus.LEARNING to ReviewStage.DAY_3
            ReviewStage.DAY_3 ->
                LearningStatus.LEARNING to ReviewStage.DAY_7
            ReviewStage.DAY_7 ->
                LearningStatus.MASTERED to ReviewStage.MASTERED_DAY_14
            ReviewStage.MASTERED_DAY_14 ->
                LearningStatus.MASTERED to ReviewStage.MASTERED_DAY_30
            ReviewStage.MASTERED_DAY_30 ->
                LearningStatus.MASTERED to ReviewStage.MASTERED_DAY_30
        }
        return common.advanceTo(nextStatus, nextStage, occurredAt)
    }

    private fun applyAssistedRecall(
        state: LearningState,
        occurredAt: Long
    ): LearningState {
        val startsLearning = state.status == LearningStatus.UNLEARNED
        val stage = if (startsLearning) ReviewStage.DAY_1 else state.stage
        val anchor = if (startsLearning) {
            occurredAt
        } else {
            state.intervalAnchorAtEpochMillis
        }
        val nextReview = if (startsLearning) {
            occurredAt + stage.minimumIntervalMillis
        } else {
            state.nextFormalReviewAtEpochMillis
        }
        return state.copy(
            status = if (startsLearning) {
                LearningStatus.LEARNING
            } else {
                state.status
            },
            stage = stage,
            assistedRecallCount = state.assistedRecallCount + 1,
            lastAnsweredAtEpochMillis = occurredAt,
            lastCorrectAtEpochMillis = occurredAt,
            intervalAnchorAtEpochMillis = anchor,
            nextFormalReviewAtEpochMillis = nextReview,
            needsShortTermReview = when (config.assistedRecallPolicy) {
                AssistedRecallPolicy.KEEP_SHORT_TERM_REVIEW -> true
                AssistedRecallPolicy.CLEAR_SHORT_TERM_REVIEW -> false
            }
        )
    }

    private fun applyFailedRecall(
        state: LearningState,
        occurredAt: Long
    ): LearningState {
        val nextStage = when (state.stage) {
            ReviewStage.NOT_STARTED,
            ReviewStage.DAY_1 -> ReviewStage.DAY_1
            ReviewStage.DAY_3 -> ReviewStage.DAY_1
            ReviewStage.DAY_7 -> ReviewStage.DAY_3
            ReviewStage.MASTERED_DAY_14,
            ReviewStage.MASTERED_DAY_30 -> ReviewStage.DAY_3
        }
        return state.copy(
            status = LearningStatus.LEARNING,
            stage = nextStage,
            failedRecallCount = state.failedRecallCount + 1,
            lastAnsweredAtEpochMillis = occurredAt,
            lastFailedAtEpochMillis = occurredAt,
            intervalAnchorAtEpochMillis = occurredAt,
            nextFormalReviewAtEpochMillis =
                occurredAt + nextStage.minimumIntervalMillis,
            needsShortTermReview = true
        )
    }

    private fun LearningState.advanceTo(
        status: LearningStatus,
        stage: ReviewStage,
        occurredAt: Long
    ): LearningState = copy(
        status = status,
        stage = stage,
        lastFormalSuccessAtEpochMillis = occurredAt,
        intervalAnchorAtEpochMillis = occurredAt,
        nextFormalReviewAtEpochMillis =
            occurredAt + stage.minimumIntervalMillis
    )
}

fun isWeakLearningItem(recentAttempts: List<RecallAttempt>): Boolean =
    recentAttempts
        .sortedByDescending { it.occurredAtEpochMillis }
        .take(3)
        .takeIf { it.size == 3 }
        ?.count { it.outcome == RecallOutcome.FAILED }
        ?.let { it >= 2 }
        ?: false

fun masteryRatePercent(
    states: Collection<LearningState>,
    targetLearningItemIds: Set<String>
): Int {
    if (targetLearningItemIds.isEmpty()) return 0
    val masteredCount = states.count {
        it.learningItemId in targetLearningItemIds &&
            it.status == LearningStatus.MASTERED
    }
    return masteredCount * 100 / targetLearningItemIds.size
}
