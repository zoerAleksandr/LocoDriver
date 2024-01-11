package com.example.route.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.core.util.DateAndTimeConverter.getMonthFullText
import com.example.domain.entities.MonthOfYear

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DialogSelectMonthOfYear(
    showMonthSelectorDialog: MutableState<Boolean>,
    currentMonthOfYear: MonthOfYear,
    monthList: List<Int>,
    yearList: List<Int>,
    selectMonthOfYear: (Pair<Int, Int>) -> Unit
) {
    var selectedMonth by remember {
        mutableIntStateOf(currentMonthOfYear.month)
    }

    var selectedYear by remember {
        mutableIntStateOf(currentMonthOfYear.year)
    }

    Dialog(onDismissRequest = { showMonthSelectorDialog.value = false }) {
        var expandedYearMenu by remember { mutableStateOf(false) }
        var expandedMonthMenu by remember { mutableStateOf(false) }
        Card(Modifier.wrapContentSize()) {
            Icon(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 12.dp),
                imageVector = Icons.Default.DateRange,
                contentDescription = null
            )
            Text(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 12.dp),
                text = "Выберите месяц",
                style = MaterialTheme.typography.headlineSmall
            )
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ExposedDropdownMenuBox(
                    modifier = Modifier.weight(2f),
                    expanded = expandedYearMenu,
                    onExpandedChange = {
                        expandedYearMenu = !expandedYearMenu
                    }
                ) {
                    OutlinedTextField(
                        modifier = Modifier.menuAnchor(),
                        readOnly = true,
                        value = selectedYear.toString(),
                        onValueChange = { },
                        label = { Text("Год") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = expandedYearMenu
                            )
                        },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        modifier = Modifier.padding(top = 6.dp),
                        expanded = expandedYearMenu,
                        onDismissRequest = {
                            expandedYearMenu = false
                        }
                    ) {
                        yearList.forEach { year ->
                            DropdownMenuItem(
                                text = { Text(text = year.toString()) },
                                onClick = {
                                    selectedYear = year
                                    expandedYearMenu = false
                                }
                            )

                        }
                    }
                }

                ExposedDropdownMenuBox(
                    modifier = Modifier
                        .weight(3f)
                        .padding(start = 8.dp),
                    expanded = expandedMonthMenu,
                    onExpandedChange = {
                        expandedMonthMenu = !expandedMonthMenu
                    }
                ) {
                    OutlinedTextField(
                        modifier = Modifier.menuAnchor(),
                        readOnly = true,
                        value = selectedMonth.getMonthFullText(),
                        onValueChange = { },
                        label = { Text("Месяц") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = expandedMonthMenu
                            )
                        },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        singleLine = true
                    )
                    DropdownMenu(
                        expanded = expandedMonthMenu,
                        onDismissRequest = {
                            expandedMonthMenu = false
                        },
                    ) {
                        monthList.forEach { month ->
                            DropdownMenuItem(
                                text = { Text(text = month.getMonthFullText()) },
                                onClick = {
                                    selectedMonth = month
                                    expandedMonthMenu = false
                                }
                            )

                        }
                    }
                }
            }

            TextButton(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(end = 12.dp, bottom = 12.dp),
                onClick = {
                    selectMonthOfYear(Pair(selectedYear, selectedMonth))
                    showMonthSelectorDialog.value = false
                }
            ) {
                Text(text = "Выбрать")
            }
        }
    }
}