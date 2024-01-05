package com.example.route.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.core.ResultState
import com.example.core.ui.theme.custom.AppTypography
import com.example.core.util.DateAndTimeConverter
import com.example.core.util.DateAndTimeConverter.getMonthShortText
import com.example.domain.entities.MonthOfYear
import com.example.route.component.HomeBottomSheetContent
import com.example.domain.entities.route.BasicData
import com.example.domain.entities.route.Route
import com.example.route.R
import com.example.route.component.CircularIndicator
import kotlinx.coroutines.launch
import com.example.core.R as CoreR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    routeListState: ResultState<List<Route>>,
    removeRouteState: ResultState<Unit>?,
    onRouteClick: (BasicData) -> Unit,
    onNewRouteClick: () -> Unit,
    onDeleteRoute: (Route) -> Unit,
    onDeleteRouteConfirmed: () -> Unit,
    reloadRoute: () -> Unit,
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit,
    totalTime: Long,
    currentMonthOfYear: MonthOfYear,
    yearList: List<Int>,
    monthList: List<Int>,
    selectMonth: (Int) -> Unit,
    selectYear: (Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            confirmValueChange = {
                it != SheetValue.Hidden
            }
        )
    )
    val heightScreen = LocalConfiguration.current.screenHeightDp
    val widthScreen = LocalConfiguration.current.screenWidthDp
    val sheetPeekHeight = heightScreen.times(0.3)
    val indicatorSize = widthScreen.times(0.75)
    val offset = try {
        scaffoldState.bottomSheetState.requireOffset()
    } catch (e: Throwable) {
        200f
    }
    val isExpand = offset == 0.0f

    if (removeRouteState is ResultState.Success) {
        LaunchedEffect(removeRouteState) {
            scope.launch {
                scaffoldState.snackbarHostState.showSnackbar(
                    message = context.getString(R.string.msg_route_deleted)
                )
                onDeleteRouteConfirmed()
            }
        }
    }
    var showMonthSelectorDialog by remember {
        mutableStateOf(false)
    }

    if (showMonthSelectorDialog) {
        Dialog(onDismissRequest = { showMonthSelectorDialog = false }) {
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
                            value = currentMonthOfYear.year.toString(),
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
                            yearList.forEach { selectionYear ->
                                DropdownMenuItem(
                                    text = { Text(text = selectionYear.toString()) },
                                    onClick = {
                                        selectYear(selectionYear)
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
                            value = currentMonthOfYear.month.toString(),
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
                            monthList.forEach { selectionMonth ->
                                DropdownMenuItem(
                                    text = { Text(text = stringResource(id = selectionMonth)) },
                                    onClick = {
                                        selectMonth(selectionMonth)
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
                        showMonthSelectorDialog = false
                    }
                ) {
                    Text(text = "Выбрать")
                }
            }
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        snackbarHost = {
            SnackbarHost(hostState = scaffoldState.snackbarHostState) { snackBarData ->
                Snackbar(snackBarData)
            }
        },
        sheetPeekHeight = sheetPeekHeight.dp,
        sheetContainerColor = MaterialTheme.colorScheme.background.copy(
            alpha = changeAlphaWithOffset(
                offset = offset
            )
        ),
        sheetDragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = changeAlphaWithOffset(
                        offset = offset
                    )
                )
            )
        },
        sheetShadowElevation = 0.dp,
        sheetContent = {
            HomeBottomSheetContent(
                routeListState,
                reloadRoute,
                onDeleteRoute,
                onRouteClick,
                offset,
                isExpand
            )
        }
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { onSettingsClick() }) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = null)
                }
                TextButton(onClick = { showMonthSelectorDialog = true }) {
                    Text(text = "${currentMonthOfYear.month.getMonthShortText()} ${currentMonthOfYear.year}")
                }
                IconButton(onClick = { onSearchClick() }) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                }
            }
            Spacer(modifier = Modifier.height(54.dp))
            CircularIndicator(
                canvasSize = indicatorSize.dp,
                valueHour = DateAndTimeConverter.getHourInDate(totalTime),
                valueMinute = DateAndTimeConverter.getRemainingMinuteFromHour(totalTime)
            )
            Button(
                modifier = Modifier
                    .fillMaxWidth(0.70f),
                onClick = { onNewRouteClick() }
            ) {
                Text(
                    text = stringResource(id = CoreR.string.adding),
                    style = AppTypography.getType().labelLarge
                )
            }
        }
    }
}