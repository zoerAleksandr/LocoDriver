package com.z_company.route.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BasicTooltipBox
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberBasicTooltipState
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.flowWithLifecycle
import com.z_company.core.ResultState
import com.z_company.core.ui.component.AsyncData
import com.z_company.core.ui.component.AsyncDataValue
import com.z_company.core.ui.component.toDp
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.core.util.ConverterLongToTime
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.core.util.MonthFullText.getMonthFullText
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.UtilForMonthOfYear.getNormaHoursInDate
import com.z_company.domain.entities.UtilForMonthOfYear.getPersonalNormaHours
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.route.UtilsForEntities.fullRest
import com.z_company.domain.entities.route.UtilsForEntities.getFollowingTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.entities.route.UtilsForEntities.isTransition
import com.z_company.domain.entities.route.UtilsForEntities.shortRest
import com.z_company.domain.util.CalculationEnergy
import com.z_company.domain.util.CalculationEnergy.rounding
import com.z_company.domain.util.ifNullOrBlank
import com.z_company.domain.util.minus
import com.z_company.domain.util.str
import com.z_company.domain.util.times
import com.z_company.repository.ShareManager
import com.z_company.route.R
import com.z_company.route.component.AnimatedCounter
import com.z_company.route.component.AnimationDialog
import com.z_company.route.component.DialogSelectMonthOfYear
import com.z_company.route.component.ItemHomeScreen
import com.z_company.route.component.LinearPagerIndicator
import com.z_company.route.component.PieChart
import com.z_company.route.viewmodel.home_view_model.AlertBeforePurchasesEvent
import com.z_company.route.viewmodel.home_view_model.ItemState
import com.z_company.route.viewmodel.home_view_model.SetTimeInTrainEvent
import com.z_company.route.viewmodel.home_view_model.StartPurchasesEvent
import com.z_company.route.viewmodel.home_view_model.UpdateEvent
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.rustore.sdk.core.feature.model.FeatureAvailabilityResult
import java.util.Calendar

@SuppressLint(
    "CoroutineCreationDuringComposition",
    "FlowOperatorInvokedInComposition",
    "SuspiciousIndentation"
)
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class
)
@Composable
fun HomeScreen(
    uiState: ResultState<Unit>,
    listRouteState: MutableList<ItemState>,
    routeListState: ResultState<List<Route>>,
    removeRouteState: ResultState<Unit>?,
    onRouteClick: (String) -> Unit,
    onNewRouteClick: () -> Unit,
    onMoreInfoClick: (String) -> Unit,
    makeCopyRoute: (String) -> Unit,
    onDeleteRoute: (Route) -> Unit,
    onDeleteRouteConfirmed: () -> Unit,
//    reloadRoute: () -> Unit,
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit,
    totalTime: Long,
    currentMonthOfYear: MonthOfYear?,
    monthList: List<Int>,
    yearList: List<Int>,
    selectYearAndMonth: (Pair<Int, Int>) -> Unit,
    minTimeRest: Long?,
    nightTimeState: ResultState<Long>?,
    singleLocomotiveTimeState: ResultState<Long>?,
    passengerTimeState: ResultState<Long>?,
    dayoffHours: ResultState<Int>?,
    holidayHours: ResultState<Long>?,
    totalTimeWithHoliday: ResultState<Long>?,
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
    checkPurchasesAvailability: () -> Unit,
    restorePurchases: () -> Unit,
    restoreResultState: ResultState<String>?,
    resetSubscriptionState: () -> Unit,
    showConfirmDialogRemoveRoute: Boolean,
    changeShowConfirmExitDialog: (Boolean) -> Unit,
    offsetInMoscow: Long,
    syncRouteState: ResultState<String>?,
    resetSyncRouteState: () -> Unit,
    syncRoute: (Route) -> Unit,
    updateEvent: SharedFlow<UpdateEvent>,
    completeUpdateRequested: () -> Unit,
    setFavoriteState: (Route) -> Unit,
    getSharedIntent: (Route) -> Intent,
    getTextWorkTime: (Route) -> String,
    dateAndTimeConverter: DateAndTimeConverter?,
    extendedServicePhaseTime: ResultState<Long>?,
    longDistanceTrainsTime: ResultState<Long>?,
    heavyTrainsTime: ResultState<Long>?,
    onePersonOperationTime: ResultState<Long>?,
    currentRoute: Route?,
    currentRouteTimeWork: SharedFlow<Long>,
    onNewLocoClick: (basicId: String) -> Unit,
    onChangedLocoClick: (loco: Locomotive) -> Unit,
    onNewTrainClick: (basicId: String) -> Unit,
    onChangedTrainClick: (train: Train) -> Unit,
    onNewPassengerClick: (basicId: String) -> Unit,
    onChangedPassengerClick: (passenger: Passenger) -> Unit,
    onGoClicked: () -> Unit,
    onAllRouteClick: () -> Unit,
    isNextDeparture: () -> Boolean,
    saveTimeEvent: SharedFlow<String>
) {
    val view = LocalView.current
    val backgroundColor = MaterialTheme.colorScheme.background

    val redOrange = Color(0xFFf1642e)
    val purple = Color(0xFF504e76)
    val green = Color(0xFFa3b565)

    // для изменения color status bar после изменения в PresentationBlock
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = backgroundColor.toArgb()
        }
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val snackbarHostState = remember { SnackbarHostState() }

    val heightScreen = LocalConfiguration.current.screenHeightDp
    val widthScreen = LocalConfiguration.current.screenWidthDp

    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        scope.launch {
            updateEvent.flowWithLifecycle(lifecycle).collect { event ->
                when (event) {
                    UpdateEvent.UpdateCompleted -> {
                        val result = snackbarHostState
                            .showSnackbar(
                                message = "Обновление загружено",
                                actionLabel = "Установить"
                            )
                        if (result == SnackbarResult.ActionPerformed) {
                            completeUpdateRequested()
                        }
                    }
                }
            }
        }
    }

//    DisposableEffect(key1 = lifecycleOwner, effect = {
//        val observer = LifecycleEventObserver { _, event ->
//            if (event == Lifecycle.Event.ON_START) {
////                reloadRoute()
////                resetStateIsLaunchedInitState()
//            }
//        }
//        lifecycleOwner.lifecycle.addObserver(observer)
//
//        onDispose { }
//    })


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
                snackbarHostState.showSnackbar("Ошибка синхронизации. Проверьте интернет.")
            }
        }
    }) { message ->
        message?.let {
            scope.launch {
                snackbarHostState.showSnackbar(message)
            }
        }
        resetSubscriptionState()
    }

    AsyncData(resultState = syncRouteState) { message ->
        message?.let {
            scope.launch {
                snackbarHostState.showSnackbar(message)
            }
        }
        resetSyncRouteState()
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
                            checkPurchasesAvailability()
                        }
                    ) {
                        Text(
                            text = "Оформить подписку за 44 руб/мес",
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
                            checkPurchasesAvailability()
                        }
                    ) {
                        Text(
                            text = "Оформить подписку за 44 руб/мес",
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
                snackbarHostState.showSnackbar(
                    message = context.getString(R.string.msg_route_deleted)
                )
                onDeleteRouteConfirmed()
            }
        }
    }

    var routeForPreview by remember {
        mutableStateOf<Route?>(null)
    }

    var routeForRemove by remember {
        mutableStateOf<Route?>(null)
    }

    var showContextDialog by remember {
        mutableStateOf(false)
    }

    var currentRouteWorkTime by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        scope.launch {
            currentRouteTimeWork.flowWithLifecycle(lifecycle).collect { time ->
                currentRouteWorkTime =
                    ConverterLongToTime.getTimeInStringFormat(time)
            }
        }
    }

    LaunchedEffect(saveTimeEvent) {
        saveTimeEvent.collectLatest {
            scope.launch {
                snackbarHostState.showSnackbar("$it")
            }
        }
    }

//    LaunchedEffect(isShowSnackbar) {
//        scope.launch {
//            isOnTheWayState.flowWithLifecycle(lifecycle).collect { state ->
//                if (isShowSnackbar) {
//                    state.message?.let { text ->
//                        scope.launch {
//                            snackbarHostState.showSnackbar(message = text)
//                        }
//                        resetStateShowSnackbar()
//                    }
//                }
//                onTheWay = state.isOnTheWay
//            }
//        }
//    }

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
        val shareManager = rememberShareManager()
        val scope = rememberCoroutineScope()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 32.dp)
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
                        max = heightScreen.times(0.65f).dp
                    )
                    .padding(start = 12.dp, end = 12.dp, top = 30.dp, bottom = 12.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = Shapes.medium
                    )
                    .clickable {}
            ) {
                calculationHomeRest(routeForPreview)
                PreviewRoute(
                    routeForPreview,
                    minTimeRest,
                    homeRestValue,
                    dateAndTimeConverter
                )
            }

            Column(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .fillMaxWidth(0.6f)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = Shapes.medium
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable {
                            showContextDialog = false
                            routeForPreview?.let { route ->
                                syncRoute(route)
                            }
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Сохранить в облаке",
                        style = AppTypography.getType().bodyMedium.copy(color = MaterialTheme.colorScheme.primary)
                    )
                    Image(
                        modifier = Modifier.size(25.dp),
                        painter = painterResource(id = R.drawable.sync_on_icon),
                        contentDescription = null,
                    )
                }
                HorizontalDivider()
//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(horizontal = 16.dp, vertical = 8.dp)
//                        .clickable {
//                            showContextDialog = false
//                            routeForPreview?.let { route ->
//                                val intent = getSharedIntent(route)
//                                val type = intent.type
//                                Log.d("ZZZ", "type = $type")
//                                context.startActivity(
//                                    Intent.createChooser(
//                                        intent,
//                                        "Поделиться маршрутом"
//                                    )
//                                )
//                            }
//                        },
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Text(
//                        text = "Поделиться",
//                        style = AppTypography.getType().bodyMedium.copy(color = MaterialTheme.colorScheme.primary)
//                    )
//                    Image(
//                        modifier = Modifier.size(25.dp),
//                        imageVector = Icons.Outlined.Share,
//                        contentDescription = null,
//                    )
//                }
//                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable {
                            showContextDialog = false
                            routeForPreview?.let { route ->
                                setFavoriteState(route)
                            }
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val text =
                        if (routeForPreview!!.basicData.isFavorite) "Убрать из избранного" else "В избранное"
                    val icon =
                        if (routeForPreview!!.basicData.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder

                    Text(
                        text = text,
                        style = AppTypography.getType().bodyMedium.copy(color = MaterialTheme.colorScheme.primary)
                    )
                    Icon(
                        modifier = Modifier.size(25.dp),
                        imageVector = icon,
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = null,
                    )
                }
                HorizontalDivider()

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
                                makeCopyRoute(route.basicData.id)
                            }
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Дублировать",
                        style = AppTypography.getType().bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.outline_content_copy_24),
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
                                changeShowConfirmExitDialog(true)
                                routeForRemove = route
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
    if (showConfirmDialogRemoveRoute) {
        AlertDialog(
            onDismissRequest = { changeShowConfirmExitDialog(false) },
            title = {
                Text(text = "Внимание!", style = AppTypography.getType().headlineSmall)
            },
            text = {
                Text(
                    text = "Удалить маршрут?",
                    style = AppTypography.getType().bodyLarge
                )
            },
            shape = Shapes.medium,
            confirmButton = {
                Button(
                    shape = Shapes.medium,
                    onClick = {
                        changeShowConfirmExitDialog(false)
                        routeForRemove?.let {
                            onDeleteRoute(it)
                        }
                    }
                ) {
                    Text(text = "Удалить", style = AppTypography.getType().titleMedium)
                }
            },
            dismissButton = {
                TextButton(onClick = { changeShowConfirmExitDialog(false) }) {
                    Text(
                        text = "Отмена",
                        style = AppTypography.getType().titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        )
    }

    val brushMain = Brush.linearGradient(
        0.1f to MaterialTheme.colorScheme.surfaceVariant,
        1500.0f to MaterialTheme.colorScheme.surface,
        start = Offset.Zero,
        end = Offset.Infinite
    )
    Scaffold(
        topBar = {
            TopAppBar(
                scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                title = {},
                actions = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            shape = Shapes.medium,
                            onClick = {
                                showMonthSelectorDialog.value = true
                            }) {
                            val text = currentMonthOfYear?.month?.let {
                                getMonthFullText(it)
                            } ?: "загрузка"
                            Text(
                                text = "$text ${currentMonthOfYear?.year}",
                                style = AppTypography.getType().headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        IconButton(
                            modifier = Modifier
                                .background(
                                    color = Color.Transparent,
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
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                containerColor = green,
                onClick = onNewRouteClick
            ) {
                Icon(
                    tint = MaterialTheme.colorScheme.background,
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null
                )
            }
        },
        bottomBar = {
            val colors: NavigationBarItemColors = NavigationBarItemDefaults.colors(
                unselectedTextColor = MaterialTheme.colorScheme.background,
                selectedTextColor = MaterialTheme.colorScheme.background,
                unselectedIconColor = MaterialTheme.colorScheme.background,
                selectedIconColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.background
            )
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                NavigationBarItem(
                    colors = colors,
                    selected = true,
                    icon = {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            imageVector = Icons.Outlined.Home, contentDescription = null
                        )
                    },
                    label = {
                        Text(
                            text = "Главная",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = {}
                )

                NavigationBarItem(
                    colors = colors,
                    selected = false,
                    icon = {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            painter = painterResource(R.drawable.rub),
                            contentDescription = null
                        )
                    },
                    label = {
                        Text(
                            text = "ЗП",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = {}
                )

                NavigationBarItem(
                    colors = colors,
                    selected = false,
                    icon = {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = null
                        )
                    },
                    label = {
                        Text(
                            text = "Настройки",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = onSettingsClick
                )

                NavigationBarItem(
                    colors = colors,
                    selected = false,
                    icon = {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null
                        )
                    },
                    label = {
                        Text(
                            text = "Профиль",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = {}
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
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
                                    snackbarHostState.showSnackbar("Ошибка: ${event.availability.cause.message}")
                                }
                            }
                        }

                        is StartPurchasesEvent.Error -> {
                            snackbarHostState.showSnackbar("Ошибка: ${event.throwable.message}")
                        }
                    }
                }
            }
        }
        val pagerState = rememberPagerState(pageCount = { 3 })
        AsyncData(uiState) {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        HorizontalPager(
                            modifier = Modifier.animateItemPlacement(),
                            state = pagerState
                        ) { page ->
                            when (page) {
                                0 -> {
                                    MainInfo(
                                        totalTime = totalTime,
                                        totalTimeWithHoliday = totalTimeWithHoliday,
                                        currentMonthOfYear = currentMonthOfYear,
                                        dateAndTimeConverter = dateAndTimeConverter,
                                        brush = brushMain
                                    )
                                }

                                1 -> {
                                    DetailWorkTimeCard(
                                        totalTime = totalTime,
                                        brush = brushMain,
                                        totalTimeWithHoliday = totalTimeWithHoliday,
                                        passengerTimeState = passengerTimeState,
                                        singleLocomotiveTimeState = singleLocomotiveTimeState,
                                        nightTimeState = nightTimeState
                                    )
                                }

                                2 -> {
                                    DetailTrainCard(
                                        totalTime = totalTime,
                                        brush = brushMain,
                                        totalTimeWithHoliday = totalTimeWithHoliday,
                                        extendedServicePhaseTime = extendedServicePhaseTime,
                                        longDistanceTrainsTime = longDistanceTrainsTime,
                                        heavyTrainsTime = heavyTrainsTime,
                                        onePersonOperationTime = onePersonOperationTime
                                    )
                                }
                            }
                        }
                        LinearPagerIndicator(
                            modifier = Modifier
                                .animateItemPlacement(),
                            state = pagerState
                        )
                    }
                }

                val brushSecondary = Brush.linearGradient(
                    0.1f to Color(0xFFefede3),
                    1500.0f to Color(0xFFFDFDFC),
                    start = Offset.Zero,
                    end = Offset.Infinite
                )

                item {
                    currentRoute?.let { route ->
                        var maxHeightBox by remember { mutableStateOf(widthScreen / 3) }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItemPlacement()
                        ) {
                            Text(
                                modifier = Modifier
                                    .padding(horizontal = 24.dp)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                onRouteClick(route.basicData.id)
                                            }
                                        )
                                    },
                                text = "Текущий маршрут",
                                style = AppTypography.getType().titleMedium
                            )
                            LazyRow(
                                modifier = Modifier.padding(top = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    Card(
                                        modifier = Modifier
                                            .onGloballyPositioned { coordinates ->
                                                val currentHeight = coordinates.size.height
                                                if (currentHeight > maxHeightBox) {
                                                    maxHeightBox = currentHeight
                                                }
                                            }
                                            .padding(start = 12.dp)
                                            .defaultMinSize(
                                                minWidth = (widthScreen / 3).dp,
                                                minHeight = (widthScreen / 3).dp,
                                            )
                                            .clickable {
                                                onRouteClick(route.basicData.id)
                                            },
                                        elevation = CardDefaults.elevatedCardElevation(
                                            defaultElevation = 2.dp,
                                        ),
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .defaultMinSize(
                                                    minWidth = (widthScreen / 3).dp,
                                                    minHeight = (widthScreen / 3).dp,
                                                )
                                                .background(brushMain)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .defaultMinSize(
                                                        minWidth = (widthScreen / 3).dp,
                                                        minHeight = maxHeightBox.toDp(),
                                                    )
                                                    .padding(vertical = 8.dp, horizontal = 16.dp),
                                                verticalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                AnimatedCounter(
                                                    count = currentRouteWorkTime,
                                                    style = AppTypography.getType().headlineMedium.copy(
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.background
                                                    )
                                                )
                                                Text(
                                                    text = "На работе",
                                                    color = MaterialTheme.colorScheme.background,
                                                    maxLines = 1,
                                                    style = AppTypography.getType().titleMedium,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                                item {
                                    Card(
                                        modifier = Modifier
                                            .onGloballyPositioned { coordinates ->
                                                val currentHeight = coordinates.size.height
                                                if (currentHeight > maxHeightBox) {
                                                    maxHeightBox = currentHeight
                                                }
                                            }
                                            .defaultMinSize(
                                                minWidth = (widthScreen / 3).dp,
                                                minHeight = (widthScreen / 3).dp,
                                            ),
                                        elevation = CardDefaults.elevatedCardElevation(
                                            defaultElevation = 2.dp,
                                        ),
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .defaultMinSize(
                                                    minWidth = (widthScreen / 3).dp,
                                                    minHeight = (widthScreen / 3).dp,
                                                )
                                                .background(MaterialTheme.colorScheme.secondary)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .defaultMinSize(
                                                        minWidth = (widthScreen / 3).dp,
                                                        minHeight = maxHeightBox.toDp(),
                                                    )
                                                    .padding(vertical = 8.dp, horizontal = 16.dp),
                                                verticalArrangement = Arrangement.SpaceBetween,
                                            ) {
                                                if (route.locomotives.isEmpty()) {
                                                    IconButton(
                                                        modifier = Modifier.align(Alignment.End),
                                                        colors = IconButtonDefaults.iconButtonColors(
                                                            containerColor = green,
                                                            contentColor = MaterialTheme.colorScheme.background
                                                        ),
                                                        onClick = {
                                                            onNewLocoClick(route.basicData.id)
                                                        }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Add,
                                                            contentDescription = null
                                                        )
                                                    }
                                                } else {
                                                    Column(
                                                        modifier = Modifier
                                                            .padding(bottom = 8.dp),
                                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        val loco = route.locomotives.last()
                                                        Box(
                                                            modifier = Modifier
                                                                .pointerInput(Unit) {
                                                                    detectTapGestures(
                                                                        onPress = {
                                                                            onChangedLocoClick(
                                                                                loco
                                                                            )
                                                                        }
                                                                    )
                                                                }
                                                        ) {
                                                            Text(
                                                                text = "${loco.series ?: ""} ${loco.number ?: ""}",
                                                                color = MaterialTheme.colorScheme.primary,
                                                                maxLines = 1,
                                                                style = AppTypography.getType().titleLarge,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                        if (route.locomotives.size > 1) {
                                                            Text(
                                                                text = "... и ещё ${route.locomotives.size - 1}",
                                                                color = MaterialTheme.colorScheme.primary,
                                                                maxLines = 1,
                                                                style = AppTypography.getType().bodyLarge,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                }
                                                Text(
                                                    text = "Локомотив",
                                                    color = MaterialTheme.colorScheme.primary,
                                                    maxLines = 1,
                                                    style = AppTypography.getType().titleMedium,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                                item {
                                    Card(
                                        modifier = Modifier
                                            .onGloballyPositioned { coordinates ->
                                                val currentHeight = coordinates.size.height
                                                if (currentHeight > maxHeightBox) {
                                                    maxHeightBox = currentHeight
                                                }
                                            }
                                            .defaultMinSize(
                                                minWidth = (widthScreen / 3).dp,
                                                minHeight = (widthScreen / 3).dp,
                                            ),
                                        elevation = CardDefaults.elevatedCardElevation(
                                            defaultElevation = 2.dp,
                                        ),
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .defaultMinSize(
                                                    minWidth = (widthScreen / 3).dp,
                                                    minHeight = (widthScreen / 3).dp,
                                                )
                                                .background(MaterialTheme.colorScheme.secondary)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .defaultMinSize(
                                                        minWidth = (widthScreen / 3).dp,
                                                        minHeight = maxHeightBox.toDp(),
                                                    )
                                                    .padding(vertical = 8.dp, horizontal = 16.dp),
                                                verticalArrangement = Arrangement.SpaceBetween,
                                            ) {
                                                if (route.trains.isEmpty()) {
                                                    IconButton(
                                                        modifier = Modifier.align(Alignment.End),
                                                        colors = IconButtonDefaults.iconButtonColors(
                                                            containerColor = green,
                                                            contentColor = MaterialTheme.colorScheme.background
                                                        ),
                                                        onClick = {
                                                            onNewTrainClick(route.basicData.id)
                                                        }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Add,
                                                            contentDescription = null
                                                        )
                                                    }
                                                } else {
                                                    Column(
                                                        modifier = Modifier
                                                            .padding(bottom = 8.dp),
                                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        val train = route.trains.last()
                                                        Column(
                                                            modifier = Modifier
                                                                .pointerInput(Unit) {
                                                                    detectTapGestures(
                                                                        onPress = {
                                                                            onChangedTrainClick(
                                                                                train
                                                                            )
                                                                        }
                                                                    )
                                                                }
                                                        ) {
                                                            train.number?.let {
                                                                Text(
                                                                    text = "№ $it",
                                                                    color = MaterialTheme.colorScheme.primary,
                                                                    maxLines = 1,
                                                                    style = AppTypography.getType().titleLarge,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }
                                                            val firstStation =
                                                                train.stations.firstOrNull()
                                                                    ?.let { it.stationName ?: "" }
                                                                    ?: ""
                                                            val lastStation =
                                                                if (train.stations.size > 1) {
                                                                    train.stations.lastOrNull()
                                                                        ?.let { " - ${it.stationName ?: ""}" }
                                                                        ?: ""
                                                                } else {
                                                                    ""
                                                                }
                                                            val trainInfoText =
                                                                "$firstStation $lastStation"

                                                            if (trainInfoText.isNotBlank()) {
                                                                Text(
                                                                    text = trainInfoText,
                                                                    color = MaterialTheme.colorScheme.primary,
                                                                    maxLines = 1,
                                                                    style = AppTypography.getType().bodyLarge,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }
                                                        }
                                                        if (route.trains.size > 1) {
                                                            Text(
                                                                text = "... и ещё ${route.trains.size - 1}",
                                                                color = MaterialTheme.colorScheme.primary,
                                                                maxLines = 1,
                                                                style = AppTypography.getType().bodyLarge,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                }
                                                Row(
                                                    modifier = Modifier.wrapContentWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                    verticalAlignment = Alignment.Bottom
                                                ) {
                                                    Text(
                                                        text = "Поезд",
                                                        color = MaterialTheme.colorScheme.primary,
                                                        maxLines = 1,
                                                        style = AppTypography.getType().titleMedium,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    if (route.trains.isNotEmpty()) {
                                                        val nextIsDeparture = isNextDeparture()
                                                        OutlinedButton(
                                                            onClick = {
                                                                onGoClicked()
                                                            },
                                                            border = BorderStroke(
                                                                width = 1.dp,
                                                                color = if (nextIsDeparture) green else purple
                                                            ),
                                                        ) {
                                                            AnimatedContent(targetState = nextIsDeparture) {
                                                                val icon = if (it) {
                                                                    R.drawable.play_arrow_24px
                                                                } else {
                                                                    R.drawable.pause_24px
                                                                }
                                                                Icon(
                                                                    painter = painterResource(icon),
                                                                    contentDescription = null,
                                                                    tint = if (it) green else purple
                                                                )
                                                            }
//                                                        AnimatedContent(targetState = onTheWay) {
//                                                            val text = if (it) {
//                                                                "Остановка"
//                                                            } else {
//                                                                "Отправление"
//                                                            }
//                                                            Text(
//                                                                text = text,
//                                                                maxLines = 1,
//                                                                overflow = TextOverflow.Ellipsis,
//                                                                style = AppTypography.getType().bodyLarge,
//                                                                color = if (it) purple else green
//                                                            )
//                                                        }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                item {
                                    Card(
                                        modifier = Modifier
                                            .onGloballyPositioned { coordinates ->
                                                val currentHeight = coordinates.size.height
                                                if (currentHeight > maxHeightBox) {
                                                    maxHeightBox = currentHeight
                                                }
                                            }
                                            .defaultMinSize(
                                                minWidth = (widthScreen / 3).dp,
                                                minHeight = (widthScreen / 3).dp,
                                            )
                                            .padding(end = 12.dp),
                                        elevation = CardDefaults.elevatedCardElevation(
                                            defaultElevation = 2.dp,
                                        ),
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .defaultMinSize(
                                                    minWidth = (widthScreen / 3).dp,
                                                    minHeight = (widthScreen / 3).dp,
                                                )
                                                .background(MaterialTheme.colorScheme.secondary)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .defaultMinSize(
                                                        minWidth = (widthScreen / 3).dp,
                                                        minHeight = maxHeightBox.toDp(),
                                                    )
                                                    .padding(vertical = 8.dp, horizontal = 16.dp),
                                                verticalArrangement = Arrangement.SpaceBetween,
                                            ) {
                                                if (route.passengers.isEmpty()) {
                                                    IconButton(
                                                        modifier = Modifier.align(Alignment.End),
                                                        colors = IconButtonDefaults.iconButtonColors(
                                                            containerColor = green,
                                                            contentColor = MaterialTheme.colorScheme.background
                                                        ),
                                                        onClick = {
                                                            onNewPassengerClick(route.basicData.id)
                                                        }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Add,
                                                            contentDescription = null
                                                        )
                                                    }
                                                } else {
                                                    Column(
                                                        modifier = Modifier
                                                            .padding(bottom = 8.dp),
                                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        val passenger = route.passengers.last()
                                                        Column(
                                                            modifier = Modifier
                                                                .pointerInput(Unit) {
                                                                    detectTapGestures(
                                                                        onPress = {
                                                                            onChangedPassengerClick(
                                                                                passenger
                                                                            )
                                                                        }
                                                                    )
                                                                }
                                                        ) {
                                                            passenger.trainNumber?.let {
                                                                Text(
                                                                    text = "№ $it",
                                                                    color = MaterialTheme.colorScheme.primary,
                                                                    maxLines = 1,
                                                                    style = AppTypography.getType().titleLarge,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }
                                                            Text(
                                                                text = "${passenger.stationDeparture ?: ""} ${passenger.stationArrival?.let { " - $it" } ?: ""} ",
                                                                color = MaterialTheme.colorScheme.primary,
                                                                maxLines = 1,
                                                                style = AppTypography.getType().bodyLarge,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                        if (route.passengers.size > 1) {
                                                            Text(
                                                                text = "... и ещё ${route.passengers.size - 1}",
                                                                color = MaterialTheme.colorScheme.primary,
                                                                maxLines = 1,
                                                                style = AppTypography.getType().bodyLarge,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                }
                                                Text(
                                                    text = "Пассажиром",
                                                    color = MaterialTheme.colorScheme.primary,
                                                    maxLines = 1,
                                                    style = AppTypography.getType().titleMedium,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItemPlacement()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 24.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Маршруты",
                                style = AppTypography.getType().titleMedium
                            )
                            TextButton(onClick = {
                                onAllRouteClick()
                            }) {
                                Text(
                                    color = MaterialTheme.colorScheme.tertiary,
                                    text = "Все"
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            var background = MaterialTheme.colorScheme.secondary

                            if (Route(BasicData(timeStartWork = Calendar.getInstance().timeInMillis)).basicData.timeStartWork!! > Calendar.getInstance().timeInMillis) {
                                background = MaterialTheme.colorScheme.surfaceBright
                            } else {
                                if (Route().isTransition(offsetInMoscow)) {
                                    background = MaterialTheme.colorScheme.surfaceDim
                                }
                            }
                            var requiredSize by remember {
                                mutableStateOf(22.sp)
                            }

                            fun changingTextSize(value: TextUnit) {
                                if (requiredSize > value) {
                                    requiredSize = value
                                }
                            }

                            if (listRouteState.isNotEmpty()) {
                                val route = listRouteState.first().route
                                ItemHomeScreen(
                                    modifier = Modifier.animateItemPlacement(),
                                    route = route,
                                    isExpand = true,
                                    onDelete = onDeleteRoute,
                                    requiredSizeText = requiredSize,
                                    changingTextSize = ::changingTextSize,
                                    onLongClick = {
                                        showContextDialog = true
                                        routeForPreview = route
                                    },
                                    containerColor = background,
                                    onClick = { onRouteClick(route.basicData.id) },
                                    getTextWorkTime = getTextWorkTime,
                                    isHeavyTrains = listRouteState[0].isHeavyTrains,
                                    isExtendedServicePhaseTrains = listRouteState[0].isExtendedServicePhaseTrains,
                                    isHolidayTimeInRoute = listRouteState[0].isHoliday
                                )
                            }
                            if (listRouteState.size > 1) {
                                val route = listRouteState[1].route
                                ItemHomeScreen(
                                    modifier = Modifier.animateItemPlacement(),
                                    route = route,
                                    isExpand = true,
                                    onDelete = onDeleteRoute,
                                    requiredSizeText = requiredSize,
                                    changingTextSize = ::changingTextSize,
                                    onLongClick = {
                                        showContextDialog = true
                                        routeForPreview = route
                                    },
                                    containerColor = background,
                                    onClick = { onRouteClick(route.basicData.id) },
                                    getTextWorkTime = getTextWorkTime,
                                    isHeavyTrains = listRouteState[1].isHeavyTrains,
                                    isExtendedServicePhaseTrains = listRouteState[1].isExtendedServicePhaseTrains,
                                    isHolidayTimeInRoute = listRouteState[1].isHoliday
                                )
                            }
                        }
                    }
                }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItemPlacement()
                    ) {
                        Text(
                            modifier = Modifier
                                .padding(horizontal = 24.dp),
                            text = "Действия"
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .padding(start = 12.dp)
                                        .size((widthScreen / 3).dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.LightGray
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.holiday_icon),
                                            contentDescription = null
                                        )
                                        Text(
                                            modifier = Modifier.weight(1f),
                                            text = "Создать график"
                                        )
                                    }
                                }
                            }
                            item {
                                Card(
                                    modifier = Modifier
                                        .padding(end = 12.dp)
                                        .size((widthScreen / 3).dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.LightGray
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.palma),
                                            contentDescription = null
                                        )
                                        Text(
                                            modifier = Modifier.weight(1f),
                                            text = "Добавить отвлечение"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Spacer(
                        modifier = Modifier
                            .height(40.dp)
                            .animateItemPlacement()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PreviewRoute(
    route: Route?,
    minTimeRest: Long?,
    homeRest: ResultState<Long?>,
    dateAndTimeConverter: DateAndTimeConverter?
) {
    Log.d("zzz", "dateAndTimeConverter in preview ${dateAndTimeConverter.hashCode()}")
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
                            text = dateAndTimeConverter?.getDateFromDateLong(route.basicData.timeStartWork)
                                ?: "загрузка",
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
                                    text = ConverterLongToTime.getTimeInStringFormat(route.getWorkTime()),
                                    style = styleData,
                                    maxLines = 1
                                )
                            }
                            Row {
                                Text(
                                    text = dateAndTimeConverter?.getTimeFromDateLong(route.basicData.timeStartWork)
                                        ?: "загрузка",
                                    style = styleHint,
                                    maxLines = 1
                                )

                                Text(
                                    text = " - ${dateAndTimeConverter?.getTimeFromDateLong(route.basicData.timeEndWork) ?: "загрузка"}",
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
                                    val shortRestText =
                                        dateAndTimeConverter?.getDateMiniAndTime(
                                            route.shortRest(minTimeRest)
                                        ) ?: "загрузка"
                                    val fullRestText = dateAndTimeConverter?.getDateMiniAndTime(
                                        route.fullRest(minTimeRest)
                                    ) ?: "загрузка"
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
                                        val homeRestInLongText =
                                            dateAndTimeConverter?.getDateMiniAndTime(
                                                homeRestInLong
                                            )
                                                ?: "загрузка"
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
                    dateAndTimeConverter?.getTimeFromDateLong(locomotive.timeStartOfAcceptance)
                        ?: "загрузка"
                val timeEndAcceptedText =
                    dateAndTimeConverter?.getTimeFromDateLong(locomotive.timeEndOfAcceptance)
                        ?: "загрузка"
                val timeStartDeliveryText =
                    dateAndTimeConverter?.getTimeFromDateLong(locomotive.timeStartOfDelivery)
                        ?: "загрузка"
                val timeEndDeliveryText =
                    dateAndTimeConverter?.getTimeFromDateLong(locomotive.timeEndOfDelivery)
                        ?: "загрузка"

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
                                                ?: ""
                                        val deliveryEnergyText =
                                            sectionElectric.deliveryEnergy?.toPlainString()
                                                ?: ""
                                        val acceptedRecoveryText =
                                            sectionElectric.acceptedRecovery?.toPlainString()
                                                ?: ""
                                        val deliveryRecoveryText =
                                            sectionElectric.deliveryRecovery?.toPlainString()
                                                ?: ""
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
                                                            Text(
                                                                text = "Расход: $consumptionEnergy",
                                                                style = styleHint,
                                                            )
                                                        }
                                                        if (sectionElectric.acceptedRecovery != null &&
                                                            sectionElectric.deliveryRecovery != null
                                                        ) {
                                                            Text(
                                                                text = "Рекуперация: $consumptionRecovery",
                                                                style = styleHint,
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

                        LocoType.DIESEL -> {
                            AnimatedVisibility(visible = locomotiveExpandItemState[index]!!) {
                                Column {
                                    locomotive.dieselSectionList.forEachIndexed { index, sectionDiesel ->
                                        val consumption =
                                            CalculationEnergy.getTotalFuelConsumption(
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
                                        val acceptedInKiloText =
                                            rounding(acceptedInKilo, 2).str()
                                        val deliveryInKilo =
                                            sectionDiesel.deliveryFuel.times(sectionDiesel.coefficient)
                                        val deliveryInKiloText =
                                            rounding(deliveryInKilo, 2).str()
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
                            train.stations.forEachIndexed { _, station ->
                                val stationNameText = station.stationName.ifNullOrBlank { "" }
                                val timeArrival =
                                    dateAndTimeConverter?.getTimeFromDateLong(station.timeArrival)
                                        ?: "загрузка"
                                val timeDeparture =
                                    dateAndTimeConverter?.getTimeFromDateLong(station.timeDeparture)
                                        ?: "загрузка"
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
                val timeDeparture =
                    dateAndTimeConverter?.getTimeFromDateLong(passenger.timeDeparture)
                        ?: "загрузка"
                val timeArrival =
                    dateAndTimeConverter?.getTimeFromDateLong(passenger.timeArrival)
                        ?: "загрузка"
                val timeFollowing =
                    ConverterLongToTime.getTimeInStringFormat(passenger.getFollowingTime())
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

@Composable
fun rememberShareManager(): ShareManager {
    val context = LocalContext.current
    return remember { ShareManager(context) }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainInfo(
    totalTime: Long,
    totalTimeWithHoliday: ResultState<Long>?,
    currentMonthOfYear: MonthOfYear?,
    dateAndTimeConverter: DateAndTimeConverter?,
    brush: Brush
) {
    Card(
        modifier = Modifier
            .padding(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 3.dp,
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max)
                .background(brush)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 24.dp)
                    .fillMaxWidth(),
            ) {
                AsyncDataValue(resultState = totalTimeWithHoliday) { time ->
                    val tooltipPosition = TooltipDefaults.rememberPlainTooltipPositionProvider()
                    val state = rememberBasicTooltipState(isPersistent = false)
                    val scope = rememberCoroutineScope()
                    var tooltipText by remember {
                        mutableStateOf("")
                    }
                    BasicTooltipBox(
                        modifier = Modifier
                            .fillMaxWidth(),
                        positionProvider = tooltipPosition,
                        tooltip = {
                            Box(
                                modifier = Modifier
                                    .background(
                                        shape = Shapes.medium,
                                        color = MaterialTheme.colorScheme.surface
                                    )
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = tooltipText,
                                )
                            }
                        },
                        state = state
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                modifier = Modifier.pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            scope.launch {
                                                tooltipText = "Общее отработанное время"
                                                state.show(MutatePriority.Default)
                                            }
                                        }
                                    )
                                },
                                text = ConverterLongToTime.getTimeInStringFormat(
                                    time
                                ),
                                style = AppTypography.getType().headlineMedium.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.background
                            )
                            if (totalTime != time) {
                                val differenceTimeInLong = time.minus(totalTime)
                                val totalTime =
                                    ConverterLongToTime.getTimeInStringFormat(
                                        totalTime
                                    )
                                val differenceTime =
                                    ConverterLongToTime.getTimeInStringFormat(
                                        differenceTimeInLong
                                    )
                                Text(
                                    modifier = Modifier.pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                scope.launch {
                                                    tooltipText = "Рабочие + праздничные часы"
                                                    state.show(MutatePriority.Default)
                                                }
                                            }
                                        )
                                    },
                                    text = " ($totalTime + $differenceTime)",
                                    style = AppTypography.getType().titleMedium,
                                    fontWeight = FontWeight.Light,
                                    color = MaterialTheme.colorScheme.background
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
                currentMonthOfYear?.let { month ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        val normaHoursInMonth =
                            month.getPersonalNormaHours()
                        val percent =
                            ((totalTime * 100).toFloat() / (normaHoursInMonth * 3_600_000L).toFloat()) / 100f
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Норма на месяц",
                                maxLines = 1,
                                modifier = Modifier.weight(1f),
                                overflow = TextOverflow.Ellipsis,
                                style = AppTypography.getType().bodyMedium,
                                color = MaterialTheme.colorScheme.background
                            )
                            Text(
                                text = "$normaHoursInMonth ч.",
                                style = AppTypography.getType().bodyLarge,
                                color = MaterialTheme.colorScheme.background
                            )
                        }
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp),
                            trackColor = MaterialTheme.colorScheme.primary.copy(
                                alpha = 0.8f
                            ),
                            color = MaterialTheme.colorScheme.background,
                            strokeCap = StrokeCap.Round,
                            progress = { percent.toFloat() },
                        )
                    }
                    Spacer(modifier = Modifier.height(7.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        val currentTime = Calendar.getInstance()
                        val normaHoursToday =
                            month.getNormaHoursInDate(currentTime.timeInMillis)
                        val percent =
                            ((totalTime * 100).toFloat() / (normaHoursToday * 3_600_000L).toFloat()) / 100f
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Норма на ${
                                    dateAndTimeConverter?.getDate(
                                        currentTime.timeInMillis
                                    ) ?: ""
                                }",
                                maxLines = 1,
                                modifier = Modifier.weight(1f),
                                overflow = TextOverflow.Ellipsis,
                                style = AppTypography.getType().bodyMedium,
                                color = MaterialTheme.colorScheme.background
                            )
                            Text(
                                text = "$normaHoursToday ч.",
                                style = AppTypography.getType().bodyLarge,
                                color = MaterialTheme.colorScheme.background
                            )
                        }
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp),
                            trackColor = MaterialTheme.colorScheme.primary.copy(
                                alpha = 0.8f
                            ),
                            color = MaterialTheme.colorScheme.background,
                            strokeCap = StrokeCap.Round,
                            progress = { percent.toFloat() },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DetailWorkTimeCard(
    brush: Brush,
    totalTime: Long,
    totalTimeWithHoliday: ResultState<Long>?,
    passengerTimeState: ResultState<Long>?,
    singleLocomotiveTimeState: ResultState<Long>?,
    nightTimeState: ResultState<Long>?
) {
    Card(
        modifier = Modifier
            .padding(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 3.dp,
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max)
                .background(brush)
        ) {
            AsyncDataValue(resultState = totalTimeWithHoliday) { totalTimeWithHoliday ->
                totalTimeWithHoliday?.let {
                    val tooltipPosition = TooltipDefaults.rememberPlainTooltipPositionProvider()
                    val state = rememberBasicTooltipState(isPersistent = false)
                    val scope = rememberCoroutineScope()
                    var tooltipText by remember {
                        mutableStateOf("")
                    }
                    BasicTooltipBox(
                        modifier = Modifier
                            .fillMaxWidth(),
                        positionProvider = tooltipPosition,
                        tooltip = {
                            Box(
                                modifier = Modifier
                                    .background(
                                        shape = Shapes.medium,
                                        color = MaterialTheme.colorScheme.surface
                                    )
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = tooltipText,
                                )
                            }
                        },
                        state = state
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 24.dp)
                                .fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    modifier = Modifier.pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                scope.launch {
                                                    tooltipText = "Общее отработанное время"
                                                    state.show(MutatePriority.Default)
                                                }
                                            }
                                        )
                                    },
                                    text = ConverterLongToTime.getTimeInStringFormat(
                                        totalTimeWithHoliday
                                    ),
                                    style = AppTypography.getType().headlineMedium.copy(
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = MaterialTheme.colorScheme.background
                                )
                                if (totalTime != totalTimeWithHoliday) {
                                    val differenceTimeInLong =
                                        totalTimeWithHoliday.minus(totalTime)
                                    val totalTime =
                                        ConverterLongToTime.getTimeInStringFormat(
                                            totalTime
                                        )
                                    val differenceTime =
                                        ConverterLongToTime.getTimeInStringFormat(
                                            differenceTimeInLong
                                        )
                                    Text(
                                        modifier = Modifier.pointerInput(Unit) {
                                            detectTapGestures(
                                                onPress = {
                                                    scope.launch {
                                                        tooltipText =
                                                            "Рабочие + праздничные часы"
                                                        state.show(MutatePriority.Default)
                                                    }
                                                }
                                            )
                                        },
                                        text = " ($totalTime + $differenceTime)",
                                        style = AppTypography.getType().titleMedium,
                                        fontWeight = FontWeight.Light,
                                        color = MaterialTheme.colorScheme.background
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                            AsyncDataValue(nightTimeState) { nightTime ->
                                nightTime?.let {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        val passengerTimeText =
                                            ConverterLongToTime.getTimeInStringFormat(nightTime)
                                        val percent =
                                            ((nightTime * 100).toFloat() / (totalTimeWithHoliday).toFloat()) / 100f
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Ночные",
                                                maxLines = 1,
                                                modifier = Modifier.weight(1f),
                                                overflow = TextOverflow.Ellipsis,
                                                style = AppTypography.getType().bodyMedium,
                                                color = MaterialTheme.colorScheme.background
                                            )
                                            Text(
                                                text = passengerTimeText,
                                                style = AppTypography.getType().bodyLarge,
                                                color = MaterialTheme.colorScheme.background
                                            )
                                        }
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(2.dp),
                                            trackColor = MaterialTheme.colorScheme.primary.copy(
                                                alpha = 0.8f
                                            ),
                                            color = MaterialTheme.colorScheme.background,
                                            strokeCap = StrokeCap.Round,
                                            progress = { percent.toFloat() },
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(7.dp))
                            AsyncDataValue(passengerTimeState) { passengerTime ->
                                passengerTime?.let {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        val passengerTimeText =
                                            ConverterLongToTime.getTimeInStringFormat(
                                                passengerTime
                                            )
                                        val percent =
                                            ((passengerTime * 100).toFloat() / (totalTimeWithHoliday).toFloat()) / 100f
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Пассажиром",
                                                maxLines = 1,
                                                modifier = Modifier.weight(1f),
                                                overflow = TextOverflow.Ellipsis,
                                                style = AppTypography.getType().bodyMedium,
                                                color = MaterialTheme.colorScheme.background
                                            )
                                            Text(
                                                text = passengerTimeText,
                                                style = AppTypography.getType().bodyLarge,
                                                color = MaterialTheme.colorScheme.background
                                            )
                                        }
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(2.dp),
                                            trackColor = MaterialTheme.colorScheme.primary.copy(
                                                alpha = 0.8f
                                            ),
                                            color = MaterialTheme.colorScheme.background,
                                            strokeCap = StrokeCap.Round,
                                            progress = { percent.toFloat() },
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(7.dp))
                            AsyncDataValue(singleLocomotiveTimeState) { singleLocomotiveTime ->
                                singleLocomotiveTime?.let {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        val passengerTimeText =
                                            ConverterLongToTime.getTimeInStringFormat(
                                                singleLocomotiveTime
                                            )
                                        val percent =
                                            ((singleLocomotiveTime * 100).toFloat() / (totalTimeWithHoliday).toFloat()) / 100f
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Резервом",
                                                maxLines = 1,
                                                modifier = Modifier.weight(1f),
                                                overflow = TextOverflow.Ellipsis,
                                                style = AppTypography.getType().bodyMedium,
                                                color = MaterialTheme.colorScheme.background
                                            )
                                            Text(
                                                text = passengerTimeText,
                                                style = AppTypography.getType().bodyLarge,
                                                color = MaterialTheme.colorScheme.background
                                            )
                                        }
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(2.dp),
                                            trackColor = MaterialTheme.colorScheme.primary.copy(
                                                alpha = 0.8f
                                            ),
                                            color = MaterialTheme.colorScheme.background,
                                            strokeCap = StrokeCap.Round,
                                            progress = { percent.toFloat() },
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DetailTrainCard(
    brush: Brush,
    totalTime: Long,
    totalTimeWithHoliday: ResultState<Long>?,
    extendedServicePhaseTime: ResultState<Long>?,
    longDistanceTrainsTime: ResultState<Long>?,
    heavyTrainsTime: ResultState<Long>?,
    onePersonOperationTime: ResultState<Long>?,
) {
    Card(
        modifier = Modifier
            .padding(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 3.dp,
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max)
                .background(brush)
        ) {

            AsyncDataValue(resultState = totalTimeWithHoliday) { totalTimeWithHoliday ->
                totalTimeWithHoliday?.let {
                    val tooltipPosition = TooltipDefaults.rememberPlainTooltipPositionProvider()
                    val state = rememberBasicTooltipState(isPersistent = false)
                    val scope = rememberCoroutineScope()
                    var tooltipText by remember {
                        mutableStateOf("")
                    }
                    BasicTooltipBox(
                        modifier = Modifier
                            .fillMaxWidth(),
                        positionProvider = tooltipPosition,
                        tooltip = {
                            Box(
                                modifier = Modifier
                                    .background(
                                        shape = Shapes.medium,
                                        color = MaterialTheme.colorScheme.surface
                                    )
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = tooltipText,
                                )
                            }
                        },
                        state = state
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 24.dp)
                                .fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    modifier = Modifier.pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                scope.launch {
                                                    tooltipText = "Общее отработанное время"
                                                    state.show(MutatePriority.Default)
                                                }
                                            }
                                        )
                                    },
                                    text = ConverterLongToTime.getTimeInStringFormat(
                                        totalTimeWithHoliday
                                    ),
                                    style = AppTypography.getType().headlineMedium.copy(
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = MaterialTheme.colorScheme.background
                                )
                                if (totalTime != totalTimeWithHoliday) {
                                    val differenceTimeInLong =
                                        totalTimeWithHoliday.minus(totalTime)
                                    val totalTime =
                                        ConverterLongToTime.getTimeInStringFormat(
                                            totalTime
                                        )
                                    val differenceTime =
                                        ConverterLongToTime.getTimeInStringFormat(
                                            differenceTimeInLong
                                        )
                                    Text(
                                        modifier = Modifier.pointerInput(Unit) {
                                            detectTapGestures(
                                                onPress = {
                                                    scope.launch {
                                                        tooltipText =
                                                            "Рабочие + праздничные часы"
                                                        state.show(MutatePriority.Default)
                                                    }
                                                }
                                            )
                                        },
                                        text = " ($totalTime + $differenceTime)",
                                        style = AppTypography.getType().titleMedium,
                                        fontWeight = FontWeight.Light,
                                        color = MaterialTheme.colorScheme.background
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                            AsyncDataValue(extendedServicePhaseTime) { extendedServicePhaseTime ->
                                extendedServicePhaseTime?.let {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        val extendedServicePhaseTimeText =
                                            ConverterLongToTime.getTimeInStringFormat(
                                                extendedServicePhaseTime
                                            )
                                        val percent =
                                            ((extendedServicePhaseTime * 100).toFloat() / (totalTimeWithHoliday).toFloat()) / 100f
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Удл. плечи обслуживания",
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = AppTypography.getType().bodyMedium,
                                                color = MaterialTheme.colorScheme.background
                                            )
                                            Text(
                                                text = extendedServicePhaseTimeText,
                                                maxLines = 1,
                                                overflow = TextOverflow.Visible,
                                                style = AppTypography.getType().bodyLarge,
                                                color = MaterialTheme.colorScheme.background
                                            )
                                        }
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(2.dp),
                                            trackColor = MaterialTheme.colorScheme.primary.copy(
                                                alpha = 0.8f
                                            ),
                                            color = MaterialTheme.colorScheme.background,
                                            strokeCap = StrokeCap.Round,
                                            progress = { percent.toFloat() },
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(7.dp))
                            AsyncDataValue(longDistanceTrainsTime) { longDistanceTrainsTime ->
                                longDistanceTrainsTime?.let {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        val longDistanceTrainsTimeText =
                                            ConverterLongToTime.getTimeInStringFormat(
                                                longDistanceTrainsTime
                                            )
                                        val percent =
                                            ((longDistanceTrainsTime * 100).toFloat() / (totalTimeWithHoliday).toFloat()) / 100f
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Длинносоставные",
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = AppTypography.getType().bodyMedium,
                                                color = MaterialTheme.colorScheme.background
                                            )
                                            Text(
                                                text = longDistanceTrainsTimeText,
                                                style = AppTypography.getType().bodyLarge,
                                                color = MaterialTheme.colorScheme.background
                                            )
                                        }
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(2.dp),
                                            trackColor = MaterialTheme.colorScheme.primary.copy(
                                                alpha = 0.8f
                                            ),
                                            color = MaterialTheme.colorScheme.background,
                                            strokeCap = StrokeCap.Round,
                                            progress = { percent.toFloat() },
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(7.dp))
                            AsyncDataValue(heavyTrainsTime) { heavyTrainsTime ->
                                heavyTrainsTime?.let {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        val heavyTrainsTimeText =
                                            ConverterLongToTime.getTimeInStringFormat(
                                                heavyTrainsTime
                                            )
                                        val percent =
                                            ((heavyTrainsTime * 100).toFloat() / (totalTimeWithHoliday).toFloat()) / 100f
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Тяжелые",
                                                maxLines = 1,
                                                modifier = Modifier.weight(1f),
                                                overflow = TextOverflow.Ellipsis,
                                                style = AppTypography.getType().bodyMedium,
                                                color = MaterialTheme.colorScheme.background
                                            )
                                            Text(
                                                text = heavyTrainsTimeText,
                                                style = AppTypography.getType().bodyLarge,
                                                color = MaterialTheme.colorScheme.background
                                            )
                                        }
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(2.dp),
                                            trackColor = MaterialTheme.colorScheme.primary.copy(
                                                alpha = 0.8f
                                            ),
                                            color = MaterialTheme.colorScheme.background,
                                            strokeCap = StrokeCap.Round,
                                            progress = { percent.toFloat() },
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(7.dp))
                            AsyncDataValue(onePersonOperationTime) { onePersonOperationTime ->
                                onePersonOperationTime?.let {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        val onePersonOperationTimeText =
                                            ConverterLongToTime.getTimeInStringFormat(
                                                onePersonOperationTime
                                            )
                                        val percent =
                                            ((onePersonOperationTime * 100).toFloat() / (totalTimeWithHoliday).toFloat()) / 100f
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Одно лицо",
                                                maxLines = 1,
                                                modifier = Modifier.weight(1f),
                                                overflow = TextOverflow.Ellipsis,
                                                style = AppTypography.getType().bodyMedium,
                                                color = MaterialTheme.colorScheme.background
                                            )
                                            Text(
                                                text = onePersonOperationTimeText,
                                                style = AppTypography.getType().bodyLarge,
                                                color = MaterialTheme.colorScheme.background
                                            )
                                        }
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(2.dp),
                                            trackColor = MaterialTheme.colorScheme.primary.copy(
                                                alpha = 0.8f
                                            ),
                                            color = MaterialTheme.colorScheme.background,
                                            strokeCap = StrokeCap.Round,
                                            progress = { percent.toFloat() },
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(7.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailInfo() {
    val localDensity = LocalDensity.current
    var cardWidthDp by remember {
        mutableStateOf(0.dp)
    }

    Card(
        modifier = Modifier
            .padding(12.dp)
            .onGloballyPositioned { coordinates ->
                cardWidthDp = with(localDensity) { coordinates.size.width.toDp() }
            },
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            contentAlignment = Alignment.Center
        ) {
            PieChart(
                data = mapOf(
                    Pair("Пассажиром", 19),
                    Pair("Резервом", 8),
                    Pair("Остальные", 220)
                ),
                centerText = "247:00",
                radiusOuter = cardWidthDp * 0.18f,
                nightTime = 80
            )
        }
    }
}