package com.z_company.shared.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsRailway
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Route
import com.z_company.shared.ui.component.AppBottomSheet
import com.z_company.shared.ui.component.BottomSheetAction
import com.z_company.shared.ui.component.ItemHomeScreen
import com.z_company.shared.util.DateAndTimeConverter
import com.z_company.shared.util.TimeFormatter
import com.z_company.shared.viewmodel.AllRouteSharedViewModel
import com.z_company.shared.viewmodel.AllRouteSharedViewModel.RouteFilter
import com.z_company.shared.viewmodel.AllRouteSharedViewModel.SortOption
import org.koin.compose.koinInject

private val redOrange = Color(0xFFf1642e)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AllRouteScreen(
    onBackClick: () -> Unit,
    onRouteClick: (BasicData) -> Unit,
) {
    val viewModel: AllRouteSharedViewModel = koinInject()
    val routes by viewModel.filteredRoutes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val selectedFilters by viewModel.selectedFilters.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var isSortSheetVisible by remember { mutableStateOf(false) }
    var isFilterSheetVisible by remember { mutableStateOf(false) }

    // Context menu / delete states
    var routeForPreview by remember { mutableStateOf<Route?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }
    var routeForRemove by remember { mutableStateOf<Route?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Sort bottom sheet
    if (isSortSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { isSortSheetVisible = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                Text(
                    text = "Сортировка",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                RadioButtonWithLabel("Старые", sortOption == SortOption.DATE_ASC) {
                    viewModel.setSort(SortOption.DATE_ASC); isSortSheetVisible = false
                }
                RadioButtonWithLabel("Новые", sortOption == SortOption.DATE_DESC) {
                    viewModel.setSort(SortOption.DATE_DESC); isSortSheetVisible = false
                }
                RadioButtonWithLabel("Мало часов на работе", sortOption == SortOption.WORKTIME_ASC) {
                    viewModel.setSort(SortOption.WORKTIME_ASC); isSortSheetVisible = false
                }
                RadioButtonWithLabel("Много часов на работе", sortOption == SortOption.WORKTIME_DESC) {
                    viewModel.setSort(SortOption.WORKTIME_DESC); isSortSheetVisible = false
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Filter bottom sheet
    if (isFilterSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { isFilterSheetVisible = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                Text(
                    text = "Фильтры",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = selectedFilters.contains(RouteFilter.ALL),
                        onClick = { viewModel.toggleFilter(RouteFilter.ALL) },
                        label = { Text("Все") },
                    )
                    FilterChip(
                        selected = selectedFilters.contains(RouteFilter.FAVORITES),
                        onClick = { viewModel.toggleFilter(RouteFilter.FAVORITES) },
                        label = { Text("Избранные") },
                        leadingIcon = if (selectedFilters.contains(RouteFilter.FAVORITES)) {
                            { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp), tint = redOrange) }
                        } else null,
                    )
                    FilterChip(
                        selected = selectedFilters.contains(RouteFilter.ONE_PERSON),
                        onClick = { viewModel.toggleFilter(RouteFilter.ONE_PERSON) },
                        label = { Text("Одно лицо") },
                    )
                    FilterChip(
                        selected = selectedFilters.contains(RouteFilter.OVER_12_HOURS),
                        onClick = { viewModel.toggleFilter(RouteFilter.OVER_12_HOURS) },
                        label = { Text("Свыше 12ч") },
                    )
                    FilterChip(
                        selected = selectedFilters.contains(RouteFilter.HEAVY),
                        onClick = { viewModel.toggleFilter(RouteFilter.HEAVY) },
                        label = { Text("Тяжелые") },
                    )
                    FilterChip(
                        selected = selectedFilters.contains(RouteFilter.HAS_BREAK),
                        onClick = { viewModel.toggleFilter(RouteFilter.HAS_BREAK) },
                        label = { Text("С перерывами") },
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Context menu (long-click)
    if (showContextMenu && routeForPreview != null) {
        AppBottomSheet(
            onDismissRequest = { showContextMenu = false },
            title = routeForPreview?.basicData?.number?.let { "Маршрут №$it" } ?: "Маршрут",
            actions = listOf(
                BottomSheetAction(
                    text = if (routeForPreview?.basicData?.isFavorite == true) "Убрать из избранного" else "Добавить в избранное"
                ) {
                    routeForPreview?.let { viewModel.setFavoriteRoute(it) }
                },
                BottomSheetAction(text = "Удалить маршрут") {
                    routeForRemove = routeForPreview
                    showDeleteConfirm = true
                },
            ),
        )
    }

    // Delete confirmation
    if (showDeleteConfirm && routeForRemove != null) {
        val timeText = routeForRemove?.basicData?.timeStartWork?.let { TimeFormatter.formatDateTime(it) } ?: ""
        AppBottomSheet(
            onDismissRequest = { showDeleteConfirm = false },
            title = "Удалить маршрут?\nот $timeText",
            actions = listOf(
                BottomSheetAction(text = "Да, удалить") {
                    routeForRemove?.let { viewModel.deleteRoute(it) }
                },
            ),
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Header row: filter | back + count | sort
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Left: filter button with badge
                BadgedBox(
                    badge = {
                        Badge(
                            containerColor = if (selectedFilters.contains(RouteFilter.ALL)) Color.Transparent else redOrange,
                            contentColor = if (selectedFilters.contains(RouteFilter.ALL)) Color.Transparent else Color.White,
                        ) {
                            Text("${selectedFilters.size}")
                        }
                    },
                ) {
                    IconButton(onClick = { isFilterSheetVisible = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Фильтры")
                    }
                }

                // Center: back + title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                    Text(
                        text = "Все маршруты",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                // Right: sort button
                IconButton(onClick = { isSortSheetVisible = true }) {
                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Сортировка")
                }
            }

            // Content
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                }

                routes.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Icon(
                                Icons.Default.DirectionsRailway,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            )
                            Text(
                                text = "Нет маршрутов",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                else -> {
                    Text(
                        text = "Маршрутов: ${routes.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                    ) {
                        items(routes, key = { it.basicData.id }) { route ->
                            ItemHomeScreen(
                                route = route,
                                convertTimeToString = { millis ->
                                    millis?.let { TimeFormatter.formatDuration(it) } ?: ""
                                },
                                onClick = { onRouteClick(route.basicData) },
                                onRequestDelete = {
                                    routeForRemove = route
                                    showDeleteConfirm = true
                                },
                                onLongClick = {
                                    routeForPreview = route
                                    showContextMenu = true
                                },
                                containerColor = MaterialTheme.colorScheme.secondary,
                                dateAndTimeConverter = null,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RadioButtonWithLabel(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
