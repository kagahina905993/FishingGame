package com.example.fishinggame

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FishingPointScreen(
    modifier: Modifier = Modifier,
    map: FishingMap,
    fishCollectionRecords: Map<String, FishCollectionRecord>,
    onSelectPoint: (String) -> Unit
) {
    val nonLordFishes = nonLordFishesForMap(map.id)
    val caughtNonLordFishCount = caughtNonLordFishCountForMap(
        mapId = map.id,
        fishCollectionRecords = fishCollectionRecords
    )
    val lordIsUnlocked = isMapLordUnlocked(
        mapId = map.id,
        fishCollectionRecords = fishCollectionRecords
    )
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = map.name,
            style = MaterialTheme.typography.labelLarge
        )
        Text(
            text = "釣りポイントを選ぶ",
            style = MaterialTheme.typography.headlineMedium
        )
        pointsForMap(map.id).forEach { point ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StreamDiorama(
                        environment = map.environment,
                        showBobber = true,
                        showFish = false
                    )
                    Text(
                        text = point.name,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(text = point.description)
                    Text(
                        text = "確認されている魚：" + point.fishSpawns
                            .joinToString("・") { spawn ->
                                if (
                                    CONTEST_LORD_UNLOCK_ENABLED &&
                                    !lordIsUnlocked &&
                                    spawn.fishName == point.lordFishName
                                ) {
                                    "？？？（主）"
                                } else {
                                    spawn.fishName
                                }
                            }
                    )
                    if (
                        CONTEST_LORD_UNLOCK_ENABLED &&
                        point.lordFishName != null
                    ) {
                        Text(
                            text = if (lordIsUnlocked) {
                                "主が現れるようになった！"
                            } else {
                                "主の出現条件：主以外の魚を集める " +
                                    "$caughtNonLordFishCount / " +
                                    "${nonLordFishes.size}種類"
                            },
                            color = if (lordIsUnlocked) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onSelectPoint(point.id) }
                    ) {
                        Text(text = "ここで釣る")
                    }
                }
            }
        }
    }
}
