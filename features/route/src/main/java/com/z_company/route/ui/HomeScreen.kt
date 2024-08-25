package com.z_company.route.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.flowWithLifecycle
import com.z_company.core.ResultState
import com.z_company.core.ui.component.AsyncData
import com.z_company.core.ui.component.AutoSizeText
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.core.util.ConverterLongToTime
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.core.util.DateAndTimeConverter.getDateMiniAndTime
import com.z_company.core.util.DateAndTimeConverter.getDateFromDateLong
import com.z_company.core.util.DateAndTimeConverter.getMonthFullText
import com.z_company.core.util.DateAndTimeConverter.getTimeFromDateLong
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.UtilForMonthOfYear.getPersonalNormaHours
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.fullRest
import com.z_company.domain.entities.route.UtilsForEntities.getFollowingTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.entities.route.UtilsForEntities.shortRest
import com.z_company.domain.util.CalculationEnergy
import com.z_company.domain.util.CalculationEnergy.rounding
import com.z_company.domain.util.ifNullOrBlank
import com.z_company.domain.util.str
import com.z_company.domain.util.times
import com.z_company.route.R
import com.z_company.route.component.AnimationDialog
import com.z_company.route.component.ButtonLocoDriver
import com.z_company.route.component.DialogSelectMonthOfYear
import com.z_company.route.component.HomeBottomSheetContent
import com.z_company.core.ui.component.CustomSnackBar
import com.z_company.route.viewmodel.AlertBeforePurchasesEvent
import com.z_company.route.viewmodel.StartPurchasesEvent
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import ru.rustore.sdk.core.feature.model.FeatureAvailabilityResult
import com.z_company.core.R as CoreR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    routeListState: ResultState<List<Route>>,
    removeRouteState: ResultState<Unit>?,
    onRouteClick: (String) -> Unit,
    onNewRouteClick: () -> Unit,
    onMoreInfoClick: (String) -> Unit,
    onDeleteRoute: (Route) -> Unit,
    onDeleteRouteConfirmed: () -> Unit,
    reloadRoute: () -> Unit,
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit,
    totalTime: Long,
    currentMonthOfYear: MonthOfYear?,
    monthList: List<Int>,
    yearList: List<Int>,
    selectYearAndMonth: (Pair<Int, Int>) -> Unit,
    minTimeRest: Long?,
    nightTime: ResultState<Long>?,
    passengerTime: ResultState<Long>?,
    dayOffHours: ResultState<Int>?,
    calculationHomeRest: (Route?) -> Unit,
    homeRestValue: ResultState<Long?>,
    firstEntryDialogState: Boolean,
    resetStateFirstEntryDialog: () -> Unit,
    purchasesEvent: SharedFlow<StartPurchasesEvent>,
    showPurchasesScreen: () -> Unit,
    showFormScreen: () -> Unit,
    isShowFormScreen: Boolean,
    showFormScreenReset: () -> Unit,
    isLoadingStateAddButton: Boolean,
    alertBeforePurchasesState: SharedFlow<AlertBeforePurchasesEvent>,
    checkPurchasesAvailability: (Context) -> Unit,
    restorePurchases: () -> Unit,
    restoreResultState: ResultState<String>?,
    resetSubscriptionState: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            confirmValueChange = {
                it != SheetValue.Hidden
            }
        )
    )
    val heightScreen = LocalConfiguration.current.screenHeightDp
    val sheetPeekHeight = remember {
        heightScreen.times(0.25)
    }

    val isExpand = scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded

    if (isShowFormScreen) {
        showFormScreen()
        showFormScreenReset()
    }

    var showNeedSubscribeDialog by remember {
        mutableStateOf(false)
    }

    var showAlertSubscribeDialog by remember {
        mutableStateOf(false)
    }

    AsyncData(resultState = restoreResultState, errorContent = {
        LaunchedEffect(Unit) {
            scope.launch {
                scaffoldState.snackbarHostState.showSnackbar("Ошибка синхронизации. Проверьте интернет.")
            }
        }
    }) { message ->
        message?.let {
            scope.launch {
                scaffoldState.snackbarHostState.showSnackbar(message)
            }
        }
        resetSubscriptionState()
    }

    AnimationDialog(
        showDialog = showAlertSubscribeDialog,
        onDismissRequest = { showAlertSubscribeDialog = !showAlertSubscribeDialog }
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .background(color = MaterialTheme.colorScheme.surface, shape = Shapes.medium)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "${stringResource(id = R.string.test_period)}\n",
                    style = AppTypography.getType().titleLarge.copy(color = MaterialTheme.colorScheme.primary)
                )
                Text(
                    text = "${stringResource(id = R.string.available_for_free_route)}\n",
                    style = AppTypography.getType().bodyMedium.copy(color = MaterialTheme.colorScheme.primary)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HorizontalDivider()
                    TextButton(
                        shape = Shapes.medium,
                        onClick = {
                            showAlertSubscribeDialog = !showAlertSubscribeDialog
                            showFormScreen()
                        }
                    ) {
                        Text(
                            text = stringResource(id = R.string.billing_common_ok),
                            style = AppTypography.getType().titleMedium.copy(color = MaterialTheme.colorScheme.tertiary)
                        )
                    }
                    HorizontalDivider()
                    TextButton(
                        shape = Shapes.medium,
                        onClick = {
                            showAlertSubscribeDialog = !showAlertSubscribeDialog
                            checkPurchasesAvailability(context)
                        }
                    ) {
                        Text(
                            text = "Оформить подписку",
                            style = AppTypography.getType().titleMedium.copy(color = MaterialTheme.colorScheme.tertiary)
                        )
                    }
                }
            }
        }
    }

    AnimationDialog(
        showDialog = showNeedSubscribeDialog,
        onDismissRequest = { showNeedSubscribeDialog = !showNeedSubscribeDialog }
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .background(color = MaterialTheme.colorScheme.surface, shape = Shapes.medium)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "${stringResource(id = R.string.dialog_title_need_purchases)}\n",
                    style = AppTypography.getType().titleLarge.copy(color = MaterialTheme.colorScheme.primary)
                )
                Text(
                    text = "${stringResource(id = R.string.available_for_free_route)}\n",
                    style = AppTypography.getType().bodyMedium.copy(color = MaterialTheme.colorScheme.primary)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HorizontalDivider()
                    TextButton(
                        shape = Shapes.medium,
                        onClick = {
                            showNeedSubscribeDialog = !showNeedSubscribeDialog
                        }
                    ) {
                        Text(
                            text = stringResource(id = R.string.billing_common_ok),
                            style = AppTypography.getType().titleMedium.copy(color = MaterialTheme.colorScheme.tertiary)
                        )
                    }
                    HorizontalDivider()

                    TextButton(
                        shape = Shapes.medium,
                        onClick = {
                            showNeedSubscribeDialog = !showNeedSubscribeDialog
                            checkPurchasesAvailability(context)
                        }
                    ) {
                        Text(
                            text = "Оформить подписку",
                            style = AppTypography.getType().titleMedium.copy(color = MaterialTheme.colorScheme.tertiary)
                        )
                    }
                    HorizontalDivider()

                    TextButton(
                        shape = Shapes.medium,
                        onClick = {
                            showNeedSubscribeDialog = !showNeedSubscribeDialog
                            restorePurchases()
                        }
                    ) {
                        Text(
                            text = "Восстановить покупки",
                            style = AppTypography.getType().titleMedium.copy(color = MaterialTheme.colorScheme.tertiary)
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        scope.launch {
            alertBeforePurchasesState.flowWithLifecycle(lifecycle).collect { event ->
                when (event) {
                    is AlertBeforePurchasesEvent.ShowDialogNeedSubscribe -> {
                        showNeedSubscribeDialog = true
                    }

                    is AlertBeforePurchasesEvent.ShowDialogAlertSubscribe -> {
                        showAlertSubscribeDialog = true
                    }
                }
            }
        }
    }

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
    var routeForPreview by remember {
        mutableStateOf<Route?>(null)
    }

    var showContextDialog by remember {
        mutableStateOf(false)
    }
    AnimationDialog(
        showDialog = firstEntryDialogState,
        onDismissRequest = resetStateFirstEntryDialog
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .background(color = MaterialTheme.colorScheme.surface, shape = Shapes.medium)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start, text = "ДОБРО ПОЖАЛОВАТЬ!\n",
                    style = AppTypography.getType().titleLarge.copy(color = MaterialTheme.colorScheme.primary)
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Подтвердите вашу электронную почту.\n",
                    style = AppTypography.getType().titleMedium.copy(color = MaterialTheme.colorScheme.primary)
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "На ваш адрес электронной почты было отправлено письмо со ссылкой для подтверждения. Пожалуйста, проверьте вашу почту и нажмите на ссылку для завершения регистрации.\n\n",
                    style = AppTypography.getType().bodyMedium.copy(color = MaterialTheme.colorScheme.primary)
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Если вы не получили письмо, проверьте папку \"Спам\" или повторите попытку позже.\n",
                    style = AppTypography.getType().bodyMedium.copy(color = MaterialTheme.colorScheme.primary)
                )

                Button(
                    modifier = Modifier.padding(top = 16.dp),
                    shape = Shapes.medium,
                    onClick = resetStateFirstEntryDialog
                ) {
                    Text(
                        text = "Понял",
                        style = AppTypography.getType().titleMedium
                    )
                }
            }
        }
    }

    AnimationDialog(
        showDialog = showContextDialog,
        onDismissRequest = { showContextDialog = false }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            showContextDialog = false
                        }
                    )
                },
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Bottom
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeightIn(
                        min = heightScreen.times(0.3f).dp,
                        max = heightScreen.times(0.8f).dp
                    )
                    .padding(start = 12.dp, end = 12.dp, top = 30.dp, bottom = 12.dp)
                    .background(color = MaterialTheme.colorScheme.surface, shape = Shapes.medium)
                    .clickable {}
            ) {

                calculationHomeRest(routeForPreview)
                PreviewRoute(routeForPreview, minTimeRest, homeRestValue)
            }

            Column(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .fillMaxWidth(0.6f)
                    .background(color = MaterialTheme.colorScheme.surface, shape = Shapes.medium)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable {
                            showContextDialog = false
                            routeForPreview?.basicData?.let { basicData ->
                                onRouteClick(basicData.id)
                            }
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Просмотр",
                        style = AppTypography.getType().bodyMedium.copy(color = MaterialTheme.colorScheme.primary)
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_visibility_24),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable {
                            showContextDialog = false
                            routeForPreview?.let { route ->
                                onDeleteRoute(route)
                            }
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Удалить",
                        style = AppTypography.getType().bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.delete_24px),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    val showMonthSelectorDialog = remember {
        mutableStateOf(false)
    }

    if (showMonthSelectorDialog.value) {
        currentMonthOfYear?.let {
            DialogSelectMonthOfYear(
                showMonthSelectorDialog,
                currentMonthOfYear,
                monthList = monthList,
                yearList = yearList,
                selectMonthOfYear = selectYearAndMonth
            )
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        snackbarHost = {
            SnackbarHost(hostState = scaffoldState.snackbarHostState) { snackBarData ->
                CustomSnackBar(snackBarData = snackBarData)
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
                onRouteLongClick = { route ->
                    showContextDialog = true
                    routeForPreview = route
                },
                isExpand = isExpand
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        LaunchedEffect(purchasesEvent) {
            scope.launch {
                purchasesEvent.flowWithLifecycle(lifecycle).collect { event ->
                    when (event) {
                        is StartPurchasesEvent.PurchasesAvailability -> {
                            when (event.availability) {
                                is FeatureAvailabilityResult.Available -> {
                                    showPurchasesScreen()
                                }

                                is FeatureAvailabilityResult.Unavailable -> {
                                    scaffoldState.snackbarHostState.showSnackbar("Ошибка: ${event.availability.cause.message}")
                                }

                                else -> {}
                            }
                        }

                        is StartPurchasesEvent.Error -> {
                            scaffoldState.snackbarHostState.showSnackbar("Ошибка: ${event.throwable.message}")
                        }
                    }
                }
            }
        }
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
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
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
                            currentMonthOfYear?.month?.getMonthFullText()
                        } ${currentMonthOfYear?.year}",
                        style = AppTypography.getType().headlineSmall,
                        maxTextSize = 24.sp,
                        color = MaterialTheme.colorScheme.primary
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
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heightScreen.times(0.14f).dp)
            )
            currentMonthOfYear?.let { monthOfYear ->
                TotalTime(
                    modifier = Modifier
                        .clickable { onMoreInfoClick(monthOfYear.id) }
                        .height(heightScreen.times(0.13f).dp),
                    valueTime = totalTime,
                    normaHours = monthOfYear.getPersonalNormaHours(),
                )
            }
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
                        painter = painterResource(id = R.drawable.dark_mode_24px),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    AsyncData(resultState = nightTime,
                        loadingContent = {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    ) {
                        AutoSizeText(
                            text = ConverterLongToTime.getTimeInStringFormat(it),
                            style = AppTypography.getType().headlineSmall,
                            maxTextSize = 24.sp,
                            fontWeight = FontWeight.Light,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }


                Row(
                    modifier = Modifier.padding(end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        modifier = Modifier.padding(end = 4.dp),
                        painter = painterResource(id = R.drawable.passenger_24px),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    AsyncData(resultState = passengerTime,
                        loadingContent = {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    ) {
                        AutoSizeText(
                            text = ConverterLongToTime.getTimeInStringFormat(it),
                            style = AppTypography.getType().headlineSmall,
                            maxTextSize = 24.sp,
                            fontWeight = FontWeight.Light,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                AsyncData(resultState = dayOffHours,
                    loadingContent = {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                ) { hours ->
                    hours?.let {
                        if (it != 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .padding(end = 4.dp),
                                    painter = painterResource(id = R.drawable.palma),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                AutoSizeText(
                                    text = "$it:00",
                                    style = AppTypography.getType().headlineSmall,
                                    maxTextSize = 24.sp,
                                    fontWeight = FontWeight.Light,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
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
                onClick = {
                    onNewRouteClick()
                }
            ) {
                if (isLoadingStateAddButton) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        AutoSizeText(
                            text = "Загрузка",
                            style = AppTypography.getType().headlineSmall.copy(color = MaterialTheme.colorScheme.onPrimary),
                            maxTextSize = 24.sp,
                        )
                    }
                } else {
                    AutoSizeText(
                        text = stringResource(id = CoreR.string.adding),
                        style = AppTypography.getType().headlineSmall.copy(color = MaterialTheme.colorScheme.onPrimary),
                        maxTextSize = 24.sp,
                    )
                }
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
                    color = MaterialTheme.colorScheme.primary
                )
            ) {
                append(DateAndTimeConverter.getTimeInStringFormat(valueTime))
            }
            withStyle(
                SpanStyle(
                    fontWeight = FontWeight.Light,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            ) {
                append(" / $normaHours")
            }
        },
        maxTextSize = 70.sp,
        fontFamily = AppTypography.Companion.AppFontFamilies.RobotoConsed
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PreviewRoute(route: Route?, minTimeRest: Long?, homeRest: ResultState<Long?>) {
    val styleTitle = AppTypography.getType().titleSmall.copy(
        fontWeight = FontWeight.W600,
        color = MaterialTheme.colorScheme.primary
    )
    val styleData = AppTypography.getType().bodyMedium.copy(
        fontWeight = FontWeight.W400,
        color = MaterialTheme.colorScheme.primary
    )
    val styleHint = AppTypography.getType().bodySmall.copy(
        fontWeight = FontWeight.W300,
        color = MaterialTheme.colorScheme.primary
    )
    val paddingBetweenBlocks = 20.dp
    val paddingInsideBlock = 14.dp
    val paddingIcon = 12.dp
    val horizontalPaddingSecondItem = 32.dp
    val iconSize = 50.dp
    val iconSizeSecond = iconSize * .8f
    val iconMiniSize = 18.dp

    val locomotiveExpandItemState = remember {
        mutableStateMapOf<Int, Boolean>()
    }
    val trainExpandItemState = remember {
        mutableStateMapOf<Int, Boolean>()
    }
    route?.let {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = paddingBetweenBlocks)
                        .animateItemPlacement(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Маршрут ${route.basicData.number ?: "б/н"}  ",
                        style = styleTitle,
                    )
                }
            }
            item {
                route.basicData.timeStartWork?.let {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = paddingBetweenBlocks)
                            .animateItemPlacement(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = getDateFromDateLong(route.basicData.timeStartWork),
                            style = styleData,
                        )
                    }
                }
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = paddingBetweenBlocks)
                        .animateItemPlacement(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "Рабочее время",
                        style = styleTitle,
                    )
                }
            }
            item {
                if (route.basicData.timeStartWork != null || route.basicData.timeEndWork != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = paddingInsideBlock)
                            .animateItemPlacement(),
                    ) {
                        // Icon
                        Box(
                            modifier = Modifier
                                .size(iconSize)
                                .background(
                                    color = MaterialTheme.colorScheme.secondary,
                                    shape = Shapes.medium
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                modifier = Modifier.fillMaxSize(0.7f),
                                tint = MaterialTheme.colorScheme.primary,
                                painter = painterResource(id = R.drawable.schedule_24px),
                                contentDescription = null
                            )
                        }
                        Column(modifier = Modifier.padding(start = paddingIcon)) {
                            Box {
                                Text(
                                    text = DateAndTimeConverter.getTimeInStringFormat(route.getWorkTime()),
                                    style = styleData,
                                    maxLines = 1
                                )
                            }
                            Row {
                                Text(
                                    text = getTimeFromDateLong(route.basicData.timeStartWork),
                                    style = styleHint,
                                    maxLines = 1
                                )

                                Text(
                                    text = " - ${getTimeFromDateLong(route.basicData.timeEndWork)}",
                                    style = styleHint,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
            item {
                if (route.basicData.timeStartWork != null && route.basicData.timeEndWork != null) {
                    val restText = if (route.basicData.restPointOfTurnover) {
                        "Отдых в ПО"
                    } else {
                        "Домашний отдых"
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = paddingInsideBlock)
                            .animateItemPlacement(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(iconSize)
                                .background(
                                    color = MaterialTheme.colorScheme.secondary,
                                    shape = Shapes.medium
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (route.basicData.restPointOfTurnover) {
                                Icon(
                                    modifier = Modifier.fillMaxSize(0.7f),
                                    tint = MaterialTheme.colorScheme.primary,
                                    painter = painterResource(id = R.drawable.hotel_24px),
                                    contentDescription = null
                                )
                            } else {
                                Icon(
                                    modifier = Modifier.fillMaxSize(0.7f),
                                    tint = MaterialTheme.colorScheme.primary,
                                    painter = painterResource(id = R.drawable.gite_24px),
                                    contentDescription = null
                                )
                            }
                        }

                        Column(modifier = Modifier.padding(start = paddingIcon)) {
                            Text(
                                text = restText,
                                style = styleData,
                                maxLines = 1,
                            )
                            if (route.basicData.restPointOfTurnover) {
                                minTimeRest?.let {
                                    val shortRestText = getTimeFromDateLong(
                                        route.shortRest(minTimeRest)
                                    )
                                    val fullRestText = getTimeFromDateLong(
                                        route.fullRest(minTimeRest)
                                    )
                                    Text(
                                        text = "$shortRestText - $fullRestText",
                                        style = styleHint,
                                        maxLines = 1,
                                    )
                                }
                            } else {
                                AsyncData(
                                    resultState = homeRest,
                                    loadingContent = {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                    },
                                    errorContent = {}
                                ) { homeRestInLong ->
                                    homeRestInLong?.let {
                                        val homeRestInLongText = getDateMiniAndTime(homeRestInLong)
                                        Text(
                                            text = "до $homeRestInLongText",
                                            style = styleHint,
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (route.locomotives.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = paddingBetweenBlocks)
                            .animateItemPlacement(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "Локомотив",
                            style = styleTitle,
                        )
                    }
                }
            }
            itemsIndexed(
                items = route.locomotives,
                key = { _, item -> item.locoId }) { index, locomotive ->
                if (locomotiveExpandItemState[index] == null) {
                    locomotiveExpandItemState[index] = true
                }
                val typeLocoText = when (locomotive.type) {
                    LocoType.ELECTRIC -> "Электровоз"
                    LocoType.DIESEL -> "Тепловоз"
                }
                val seriesText = locomotive.series.ifNullOrBlank { "" }
                val numberText = locomotive.number.ifNullOrBlank { "" }
                val timeStartAcceptedText =
                    getTimeFromDateLong(locomotive.timeStartOfAcceptance)
                val timeEndAcceptedText =
                    getTimeFromDateLong(locomotive.timeEndOfAcceptance)
                val timeStartDeliveryText =
                    getTimeFromDateLong(locomotive.timeStartOfDelivery)
                val timeEndDeliveryText =
                    getTimeFromDateLong(locomotive.timeEndOfDelivery)

                val rotationSectionButton =
                    animateFloatAsState(
                        targetValue = if (locomotiveExpandItemState[index]!!) 180f else 0f,
                        label = ""
                    )

                Column(modifier = Modifier.animateItemPlacement()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = paddingInsideBlock),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row {
                            // Icon
                            Box(
                                modifier = Modifier
                                    .size(iconSize)
                                    .background(
                                        color = MaterialTheme.colorScheme.secondary,
                                        shape = Shapes.medium
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                when (locomotive.type) {
                                    LocoType.ELECTRIC -> {
                                        Icon(
                                            modifier = Modifier.fillMaxSize(0.7f),
                                            tint = MaterialTheme.colorScheme.primary,
                                            painter = painterResource(id = R.drawable.electric_loco),
                                            contentDescription = null
                                        )
                                    }

                                    LocoType.DIESEL -> {
                                        Icon(
                                            modifier = Modifier.fillMaxSize(0.7f),
                                            tint = MaterialTheme.colorScheme.primary,
                                            painter = painterResource(id = R.drawable.diesel_loco),
                                            contentDescription = null
                                        )
                                    }
                                }
                            }
                            Column(
                                modifier = Modifier
                                    .padding(start = paddingIcon)
                            ) {
                                Row {
                                    if (locomotive.series == null) {
                                        Text(
                                            text = "$typeLocoText ",
                                            style = styleData,
                                        )
                                    } else {
                                        Text(
                                            text = seriesText,
                                            style = styleData,
                                        )
                                    }
                                    locomotive.number?.let {
                                        Text(
                                            text = " - $numberText",
                                            style = styleData,
                                        )
                                    }
                                }
                                if (locomotive.timeStartOfAcceptance != null || locomotive.timeEndOfAcceptance != null) {
                                    Row {
                                        Text(
                                            text = "Приемка: ",
                                            style = styleHint,
                                        )
                                        Text(
                                            text = "$timeStartAcceptedText - ",
                                            style = styleHint,
                                        )
                                        Text(
                                            text = timeEndAcceptedText,
                                            style = styleHint,
                                        )
                                    }
                                }
                                if (locomotive.timeStartOfDelivery != null || locomotive.timeEndOfDelivery != null) {
                                    Row {
                                        Text(
                                            text = "Сдача: ",
                                            style = styleHint,
                                        )
                                        Text(
                                            text = "$timeStartDeliveryText - ",
                                            style = styleHint,
                                        )
                                        Text(
                                            text = timeEndDeliveryText,
                                            style = styleHint,
                                        )
                                    }
                                }
                            }
                        }
                        IconButton(
                            modifier = Modifier.graphicsLayer(
                                rotationZ = rotationSectionButton.value
                            ),
                            onClick = {
                                locomotiveExpandItemState[index] =
                                    !locomotiveExpandItemState[index]!!
                            }) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    when (locomotive.type) {
                        LocoType.ELECTRIC -> {
                            AnimatedVisibility(visible = locomotiveExpandItemState[index]!!) {
                                Column {
                                    locomotive.electricSectionList.forEachIndexed { index, sectionElectric ->
                                        val acceptedEnergyText =
                                            sectionElectric.acceptedEnergy?.toPlainString()
                                        val deliveryEnergyText =
                                            sectionElectric.deliveryEnergy?.toPlainString()
                                        val acceptedRecoveryText =
                                            sectionElectric.acceptedRecovery?.toPlainString()
                                        val deliveryRecoveryText =
                                            sectionElectric.deliveryRecovery?.toPlainString()
                                        val consumptionEnergy =
                                            CalculationEnergy.getTotalEnergyConsumption(
                                                accepted = sectionElectric.acceptedEnergy,
                                                delivery = sectionElectric.deliveryEnergy
                                            )?.toPlainString() ?: ""
                                        val consumptionRecovery =
                                            CalculationEnergy.getTotalEnergyConsumption(
                                                accepted = sectionElectric.acceptedRecovery,
                                                delivery = sectionElectric.deliveryRecovery
                                            )?.toPlainString() ?: ""

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    top = paddingInsideBlock,
                                                    start = horizontalPaddingSecondItem
                                                ),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            // Icon
                                            Box(
                                                modifier = Modifier
                                                    .size(iconSizeSecond)
                                                    .background(
                                                        color = MaterialTheme.colorScheme.secondary,
                                                        shape = Shapes.medium
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                val image: Int = when (index) {
                                                    0 -> R.drawable.one
                                                    1 -> R.drawable.two
                                                    2 -> R.drawable.three
                                                    3 -> R.drawable.four
                                                    4 -> R.drawable.five
                                                    5 -> R.drawable.sex
                                                    6 -> R.drawable.seven
                                                    7 -> R.drawable.eight
                                                    8 -> R.drawable.nine
                                                    else -> R.drawable.one
                                                }
                                                Icon(
                                                    modifier = Modifier.fillMaxSize(0.7f),
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    painter = painterResource(id = image),
                                                    contentDescription = null
                                                )
                                            }
                                            Column(modifier = Modifier.padding(start = paddingIcon)) {
                                                if (sectionElectric.acceptedEnergy != null ||
                                                    sectionElectric.deliveryEnergy != null ||
                                                    sectionElectric.acceptedRecovery != null ||
                                                    sectionElectric.deliveryRecovery != null
                                                ) {

                                                    if (sectionElectric.acceptedEnergy != null ||
                                                        sectionElectric.deliveryEnergy != null
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(
                                                                paddingIcon / 2
                                                            )
                                                        ) {
                                                            // Icon energy symbol
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(iconMiniSize),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    modifier = Modifier.fillMaxSize(
                                                                        0.7f
                                                                    ),
                                                                    tint = MaterialTheme.colorScheme.primary,
                                                                    painter = painterResource(id = R.drawable.electric_bolt_24px),
                                                                    contentDescription = null
                                                                )
                                                            }
                                                            Text(
                                                                text = "$acceptedEnergyText - $deliveryEnergyText",
                                                                style = styleHint,
                                                            )
                                                        }
                                                    }

                                                    if (sectionElectric.acceptedRecovery != null ||
                                                        sectionElectric.deliveryRecovery != null
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(
                                                                paddingIcon / 2
                                                            )
                                                        ) {
                                                            // Icon recovery symbol
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(iconMiniSize),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    modifier = Modifier.fillMaxSize(
                                                                        0.7f
                                                                    ),
                                                                    tint = MaterialTheme.colorScheme.primary,
                                                                    painter = painterResource(id = R.drawable.cycle_24px),
                                                                    contentDescription = null
                                                                )
                                                            }
                                                            Text(
                                                                text = "$acceptedRecoveryText - $deliveryRecoveryText",
                                                                style = styleHint,
                                                            )
                                                        }
                                                    }

                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(
                                                            paddingIcon / 2
                                                        )
                                                    ) {
                                                        if (sectionElectric.acceptedEnergy != null &&
                                                            sectionElectric.deliveryEnergy != null
                                                        ) {
                                                            consumptionEnergy.let {
                                                                Text(
                                                                    text = "Расход: $consumptionEnergy",
                                                                    style = styleHint,
                                                                )
                                                            }
                                                        }
                                                        if (sectionElectric.acceptedRecovery != null &&
                                                            sectionElectric.deliveryRecovery != null
                                                        ) {
                                                            consumptionRecovery.let {
                                                                Text(
                                                                    text = "Рекуперация: $consumptionRecovery",
                                                                    style = styleHint,
                                                                )
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    Text(
                                                        text = "Нет данных",
                                                        style = styleHint
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        LocoType.DIESEL -> {
                            AnimatedVisibility(visible = locomotiveExpandItemState[index]!!) {
                                Column {
                                    locomotive.dieselSectionList.forEachIndexed { index, sectionDiesel ->
                                        val consumption = CalculationEnergy.getTotalFuelConsumption(
                                            accepted = sectionDiesel.acceptedFuel,
                                            delivery = sectionDiesel.deliveryFuel,
                                            refuel = sectionDiesel.fuelSupply
                                        )
                                        val consumptionInKilo =
                                            CalculationEnergy.getTotalFuelInKiloConsumption(
                                                consumption = consumption,
                                                coefficient = sectionDiesel.coefficient
                                            )
                                        val consumptionText = consumption.str()
                                        val consumptionInKiloText = consumptionInKilo.str()
                                        val acceptedText = sectionDiesel.acceptedFuel.str()
                                        val deliveryText = sectionDiesel.deliveryFuel.str()
                                        val acceptedInKilo =
                                            sectionDiesel.acceptedFuel.times(sectionDiesel.coefficient)
                                        val acceptedInKiloText = rounding(acceptedInKilo, 2).str()
                                        val deliveryInKilo =
                                            sectionDiesel.deliveryFuel.times(sectionDiesel.coefficient)
                                        val deliveryInKiloText = rounding(deliveryInKilo, 2).str()
                                        val fuelSupplyText = sectionDiesel.fuelSupply.str()
                                        val coefficientText = sectionDiesel.coefficient.str()

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    top = paddingInsideBlock,
                                                    start = horizontalPaddingSecondItem
                                                ),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            // Icon
                                            Box(
                                                modifier = Modifier
                                                    .size(iconSizeSecond)
                                                    .background(
                                                        color = MaterialTheme.colorScheme.secondary,
                                                        shape = Shapes.medium
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                val image: Int = when (index) {
                                                    0 -> R.drawable.one
                                                    1 -> R.drawable.two
                                                    2 -> R.drawable.three
                                                    3 -> R.drawable.four
                                                    4 -> R.drawable.five
                                                    5 -> R.drawable.sex
                                                    6 -> R.drawable.seven
                                                    7 -> R.drawable.eight
                                                    8 -> R.drawable.nine
                                                    else -> R.drawable.one
                                                }
                                                Icon(
                                                    modifier = Modifier.fillMaxSize(0.7f),
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    painter = painterResource(id = image),
                                                    contentDescription = null
                                                )
                                            }
                                            Column(modifier = Modifier.padding(start = paddingIcon)) {
                                                if (acceptedText.isNotEmpty() || deliveryText.isNotEmpty()) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(
                                                            paddingIcon / 2
                                                        )
                                                    ) {
                                                        Text(
                                                            text = "$acceptedText - $deliveryText",
                                                            style = styleHint
                                                        )
                                                        Text(
                                                            text = "$consumptionText л.",
                                                            style = styleHint
                                                        )
                                                    }
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(
                                                            paddingIcon / 2
                                                        )
                                                    ) {
                                                        Text(
                                                            text = "$acceptedInKiloText - $deliveryInKiloText",
                                                            style = styleHint
                                                        )
                                                        Text(
                                                            text = "$consumptionInKiloText кг.",
                                                            style = styleHint
                                                        )
                                                    }
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(
                                                            paddingIcon / 2
                                                        )
                                                    ) {
                                                        Text(
                                                            text = "k: $coefficientText",
                                                            style = styleHint
                                                        )
                                                        sectionDiesel.fuelSupply?.let {
                                                            Text(
                                                                text = "Снабжение: $fuelSupplyText л.",
                                                                style = styleHint
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    Text(
                                                        text = "Нет данных",
                                                        style = styleHint
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (route.trains.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = paddingBetweenBlocks)
                            .animateItemPlacement(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "Поезд",
                            style = styleTitle,
                        )
                    }
                }
            }
            itemsIndexed(route.trains, key = { _, train -> train.trainId }) { index, train ->
                if (trainExpandItemState[index] == null) {
                    trainExpandItemState[index] = true
                }
                val numberText = train.number.ifNullOrBlank { "" }
                val weightText = train.weight.ifNullOrBlank { "" }
                val axleText = train.axle.ifNullOrBlank { "" }
                val lengthText = train.conditionalLength.ifNullOrBlank { "" }

                val rotationStationButton =
                    animateFloatAsState(
                        targetValue = if (trainExpandItemState[index]!!) 180f else 0f,
                        label = ""
                    )

                Column(modifier = Modifier.animateItemPlacement()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = paddingInsideBlock),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row {
                            // Icon
                            Box(
                                modifier = Modifier
                                    .size(iconSize)
                                    .background(
                                        color = MaterialTheme.colorScheme.secondary,
                                        shape = Shapes.medium
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    modifier = Modifier.fillMaxSize(0.7f),
                                    tint = MaterialTheme.colorScheme.primary,
                                    painter = painterResource(id = R.drawable.description_24px),
                                    contentDescription = null
                                )
                            }
                            Column(modifier = Modifier.padding(start = paddingIcon)) {
                                Box {
                                    Text(
                                        text = numberText,
                                        style = styleData,
                                    )
                                }
                                Row {
                                    train.weight?.let {
                                        Text(
                                            text = "Вес: ",
                                            style = styleHint,
                                        )
                                        Text(
                                            text = weightText,
                                            style = styleHint,
                                        )
                                    }
                                    train.axle?.let {
                                        Text(
                                            text = "  Оси: ",
                                            style = styleHint,
                                        )
                                        Text(
                                            text = axleText,
                                            style = styleHint,
                                        )
                                    }
                                    train.conditionalLength?.let {
                                        Text(
                                            text = "  у.д.: ",
                                            style = styleHint,
                                        )
                                        Text(
                                            text = lengthText,
                                            style = styleHint,
                                        )
                                    }
                                }
                            }
                        }
                        IconButton(
                            modifier = Modifier.graphicsLayer(
                                rotationZ = rotationStationButton.value
                            ),
                            onClick = {
                                trainExpandItemState[index] = !trainExpandItemState[index]!!
                            }) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    AnimatedVisibility(visible = trainExpandItemState[index]!!) {
                        Column {
                            train.stations.forEachIndexed { index, station ->
                                val stationNameText = station.stationName.ifNullOrBlank { "" }
                                val timeArrival = getTimeFromDateLong(station.timeArrival)
                                val timeDeparture = getTimeFromDateLong(station.timeDeparture)
                                // Icon
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            top = paddingInsideBlock,
                                            start = horizontalPaddingSecondItem
                                        ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(iconSizeSecond)
                                            .background(
                                                color = MaterialTheme.colorScheme.secondary,
                                                shape = Shapes.medium
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            modifier = Modifier.fillMaxSize(0.7f),
                                            tint = MaterialTheme.colorScheme.primary,
                                            painter = painterResource(id = R.drawable.location_on_24px),
                                            contentDescription = null
                                        )
                                    }
                                    Column(modifier = Modifier.padding(start = paddingIcon)) {
                                        Text(text = stationNameText, style = styleHint)

                                        Row {
                                            Text(text = timeArrival, style = styleHint)
                                            if (timeArrival.isNotBlank() && timeDeparture.isNotBlank()) {
                                                Text(text = " - ", style = styleHint)
                                            }
                                            Text(text = timeDeparture, style = styleHint)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (route.passengers.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = paddingBetweenBlocks)
                            .animateItemPlacement(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "Пассажир",
                            style = styleTitle,
                        )
                    }
                }
            }
            items(route.passengers, key = { passenger -> passenger.passengerId }) { passenger ->
                val numberText = passenger.trainNumber.ifNullOrBlank { "" }
                val stationDeparture = passenger.stationDeparture.ifNullOrBlank { "" }
                val stationArrival = passenger.stationArrival.ifNullOrBlank { "" }
                val timeDeparture = getTimeFromDateLong(passenger.timeDeparture)
                val timeArrival = getTimeFromDateLong(passenger.timeArrival)
                val timeFollowing =
                    DateAndTimeConverter.getTimeInStringFormat(passenger.getFollowingTime())
                        .ifNullOrBlank { "" }
                val notesText = passenger.notes.ifNullOrBlank { "" }

                Column(modifier = Modifier.animateItemPlacement()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = paddingInsideBlock)
                    ) {
                        // Icon
                        Box(
                            modifier = Modifier
                                .size(iconSize)
                                .background(
                                    color = MaterialTheme.colorScheme.secondary,
                                    shape = Shapes.medium
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                modifier = Modifier.fillMaxSize(0.7f),
                                tint = MaterialTheme.colorScheme.primary,
                                painter = painterResource(id = R.drawable.passenger_24px),
                                contentDescription = null
                            )
                        }
                        Column(modifier = Modifier.padding(start = paddingIcon)) {
                            Text(text = timeFollowing, style = styleData)
                            if (passenger.timeDeparture != null || passenger.timeArrival != null) {
                                Row {
                                    Text(text = "$timeDeparture - ", style = styleHint)
                                    Text(text = timeArrival, style = styleHint)
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                top = paddingInsideBlock,
                                start = horizontalPaddingSecondItem
                            )
                    ) {
                        // Icon
                        Box(
                            modifier = Modifier
                                .size(iconSizeSecond)
                                .background(
                                    color = MaterialTheme.colorScheme.secondary,
                                    shape = Shapes.medium
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                modifier = Modifier.fillMaxSize(0.7f),
                                tint = MaterialTheme.colorScheme.primary,
                                painter = painterResource(id = R.drawable.number_123_24px),
                                contentDescription = null
                            )
                        }
                        Column(modifier = Modifier.padding(start = paddingIcon)) {
                            Text(text = numberText, style = styleData)
                            if (stationDeparture.isNotBlank() && stationArrival.isNotBlank()) {
                                Row {
                                    Text(text = "$stationDeparture - ", style = styleHint)
                                    Text(text = stationArrival, style = styleHint)
                                }
                            }
                            Text(
                                text = notesText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = styleHint
                            )
                        }
                    }
                }
            }
            if (!route.basicData.notes.isNullOrBlank() || route.photos.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = paddingBetweenBlocks)
                            .animateItemPlacement(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "Заметки",
                            style = styleTitle,
                        )
                    }
                }
            }
            item {
                val notesText = route.basicData.notes.ifNullOrBlank { "" }
                Column(
                    modifier = Modifier
                        .padding(top = paddingInsideBlock)
                        .fillMaxWidth()
                ) {
                    route.basicData.notes?.let {
                        Row {
                            Box(
                                modifier = Modifier
                                    .size(iconSize)
                                    .background(
                                        color = MaterialTheme.colorScheme.secondary,
                                        shape = Shapes.medium
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    modifier = Modifier.fillMaxSize(0.7f),
                                    tint = MaterialTheme.colorScheme.primary,
                                    painter = painterResource(id = R.drawable.notes_24px),
                                    contentDescription = null
                                )
                            }
                            Text(
                                text = notesText,
                                style = styleData,
                                modifier = Modifier.padding(start = paddingIcon)
                            )
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.padding(top = 24.dp))
            }
        }
    }
}



