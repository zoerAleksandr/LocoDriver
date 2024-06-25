package com.z_company.settings.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.z_company.core.R as CoreR
import com.z_company.core.ui.theme.custom.AppTypography
import java.util.Calendar
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.DateRangePickerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.util.ConverterLongToTime
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.core.util.DateAndTimeConverter.getMonthFullText
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.ReleasePeriod
import com.z_company.domain.entities.UtilForMonthOfYear.getNormaHours
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SelectReleaseDaysScreen(
    onBack: () -> Unit,
    onSaveClick: () -> Unit,
    monthOfYear: MonthOfYear?,
    releasePeriodListState: SnapshotStateList<ReleasePeriod>?,
    addingReleasePeriod: (ReleasePeriod) -> Unit,
    removingReleasePeriod: (ReleasePeriod) -> Unit,
) {
    val dateRangePickerState = rememberDateRangePickerState()
    val scope = rememberCoroutineScope()
    val styleTitle = AppTypography.getType().bodySmall
    val styleData = AppTypography.getType().bodyMedium

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = CoreR.drawable.ic_arrow_back),
                            contentDescription = stringResource(id = CoreR.string.cd_back)
                        )
                    }
                },
                title = {
                    Text(text = stringResource(id = CoreR.string.norma_hours))
                },
                actions = {
                    TextButton(onClick = onSaveClick) {
                        Text(
                            text = "Готово",
                            style = AppTypography.getType().bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors().copy(
                    containerColor = Color.Transparent,
                )
            )
        },

        ) {
        val sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )
        var showBottomSheet by remember { mutableStateOf(false) }

        if (showBottomSheet) {
            SelectRangeDateBottomSheet(
                onConfirmRequest = { period ->
                    addingReleasePeriod(period)
                },
                onDismissRequest = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            showBottomSheet = false
                        }
                    }
                },
                sheetState = sheetState,
                dateRangePickerState = dateRangePickerState
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = it.calculateTopPadding(),
                    bottom = it.calculateBottomPadding(),
                    start = 16.dp,
                    end = 16.dp
                ),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = Shapes.medium
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = monthOfYear?.month?.getMonthFullText() ?: "",
                            style = styleData
                        )
                        Text(
                            text = ConverterLongToTime.getTimeInStringFormat(
                                monthOfYear?.getNormaHours()?.toLong()?.times(3_600_000)
                            ),
                            style = styleData
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = Shapes.medium
                        )
                        .padding(16.dp)
                ) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        item {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = "Периоды отвлечения: ",
                                style = styleData
                            )
                        }
                        releasePeriodListState?.let { releaseDayList ->
                            releaseDayList.sortBy { period ->
                                period.start.timeInMillis
                            }
                            items(releasePeriodListState, key = { period ->
                                period.id
                            }) { period ->
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
                                            text = DateAndTimeConverter.getDateFromDateLong(period.start.timeInMillis),
                                            style = styleData
                                        )
                                        period.end?.let {
                                            Text(text = " - ", style = styleData)
                                            Text(
                                                text = DateAndTimeConverter.getDateFromDateLong(it.timeInMillis),
                                                style = styleData
                                            )
                                        }
                                    }
                                    Icon(
                                        modifier = Modifier.clickable {
                                            removingReleasePeriod(period)
                                        },
                                        imageVector = Icons.Outlined.Clear,
                                        contentDescription = null
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = Shapes.medium,
                onClick = {
                    showBottomSheet = true
                }) {
                Text("Добавить отвлечение")
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SelectRangeDateBottomSheet(
    onDismissRequest: () -> Unit,
    onConfirmRequest: (ReleasePeriod) -> Unit,
    sheetState: SheetState,
    dateRangePickerState: DateRangePickerState
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        dragHandle = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween

            ) {
                TextButton(onClick = {
                    dateRangePickerState.setSelection(null, null)
                }) {
                    Text(
                        text = "Сбросить",
                        style = AppTypography.getType().bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                TextButton(onClick = {
                    val startRangeInMillis = dateRangePickerState.selectedStartDateMillis
                    val endRangeInMillis = dateRangePickerState.selectedEndDateMillis

                    var startRangeCalendar: Calendar? = null
                    startRangeInMillis?.let {
                        startRangeCalendar = Calendar.getInstance().also { calendar ->
                            calendar.timeInMillis = it
                        }
                    }

                    var endRangeCalendar: Calendar? = null
                    endRangeInMillis?.let {
                        endRangeCalendar = Calendar.getInstance().also { calendar ->
                            calendar.timeInMillis = it
                        }
                    }
                    startRangeCalendar?.let {
                        onConfirmRequest(
                            ReleasePeriod(
                                start = it,
                                end = endRangeCalendar
                            )
                        )
                    }
                    onDismissRequest()
                }) {
                    Text(
                        text = "Выбрать",
                        style = AppTypography.getType().bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                showModeToggle = false
            )
        }
    }
}