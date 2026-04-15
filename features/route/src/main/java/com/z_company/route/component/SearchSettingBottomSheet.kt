package com.z_company.route.component

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.z_company.route.component.AppDateTimePicker
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.FilterNames
import com.z_company.domain.entities.FilterSearch
import com.z_company.domain.entities.TimePeriod
import java.util.Calendar
import com.z_company.core.R

@OptIn(
    ExperimentalLayoutApi::class,
    ExperimentalMaterial3Api::class,
)
@Composable
fun SearchSettingBottomSheet(
    filter: FilterSearch,
    setFilter: (Pair<String, Boolean>) -> Unit,
    bottomSheetState: SheetState,
    closeSheet: () -> Unit,
    clearFilter: () -> Unit,
    setPeriodFilter: (TimePeriod) -> Unit,
    dateAndTimeConverter: DateAndTimeConverter?
) {
    val hintStyle = MaterialTheme.typography.bodyMedium
    val dataStyle = MaterialTheme.typography.bodyLarge

    val primaryColor = MaterialTheme.colorScheme.primary

    var startDate: Long? by remember {
        mutableStateOf(null)
    }

    var endDate: Long? by remember {
        mutableStateOf(null)
    }

    filter.timePeriod.let { period ->
        startDate = period.startDate
        endDate = period.endDate
    }

    var showDatePickerStart by rememberSaveable {
        mutableStateOf(false)
    }

    if (showDatePickerStart) {
        AppDateTimePicker(
            title = "Начало периода",
            onDateTimeSelected = { timestamp ->
                startDate = timestamp
                setPeriodFilter(
                    TimePeriod(
                        startDate,
                        endDate
                    )
                )
            },
            onDismiss = { showDatePickerStart = false },
            startDateTime = startDate ?: Calendar.getInstance().timeInMillis,
        )
    }

    var showDatePickerEnd by rememberSaveable {
        mutableStateOf(false)
    }

    if (showDatePickerEnd) {
        AppDateTimePicker(
            title = "Конец периода",
            onDateTimeSelected = { timestamp ->
                endDate = timestamp
                setPeriodFilter(
                    TimePeriod(
                        startDate,
                        endDate
                    )
                )
            },
            onDismiss = { showDatePickerEnd = false },
            startDateTime = endDate ?: Calendar.getInstance().timeInMillis,
        )
    }

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
                        Icon(
                            painter = painterResource(R.drawable.ic_clear),
                            tint = primaryColor,
                            contentDescription = null
                        )
                    }
                    TextButton(
                        onClick = {
                            clearFilter()
                        }
                    ) {
                        Text(
                            text = "Сбросить",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                Text(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .align(Alignment.TopCenter),
                    text = "Параметры",
                    color = primaryColor,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                modifier = Modifier.padding(top = 32.dp),
                text = "Где искать",
                style = dataStyle,
                color = primaryColor,
                textAlign = TextAlign.Center
            )
            FlowRow(
                modifier = Modifier.padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                filter.generalData.let { pair ->
                    FilterChip(
                        selected = pair.second,
                        onClick = {
                            setFilter(
                                Pair(
                                    FilterNames.GENERAL_DATA.value,
                                    !pair.second
                                )
                            )
                        },
                        label = {
                            Text(
                                modifier = Modifier.padding(4.dp),
                                text = pair.first,
                                style = hintStyle,
                                color = primaryColor
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.surfaceDim,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            selected = pair.second,
                            enabled = pair.second,
                            selectedBorderWidth = 1.dp,
                            selectedBorderColor = MaterialTheme.colorScheme.tertiary,
                            disabledBorderColor = Color.Transparent
                        )
                    )
                }

                filter.locoData.let { pair ->
                    FilterChip(
                        selected = pair.second,
                        onClick = {
                            setFilter(
                                Pair(
                                    FilterNames.LOCO_DATA.value,
                                    !pair.second
                                )
                            )
                        },
                        label = {
                            Text(
                                modifier = Modifier.padding(4.dp),
                                text = pair.first,
                                style = hintStyle,
                                color = primaryColor
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.surfaceDim
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            selected = pair.second,
                            enabled = pair.second,
                            selectedBorderWidth = 1.dp,
                            selectedBorderColor = MaterialTheme.colorScheme.tertiary,
                            disabledBorderColor = Color.Transparent
                        )
                    )
                }

                filter.trainData.let { pair ->
                    FilterChip(
                        selected = pair.second,
                        onClick = {
                            setFilter(
                                Pair(
                                    FilterNames.TRAIN_DATA.value,
                                    !pair.second
                                )
                            )
                        },
                        label = {
                            Text(
                                modifier = Modifier.padding(4.dp),
                                text = pair.first,
                                style = hintStyle,
                                color = primaryColor
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.surfaceDim
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            selected = pair.second,
                            enabled = pair.second,
                            selectedBorderWidth = 1.dp,
                            selectedBorderColor = MaterialTheme.colorScheme.tertiary,
                            disabledBorderColor = Color.Transparent
                        )
                    )
                }

                filter.passengerData.let { pair ->
                    FilterChip(
                        selected = pair.second,
                        onClick = {
                            setFilter(
                                Pair(
                                    FilterNames.PASSENGER_DATA.value,
                                    !pair.second
                                )
                            )
                        },
                        label = {
                            Text(
                                modifier = Modifier.padding(4.dp),
                                text = pair.first,
                                style = hintStyle,
                                color = primaryColor
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.surfaceDim
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            selected = pair.second,
                            enabled = pair.second,
                            selectedBorderWidth = 1.dp,
                            selectedBorderColor = MaterialTheme.colorScheme.tertiary,
                            disabledBorderColor = Color.Transparent
                        )
                    )
                }

                filter.notesData.let { pair ->
                    FilterChip(
                        selected = pair.second,
                        onClick = {
                            setFilter(
                                Pair(
                                    FilterNames.NOTES_DATA.value,
                                    !pair.second
                                )
                            )
                        },
                        label = {
                            Text(
                                modifier = Modifier.padding(4.dp),
                                text = pair.first,
                                style = hintStyle,
                                color = primaryColor
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.surfaceDim
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            selected = pair.second,
                            enabled = pair.second,
                            selectedBorderWidth = 1.dp,
                            selectedBorderColor = MaterialTheme.colorScheme.tertiary,
                            disabledBorderColor = Color.Transparent
                        )
                    )
                }
            }
            Text(
                modifier = Modifier.padding(top = 32.dp),
                text = "Период времени",
                style = dataStyle,
                textAlign = TextAlign.Center,
                color = primaryColor
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            showDatePickerStart = true
                        }
                        .border(
                            width = 1.dp,
                            shape = Shapes.small,
                            color = MaterialTheme.colorScheme.outline
                        )
                        .padding(horizontal = 18.dp, vertical = 6.dp),
                ) {
                    val dateStartText = dateAndTimeConverter?.getDateFromDateLong(startDate) ?: ""
                    val startPeriodText = "c $dateStartText"

                    Text(
                        text = startPeriodText,
                        style = hintStyle,
                        color = primaryColor
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            showDatePickerEnd = true
                        }
                        .border(
                            width = 1.dp,
                            shape = Shapes.small,
                            color = MaterialTheme.colorScheme.outline
                        )
                        .padding(horizontal = 18.dp, vertical = 6.dp),
                ) {
                    val dateEndText = dateAndTimeConverter?.getDateFromDateLong(endDate) ?: ""
                    val endPeriodText = "по $dateEndText"

                    Text(
                        text = endPeriodText,
                        style = hintStyle,
                        color = primaryColor
                    )
                }
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
