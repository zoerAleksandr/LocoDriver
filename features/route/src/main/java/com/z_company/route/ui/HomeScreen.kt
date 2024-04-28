package com.z_company.route.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.core.ResultState
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.core.util.DateAndTimeConverter.getMonthFullText
import com.z_company.domain.entities.MonthOfYear
import com.z_company.route.component.HomeBottomSheetContent
import com.z_company.route.component.DialogSelectMonthOfYear
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Route
import com.z_company.route.R
import com.z_company.route.component.ButtonLocoDriver
import kotlinx.coroutines.launch
import java.util.Locale
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
    val sheetPeekHeight = heightScreen.times(0.3)
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
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                IconButton(
                    modifier = Modifier.background(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = Shapes.medium
                    ),
                    onClick = { onSettingsClick() }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null
                    )
                }
                TextButton(
                    shape = Shapes.medium,
                    onClick = {
                        showMonthSelectorDialog.value = true
                    }) {
                    Text(
                        text = "${
                            currentMonthOfYear.month.getMonthFullText().toLowerCase(Locale.ROOT)
                        } ${currentMonthOfYear.year}",
                        style = AppTypography.getType().headlineSmall,

                        )
                }
                IconButton(
                    modifier = Modifier.background(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = Shapes.medium
                    ),
                    onClick = { onSearchClick() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null
                    )
                }
            }
            Spacer(modifier = Modifier.height(124.dp))
            TotalTime(
                valueTime = totalTime,
                normaHours = currentMonthOfYear.normaHours,
                nightHours = 12,
                passengerHours = 8
            )
            Spacer(modifier = Modifier.height(100.dp))
            ButtonLocoDriver(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                onClick = { onNewRouteClick() }
            ) {
                Text(
                    text = stringResource(id = CoreR.string.adding),
                    style = AppTypography.getType().headlineSmall
                )
            }
        }
    }
}

@Composable
fun TotalTime(valueTime: Long, normaHours: Int, nightHours: Long, passengerHours: Long) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                modifier = Modifier
                    .padding(0.dp),
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Medium,
                            fontSize = 70.sp
                        )
                    ) {
                        append(DateAndTimeConverter.getTimeInStringFormat(valueTime))
                    }
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Light,
                            fontSize = 24.sp
                        )
                    ) {
                        append(" / $normaHours")
                    }
                },
                fontFamily = AppTypography.Companion.AppFontFamilies.RobotoConsed
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            Row(
                modifier = Modifier.padding(end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = Modifier.padding(end = 4.dp),
                    painter = painterResource(id = CoreR.drawable.ic_star_border),
                    contentDescription = null
                )
                Text(
                    text = DateAndTimeConverter.getTimeInStringFormat(nightHours),
                    style = AppTypography.getType().headlineSmall,
                    fontWeight = FontWeight.Light
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    modifier = Modifier.padding(end = 4.dp),
                    painter = painterResource(id = CoreR.drawable.ic_star_border),
                    contentDescription = null
                )
                Text(
                    text = DateAndTimeConverter.getTimeInStringFormat(passengerHours),
                    style = AppTypography.getType().headlineSmall,
                    fontWeight = FontWeight.Light
                )
            }
        }
    }
}