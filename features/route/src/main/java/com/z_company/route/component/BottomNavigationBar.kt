package com.z_company.route.component

import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.z_company.route.navigation.NavigationItem

@Composable
fun BottomNavigationBar(
    navController: NavController,
    onAddClick: (() -> Unit)? = null
) {
    val items = listOf(
        NavigationItem.Home,
        NavigationItem.Money,
        NavigationItem.Add,
        NavigationItem.Setting,
        NavigationItem.Profile
    )
    BottomNavigation(
        backgroundColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        elevation = 10.dp
    ) {
        val navBackStackEntry = navController.currentBackStackEntryAsState().value
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            BottomNavigationItem(
                icon = {
                    Icon(
                        painterResource(id = item.icon),
                        contentDescription = item.title
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                selectedContentColor = MaterialTheme.colorScheme.tertiary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                alwaysShowLabel = true,
                selected = currentRoute == item.route.route,
                onClick = {
                    // Кнопка «+» — перехватывается для проверки подписки до навигации
                    if (item is NavigationItem.Add && onAddClick != null) {
                        onAddClick()
                        return@BottomNavigationItem
                    }
                    if (currentRoute != item.route.route) {
                        val startDest = navController.graph.findStartDestination()
                        if (item.route.route == startDest.route) {
                            // Возврат на стартовый экран (Главная): явно pop до него.
                            // navigate + launchSingleTop не обновляет UI если HomeRoute
                            // уже в стеке ниже — popBackStack надёжнее.
                            val popped = navController.popBackStack(
                                route = startDest.route!!,
                                inclusive = false
                            )
                            if (!popped) {
                                // HomeRoute не был в стеке (было сохранено через saveState)
                                navController.navigate(item.route.route) {
                                    popUpTo(startDest.id) { saveState = false }
                                    launchSingleTop = true
                                }
                            }
                        } else {
                            navController.navigate(item.route.route) {
                                popUpTo(startDest.id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                }
            )
        }
    }
}