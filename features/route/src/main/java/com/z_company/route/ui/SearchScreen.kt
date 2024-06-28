package com.z_company.route.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.core.util.DateAndTimeFormat
import com.z_company.core.util.splitBySpaceAndComma
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Route
import com.z_company.route.R
import com.z_company.route.component.BottomShadow
import com.z_company.route.component.BottomSheetSearch
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
    addResponse: (String) -> Unit,
    clearFilter: () -> Unit,
    setSearchFilter: (Pair<String, Boolean>) -> Unit,
    setPeriodFilter: (TimePeriod) -> Unit,
    searchFilter: FilterSearch,
    isVisibleHints: Boolean,
    isVisibleHistory: Boolean,
    isVisibleResult: Boolean,
    hints: List<String>,
    searchResultList: List<Route>,
    onRouteClick: (BasicData) -> Unit,
    searchHistoryList: List<String>,
    removeHistoryResponse: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var openBottomSheet by rememberSaveable { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    val closeSheet: () -> Unit = {
        scope.launch {
            bottomSheetState.hide()
            openBottomSheet = false
            sendRequest(query.text)
        }
    }

    if (openBottomSheet) {
        ModalBottomSheet(
            sheetState = bottomSheetState,
            onDismissRequest = closeSheet,
            containerColor = MaterialTheme.colorScheme.background,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = closeSheet
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = null)
                        }
                        TextButton(
                            onClick = {
                                clearFilter()
                            }
                        ) {
                            Text(
                                text = "Сбросить",
                                style = AppTypography.getType().bodyMedium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                    Text(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .align(Alignment.TopCenter),
                        text = "Параметры",
                        style = AppTypography.getType().titleMedium
                    )
                }
            }
        ) {
            BottomSheetSearch(
                searchFilter,
                setSearchFilter,
                setPeriodFilter
            )
        }
    }

    Scaffold { paddingValues ->
        ConstraintLayout(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val (searchBar, shadow, hintsChips, historyList, list) = createRefs()
            val scrollState = rememberLazyListState()

            SearchBar(
                modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp, top = 16.dp)
                    .constrainAs(searchBar) {
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        top.linkTo(parent.top)
                        width = Dimension.fillToConstraints
                    },
                query = query,
                onQueryChange = {
                    setQueryValue(it)
                    scope.launch {
                        scrollState.scrollToItem(0)
                    }
                },
                onSearchFocusChange = { isFocus ->
                    if (isFocus) {
//                        viewModel.searchState.value = SearchStateScreen.Input(hints)
                    }
                },
                onBack = onBack,
                onSearch = {
                    sendRequest(query.text)
                    addResponse(query.text)
                },
                openSetting = { openBottomSheet = true }
            )

            AnimatedVisibility(
                modifier = Modifier
                    .zIndex(1f)
                    .constrainAs(shadow) {
                        top.linkTo(searchBar.bottom)
                    }
                    .padding(top = 8.dp),
                visible = !scrollState.isScrollInInitialState(),
                enter = fadeIn(animationSpec = tween(durationMillis = 300)),
                exit = fadeOut(animationSpec = tween(durationMillis = 300))
            ) {
                BottomShadow()
            }

            Box(modifier = Modifier
                .constrainAs(hintsChips) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(searchBar.bottom)
                    width = Dimension.fillToConstraints
                }
                .padding(top = 16.dp)) {
                AnimatedVisibility(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    visible = isVisibleHints,
                    enter = fadeIn(
                        animationSpec = tween(
                            durationMillis = 500,
                            delayMillis = 100
                        )
                    ) + slideInVertically(
                        animationSpec = tween(
                            durationMillis = animationSlideTime, delayMillis = 100
                        )
                    ),
                    exit = fadeOut(animationSpec = tween(durationMillis = 150)) + slideOutVertically(
                        animationSpec = tween(durationMillis = animationSlideTime)
                    )
                ) {
                    FlowRow(
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
                                    modifier = Modifier.wrapContentSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = s.trim())
                                }
                            })
                        }
                    }
                }
            }
            Box(modifier = Modifier.constrainAs(historyList) {
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                top.linkTo(anchor = hintsChips.bottom)
                bottom.linkTo(parent.bottom)
                height = Dimension.fillToConstraints
            }) {
                AnimatedVisibility(
                    modifier = Modifier.padding(horizontal = 8.dp),
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
                        setQueryValue
                    )
                }
            }

            Box(modifier = Modifier
                .constrainAs(list) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(searchBar.bottom)
                    bottom.linkTo(parent.bottom)
                    height = Dimension.fillToConstraints
                }
                .padding(top = 8.dp)) {
                AnimatedVisibility(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    visible = isVisibleResult,
                    enter = fadeIn(
                        animationSpec = tween(
                            durationMillis = 200,
                            delayMillis = 200
                        )
                    ) + slideInVertically(
                        initialOffsetY = { it / 2 }, animationSpec = tween(
                            durationMillis = animationSlideTime, delayMillis = 200
                        )
                    ),
                    exit = fadeOut(
                        animationSpec = tween(
                            durationMillis = 200,
                            delayMillis = 100
                        )
                    ) + slideOutVertically(
                        targetOffsetY = { it / 2 }, animationSpec = tween(
                            durationMillis = animationSlideTime, delayMillis = 100
                        )
                    )
                ) {
                    LazyColumn(
                        verticalArrangement = Arrangement.Top, state = scrollState
                    ) {
                        if (searchResultList.isEmpty()) {
                            item {
                                ItemEmptyList()
                            }
                        } else {
                            items(searchResultList) { route ->
                                SearchListItem(
                                    route = route,
                                    searchTag = SearchTag.BASIC_DATA, // TODO,
                                    searchValue = query.text
                                ) {
                                    onRouteClick(route.basicData)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryResponse(
    historyList: List<String>,
    scrollState: LazyListState,
    removeFromList: (String) -> Unit,
    itemOnClick: (TextFieldValue) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(), state = scrollState
    ) {
        items(historyList) { request ->
            HistoryItem(request = request,
                removeOnClick = { removeFromList.invoke(request) },
                itemOnClick = { itemOnClick.invoke(TextFieldValue(request)) })
        }
    }
}

@Composable
fun HistoryItem(request: String, removeOnClick: () -> Unit, itemOnClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 8.dp)
                .clickable { itemOnClick.invoke() }, verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painterResource(id = com.z_company.core.R.drawable.ic_star_border),
                contentDescription = null
            )
            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp), text = request
            )
            IconButton(onClick = { removeOnClick.invoke() }) {
                Icon(imageVector = Icons.Default.Close, contentDescription = null)
            }
        }
        Divider(modifier = Modifier.padding(start = 48.dp, end = 12.dp))
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

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(start = 32.dp, end = 32.dp, top = 16.dp)
        .clickable {
            onClick.invoke()
        }) {
        Text(text = date, style = AppTypography.getType().labelSmall)
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
                    StringBuilder(route.basicData.toString())
                }

                SearchTag.LOCO -> {
                    val text = StringBuilder()
                    route.locomotives.forEachIndexed { index, loco ->
                        if (index == 0) {
                            text.append("$loco")
                        } else {
                            text.append("\n\n$loco")
                        }
                    }
                    text
                }

                SearchTag.TRAIN -> {
                    val text = StringBuilder()
                    route.trains.forEachIndexed { index, train ->
                        if (index == 0) {
                            text.append("$train")
                        } else {
                            text.append("\n\n$train")
                        }
                    }
                    text
                }

                SearchTag.PASSENGER -> {
                    val text = StringBuilder()
                    route.passengers.forEachIndexed { index, passenger ->
                        if (index == 0) {
                            text.append("$passenger")
                        } else {
                            text.append("\n\n$passenger")
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
                    var firstIndex = shownText.indexOf(value, 0, true)
                    while (firstIndex != -1) {
                        addStyle(
                            style = SpanStyle(background = Color.Yellow),
                            start = firstIndex,
                            end = firstIndex + value.length
                        )
                        firstIndex = shownText.indexOf(value, firstIndex + 1)
                    }
                }
            }

            Text(text = textWithSelection)
        }
    }
}

@Composable
private fun ItemEmptyList() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp), contentAlignment = Alignment.Center
    ) {
        Text(text = "Ничего не найдено")
    }
}