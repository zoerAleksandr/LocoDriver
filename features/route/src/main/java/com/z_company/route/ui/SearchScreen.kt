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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.z_company.core.ui.component.SearchAsyncData
import com.z_company.core.ui.component.customDatePicker.noRippleEffect
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.core.util.DateAndTimeFormat
import com.z_company.core.util.EntityString
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
import com.z_company.route.component.ChipApp
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
    onSearch: () -> Unit,
    entityString: EntityString?,
    dateAndTimeConverter: DateAndTimeConverter?
) {
    val scope = rememberCoroutineScope()
    var openBottomSheet by rememberSaveable { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val scrollState = rememberLazyListState()


    val hintStyle = MaterialTheme.typography.bodyMedium
    val dataStyle = MaterialTheme.typography.bodyLarge

    val primaryColor = MaterialTheme.colorScheme.primary

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
            setPeriodFilter = setPeriodFilter,
            dateAndTimeConverter = dateAndTimeConverter
        )
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
        ) {
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        hints.forEach { s ->
                            ChipApp(
                                selected = false,
                                label = s.trim(),
                                onClick = {
                                    if (s.contains(query.text)) {
                                        setQueryValue(TextFieldValue(s))
                                    } else {
                                        setQueryValue(TextFieldValue("${query.text} $s"))
                                    }
                                    onSearch()
                                }
                            )
                        }
                    }
                }) { resultList ->
                resultList?.let { list ->
                    LazyColumn(
                        modifier = Modifier.padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        state = scrollState
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
                                    searchValue = query.text,
                                    entityString = entityString
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
        if (historyList.isNotEmpty()) {
            item {
                Text(
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                    text = "НЕДАВНИЕ ЗАПРОСЫ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(historyList) { request ->
            HistoryItem(
                modifier = Modifier.animateItem(),
                request = request.responseText,
                removeOnClick = { removeFromList.invoke(request.responseText) },
                itemOnClick = { itemOnClick.invoke(TextFieldValue(request.responseText)) })
        }
    }
}

@Composable
fun HistoryItem(
    modifier: Modifier,
    request: String,
    removeOnClick: () -> Unit,
    itemOnClick: () -> Unit
) {
    val hintStyle = MaterialTheme.typography.bodyMedium

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clickable { itemOnClick.invoke() }, verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painterResource(id = R.drawable.outline_history_24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                text = request,
                style = hintStyle,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = { removeOnClick.invoke() }) {
                Icon(
                    painter = painterResource(com.z_company.core.R.drawable.ic_clear),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
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
    entityString: EntityString?,
    onClick: () -> Unit
) {
    val date = route.basicData.timeStartWork?.let {
        SimpleDateFormat(DateAndTimeFormat.DATE_FORMAT, Locale.getDefault()).format(it)
    } ?: ""

    val tagLabel = when (searchTag) {
        SearchTag.BASIC_DATA -> "Основные данные"
        SearchTag.LOCO -> "Локомотив"
        SearchTag.TRAIN -> "Поезд"
        SearchTag.PASSENGER -> "Следование пассажиром"
        SearchTag.OTHER_WORK -> "Прочая работа"
        SearchTag.PARTNER -> "Напарник"
        SearchTag.NOTES -> "Примечания"
    }

    val numberText = route.basicData.number?.takeIf { it.isNotBlank() }
        ?.let { "№$it" } ?: "Маршрут"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 1.dp, shape = Shapes.medium)
            .background(color = MaterialTheme.colorScheme.surface, shape = Shapes.medium)
            .clip(Shapes.medium)
            .clickable { onClick.invoke() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Шапка карточки: анкер маршрута (№ + дата) — как на карточках Главного/Маршрутов.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = numberText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = com.z_company.core.ui.theme.MonoFont,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.primary,
            )
            if (date.isNotBlank()) {
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = com.z_company.core.ui.theme.MonoFont,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // Лейбл группы, в которой нашлось совпадение (как заголовки групп в карточке).
        Text(
            text = tagLabel.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = com.z_company.core.ui.theme.MonoFont,
                letterSpacing = 1.2.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            val shownText = when (searchTag) {
                SearchTag.BASIC_DATA -> {
                    StringBuilder(entityString?.basicDataStr(route.basicData) ?: "")
                }

                SearchTag.LOCO -> {
                    val text = StringBuilder()
                    route.locomotives.forEachIndexed { index, loco ->
                        if (index == 0) {
                            text.append(entityString?.locomotiveStr(loco))
                        } else {
                            text.append("\n\n${entityString?.locomotiveStr(loco)}")
                        }
                    }
                    text
                }

                SearchTag.TRAIN -> {
                    val text = StringBuilder()
                    route.trains.forEachIndexed { index, train ->
                        if (index == 0) {
                            text.append(entityString?.trainStr(train))
                        } else {
                            text.append("\n\n${entityString?.trainStr(train)}")
                        }
                    }
                    text
                }

                SearchTag.PASSENGER -> {
                    val text = StringBuilder()
                    route.passengers.forEachIndexed { index, passenger ->
                        if (index == 0) {
                            text.append(entityString?.passengerStr(passenger))
                        } else {
                            text.append("\n\n${entityString?.passengerStr(passenger)}")
                        }
                    }
                    text
                }

                SearchTag.OTHER_WORK -> {
                    val text = StringBuilder()
                    route.otherWorks.forEachIndexed { index, otherWork ->
                        if (index == 0) {
                            text.append(entityString?.otherWorkStr(otherWork))
                        } else {
                            text.append("\n\n${entityString?.otherWorkStr(otherWork)}")
                        }
                    }
                    text
                }

                SearchTag.PARTNER -> {
                    val text = StringBuilder()
                    route.partners.forEachIndexed { index, partner ->
                        if (index == 0) {
                            text.append(entityString?.partnerStr(partner))
                        } else {
                            text.append("\n\n${entityString?.partnerStr(partner)}")
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

            Text(
                text = textWithSelection,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ItemEmptyList() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.search_24px),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(40.dp)
        )
        Text(
            text = "Ничего не найдено",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Измените запрос или проверьте фильтры",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}