package com.z_company.shared.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Нижняя навигация — зеркало Android BottomNavigationBar.
 *
 * 5 вкладок: Главная, Зарплата, Добавить, Настройки, Профиль.
 * Использует Material 3 NavigationBar (кросс-платформенный).
 */

enum class BottomTab(
    val label: String,
    val icon: ImageVector,
) {
    Home("Главная", Icons.Default.Home),
    Salary("Зарплата", Icons.Default.Wallet),
    Add("Добавить", Icons.Default.AddCircle),
    Settings("Настройки", Icons.Default.Settings),
    Profile("Профиль", Icons.Default.AccountCircle),
}

@Composable
fun SharedBottomNavigationBar(
    currentTab: BottomTab,
    onTabSelected: (BottomTab) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.secondary,
    ) {
        BottomTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = currentTab == tab,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                    )
                },
                label = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    selectedTextColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unselectedIconColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    unselectedTextColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    indicatorColor = MaterialTheme.colorScheme.secondary,
                ),
            )
        }
    }
}
