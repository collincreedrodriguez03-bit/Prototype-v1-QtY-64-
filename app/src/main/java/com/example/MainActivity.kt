package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.SignalEngineScreen
import com.example.ui.theme.QtY64Theme
import com.example.ui.viewmodel.SignalEngineViewModel

class MainActivity : ComponentActivity() {
  private val viewModel: SignalEngineViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      QtY64Theme {
        SignalEngineScreen(viewModel = viewModel)
      }
    }
  }
}

