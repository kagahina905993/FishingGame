package com.example.fishinggame

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FishCollectionScreen(
    modifier: Modifier = Modifier,
    state: GameUiState,
    onResetCollection: () -> Unit
) {
    var showResetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "魚図鑑",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "現在の釣り場で発見：${state.availableCaughtSpeciesCount}" +
                " / ${currentlyAvailableFishes.size}種類"
        )
        FishRarity.entries.forEach { rarity ->
            val rarityFishes = currentlyAvailableFishes.filter {
                it.rarity == rarity
            }
            if (rarityFishes.isNotEmpty()) {
                val caughtCount = rarityFishes.count { fish ->
                    (state.fishCollectionRecords[fish.name]
                        ?.caughtCount ?: 0) > 0
                }
                Text(
                    text = "${rarity.displayText}：$caughtCount" +
                        " / ${rarityFishes.size}"
                )
            }
        }
        if (state.isAvailableFishCollectionComplete) {
            Text(
                text = "魚図鑑コンプリート！",
                color = MaterialTheme.colorScheme.primary
            )
        }

        currentlyAvailableFishes.forEachIndexed { index, fish ->
            FishCollectionCard(
                index = index,
                fish = fish,
                record = state.fishCollectionRecords[fish.name]
            )
        }

        if (state.availableCaughtSpeciesCount > 0) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showResetDialog = true }
            ) {
                Text(text = "魚図鑑をリセット")
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(text = "魚図鑑をリセットしますか？") },
            text = {
                Text(text = "保存された魚の捕獲数がすべて0に戻ります。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onResetCollection()
                        showResetDialog = false
                    }
                ) {
                    Text(text = "リセットする")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(text = "キャンセル")
                }
            }
        )
    }
}

@Composable
private fun FishCollectionCard(
    index: Int,
    fish: Fish,
    record: FishCollectionRecord?
) {
    val caughtCount = record?.caughtCount ?: 0
    val isCaught = caughtCount > 0

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CollectionFishArtwork(
                    fish = fish,
                    isCaught = isCaught
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "No.${index + 1}",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = if (isCaught) fish.name else "？？？",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = if (isCaught) {
                            if (isFishingAreaLord(fish.name)) {
                                "主・${fish.rarity.displayText}"
                            } else {
                                fish.rarity.displayText
                            }
                        } else {
                            "未発見"
                        },
                        color = if (isCaught) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            if (isCaught) {
                Text(text = fish.description)
                Text(
                    text = "サイズ範囲：" +
                        "%.0f〜%.0fcm".format(
                            fish.minSizeCm,
                            fish.maxSizeCm
                        )
                )
                Text(text = "捕獲数：${caughtCount}匹")
                Text(
                    text = "最大サイズ：" +
                        if (record != null && record.largestSizeCm > 0f) {
                            "%.1fcm".format(record.largestSizeCm)
                        } else {
                            "記録なし"
                        }
                )
                Text(
                    text = "初発見：" + formatRecordedDate(
                        record?.firstCaughtAtEpochMillis ?: 0L
                    )
                )
                Text(
                    text = "最新捕獲：" + formatRecordedDate(
                        record?.lastCaughtAtEpochMillis ?: 0L
                    )
                )
                Text(
                    text = "初めて釣った場所：" + formatFishingLocation(
                        record?.firstCaughtMapId,
                        record?.firstCaughtPointId
                    )
                )
                Text(
                    text = "最近釣った場所：" + formatFishingLocation(
                        record?.lastCaughtMapId,
                        record?.lastCaughtPointId
                    )
                )
            } else {
                Text(
                    text = "この釣り場で魚を釣り上げると情報が解放されます。",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun CollectionFishArtwork(
    fish: Fish,
    isCaught: Boolean
) {
    val shape = RoundedCornerShape(12.dp)
    val backgroundColor = Color(0xFFE7E9E8)
    val borderColor = Color(0xFF8A9698)

    Box(
        modifier = Modifier
            .size(width = 120.dp, height = 80.dp)
            .clip(shape)
            .background(backgroundColor)
            .border(BorderStroke(1.dp, borderColor), shape),
        contentAlignment = Alignment.Center
    ) {
        FishArtwork(
            fish = fish,
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            silhouette = !isCaught
        )
    }
}

private fun formatFishingLocation(
    mapId: String?,
    pointId: String?
): String {
    val mapName = findFishingMap(mapId)?.name
    val pointName = findFishingPoint(pointId)?.name
    return when {
        mapName != null && pointName != null -> "$mapName・$pointName"
        mapName != null -> mapName
        pointName != null -> pointName
        else -> "記録なし"
    }
}

private fun formatRecordedDate(epochMillis: Long): String {
    if (epochMillis <= 0L) return "記録なし"
    return SimpleDateFormat(
        "yyyy/MM/dd HH:mm",
        Locale.JAPAN
    ).format(Date(epochMillis))
}
