package com.example.locodriver.ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import com.example.locodriver.ui.navigation.MainNavigation
import com.example.locodriver.ui.theme.LocoDriverTheme

@OptIn(
    ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class,
    ExperimentalAnimationApi::class
)
@Composable
fun LocoDriverApp(
    appState: LocoDriverAppState,
    isLoggedIn: Boolean,
) {
    LocoDriverTheme() {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            MainNavigation(appState, isLoggedIn)
        }
    }
}