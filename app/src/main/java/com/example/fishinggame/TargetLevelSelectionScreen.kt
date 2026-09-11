package com.example.fishinggame

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TargetLevelSelectionScreen(
    state: GameUiState,
    onSelectTargetLevel: (EikenLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "目標レベルを選ぼう",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "目標にする英検相当級を選んでください。" +
                "現在の実力を判定するものではなく、あとから変更できます。"
        )

        EikenLevel.entries.forEach { level ->
            val wordCount = state.allWords.count { it.eikenLevel == level }
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = wordCount > 0,
                onClick = { onSelectTargetLevel(level) }
            ) {
                Text(
                    text = level.displayName +
                        if (state.targetEikenLevel == level) {
                            "（現在の目標）"
                        } else {
                            ""
                        }
                )
            }
        }

        Text(
            text = "各級は公式単語表ではなく、CEFR-J等を基にした" +
                "ゲーム内の推定分類です。",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
