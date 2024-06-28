package com.z_company.route.component

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.route.ui.FilterNames
import com.z_company.route.ui.FilterSearch
import com.z_company.route.ui.TimePeriod

@OptIn(
    ExperimentalLayoutApi::class,
    ExperimentalMaterial3Api::class,
)
@Composable
fun BottomSheetSearch(
    filter: FilterSearch,
    setFilter: (Pair<String, Boolean>) -> Unit,
    setPeriodFilter: (TimePeriod) -> Unit
) {
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

    val startDatePickerState = rememberDatePickerState()

    filter.timePeriod.let { period ->
        startDate = period.startDate
        endDate = period.endDate
    }

    if (showDatePickerStart) {
        DatePickerDialog(
            datePickerState = startDatePickerState,
            onDismissRequest = { showDatePickerStart = false },
            onConfirmRequest = {
                setPeriodFilter(TimePeriod(startDatePickerState.selectedDateMillis, endDate))
                startDate = startDatePickerState.selectedDateMillis!!
                showDatePickerStart = false
            },
            onClearRequest = { startDate = null }
        )
    }

    val endDatePickerState = rememberDatePickerState()
    if (showDatePickerEnd) {
        DatePickerDialog(
            datePickerState = endDatePickerState,
            onDismissRequest = { showDatePickerStart = false },
            onConfirmRequest = {
                setPeriodFilter(TimePeriod(startDate, endDatePickerState.selectedDateMillis))
                endDate = endDatePickerState.selectedDateMillis!!
                showDatePickerEnd = false
            },
            onClearRequest = { endDate = null }
        )
    }

    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            modifier = Modifier.padding(top = 32.dp),
            text = "Где искать",
            style = AppTypography.getType().titleLarge,
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
                    label = { Text(pair.first) },
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
                    label = { Text(pair.first) },
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
                    label = { Text(pair.first) },
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
                    label = { Text(pair.first) },
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
                    label = { Text(pair.first) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                    )
                )
            }
        }
        Text(
            modifier = Modifier.padding(top = 32.dp),
            text = "Период времени",
            style = AppTypography.getType().titleLarge,
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
                val dateStartText = startDate?.toString()
                    ?: "c"

                Text(
                    text = dateStartText,
                    style = AppTypography.getType().bodyLarge,
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
                val dateEndText = endDate?.toString()
                    ?: "по"

                Text(
                    text = dateEndText,
                    style = AppTypography.getType().bodyLarge,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        )
    }
}
