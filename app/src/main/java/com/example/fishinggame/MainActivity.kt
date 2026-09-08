package com.example.fishinggame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.fishinggame.ui.theme.FishinggameTheme

class MainActivity : ComponentActivity() {
    private val gameViewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FishinggameTheme {
                FishingGameApp(gameViewModel = gameViewModel)
            }
        }
    }
}
