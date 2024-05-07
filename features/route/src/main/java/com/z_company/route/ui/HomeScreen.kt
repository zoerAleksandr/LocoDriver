package com.z_company.route.ui

import android.graphics.drawable.Drawable
import android.os.Build
import android.view.Window
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.z_company.core.ResultState
import com.z_company.core.ui.component.AutoSizeText
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
import com.z_company.core.R as CoreR

@RequiresApi(Build.VERSION_CODES.S)
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
    val sheetPeekHeight = remember {
        heightScreen.times(0.3)
    }

    val isExpand = scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded

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
    var showContextDialog by remember {
        mutableStateOf(false)
    }

    CustomDialog(
        showDialog = showContextDialog,
        onDismissRequest = { showContextDialog = false }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            showContextDialog = false
                        }
                    )
                },
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heightScreen.times(0.7f).dp)
                    .padding(start = 12.dp, end = 12.dp, top = 30.dp, bottom = 12.dp)
                    .background(color = Color.Yellow, shape = Shapes.medium)
                    .clickable {}
            )

            Column(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .fillMaxWidth(0.5f)
                    .background(color = Color.Red, shape = Shapes.medium)
            ) {
                Text(text = "item 1")
                HorizontalDivider()
                Text(text = "item 2")
                HorizontalDivider()
                Text(text = "item 3")
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
        sheetContainerColor = MaterialTheme.colorScheme.background,
        sheetDragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        sheetShadowElevation = 0.dp,
        sheetContent = {
            HomeBottomSheetContent(
                routeListState = routeListState,
                reloadRoute = reloadRoute,
                onDeleteRoute = onDeleteRoute,
                onRouteClick = onRouteClick,
                onRouteLongClick = { showContextDialog = true },
                isExpand = isExpand
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(start = 32.dp, end = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heightScreen.times(0.02f).dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heightScreen.times(0.07f).dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    modifier = Modifier
                        .background(
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
                    modifier = Modifier,
                    shape = Shapes.medium,
                    onClick = {
                        showMonthSelectorDialog.value = true
                    }) {
                    AutoSizeText(
                        text = "${
                            currentMonthOfYear.month.getMonthFullText()
                        } ${currentMonthOfYear.year}",
                        style = AppTypography.getType().headlineSmall,
                        maxTextSize = 24.sp,
                    )
                }
                IconButton(
                    modifier = Modifier
                        .background(
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
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heightScreen.times(0.14f).dp)
            )
            TotalTime(
                modifier = Modifier
                    .height(heightScreen.times(0.13f).dp),
                valueTime = totalTime,
                normaHours = currentMonthOfYear.normaHours,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heightScreen.times(0.05f).dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.Top
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
                    AutoSizeText(
                        text = DateAndTimeConverter.getTimeInStringFormat(12L),
                        style = AppTypography.getType().headlineSmall,
                        maxTextSize = 24.sp,
                        fontWeight = FontWeight.Light
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        modifier = Modifier.padding(end = 4.dp),
                        painter = painterResource(id = CoreR.drawable.ic_star_border),
                        contentDescription = null
                    )
                    AutoSizeText(
                        text = DateAndTimeConverter.getTimeInStringFormat(8L),
                        style = AppTypography.getType().headlineSmall,
                        maxTextSize = 24.sp,
                        fontWeight = FontWeight.Light
                    )
                }
            }
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heightScreen.times(0.19f).dp)
            )
            ButtonLocoDriver(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heightScreen.times(0.08f).dp),
                onClick = { onNewRouteClick() }
            ) {
                AutoSizeText(
                    text = stringResource(id = CoreR.string.adding),
                    style = AppTypography.getType().headlineSmall,
                    maxTextSize = 24.sp,
                )
            }
        }
    }
}

@Composable
fun TotalTime(
    modifier: Modifier,
    valueTime: Long,
    normaHours: Int
) {
    AutoSizeText(
        modifier = modifier.fillMaxWidth(),
        alignment = Alignment.BottomStart,
        text = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    fontWeight = FontWeight.Medium,
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
        maxTextSize = 70.sp,
        fontFamily = AppTypography.Companion.AppFontFamilies.RobotoConsed
    )
}

@Composable
fun CustomDialog(
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {

    var showAnimatedDialog by remember { mutableStateOf(false) }

    LaunchedEffect(showDialog) {
        if (showDialog) showAnimatedDialog = true
    }

    if (showAnimatedDialog) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            val dialogWindow = getDialogWindow()

            SideEffect {
                dialogWindow.let { window ->
                    window?.setWindowAnimations(-1)
                }
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                var animateIn by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { animateIn = true }
                AnimatedVisibility(
                    visible = animateIn && showDialog,
                    enter = fadeIn(spring(stiffness = Spring.StiffnessHigh)) + scaleIn(
                        initialScale = .8f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ),
                    exit = fadeOut() + scaleOut(targetScale = .95f)
                ) {
                    Box(
                        Modifier
                            .pointerInput(Unit) { detectTapGestures { } }
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        content()
                    }

                    DisposableEffect(Unit) {
                        onDispose {
                            showAnimatedDialog = false
                        }
                    }
                }
            }
        }
    }
}

@ReadOnlyComposable
@Composable
fun getDialogWindow(): Window? = (LocalView.current.parent as? DialogWindowProvider)?.window
