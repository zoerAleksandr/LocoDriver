package com.z_company.iosapp

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.z_company.iosapp.navigation.AppNavHost

/**
 * Корневой Composable iOS-приложения.
 *
 * Все экраны — из :features:shared (Compose Multiplatform).
 * Навигация: AppNavHost() → NavHost с маршрутами из IosRouterImpl.
 */
@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AppNavHost()
        }
    }
}
