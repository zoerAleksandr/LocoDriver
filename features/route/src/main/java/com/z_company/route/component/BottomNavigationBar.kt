package com.z_company.route.component

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentRoute = navBackStackEntry?.destination?.route

    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .height(72.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route.route
                val onClick = {
                    // Кнопка «+» — перехватывается для проверки подписки до навигации
                    if (item is NavigationItem.Add && onAddClick != null) {
                        onAddClick()
                    } else if (currentRoute != item.route.route) {
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

                if (item is NavigationItem.Add) {
                    AddFab(
                        modifier = Modifier.weight(1f),
                        item = item,
                        onClick = onClick,
                    )
                } else {
                    NavBarItem(
                        modifier = Modifier.weight(1f),
                        item = item,
                        selected = selected,
                        onClick = onClick,
                    )
                }
            }
        }
    }
}

/** Обычный пункт: иконка в капсуле (accentSoft при выборе) + подпись. */
@Composable
private fun NavBarItem(
    modifier: Modifier,
    item: NavigationItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val contentColor = if (selected) MaterialTheme.colorScheme.tertiary
    else MaterialTheme.colorScheme.onSurfaceVariant
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = null,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (selected) MaterialTheme.colorScheme.primaryContainer
                    else androidx.compose.ui.graphics.Color.Transparent
                )
                .padding(horizontal = 18.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = item.icon),
                contentDescription = item.title,
                tint = contentColor,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        // Ярлыки — узкие фиксированные сегменты; ограничиваем масштаб шрифта, чтобы
        // «Настройки»/«Зарплата» помещались одной строкой и не обрезались.
        val labelDensity = LocalDensity.current.let { d ->
            if (d.fontScale > 1.15f) Density(d.density, 1.15f) else d
        }
        CompositionLocalProvider(LocalDensity provides labelDensity) {
            Text(
                text = item.title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Центральная кнопка «+» — акцентная капсула (FAB-стиль). */
@Composable
private fun AddFab(
    modifier: Modifier,
    item: NavigationItem,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiary)
                .selectable(
                    selected = false,
                    onClick = onClick,
                    role = Role.Button,
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = com.z_company.core.R.drawable.ic_add),
                contentDescription = item.title,
                tint = MaterialTheme.colorScheme.onTertiary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
