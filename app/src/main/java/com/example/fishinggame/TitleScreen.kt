package com.example.fishinggame

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun TitleScreen(
    modifier: Modifier = Modifier,
    state: GameUiState,
    onStart: () -> Unit,
    onOpenCollection: () -> Unit,
    onOpenLicenses: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "WORD REEL FISHING",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Text(
            text = "英単語を巻いて、渓流の魚を釣り上げよう",
            textAlign = TextAlign.Center
        )
        StreamDiorama(
            modifier = Modifier.fillMaxWidth(),
            showBobber = true,
            showFish = true
        )
        Text(
            text = "渓流図鑑：${state.availableCaughtSpeciesCount}" +
                " / ${currentlyAvailableFishes.size}種類"
        )
        Spacer(modifier = Modifier.height(4.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onStart
        ) {
            Text(text = "釣りに出かける")
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onOpenCollection
        ) {
            Text(text = "魚図鑑")
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onOpenLicenses
        ) {
            Text(text = "単語データの権利情報")
        }
    }
}
