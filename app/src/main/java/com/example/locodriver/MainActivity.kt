package com.example.locodriver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.activity.viewModels
import com.example.locodriver.ui.LocoDriverApp
import com.example.locodriver.ui.rememberLocoDriverAppState
import com.example.locodriver.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(mainViewModel)
        setContent {
            val appState = rememberLocoDriverAppState()
            LocoDriverApp(appState = appState, isLoggedIn = true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(mainViewModel)
    }
}