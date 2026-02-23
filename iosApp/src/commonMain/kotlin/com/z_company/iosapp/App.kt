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
 * Шаг 15: подключена KMP-навигация (org.jetbrains.androidx.navigation).
 *   App() → AppNavHost() → NavHost с маршрутами, зеркалящими features/route/navigation/Routes.kt
 *
 * Шаг 16 (следующий): заменить stub-экраны реальными реализациями,
 *   портируя ViewModel из features/ на Compose Multiplatform.
 *
 * Структура навигации:
 *   App()
 *   └── AppNavHost()
 *       ├── HomeScreen              (список маршрутов)
 *       ├── FormScreen              (создание/редактирование маршрута)
 *       ├── FormLocoScreen          (форма локомотива)
 *       ├── FormTrainScreen         (форма поезда)
 *       ├── FormPassengerScreen
 *       ├── SettingsScreen
 *       ├── ProfileScreen
 *       ├── SalaryCalculationScreen
 *       └── StubScreen              (остальные маршруты — в разработке)
 */
@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AppNavHost()
        }
    }
}
