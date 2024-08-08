package com.z_company.route.component

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.FilterNames
import com.z_company.domain.entities.FilterSearch
import com.z_company.domain.entities.TimePeriod

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
    setPeriodFilter: (TimePeriod) -> Unit
) {
    val dataTextStyle = AppTypography.getType().titleLarge.copy(fontWeight = FontWeight.Light)
    val subTitleTextStyle = AppTypography.getType().titleLarge
        .copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal
        )
    val hintStyle = AppTypography.getType().titleLarge
        .copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Light
        )
    var showDatePickerStart by rememberSaveable {
        mutableStateOf(false)
    }

    var showDatePickerEnd by rememberSaveable {
        mutableStateOf(false)
    }

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

    val startDatePickerState = rememberDatePickerState()
    if (showDatePickerStart) {
        CustomDatePickerDialog(
            datePickerState = startDatePickerState,
            onDismissRequest = { showDatePickerStart = false },
            onConfirmRequest = {
                startDatePickerState.selectedDateMillis?.let { selectDate ->
                    setPeriodFilter(
                        TimePeriod(
                            selectDate,
                            endDate
                        )
                    )
                    startDate = selectDate
                }
                showDatePickerStart = false
            }
        )
    }

    val endDatePickerState = rememberDatePickerState()
    if (showDatePickerEnd) {
        CustomDatePickerDialog(
            datePickerState = endDatePickerState,
            onDismissRequest = { showDatePickerEnd = false },
            onConfirmRequest = {
                endDatePickerState.selectedDateMillis?.let { selectDate ->
                    setPeriodFilter(
                        TimePeriod(
                            startDate,
                            selectDate,
                        )
                    )
                    endDate = selectDate
                }
                showDatePickerEnd = false
            }
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
                        Icon(imageVector = Icons.Default.Close, contentDescription = null)
                    }
                    TextButton(
                        onClick = {
                            clearFilter()
                        }
                    ) {
                        Text(
                            text = "Сбросить",
                            style = subTitleTextStyle,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                Text(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .align(Alignment.TopCenter),
                    text = "Параметры",
                    style = AppTypography.getType().headlineMedium.copy(fontWeight = FontWeight.Light)
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
                style = subTitleTextStyle,
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
                                style = dataTextStyle
                            ) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
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
                                style = dataTextStyle
                            ) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
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
                                style = dataTextStyle
                            ) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
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
                                style = dataTextStyle
                            ) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
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
                                style = dataTextStyle
                            ) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                        )
                    )
                }
            }
            Text(
                modifier = Modifier.padding(top = 32.dp),
                text = "Период времени",
                style = subTitleTextStyle,
                textAlign = TextAlign.Center
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
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
                    val dateStartText = DateAndTimeConverter.getDateFromDateLong(startDate)
                    val startPeriodText = "c $dateStartText"

                    Text(
                        text = startPeriodText,
                        style = dataTextStyle,
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
                    val dateEndText = DateAndTimeConverter.getDateFromDateLong(endDate)
                    val endPeriodText = "по $dateEndText"

                    Text(
                        text = endPeriodText,
                        style = dataTextStyle,
                    )
                }
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
