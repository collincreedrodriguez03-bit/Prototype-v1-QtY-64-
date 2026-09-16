package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.viewmodel.SignalEngineViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read app name string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("QtY 64", appName)
  }

  @Test
  fun `view model initializes with valid initial state`() {
    val viewModel = SignalEngineViewModel()
    val state = viewModel.uiState.value
    assertNotNull(state.activeContract)
    assertEquals("BTC/USD", state.config.symbol)
    assertEquals(15, state.config.timeframeMinutes)
  }
}

