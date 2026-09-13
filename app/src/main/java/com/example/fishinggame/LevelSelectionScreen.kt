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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LevelSelectionScreen(
    modifier: Modifier = Modifier,
    state: GameUiState,
    onStartTargetLevel: () -> Unit,
    onChangeTargetLevel: () -> Unit,
    onSelectReviewMode: () -> Unit,
    onSelectWeakMode: () -> Unit,
    onSelectQuestionMode: (QuestionMode) -> Unit,
    onSelectDebugSentenceQuestions: () -> Unit,
    onOpenFishCollection: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "${state.selectedMap?.name ?: "マップ未選択"}" +
                " ＞ ${state.selectedPoint?.name ?: "ポイント未選択"}",
            style = MaterialTheme.typography.labelLarge
        )
        Text(text = "問題の設定")

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
                "文章入力・文章4択では登録済みの文章問題だけを出題します。" +
                "復習・苦手では" +
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
            enabled = state.availableReviewWordCount > 0 &&
                !state.isLearningHistoryLoading,
            onClick = onSelectReviewMode
        ) {
            Text(
                text = "復習する（${state.availableReviewWordCount}語）"
            )
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = state.availableWeakWordCount > 0 &&
                !state.isLearningHistoryLoading,
            onClick = onSelectWeakMode
        ) {
            Text(text = "苦手単語（${state.availableWeakWordCount}語）")
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
            text = "目標レベル",
            style = MaterialTheme.typography.titleMedium
        )
        val targetLevel = state.targetEikenLevel
        if (targetLevel == null) {
            Text(text = "目標レベルが選択されていません")
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onChangeTargetLevel
            ) {
                Text(text = "目標レベルを選ぶ")
            }
        } else {
            val wordCount = state.allWords.count {
                it.eikenLevel == targetLevel
            }
            Text(text = "${targetLevel.displayName}（${wordCount}語）")
            Text(
                text = "通常問題は目標級を中心に、存在する範囲で" +
                    "直下の級とさらに下の基礎確認問題を少し混ぜます。",
                style = MaterialTheme.typography.bodySmall
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = wordCount > 0,
                onClick = onStartTargetLevel
            ) {
                Text(text = "この目標レベルで釣りを始める")
            }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onChangeTargetLevel
            ) {
                Text(text = "目標レベルを変更")
            }
            Text(
                text = "公式単語表ではなく、CEFR-J等を基にした" +
                    "ゲーム内の推定分類です",
                style = MaterialTheme.typography.bodySmall
            )
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
