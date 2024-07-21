package com.z_company.route.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.core.util.DateAndTimeConverter.getMonthFullText
import com.z_company.domain.entities.MonthOfYear

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

    val dataTextStyle = AppTypography.getType().titleLarge.copy(fontWeight = FontWeight.Light)
    val subTitleTextStyle = AppTypography.getType().titleLarge
        .copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal
        )

    Dialog(onDismissRequest = { showMonthSelectorDialog.value = false }) {
        var expandedYearMenu by remember { mutableStateOf(false) }
        var expandedMonthMenu by remember { mutableStateOf(false) }
        Card(
            modifier = Modifier.wrapContentSize(),
            shape = Shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
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
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
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
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = expandedYearMenu
                            )
                        },
                        textStyle = dataTextStyle,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                        ),
                        shape = Shapes.medium,
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
                                text = { Text(text = year.toString(), style = dataTextStyle) },
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
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = expandedMonthMenu
                            )
                        },
                        textStyle = dataTextStyle,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                        ),
                        shape = Shapes.medium,
                    )
                    DropdownMenu(
                        expanded = expandedMonthMenu,
                        onDismissRequest = {
                            expandedMonthMenu = false
                        },
                    ) {
                        monthList.forEach { month ->
                            DropdownMenuItem(
                                text = { Text(text = month.getMonthFullText(), style = dataTextStyle) },
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
                Text(
                    text = "Выбрать",
                    style = subTitleTextStyle
                )
            }
        }
    }
}