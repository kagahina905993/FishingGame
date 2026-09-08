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
fun MapSelectionScreen(
    modifier: Modifier = Modifier,
    onSelectMap: (String) -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "マップを選ぶ",
            style = MaterialTheme.typography.headlineMedium
        )
        fishingMaps.forEach { map ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StreamDiorama(showBobber = true)
                    Text(
                        text = map.name,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(text = map.description)
                    Text(text = "釣りポイント：${map.pointIds.size}か所")
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onSelectMap(map.id) }
                    ) {
                        Text(text = "このマップへ")
                    }
                }
            }
        }
    }
}
