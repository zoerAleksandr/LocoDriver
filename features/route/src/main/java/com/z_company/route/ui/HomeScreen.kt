package com.z_company.route.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.z_company.core.ResultState
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.core.util.DateAndTimeConverter.getMonthShortText
import com.z_company.domain.entities.MonthOfYear
import com.z_company.route.component.HomeBottomSheetContent
import com.z_company.route.component.DialogSelectMonthOfYear
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Route
import com.z_company.route.R
import com.z_company.route.component.CircularIndicator
import kotlinx.coroutines.launch
import com.z_company.core.R as CoreR

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
    monthList: List<Int>,
    yearList: List<Int>,
    selectYearAndMonth: (Pair<Int, Int>) -> Unit,
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
    val showMonthSelectorDialog = remember {
        mutableStateOf(false)
    }

    if (showMonthSelectorDialog.value) {
        DialogSelectMonthOfYear(
            showMonthSelectorDialog,
            currentMonthOfYear,
            monthList = monthList,
            yearList = yearList,
            selectMonthOfYear = selectYearAndMonth
        )
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
                TextButton(onClick = {
                    showMonthSelectorDialog.value = true
                }) {
                    Text(text = "${currentMonthOfYear.year} ${currentMonthOfYear.month.getMonthShortText()}")
                }
                IconButton(onClick = { onSearchClick() }) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                }
            }
            Spacer(modifier = Modifier.height(54.dp))
            CircularIndicator(
                canvasSize = indicatorSize.dp,
                valueHour = DateAndTimeConverter.getHourInDate(totalTime),
                valueMinute = DateAndTimeConverter.getRemainingMinuteFromHour(totalTime),
                maxIndicatorValue = currentMonthOfYear.normaHours
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