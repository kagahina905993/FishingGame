package com.example.fishinggame

import android.content.Context
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Entity(
    tableName = "learning_attempts",
    indices = [
        Index(value = ["learningItemId", "occurredAtEpochMillis"]),
        Index(value = ["wordId"])
    ]
)
data class LearningAttemptEntity(
    @PrimaryKey val attemptId: String,
    val learningItemId: String,
    val wordId: String,
    val direction: String,
    val questionFormat: String,
    val outcome: String,
    val occurredAtEpochMillis: Long,
    val responseTimeMillis: Long?,
    val usedHint: Boolean,
    val gameMode: String,
    val courseId: String?,
    val reviewPurpose: String,
    val completionOutcome: String?,
    val completedAtEpochMillis: Long?,
    val submissionCount: Int,
    val algorithmVersion: Int
)

@Entity(
    tableName = "learning_states",
    indices = [
        Index(value = ["wordId"]),
        Index(value = ["needsShortTermReview"]),
        Index(value = ["nextFormalReviewAtEpochMillis"])
    ]
)
data class LearningStateEntity(
    @PrimaryKey val learningItemId: String,
    val wordId: String,
    val direction: String,
    val status: String,
    val stage: String,
    val independentRecallCount: Int,
    val assistedRecallCount: Int,
    val failedRecallCount: Int,
    val lastAnsweredAtEpochMillis: Long?,
    val lastCorrectAtEpochMillis: Long?,
    val lastIndependentRecallAtEpochMillis: Long?,
    val lastFormalSuccessAtEpochMillis: Long?,
    val lastFailedAtEpochMillis: Long?,
    val intervalAnchorAtEpochMillis: Long?,
    val nextFormalReviewAtEpochMillis: Long?,
    val needsShortTermReview: Boolean,
    val recentOutcomeCodes: String,
    val algorithmVersion: Int
)

@Dao
interface LearningDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAttempt(attempt: LearningAttemptEntity)

    @Query(
        """
        UPDATE learning_attempts
        SET submissionCount = submissionCount + 1,
            usedHint = CASE
                WHEN :usedHint = 1 THEN 1
                ELSE usedHint
            END,
            completionOutcome = CASE
                WHEN :wasCorrect = 1 THEN 'ASSISTED'
                ELSE completionOutcome
            END,
            completedAtEpochMillis = CASE
                WHEN :wasCorrect = 1 THEN :occurredAtEpochMillis
                ELSE completedAtEpochMillis
            END
        WHERE attemptId = :attemptId
        """
    )
    suspend fun recordAdditionalSubmission(
        attemptId: String,
        wasCorrect: Boolean,
        usedHint: Boolean,
        occurredAtEpochMillis: Long
    ): Int

    @Upsert
    suspend fun upsertState(state: LearningStateEntity)

    @Query("SELECT * FROM learning_states WHERE learningItemId = :id")
    suspend fun getState(id: String): LearningStateEntity?

    @Query("SELECT * FROM learning_states")
    suspend fun getAllStates(): List<LearningStateEntity>

    @Query(
        """
        SELECT * FROM learning_attempts
        WHERE learningItemId = :learningItemId
        ORDER BY occurredAtEpochMillis DESC
        LIMIT :limit
        """
    )
    suspend fun getRecentAttempts(
        learningItemId: String,
        limit: Int
    ): List<LearningAttemptEntity>

    @Query(
        """
        SELECT * FROM learning_states
        WHERE nextFormalReviewAtEpochMillis IS NOT NULL
          AND nextFormalReviewAtEpochMillis <= :nowEpochMillis
        ORDER BY nextFormalReviewAtEpochMillis ASC
        """
    )
    suspend fun getFormalReviewCandidates(
        nowEpochMillis: Long
    ): List<LearningStateEntity>

    @Query(
        """
        SELECT * FROM learning_states AS state
        WHERE state.needsShortTermReview = 1
          AND state.lastFailedAtEpochMillis IS NOT NULL
          AND (
              :nowEpochMillis - state.lastFailedAtEpochMillis >=
                  :fallbackDelayMillis
              OR (
                  SELECT COUNT(DISTINCT attempt.learningItemId)
                  FROM learning_attempts AS attempt
                  WHERE attempt.occurredAtEpochMillis >
                        state.lastFailedAtEpochMillis
                    AND attempt.learningItemId != state.learningItemId
              ) >= :minimumOtherItems
          )
        ORDER BY state.lastFailedAtEpochMillis ASC
        """
    )
    suspend fun getEligibleShortTermReviewStates(
        nowEpochMillis: Long,
        fallbackDelayMillis: Long,
        minimumOtherItems: Int
    ): List<LearningStateEntity>

    @Query("SELECT COUNT(*) FROM learning_attempts")
    suspend fun countAttempts(): Int

    @Query(
        "SELECT COUNT(*) FROM learning_attempts " +
            "WHERE completionOutcome = 'ASSISTED'"
    )
    suspend fun countAssistedCompletions(): Int
}

@Database(
    entities = [LearningAttemptEntity::class, LearningStateEntity::class],
    version = 1,
    exportSchema = true
)
abstract class LearningDatabase : RoomDatabase() {
    abstract fun learningDao(): LearningDao

    companion object {
        @Volatile
        private var instance: LearningDatabase? = null

        fun getInstance(context: Context): LearningDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    LearningDatabase::class.java,
                    "learning-history.db"
                ).build().also { instance = it }
            }
    }
}
