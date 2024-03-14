package com.z_company.loco_driver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.activity.viewModels
import com.z_company.loco_driver.ui.LocoDriverApp
import com.z_company.loco_driver.ui.rememberLocoDriverAppState
import com.z_company.loco_driver.viewmodel.MainViewModel

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