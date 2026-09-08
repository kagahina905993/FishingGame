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
    onSelectPoint: (String) -> Unit
) {
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
                        showBobber = true,
                        showFish = true
                    )
                    Text(
                        text = point.name,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(text = point.description)
                    Text(
                        text = "確認されている魚：" +
                            point.fishSpawns.size + "種類"
                    )
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
