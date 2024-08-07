package com.z_company.route.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.z_company.core.ui.component.SearchAsyncData
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.core.util.DateAndTimeFormat
import com.z_company.core.util.str
import com.z_company.domain.entities.FilterSearch
import com.z_company.domain.entities.RouteWithTag
import com.z_company.domain.entities.SearchStateScreen
import com.z_company.domain.entities.SearchTag
import com.z_company.domain.entities.TimePeriod
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.SearchResponse
import com.z_company.domain.util.splitBySpaceAndComma
import com.z_company.route.R
import com.z_company.route.component.BottomShadow
import com.z_company.route.component.SearchSettingBottomSheet
import kotlinx.coroutines.launch
import com.z_company.route.component.SearchBar
import com.z_company.route.extention.isScrollInInitialState
import java.text.SimpleDateFormat
import java.util.Locale

private const val animationSlideTime = 150

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class,
    ExperimentalLayoutApi::class
)
@Composable
fun SearchScreen(
    setQueryValue: (TextFieldValue) -> Unit,
    query: TextFieldValue,
    onBack: () -> Unit,
    sendRequest: (String) -> Unit,
    clearFilter: () -> Unit,
    setSearchFilter: (Pair<String, Boolean>) -> Unit,
    setPeriodFilter: (TimePeriod) -> Unit,
    searchFilter: FilterSearch,
    isVisibleHistory: Boolean,
    hints: List<String>,
    searchState: SearchStateScreen<List<RouteWithTag>?>,
    onRouteClick: (String) -> Unit,
    searchHistoryList: List<SearchResponse>,
    removeHistoryResponse: (String) -> Unit,
    onSearch: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var openBottomSheet by rememberSaveable { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    val hintStyle = AppTypography.getType().titleLarge
        .copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Light
        )

    val closeSheet: () -> Unit = {
        scope.launch {
            bottomSheetState.hide()
            openBottomSheet = false
            sendRequest(query.text)
        }
    }

    if (openBottomSheet) {
        SearchSettingBottomSheet(
            bottomSheetState = bottomSheetState,
            closeSheet = closeSheet,
            clearFilter = clearFilter,
            filter = searchFilter,
            setFilter = setSearchFilter,
            setPeriodFilter = setPeriodFilter
        )
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(12.dp)
        ) {
            val scrollState = rememberLazyListState()

            SearchBar(
                modifier = Modifier
                    .fillMaxWidth(),
                query = query,
                onQueryChange = {
                    setQueryValue(it)
                    scope.launch {
                        scrollState.scrollToItem(0)
                    }
                },
                onBack = onBack,
                onSearch = onSearch,
                openSetting = { openBottomSheet = true }
            )
            AnimatedVisibility(
                modifier = Modifier
                    .zIndex(1f)
                    .padding(top = 8.dp),
                visible = !scrollState.isScrollInInitialState(),
                enter = fadeIn(animationSpec = tween(durationMillis = 300)),
                exit = fadeOut(animationSpec = tween(durationMillis = 300))
            ) {
                BottomShadow()
            }
            SearchAsyncData(
                resultState = searchState,
                inputContent = {
                    FlowRow(
                        modifier = Modifier.padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        hints.forEach { s ->
                            AssistChip(onClick = {
                                if (s.contains(query.text)) {
                                    setQueryValue(TextFieldValue(s))
                                } else {
                                    setQueryValue(TextFieldValue("${query.text} $s"))
                                }
                            }, label = {
                                Box(
                                    modifier = Modifier
                                        .wrapContentSize()
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = s.trim(), style = hintStyle)
                                }
                            })
                        }
                    }
                }) { resultList ->
                resultList?.let { list ->
                    LazyColumn(
                        verticalArrangement = Arrangement.Top, state = scrollState
                    ) {
                        if (list.isEmpty()) {
                            item {
                                ItemEmptyList()
                            }
                        } else {
                            items(list) { route ->
                                SearchListItem(
                                    route = route.route,
                                    searchTag = route.tag,
                                    searchValue = query.text
                                ) {
                                    onRouteClick(route.route.basicData.id)
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                modifier = Modifier
                    .fillMaxWidth(),
                visible = isVisibleHistory,
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = 500,
                        delayMillis = 300
                    )
                ) + slideInVertically(
                    animationSpec = tween(
                        durationMillis = animationSlideTime, delayMillis = 300
                    )
                ),
                exit = fadeOut(animationSpec = tween(durationMillis = 150)) + slideOutVertically(
                    animationSpec = tween(durationMillis = animationSlideTime)
                )
            ) {
                HistoryResponse(
                    searchHistoryList,
                    scrollState,
                    removeHistoryResponse,
                    itemOnClick = {
                        setQueryValue(it)
                        onSearch()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryResponse(
    historyList: List<SearchResponse>,
    scrollState: LazyListState,
    removeFromList: (String) -> Unit,
    itemOnClick: (TextFieldValue) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(), state = scrollState
    ) {
        items(historyList) { request ->
            HistoryItem(
                modifier = Modifier.animateItemPlacement(),
                request = request.responseText,
                removeOnClick = { removeFromList.invoke(request.responseText) },
                itemOnClick = { itemOnClick.invoke(TextFieldValue(request.responseText)) })
        }
    }
}

@Composable
fun HistoryItem(modifier: Modifier, request: String, removeOnClick: () -> Unit, itemOnClick: () -> Unit) {
    val hintStyle = AppTypography.getType().titleLarge
        .copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Light
        )
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clickable { itemOnClick.invoke() }, verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painterResource(id = R.drawable.outline_history_24),
                contentDescription = null
            )
            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                text = request,
                style = hintStyle
            )
            IconButton(onClick = { removeOnClick.invoke() }) {
                Icon(imageVector = Icons.Default.Close, contentDescription = null)
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
    onClick: () -> Unit
) {
    val date = SimpleDateFormat(
        DateAndTimeFormat.DATE_FORMAT, Locale.getDefault()
    ).format(route.basicData.timeStartWork)

    val hintStyle = AppTypography.getType().titleLarge
        .copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Light
        )
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(start = 24.dp, end = 12.dp, top = 16.dp)
        .clickable {
            onClick.invoke()
        }) {
        Text(text = date,
            style = hintStyle)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 16.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface, shape = Shapes.extraSmall
                )
                .padding(8.dp)
        ) {
            val shownText = when (searchTag) {
                SearchTag.BASIC_DATA -> {
                    StringBuilder(route.basicData.str())
                }

                SearchTag.LOCO -> {
                    val text = StringBuilder()
                    route.locomotives.forEachIndexed { index, loco ->
                        if (index == 0) {
                            text.append(loco.str())
                        } else {
                            text.append("\n\n${loco.str()}")
                        }
                    }
                    text
                }

                SearchTag.TRAIN -> {
                    val text = StringBuilder()
                    route.trains.forEachIndexed { index, train ->
                        if (index == 0) {
                            text.append(train.str())
                        } else {
                            text.append("\n\n${train.str()}")
                        }
                    }
                    text
                }

                SearchTag.PASSENGER -> {
                    val text = StringBuilder()
                    route.passengers.forEachIndexed { index, passenger ->
                        if (index == 0) {
                            text.append(passenger.str())
                        } else {
                            text.append("\n\n${passenger.str()}")
                        }
                    }
                    text
                }

                SearchTag.NOTES -> {
                    StringBuilder(route.basicData.notes.toString())
                }
            }

            val textWithSelection = buildAnnotatedString {
                append(shownText)

                val values = searchValue.trim().splitBySpaceAndComma()
                values.forEach { value ->
                    if (value.isNotEmpty()) {
                        var firstIndex = shownText.indexOf(value, 0, true)
                        while (firstIndex != -1) {
                            addStyle(
                                style = SpanStyle(background = MaterialTheme.colorScheme.inversePrimary),
                                start = firstIndex,
                                end = firstIndex + value.length
                            )
                            firstIndex = shownText.indexOf(value, firstIndex + 1)
                        }
                    }
                }
            }

            Text(text = textWithSelection, style = hintStyle)
        }
    }
}

@Composable
private fun ItemEmptyList() {
    val hintStyle = AppTypography.getType().titleLarge
        .copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Light
        )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp), contentAlignment = Alignment.Center
    ) {
        Text(text = "Ничего не найдено", style = hintStyle)
    }
}