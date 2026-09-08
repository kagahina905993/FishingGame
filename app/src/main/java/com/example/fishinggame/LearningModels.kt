package com.example.fishinggame

const val LEARNING_ALGORITHM_VERSION = 1

private const val DAY_MILLIS = 24L * 60L * 60L * 1000L

enum class LearningDirection(val code: String) {
    JAPANESE_TO_ENGLISH("ja-en"),
    ENGLISH_TO_JAPANESE("en-ja")
}

enum class QuestionFormat(val code: String) {
    FULL_INPUT("full-input"),
    FILL_IN_THE_BLANK("fill-in-the-blank"),
    MULTIPLE_CHOICE("multiple-choice")
}

enum class RecallOutcome(val code: String) {
    INDEPENDENT("independent"),
    ASSISTED("assisted"),
    FAILED("failed")
}

fun recallOutcomeForSubmission(
    wasCorrect: Boolean,
    questionFormat: QuestionFormat
): RecallOutcome = when {
    !wasCorrect -> RecallOutcome.FAILED
    questionFormat == QuestionFormat.MULTIPLE_CHOICE ->
        RecallOutcome.ASSISTED
    else -> RecallOutcome.INDEPENDENT
}

enum class ReviewPurpose {
    NONE,
    SHORT_TERM,
    FORMAL
}

enum class LearningStatus(val code: String) {
    UNLEARNED("unlearned"),
    LEARNING("learning"),
    MASTERED("mastered")
}

/** The interval that must elapse before the next formal success. */
enum class ReviewStage(
    val code: String,
    val minimumIntervalMillis: Long
) {
    NOT_STARTED("not-started", 0L),
    DAY_1("day-1", DAY_MILLIS),
    DAY_3("day-3", 3L * DAY_MILLIS),
    DAY_7("day-7", 7L * DAY_MILLIS),
    MASTERED_DAY_14("mastered-day-14", 14L * DAY_MILLIS),
    MASTERED_DAY_30("mastered-day-30", 30L * DAY_MILLIS)
}

enum class AssistedRecallPolicy {
    KEEP_SHORT_TERM_REVIEW,
    CLEAR_SHORT_TERM_REVIEW
}

data class LearningAlgorithmConfig(
    val assistedRecallPolicy: AssistedRecallPolicy,
    val shortTermMinimumOtherItems: Int = 4,
    val shortTermFallbackDelayMillis: Long = 10L * 60L * 1000L
)

/**
 * One completed presentation of a LearningItem.
 *
 * Mapping multiple submissions within the same presentation to this outcome is
 * deliberately kept outside the scheduler until the product rule is approved.
 */
data class RecallAttempt(
    val attemptId: String,
    val learningItemId: String,
    val wordId: String,
    val direction: LearningDirection,
    val questionFormat: QuestionFormat,
    val outcome: RecallOutcome,
    val occurredAtEpochMillis: Long,
    val responseTimeMillis: Long? = null,
    val usedHint: Boolean = false,
    val gameMode: String,
    val courseId: String? = null,
    val reviewPurpose: ReviewPurpose = ReviewPurpose.NONE,
    val completionOutcome: RecallOutcome? = outcome
        .takeIf { it != RecallOutcome.FAILED },
    val completedAtEpochMillis: Long? = occurredAtEpochMillis
        .takeIf { completionOutcome != null },
    val submissionCount: Int = 1,
    val algorithmVersion: Int = LEARNING_ALGORITHM_VERSION
)

data class LearningState(
    val learningItemId: String,
    val wordId: String,
    val direction: LearningDirection,
    val status: LearningStatus = LearningStatus.UNLEARNED,
    val stage: ReviewStage = ReviewStage.NOT_STARTED,
    val independentRecallCount: Int = 0,
    val assistedRecallCount: Int = 0,
    val failedRecallCount: Int = 0,
    val lastAnsweredAtEpochMillis: Long? = null,
    val lastCorrectAtEpochMillis: Long? = null,
    val lastIndependentRecallAtEpochMillis: Long? = null,
    val lastFormalSuccessAtEpochMillis: Long? = null,
    val lastFailedAtEpochMillis: Long? = null,
    val intervalAnchorAtEpochMillis: Long? = null,
    val nextFormalReviewAtEpochMillis: Long? = null,
    val needsShortTermReview: Boolean = false,
    val recentOutcomes: List<RecallOutcome> = emptyList(),
    val algorithmVersion: Int = LEARNING_ALGORITHM_VERSION
) {
    val totalAttemptCount: Int
        get() = independentRecallCount + assistedRecallCount +
            failedRecallCount

    fun isFormalReviewAvailable(nowEpochMillis: Long): Boolean =
        nextFormalReviewAtEpochMillis?.let { nowEpochMillis >= it } == true

    val isWeak: Boolean
        get() = recentOutcomes.size == 3 &&
            recentOutcomes.count { it == RecallOutcome.FAILED } >= 2
}

interface LearningTimeProvider {
    fun nowEpochMillis(): Long
}

object SystemLearningTimeProvider : LearningTimeProvider {
    override fun nowEpochMillis(): Long = System.currentTimeMillis()
}
