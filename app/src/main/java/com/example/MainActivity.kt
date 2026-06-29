package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.data.CyberDatabase
import com.example.data.ScoreRepository
import com.example.ui.CyberGameScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.CyberTilesViewModel
import com.example.viewmodel.CyberTilesViewModelFactory

class MainActivity : ComponentActivity() {
    private val database by lazy { CyberDatabase.getDatabase(this) }
    private val repository by lazy { ScoreRepository(database.highScoreDao()) }
    private val viewModel: CyberTilesViewModel by viewModels {
        CyberTilesViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                CyberGameScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
