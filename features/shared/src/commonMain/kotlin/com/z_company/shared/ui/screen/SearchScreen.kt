package com.z_company.shared.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.z_company.domain.entities.SearchStateScreen
import com.z_company.domain.entities.SearchTag
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Route
import com.z_company.shared.util.TimeFormatter
import com.z_company.shared.viewmodel.SearchSharedViewModel
import org.koin.compose.koinInject

private const val ANIMATION_SLIDE_TIME = 150

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    onBackClick: () -> Unit,
    onRouteClick: (BasicData) -> Unit,
) {
    val viewModel: SearchSharedViewModel = koinInject()
    val uiState by viewModel.uiState.collectAsState()
    val queryText by viewModel.queryText.collectAsState()

    val scrollState = rememberLazyListState()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
        ) {
            // Search bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
                OutlinedTextField(
                    value = queryText,
                    onValueChange = { viewModel.setQueryValue(it) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Поиск по маршрутам") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (queryText.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setQueryValue("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Очистить")
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Search,
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = { viewModel.onSearch() },
                    ),
                    shape = RoundedCornerShape(12.dp),
                )
            }

            // Hints as AssistChips
            if (uiState.isVisibleHints && uiState.hints.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    uiState.hints.forEach { hint ->
                        AssistChip(
                            onClick = {
                                if (hint.contains(queryText)) {
                                    viewModel.setQueryValue(hint)
                                } else {
                                    viewModel.setQueryValue("$queryText $hint")
                                }
                                viewModel.onSearch()
                            },
                            label = {
                                Box(
                                    modifier = Modifier.wrapContentSize().padding(4.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = hint.trim(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            },
                        )
                    }
                }
            }

            // Search results
            when (val state = uiState.searchState) {
                is SearchStateScreen.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                }

                is SearchStateScreen.Success -> {
                    val results = state.data
                    if (results != null && uiState.isVisibleResult) {
                        if (results.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "Ничего не найдено",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.padding(top = 12.dp),
                                verticalArrangement = Arrangement.Top,
                                state = scrollState,
                            ) {
                                items(results, key = { it.route.basicData.id }) { routeWithTag ->
                                    SearchListItem(
                                        route = routeWithTag.route,
                                        searchTag = routeWithTag.tag,
                                        searchValue = queryText,
                                        entityString = viewModel.entityString,
                                        onClick = { onRouteClick(routeWithTag.route.basicData) },
                                    )
                                }
                            }
                        }
                    } else if (queryText.isBlank()) {
                        if (!uiState.isVisibleHistory || uiState.searchHistoryList.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    )
                                    Text(
                                        text = "Введите запрос для поиска",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                is SearchStateScreen.Input -> { /* hints shown above */ }
                is SearchStateScreen.Failure -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Ошибка поиска: ${state.entity.message ?: "неизвестная ошибка"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            // History with slide + fade animation
            AnimatedVisibility(
                modifier = Modifier.fillMaxWidth(),
                visible = uiState.isVisibleHistory && uiState.searchHistoryList.isNotEmpty() && queryText.isEmpty(),
                enter = fadeIn(
                    animationSpec = tween(durationMillis = 500, delayMillis = 300)
                ) + slideInVertically(
                    animationSpec = tween(durationMillis = ANIMATION_SLIDE_TIME, delayMillis = 300)
                ),
                exit = fadeOut(
                    animationSpec = tween(durationMillis = 150)
                ) + slideOutVertically(
                    animationSpec = tween(durationMillis = ANIMATION_SLIDE_TIME)
                ),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    state = scrollState,
                ) {
                    items(uiState.searchHistoryList) { response ->
                        HistoryItem(
                            request = response.responseText,
                            removeOnClick = { viewModel.removeHistoryResponse(response.responseText) },
                            itemOnClick = {
                                viewModel.setQueryValue(response.responseText)
                                viewModel.onSearch()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryItem(
    request: String,
    removeOnClick: () -> Unit,
    itemOnClick: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clickable { itemOnClick() },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                modifier = Modifier.weight(1f).padding(start = 8.dp),
                text = request,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            IconButton(onClick = removeOnClick) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        HorizontalDivider(modifier = Modifier.padding(start = 48.dp, end = 12.dp))
    }
}

@Composable
private fun SearchListItem(
    route: Route,
    searchTag: SearchTag,
    searchValue: String,
    entityString: com.z_company.shared.util.EntityString?,
    onClick: () -> Unit,
) {
    val dateText = route.basicData.timeStartWork?.let { TimeFormatter.formatDate(it) } ?: ""

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Text(
            text = dateText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 16.dp, bottom = 12.dp)
                .shadow(elevation = 2.dp, shape = MaterialTheme.shapes.medium)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.medium,
                )
                .padding(8.dp),
        ) {
            val shownText = buildShownText(route, searchTag, entityString)

            val textWithSelection = buildAnnotatedString {
                append(shownText)

                val values = searchValue.trim().split(" ", ",").filter { it.isNotEmpty() }
                values.forEach { value ->
                    if (value.isNotEmpty()) {
                        var firstIndex = shownText.indexOf(value, 0, true)
                        while (firstIndex != -1) {
                            addStyle(
                                style = SpanStyle(background = MaterialTheme.colorScheme.inversePrimary),
                                start = firstIndex,
                                end = firstIndex + value.length,
                            )
                            firstIndex = shownText.indexOf(value, firstIndex + 1, true)
                        }
                    }
                }
            }

            Text(
                text = textWithSelection,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun buildShownText(
    route: Route,
    searchTag: SearchTag,
    entityString: com.z_company.shared.util.EntityString?,
): String {
    return when (searchTag) {
        SearchTag.BASIC_DATA -> entityString?.basicDataStr(route.basicData) ?: ""
        SearchTag.LOCO -> {
            route.locomotives.mapIndexed { index, loco ->
                if (index == 0) entityString?.locomotiveStr(loco) ?: ""
                else "\n\n${entityString?.locomotiveStr(loco) ?: ""}"
            }.joinToString("")
        }
        SearchTag.TRAIN -> {
            route.trains.mapIndexed { index, train ->
                if (index == 0) entityString?.trainStr(train) ?: ""
                else "\n\n${entityString?.trainStr(train) ?: ""}"
            }.joinToString("")
        }
        SearchTag.PASSENGER -> {
            route.passengers.mapIndexed { index, passenger ->
                if (index == 0) entityString?.passengerStr(passenger) ?: ""
                else "\n\n${entityString?.passengerStr(passenger) ?: ""}"
            }.joinToString("")
        }
        SearchTag.NOTES -> route.basicData.notes ?: ""
    }
}
