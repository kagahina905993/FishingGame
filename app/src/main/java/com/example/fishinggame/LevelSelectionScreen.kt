package com.example.fishinggame

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LevelSelectionScreen(
    modifier: Modifier = Modifier,
    state: GameUiState,
    onSelectLevel: (Int) -> Unit,
    onSelectSchoolGrade: (SchoolGrade) -> Unit,
    onSelectEikenLevel: (EikenLevel) -> Unit,
    onSelectReviewMode: () -> Unit,
    onSelectWeakMode: () -> Unit,
    onSelectQuestionMode: (QuestionMode) -> Unit,
    onSelectDebugSentenceQuestions: () -> Unit,
    onOpenFishCollection: () -> Unit
) {
    var selectedRangeTab by rememberSaveable { mutableIntStateOf(0) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "${state.selectedMap?.name ?: "マップ未選択"}" +
                " ＞ ${state.selectedPoint?.name ?: "ポイント未選択"}",
            style = MaterialTheme.typography.labelLarge
        )
        Text(text = "出題範囲を選んでください")

        Text(
            text = "出題形式",
            style = MaterialTheme.typography.titleMedium
        )
        QuestionMode.entries.chunked(2).forEach { modes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                modes.forEach { mode ->
                    FilterChip(
                        modifier = Modifier.weight(1f),
                        selected = state.questionMode == mode,
                        onClick = { onSelectQuestionMode(mode) },
                        label = {
                            Text(
                                text = mode.displayName,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    )
                }
            }
        }
        Text(
            text = "文章問題 ${state.sentenceQuestionsByWordId.size}語。" +
                "未登録語は単語入力になります。復習・苦手では" +
                "文章4択を文章入力、意味4択を単語入力へ切り替えます。",
            style = MaterialTheme.typography.bodySmall
        )
        if (BuildConfig.DEBUG) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = state.questionMode in setOf(
                    QuestionMode.SENTENCE_INPUT,
                    QuestionMode.SENTENCE_MULTIPLE_CHOICE,
                    QuestionMode.MIXED
                ) &&
                    state.sentenceQuestionsByWordId.isNotEmpty(),
                onClick = onSelectDebugSentenceQuestions
            ) {
                Text(text = "デバッグ：文章問題だけで開始")
            }
        }

        Text(
            text = "学習記録",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "習得 ${state.masteredLearningItemCount}" +
                " / ${state.allWords.size}語（${state.masteryPercent}%）\n" +
                "初回自力正答率 " +
                "${state.savedInitialRecallAccuracyPercent}%\n" +
                "補助あり完了 ${state.savedAssistedCompletionCount}回"
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = state.reviewLearningItemIds.isNotEmpty() &&
                !state.isLearningHistoryLoading,
            onClick = onSelectReviewMode
        ) {
            Text(
                text = "復習する（${state.reviewLearningItemIds.size}語）"
            )
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = state.weakLearningItemIds.isNotEmpty() &&
                !state.isLearningHistoryLoading,
            onClick = onSelectWeakMode
        ) {
            Text(text = "苦手単語（${state.weakLearningItemIds.size}語）")
        }
        if (state.isLearningHistoryLoading) {
            Text(text = "学習履歴を更新しています…")
        }
        state.learningHistoryError?.let { error ->
            Text(
                text = "学習履歴：$error",
                color = MaterialTheme.colorScheme.error
            )
        }

        Text(
            text = "通常学習の出題範囲",
            style = MaterialTheme.typography.titleMedium
        )
        PrimaryTabRow(selectedTabIndex = selectedRangeTab) {
            listOf("学年", "英検", "ゲーム").forEachIndexed { index, label ->
                Tab(
                    selected = selectedRangeTab == index,
                    onClick = { selectedRangeTab = index },
                    text = { Text(label) }
                )
            }
        }

        when (selectedRangeTab) {
            0 -> SchoolGrade.entries.forEach { schoolGrade ->
                val wordCount = state.allWords.count {
                    it.schoolGrade == schoolGrade
                }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = wordCount > 0,
                    onClick = { onSelectSchoolGrade(schoolGrade) }
                ) {
                    Text(text = "${schoolGrade.displayName}（${wordCount}語）")
                }
            }

            1 -> {
                Text(text = "英検相当の推定級（新出語）")
                EikenLevel.entries.forEach { eikenLevel ->
                    val wordCount = state.allWords.count {
                        it.eikenLevel == eikenLevel
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = wordCount > 0,
                        onClick = { onSelectEikenLevel(eikenLevel) }
                    ) {
                        Text(text = "${eikenLevel.displayName}（${wordCount}語）")
                    }
                }
                Text(
                    text = "公式単語表ではなく、CEFR-J等を基にした" +
                        "学習用の推定分類です"
                )
            }

            else -> state.allWords
                .map { it.level }
                .distinct()
                .sorted()
                .forEach { level ->
                    val wordCount = state.allWords.count { it.level == level }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onSelectLevel(level) }
                    ) {
                        Text(text = "レベル$level（${wordCount}語）")
                    }
                }
        }

        Text(
            text = "渓流図鑑：${state.availableCaughtSpeciesCount}" +
                " / ${currentlyAvailableFishes.size}種類",
            style = MaterialTheme.typography.titleMedium
        )
        if (state.isAvailableFishCollectionComplete) {
            Text(text = "魚図鑑コンプリート！")
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onOpenFishCollection
        ) {
            Text(text = "魚図鑑を見る")
        }
    }
}
