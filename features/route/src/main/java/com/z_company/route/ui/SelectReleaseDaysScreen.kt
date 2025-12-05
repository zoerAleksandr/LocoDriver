package com.z_company.route.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import com.z_company.core.ResultState
import com.z_company.core.ui.component.DateRangePickerBottomSheet
import com.z_company.core.ui.component.customDatePicker.noRippleEffect
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.util.ConverterLongToTime
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.core.util.MonthFullText.getMonthFullText
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.ReleasePeriod
import com.z_company.domain.entities.UtilForMonthOfYear.getPersonalNormaHours
import com.z_company.route.component.ChipApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectReleaseDaysScreen(
    onSaveClick: () -> Unit,
    saveReleaseDaysState: ResultState<Unit>?,
    onReleaseDaysSaved: () -> Unit,
    monthOfYear: MonthOfYear?,
    releasePeriodListState: SnapshotStateList<ReleasePeriod>?,
    addingReleasePeriod: (ReleasePeriod) -> Unit,
    removingReleasePeriod: (ReleasePeriod) -> Unit,
    yearList: List<Int>,
    monthList: List<Int>,
    selectMonthOfYear: (Pair<Int, Int>) -> Unit,
    dateAndTimeConverter: DateAndTimeConverter?
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    TextButton(
                        onClick = onSaveClick,
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = Color.Transparent,
                            disabledContentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            contentColor = MaterialTheme.colorScheme.tertiary,
                            containerColor = Color.Transparent
                        )
                    ) {
                        Text(
                            text = "Сохранить",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                },
                title = {},
                colors = TopAppBarDefaults.topAppBarColors().copy(
                    containerColor = Color.Transparent,
                )
            )
        }) { paddingValues ->
        if (saveReleaseDaysState is ResultState.Success) {
            LaunchedEffect(saveReleaseDaysState) {
                onReleaseDaysSaved()
            }
        } else {
            Box(modifier = Modifier.padding(paddingValues)) {
                SelectReleaseDaysContent(
                    monthOfYear = monthOfYear,
                    releasePeriodListState = releasePeriodListState,
                    addingReleasePeriod = addingReleasePeriod,
                    removingReleasePeriod = removingReleasePeriod,
                    yearList = yearList,
                    monthList = monthList,
                    selectMonthOfYear = selectMonthOfYear,
                    dateAndTimeConverter = dateAndTimeConverter
                )
            }
        }
    }
}

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalLayoutApi::class
)
@Composable
fun SelectReleaseDaysContent(
    monthOfYear: MonthOfYear?,
    releasePeriodListState: SnapshotStateList<ReleasePeriod>?,
    addingReleasePeriod: (ReleasePeriod) -> Unit,
    removingReleasePeriod: (ReleasePeriod) -> Unit,
    yearList: List<Int>,
    monthList: List<Int>,
    selectMonthOfYear: (Pair<Int, Int>) -> Unit,
    dateAndTimeConverter: DateAndTimeConverter?
) {
    val styleData = MaterialTheme.typography.bodyLarge

    var showBottomSheet by remember { mutableStateOf(false) }

    if (showBottomSheet) {
        DateRangePickerBottomSheet(
            onDateRangeSelected = { list ->
                val releasePeriod = ReleasePeriod(days = list)
                addingReleasePeriod(releasePeriod)
            },
            onDismiss = {
                showBottomSheet = false
            },
            title = "Период отвлечения",
            singleMode = false
        )
    }

    var showMonthSelectorDialog by remember {
        mutableStateOf(false)
    }

    val monthSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    if (showMonthSelectorDialog) {
        monthOfYear?.let { currentMonthOfYear ->
            ModalBottomSheet(
                onDismissRequest = { showMonthSelectorDialog = false },
                sheetState = monthSheetState,
                containerColor = MaterialTheme.colorScheme.secondary,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Выберите месяц и год",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    var selectedMonth by remember { mutableIntStateOf(currentMonthOfYear.month) }

                    var selectedYear by remember { mutableIntStateOf(currentMonthOfYear.year) }

                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        monthList.forEach { m ->
                            val selected = selectedMonth == m
                            ChipApp(
                                selected = selected,
                                onClick = { selectedMonth = m },
                                label = getMonthFullText(m)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        yearList.forEach { y ->
                            val selected = selectedYear == y
                            ChipApp(
                                selected = selected,
                                onClick = { selectedYear = y },
                                label = "$y"
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            selectMonthOfYear(selectedYear to selectedMonth)
                            showMonthSelectorDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Text(
                            text = "Применить",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )

                    }
                    Spacer(modifier = Modifier.height(24.dp))

                }

            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .noRippleEffect {
                        showMonthSelectorDialog = true
                    }
                    .shadow(elevation = 2.dp, shape = Shapes.medium)
                    .background(
                        color = MaterialTheme.colorScheme.secondary,
                        shape = Shapes.medium
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = getMonthFullText(monthOfYear?.month),
                        style = styleData,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = ConverterLongToTime.getTimeInStringFormat(
                            monthOfYear?.getPersonalNormaHours()?.toLong()?.times(3_600_000)
                        ),
                        style = styleData,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Box(
                modifier = Modifier
                    .shadow(elevation = 2.dp, shape = Shapes.medium)
                    .background(
                        color = MaterialTheme.colorScheme.secondary,
                        shape = Shapes.medium
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    item {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = "Периоды отвлечения: ",
                            style = styleData,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    releasePeriodListState?.let { releaseDayList ->
                        if (releaseDayList.isNotEmpty()) {

                            // Сортируем список один раз, только по периодам с непустыми днями
                            val sortedReleasePeriods = releaseDayList
                                .filter { it.days.isNotEmpty() }                    // убираем пустые
                                .sortedBy { it.days.minOf { day -> day.timeInMillis } }  // берём самую раннюю дату в периоде
//
//                            releaseDayList.forEach { period ->
//                                if (period.days.isNotEmpty()) {
//                                    releaseDayList.sortBy { periodDays ->
//                                        periodDays.days.first().timeInMillis
//                                    }
//                                }
//                            }
                            items(sortedReleasePeriods, key = { period ->
                                period.id
                            }) { period ->
                                if (period.days.isNotEmpty()) {
                                    HorizontalDivider()
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .animateItemPlacement()
                                            .padding(top = 8.dp, start = 16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = dateAndTimeConverter?.getDateFromDateLong(
                                                    period.days.first().timeInMillis
                                                ) ?: "",
                                                style = styleData,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            if (period.days.size > 1) {
                                                period.days.last().let {
                                                    Text(
                                                        text = " - ",
                                                        style = styleData,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        text = dateAndTimeConverter?.getDateFromDateLong(
                                                            it.timeInMillis
                                                        ) ?: "",
                                                        style = styleData,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                        Icon(
                                            modifier = Modifier.clickable {
                                                removingReleasePeriod(period)
                                            },
                                            imageVector = Icons.Outlined.Clear,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            shape = Shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            onClick = {
                showBottomSheet = true
            }) {
            Text(
                text = "Добавить отвлечение",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}