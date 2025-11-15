package com.z_company.route.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.flowWithLifecycle
import com.z_company.core.ResultState
import com.z_company.core.ui.component.AsyncData
import com.z_company.core.ui.component.CustomSnackBar
import com.z_company.core.ui.component.DateTimePickerBottomSheet
import com.z_company.core.ui.component.customDatePicker.noRippleEffect
import com.z_company.core.ui.snackbar.ISnackbarManager
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.core.util.ConverterLongToTime
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.route.UtilsForEntities.getPassengerTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.util.minus
import com.z_company.domain.util.toMoneyString
import com.z_company.route.R
import com.z_company.route.component.AppBottomSheet
import com.z_company.route.component.BottomShadow
import com.z_company.route.component.BottomSheetAction
import com.z_company.route.component.OutlinedTextFieldApp
import com.z_company.route.component.SwitchApp
import com.z_company.route.extention.isScrollInInitialState
import com.z_company.route.viewmodel.DialogRestUiState
import com.z_company.route.viewmodel.FormViewModel
import com.z_company.route.viewmodel.RouteFormUiState
import com.z_company.route.viewmodel.SalaryForRouteState
import com.z_company.route.viewmodel.home_view_model.AlertBeforePurchasesEvent
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.util.Calendar

const val LINK_TO_SETTING = "LINK_TO_SETTING"
const val LINK_TO_SALARY_SETTING = "LINK_TO_SALARY_SETTING"

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class,
    ExperimentalFoundationApi::class, ExperimentalLayoutApi::class
)
@Composable
fun FormScreen(
    viewModel: FormViewModel,
    formUiState: RouteFormUiState,
    salaryForRouteState: SalaryForRouteState,
    dialogRestUiState: DialogRestUiState,
    currentRoute: Route?,
    isCopy: Boolean,
    exitScreen: () -> Unit,
    onSettingClick: () -> Unit,
    resetSaveState: () -> Unit,
    onNumberChanged: (String) -> Unit,
    checkedOnePersonOperation: (Boolean) -> Unit,
    onNotesChanged: (String) -> Unit,
    onTimeStartWorkChanged: (Long?) -> Unit,
    onTimeEndWorkChanged: (Long?) -> Unit,
    onRestChanged: (Boolean) -> Unit,
    onChangedLocoClick: (loco: Locomotive) -> Unit,
    onNewLocoClick: (basicId: String) -> Unit,
    onDeleteLoco: (loco: Locomotive) -> Unit,
    onChangeTrainClick: (train: Train) -> Unit,
    onNewTrainClick: (basicId: String) -> Unit,
    onDeleteTrain: (train: Train) -> Unit,
    onChangePassengerClick: (passenger: Passenger) -> Unit,
    onNewPassengerClick: (basicId: String) -> Unit,
    onDeletePassenger: (passenger: Passenger) -> Unit,
    nightTime: Long?,
    onSalarySettingClick: () -> Unit,
    setFavoriteState: () -> Unit,
    dateAndTimeConverter: DateAndTimeConverter?
) {
    val scope = rememberCoroutineScope()

    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycle = lifecycleOwner.lifecycle

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    var isShowNeedSubscribeDialog by remember {
        mutableStateOf(false)
    }

    var isShowAlertSubscribeDialog by remember {
        mutableStateOf(false)
    }

    if (isShowAlertSubscribeDialog) {
        AppBottomSheet(
            onDismissRequest = { isShowAlertSubscribeDialog = false },
            sheetState = sheetState,
            headerContent = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "${stringResource(id = R.string.test_period)}\n",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${stringResource(id = R.string.available_for_free_route)}\n",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            actions = listOf(
                BottomSheetAction(text = stringResource(id = R.string.billing_common_ok)) {
                    viewModel::saveRoute
                },
                BottomSheetAction(text = "Оформить подписку за 44 руб/мес") {
                    viewModel::checkPurchasesAvailability
                }
            ),
        )
    }

    if (isShowNeedSubscribeDialog) {
        AppBottomSheet(
            onDismissRequest = { isShowNeedSubscribeDialog = false },
            sheetState = sheetState,
            headerContent = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "${stringResource(id = R.string.dialog_title_need_purchases)}\n",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(id = R.string.available_for_free_route),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            actions = listOf(
                BottomSheetAction(text = "Оформить подписку за 44 руб/мес") {
                    viewModel::checkPurchasesAvailability
                },
                BottomSheetAction(text = "Восстановить покупки") {
                    viewModel::restorePurchases
                }
            )
        )
    }

    LaunchedEffect(Unit) {
        scope.launch {
            viewModel.alertBeforePurchasesEvent.flowWithLifecycle(lifecycle).collect { event ->
                when (event) {
                    is AlertBeforePurchasesEvent.ShowDialogNeedSubscribe -> {
                        isShowNeedSubscribeDialog = true
                    }

                    is AlertBeforePurchasesEvent.ShowDialogAlertSubscribe -> {
                        isShowAlertSubscribeDialog = true
                    }
                }
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarManager: ISnackbarManager = koinInject()

    LaunchedEffect(Unit) {
        snackbarManager.events
            .flowWithLifecycle(lifecycle)
            .collectLatest { event ->
                val result = snackbarHostState.showSnackbar(
                    message = event.message,
                    actionLabel = event.actionLabel,
                    duration = event.duration
                )
                if (result == SnackbarResult.ActionPerformed) {
                    event.onAction?.let { onAction ->
                        // запускаем suspend-колбек в scope
                        launch {
                            try {
                                onAction()
                            } catch (_: Exception) { /* optional logging */
                            }
                        }
                    }
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        TextButton(
                            onClick = viewModel::onSaveClick,
                            enabled = formUiState.errorMessage == null,
                            colors = ButtonDefaults.buttonColors(
                                disabledContainerColor = Color.Transparent,
                                disabledContentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                contentColor = MaterialTheme.colorScheme.tertiary,
                                containerColor = Color.Transparent
                            )
                        ) {
                            Text(
                                text = "Готово",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }

                        AnimatedContent(
                            targetState = currentRoute?.basicData?.isFavorite == true,
                            label = ""
                        ) {
                            IconButton(
                                onClick = setFavoriteState
                            ) {
                                Icon(
                                    tint = if (it) MaterialTheme.colorScheme.error else LocalContentColor.current,
                                    imageVector = if (it) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { snackBarData ->
                CustomSnackBar(snackBarData = snackBarData)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) {
        if (formUiState.saveRouteState is ResultState.Error) {
            LaunchedEffect(Unit) {
                scope.launch {
                    snackbarManager.show("Ошибка: ${formUiState.saveRouteState.entity.message}")
                }
                resetSaveState()
            }
        }

        if (formUiState.saveRouteState is ResultState.Success) {
            LaunchedEffect(formUiState.saveRouteState) {
                exitScreen()
            }
        }

        if (formUiState.exitFromScreen) {
            LaunchedEffect(Unit) {
                exitScreen()
            }
        }

        var showBottomSheetRemoveTimeStartWork by remember {
            mutableStateOf(false)
        }

        if (showBottomSheetRemoveTimeStartWork) {
            AppBottomSheet(
                onDismissRequest = { showBottomSheetRemoveTimeStartWork = false },
                sheetState = sheetState,
                headerContent = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Время явки",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = listOf(
                    BottomSheetAction(text = "Удалить значение") {
                        onTimeStartWorkChanged(null)
                    }
                )
            )
        }


        var showBottomSheetRemoveTimeEndWork by remember {
            mutableStateOf(false)
        }

        if (showBottomSheetRemoveTimeEndWork) {
            AppBottomSheet(
                onDismissRequest = { showBottomSheetRemoveTimeEndWork = false },
                sheetState = sheetState,
                headerContent = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Время сдачи",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = listOf(
                    BottomSheetAction(text = "Удалить значение") {
                        onTimeEndWorkChanged(null)
                    }
                )
            )
        }

        Box(Modifier.padding(it)) {
            AsyncData(resultState = formUiState.routeDetailState) {
                if (formUiState.routeDetailState is ResultState.Success) {
                    currentRoute?.let { route ->
                        val scrollState = rememberLazyListState()

                        var showStartDatePickerCopyRoute by remember {
                            mutableStateOf(false)
                        }

                        var showStartDatePicker by remember {
                            mutableStateOf(false)
                        }

                        var showEndDatePicker by remember {
                            mutableStateOf(false)
                        }

                        if (showStartDatePicker) {
                            DateTimePickerBottomSheet(
                                title = "Явка",
                                onDateTimeSelected = { timestamp ->
                                    onTimeStartWorkChanged(timestamp)
                                },
                                onDismiss = { showStartDatePicker = false },
                                startDateTime = route.basicData.timeStartWork
                                    ?: Calendar.getInstance().timeInMillis
                            )
                        }

                        if (showEndDatePicker) {
                            DateTimePickerBottomSheet(
                                title = "Сдача",
                                onDateTimeSelected = { timestamp ->
                                    onTimeEndWorkChanged(timestamp)
                                },
                                onDismiss = { showEndDatePicker = false },
                                startDateTime = route.basicData.timeEndWork
                                    ?: route.basicData.timeStartWork
                                    ?: Calendar.getInstance().timeInMillis
                            )
                        }

                        LaunchedEffect(isCopy) {
                            if (isCopy) {
                                showStartDatePickerCopyRoute = true
                            }
                        }

                        // Диалог при копировании маршрута
                        if (showStartDatePickerCopyRoute) {
                            DateTimePickerBottomSheet(
                                title = "Явка",
                                onDateTimeSelected = { timestamp ->
                                    showStartDatePickerCopyRoute = false
//                                    val oldValueStartCalendar = Calendar.getInstance().also {
//                                        it.timeInMillis = route.basicData.timeStartWork ?: Calendar.getInstance().timeInMillis
//                                    }
//
//                                    startCalendar.timeInMillis =
//                                        startDateCopyRoutePickerState.selectedDateMillis!!
//                                    startCalendar.set(
//                                        Calendar.HOUR_OF_DAY,
//                                        oldValueStartCalendar.get(Calendar.HOUR_OF_DAY)
//                                    )
//                                    startCalendar.set(
//                                        Calendar.MINUTE,
//                                        oldValueStartCalendar.get(Calendar.MINUTE)
//                                    )
//                                    startCalendar.set(Calendar.SECOND, 0)
//                                    startCalendar.set(Calendar.MILLISECOND, 0)

                                    onTimeStartWorkChanged(timestamp)
                                    val workTimeInMillis = route.getWorkTime()
                                    workTimeInMillis?.let { workTime ->
//                                        endCalendar.timeInMillis = timestamp + workTime
                                        onTimeEndWorkChanged(timestamp + workTime)
                                    }
                                },
                                onDismiss = { showStartDatePickerCopyRoute = false },
                                startDateTime = route.basicData.timeStartWork
                                    ?: Calendar.getInstance().timeInMillis
                            )
                        }

                        // Тень при скроле
                        AnimatedVisibility(
                            modifier = Modifier.zIndex(1f),
                            visible = !scrollState.isScrollInInitialState(),
                            enter = fadeIn(animationSpec = tween(durationMillis = 300)),
                            exit = fadeOut(animationSpec = tween(durationMillis = 300))
                        ) {
                            BottomShadow()
                        }

                        var isVisibleDetailMoney by remember {
                            mutableStateOf(false)
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                            state = scrollState,
                        ) {
                            val startTimeInLong = route.basicData.timeStartWork
                            val endTimeInLong = route.basicData.timeEndWork
                            val workTimeInLong = endTimeInLong - startTimeInLong
                            val workTimeInFormatted =
                                ConverterLongToTime.getTimeInStringFormat(workTimeInLong)

                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateItemPlacement(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    val widthScreen =
                                        LocalConfiguration.current.screenWidthDp.toFloat()
                                    val errorGradient = Brush.radialGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                                            MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                        ),
                                        center = Offset(Float.POSITIVE_INFINITY, 0f),
                                        radius = widthScreen * 2
                                    )

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .animateItemPlacement(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        formUiState.errorMessage?.let { message ->
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth(),
                                                shape = MaterialTheme.shapes.medium,
                                                elevation = CardDefaults.elevatedCardElevation(
                                                    defaultElevation = 3.dp,
                                                    pressedElevation = 0.dp
                                                )
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .background(
                                                            brush = errorGradient,
                                                            shape = MaterialTheme.shapes.medium
                                                        )
                                                        .padding(
                                                            vertical = 12.dp,
                                                            horizontal = 16.dp
                                                        ),
                                                    verticalArrangement = Arrangement.spacedBy(
                                                        12.dp
                                                    )
                                                ) {
                                                    Text(
                                                        text = message,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.secondary
                                                    )
                                                }
                                            }
                                        }
                                        if (workTimeInLong != null && formUiState.errorMessage == null) {
                                            Text(
                                                modifier = Modifier.fillMaxWidth(),
                                                textAlign = TextAlign.Start,
                                                text = workTimeInFormatted,
                                                color = MaterialTheme.colorScheme.primary,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            FlowRow(
                                                modifier = Modifier
                                                    .fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                val holidayTime by viewModel.holidayTime.collectAsState()

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .padding(end = 4.dp),
                                                        painter = painterResource(id = R.drawable.dark_mode_24px),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        text = ConverterLongToTime.getTimeInStringFormat(
                                                            nightTime
                                                        ),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .padding(end = 4.dp),
                                                        painter = painterResource(id = R.drawable.passenger_24px),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        text = ConverterLongToTime.getTimeInStringFormat(
                                                            route.getPassengerTime() ?: 0L
                                                        ),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Image(
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .padding(end = 8.dp),
                                                        painter = painterResource(id = R.drawable.icon_holiday),
                                                        contentDescription = null
                                                    )
                                                    Text(
                                                        text = ConverterLongToTime.getTimeInStringFormat(
                                                            holidayTime ?: 0L
                                                        ),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .noRippleEffect {
                                                isVisibleDetailMoney = !isVisibleDetailMoney
                                            },
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    )
                                    {
                                        Text(
                                            text = "Заработано",
                                            style = MaterialTheme.typography.bodyMedium
                                        )

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            if (salaryForRouteState.isCalculated) {
                                                Text(
                                                    text = salaryForRouteState.totalPayment.toMoneyString(),
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            } else {
                                                Text(
                                                    text = null.toMoneyString(),
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }

                                            AnimatedContent(
                                                targetState = isVisibleDetailMoney,
                                                label = ""
                                            ) {
                                                val icon = if (it) {
                                                    Icons.Default.KeyboardArrowUp
                                                } else {
                                                    Icons.Default.KeyboardArrowDown
                                                }
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }

                                    AnimatedVisibility(
                                        visible = isVisibleDetailMoney,
                                        enter = slideInVertically(
                                            animationSpec = tween(
                                                durationMillis = 150
                                            )
                                        ) + fadeIn(
                                            animationSpec = tween(durationMillis = 100)
                                        ),
                                        exit = fadeOut(animationSpec = tween(durationMillis = 100))
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .background(
                                                    color = MaterialTheme.colorScheme.surfaceDim,
                                                    shape = Shapes.medium
                                                )
                                                .border(
                                                    width = 0.5.dp,
                                                    color = MaterialTheme.colorScheme.tertiary,
                                                    shape = Shapes.medium
                                                )
                                                .padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (salaryForRouteState.isCalculated) {
                                                if (salaryForRouteState.paymentAtTariffRate == 0.0) {
                                                    val link = buildAnnotatedString {
                                                        val text =
                                                            "Установите значение тарифной ставки в настройках."

                                                        val endIndex = text.length - 1
                                                        val startIndex =
                                                            startIndexLastWord(text)

                                                        append(text)
                                                        addStyle(
                                                            style = SpanStyle(
                                                                color = MaterialTheme.colorScheme.tertiary,
                                                                textDecoration = TextDecoration.Underline
                                                            ),
                                                            start = startIndex,
                                                            end = endIndex
                                                        )

                                                        addStringAnnotation(
                                                            tag = LINK_TO_SALARY_SETTING,
                                                            annotation = LINK_TO_SALARY_SETTING,
                                                            start = startIndex,
                                                            end = endIndex
                                                        )
                                                    }

                                                    Box(modifier = Modifier.fillMaxWidth()) {
                                                        ClickableText(
                                                            text = link,
                                                            style = AppTypography.getType().bodyMedium.copy(
                                                                fontStyle = FontStyle.Italic,
                                                                fontWeight = FontWeight.Light,
                                                                color = MaterialTheme.colorScheme.primary
                                                            ),
                                                        ) {
                                                            link.getStringAnnotations(
                                                                LINK_TO_SALARY_SETTING,
                                                                it,
                                                                it
                                                            )
                                                                .firstOrNull()?.let {
                                                                    onSalarySettingClick()
                                                                }
                                                        }
                                                    }
                                                } else {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = "Почасовая оплата",
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                        Text(
                                                            text = salaryForRouteState.paymentAtTariffRate.toMoneyString(),
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                    }

                                                }

                                                if (salaryForRouteState.paymentHolidayMoney != 0.0) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = "Праздничные",
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                        Text(
                                                            text = salaryForRouteState.paymentHolidayMoney.toMoneyString(),
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                    }
                                                }

                                                if (salaryForRouteState.zonalSurchargeMoney != 0.0) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = "Зональная надбавка",
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                        Text(
                                                            text = salaryForRouteState.zonalSurchargeMoney.toMoneyString(),
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                    }
                                                }

                                                if (salaryForRouteState.paymentAtNightTime != 0.0) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = "Ночные",
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                        Text(
                                                            text = salaryForRouteState.paymentAtNightTime.toMoneyString(),
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                    }
                                                }

                                                if (salaryForRouteState.paymentAtPassengerTime != 0.0) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = "Пассажиром",
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                        Text(
                                                            text = salaryForRouteState.paymentAtPassengerTime.toMoneyString(),
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                    }
                                                }

                                                if (salaryForRouteState.paymentAtOnePerson != 0.0) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = "Одно лицо",
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                        Text(
                                                            text = salaryForRouteState.paymentAtOnePerson.toMoneyString(),
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                    }
                                                }

                                                if (salaryForRouteState.surchargesAtTrain != 0.0) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = "Доплаты за поезд",
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                        Text(
                                                            text = salaryForRouteState.surchargesAtTrain.toMoneyString(),
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                    }
                                                }

                                                if (salaryForRouteState.otherSurcharge != 0.0) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = "Прочие доплаты",
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                        Text(
                                                            text = salaryForRouteState.otherSurcharge.toMoneyString(),
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                    }
                                                }
                                            } else {
                                                Box(modifier = Modifier.fillMaxWidth()) {
                                                    Text(
                                                        text = "Укажите начало и окончание рабочего времени для расчета заработной платы за поездку",
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .animateItemPlacement(),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            val text = if (route.basicData.isOnePersonOperation) {
                                                "В одно лицо"
                                            } else {
                                                "В два лица"
                                            }

                                            Text(
                                                text = text,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.primary
                                            )

                                            SwitchApp(
                                                checked = route.basicData.isOnePersonOperation,
                                                onCheckedChange = checkedOnePersonOperation,
                                                positiveContent = {
                                                    Box(
                                                        modifier = Modifier.padding(horizontal = 16.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(id = R.drawable.person_rounded_24px),
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary.copy(
                                                                alpha = 0.8f
                                                            )
                                                        )
                                                    }
                                                },
                                                negativeContent = {
                                                    Box(
                                                        modifier = Modifier.padding(horizontal = 16.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(id = R.drawable.group_24px),
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary.copy(
                                                                alpha = 0.8f
                                                            )
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            val text = if (route.basicData.restPointOfTurnover) {
                                                "Отдых в ПО"
                                            } else {
                                                "Домашний"
                                            }
                                            Text(
                                                text = text,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            SwitchApp(
                                                checked = route.basicData.restPointOfTurnover,
                                                onCheckedChange = onRestChanged,
                                                positiveContent = {
                                                    Box(
                                                        modifier = Modifier.padding(horizontal = 16.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(id = R.drawable.hotel_24px),
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary.copy(
                                                                alpha = 0.8f
                                                            )
                                                        )
                                                    }
                                                },
                                                negativeContent = {
                                                    Box(
                                                        modifier = Modifier.padding(horizontal = 16.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(id = R.drawable.home_24px),
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary.copy(
                                                                alpha = 0.8f
                                                            )
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                        var isVisibleDetailRest by remember {
                                            mutableStateOf(false)
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            TextButton(
                                                onClick = {
                                                    isVisibleDetailRest = !isVisibleDetailRest
                                                }
                                            ) {
                                                Text(
                                                    text = "Рассчитать отдых",
                                                    color = MaterialTheme.colorScheme.tertiary,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }

                                        AnimatedVisibility(
                                            visible = isVisibleDetailRest,
                                            enter = slideInVertically(
                                                animationSpec = tween(
                                                    durationMillis = 150
                                                )
                                            ) + fadeIn(
                                                animationSpec = tween(durationMillis = 100)
                                            ),
                                            exit = fadeOut(animationSpec = tween(durationMillis = 100))
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .background(
                                                        color = MaterialTheme.colorScheme.surfaceDim,
                                                        shape = Shapes.medium
                                                    )
                                                    .border(
                                                        width = 0.5.dp,
                                                        color = MaterialTheme.colorScheme.tertiary,
                                                        shape = Shapes.medium
                                                    )
                                                    .padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                if (route.basicData.restPointOfTurnover) {
                                                    InfoRestPointOfTurnoverTime(
                                                        minTimeDuration = dialogRestUiState.minTimeDuration,
                                                        fullTimeDuration = dialogRestUiState.fullTimeDuration,
                                                        timeEndMinTimeRest = dialogRestUiState.timeEndMinTimeRestPointOfTurnover,
                                                        timeEndFullTimeRest = dialogRestUiState.timeEndFullTimeRestPointOfTurnover,
                                                        onSettingClick = onSettingClick,
                                                        dateAndTimeConverter = dateAndTimeConverter
                                                    )
                                                } else {
                                                    InfoRestOfHomeOfTime(
                                                        restDuration = dialogRestUiState.homeRestDuration,
                                                        timeEndHomeRest = dialogRestUiState.timeEndHomeRest,
                                                        onSettingClick = onSettingClick,
                                                        dateAndTimeConverter = dateAndTimeConverter
                                                    )
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
                                        .animateItemPlacement(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    val prefixTextColor =
                                        if (route.basicData.number.isNullOrBlank()) MaterialTheme.colorScheme.primary.copy(
                                            alpha = 0.6f
                                        ) else MaterialTheme.colorScheme.primary

                                    OutlinedTextFieldApp(
                                        modifier = Modifier
                                            .fillMaxWidth(),
                                        value = route.basicData.number ?: "",
                                        onValueChange = onNumberChanged,
                                        placeholder = {
                                            Text(
                                                text = "маршрута",
                                                color = MaterialTheme.colorScheme.primary.copy(
                                                    alpha = 0.6f
                                                ),
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        },
                                        prefix = {
                                            Text(
                                                text = "№ ",
                                                color = prefixTextColor,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyLarge,
                                        shape = Shapes.medium,
                                        keyboardOptions = KeyboardOptions.Default.copy(
                                            keyboardType = KeyboardType.Number
                                        )
                                    )

                                    val animatedBackgroundColorsStartWork by animateColorAsState(
                                        targetValue = if (route.basicData.timeStartWork == null) MaterialTheme.colorScheme.surface
                                        else MaterialTheme.colorScheme.secondary,
                                        animationSpec = tween(
                                            durationMillis = 200,
                                            easing = FastOutSlowInEasing
                                        )
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .shadow(elevation = 2.dp, shape = Shapes.medium)
                                            .background(
                                                color = animatedBackgroundColorsStartWork,
                                                shape = Shapes.medium
                                            )
                                            .combinedClickable(
                                                onClick = {
                                                    showStartDatePicker = true
                                                },
                                                onLongClick = {
                                                    startTimeInLong?.let {
                                                        showBottomSheetRemoveTimeStartWork = true
                                                    }
                                                }
                                            )
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Явка",
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            var textColor =
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                            val dateAndTimeStartText =
                                                startTimeInLong?.let {
                                                    textColor =
                                                        MaterialTheme.colorScheme.primary
                                                    dateAndTimeConverter?.getDateAndTime(
                                                        startTimeInLong
                                                    )
                                                } ?: "укажите время"

                                            Text(
                                                text = dateAndTimeStartText,
                                                color = textColor,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        }
                                    }

                                    val animatedBackgroundColorsEndWork by animateColorAsState(
                                        targetValue = if (route.basicData.timeEndWork == null) MaterialTheme.colorScheme.surface
                                        else MaterialTheme.colorScheme.secondary,
                                        animationSpec = tween(
                                            durationMillis = 200,
                                            easing = FastOutSlowInEasing
                                        )
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .shadow(elevation = 2.dp, shape = Shapes.medium)
                                            .background(
                                                color = animatedBackgroundColorsEndWork,
                                                shape = Shapes.medium
                                            )
                                            .combinedClickable(
                                                onClick = {
                                                    showEndDatePicker = true
                                                },
                                                onLongClick = {
                                                    endTimeInLong?.let {
                                                        showBottomSheetRemoveTimeEndWork = true
                                                    }
                                                }
                                            )
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Сдача",
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            var textColor =
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)

                                            val dateAndTimeEndText = endTimeInLong?.let {
                                                textColor = MaterialTheme.colorScheme.primary
                                                dateAndTimeConverter?.getDateAndTime(
                                                    endTimeInLong
                                                )
                                            } ?: "укажите время"
                                            Text(
                                                text = dateAndTimeEndText,
                                                color = textColor,
                                                style = MaterialTheme.typography.bodyLarge,
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
                                        .padding(bottom = 32.dp, top = 16.dp),
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val basicId = route.basicData.id
                                    ItemAddingScreen(
                                        title = stringResource(id = R.string.locomotive),
                                        contentList = route.locomotives,
                                        onChangeElementClick = onChangedLocoClick,
                                        onNewElementClick = onNewLocoClick,
                                        basicId = basicId,
                                        onDeleteClick = onDeleteLoco
                                    ) { index, locomotive ->
                                        LocomotiveSubItem(locomotive, index)
                                    }
                                    ItemAddingScreen(
                                        title = stringResource(id = R.string.train),
                                        contentList = route.trains,
                                        onChangeElementClick = onChangeTrainClick,
                                        onNewElementClick = onNewTrainClick,
                                        basicId = basicId,
                                        onDeleteClick = onDeleteTrain
                                    ) { index, train ->
                                        TrainSubItem(index, train)
                                    }
                                    ItemAddingScreen(
                                        title = stringResource(id = R.string.passenger),
                                        contentList = route.passengers,
                                        onChangeElementClick = onChangePassengerClick,
                                        onNewElementClick = onNewPassengerClick,
                                        basicId = basicId,
                                        onDeleteClick = onDeletePassenger
                                    ) { index, passenger ->
                                        PassengerSubItem(index, passenger)
                                    }
                                    ItemNotes(
                                        modifier = Modifier.padding(top = 8.dp),
                                        notes = route.basicData.notes,
                                        onNotesChanged = onNotesChanged,
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

@Composable
fun <T> ItemAddingScreen(
    title: String,
    contentList: List<T>?,
    onChangeElementClick: (element: T) -> Unit,
    onNewElementClick: (basicId: String) -> Unit,
    basicId: String,
    onDeleteClick: (element: T) -> Unit,
    subItem: @Composable RowScope.(index: Int, element: T) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall
            )
            TextButton(onClick = { onNewElementClick(basicId) }) {
                Text(
                    text = "Добавить",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
        contentList?.let { elements ->
            Column(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                elements.forEachIndexed { index, element ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChangeElementClick(element) }
                            .shadow(elevation = 2.dp, shape = Shapes.medium)
                            .background(
                                color = MaterialTheme.colorScheme.secondary,
                                shape = Shapes.medium
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(0.95f)
                                .padding(end = 8.dp)
                        ) {
                            subItem(index, element)
                        }
                        Icon(
                            modifier = Modifier
                                .weight(0.05f)
                                .clickable { onDeleteClick(element) },
                            imageVector = Icons.Outlined.Clear, contentDescription = null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LocomotiveSubItem(locomotive: Locomotive, index: Int) {
    val series = locomotive.series ?: locomotive.type.text
    val number = locomotive.number ?: ""
    val numberText = if (locomotive.number != null) {
        "№$number"
    } else {
        ""
    }
    val type = locomotive.type.text
    if (locomotive.series.isNullOrBlank() && locomotive.number.isNullOrBlank()) {
        Text(
            text = "$type № ${index + 1}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
    } else {
        Text(
            text = "$series $numberText",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun TrainSubItem(index: Int, train: Train) {
    if (train.number.isNullOrBlank()) {
        Text(
            text = "Поезд № ${index + 1}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
    } else {
        val stationStart = if (train.stations.isNotEmpty()) {
            train.stations.first().stationName ?: ""
        } else {
            ""
        }

        val stationEnd = if (train.stations.isNotEmpty() && train.stations.size > 1) {
            " - ${train.stations.last().stationName ?: ""}"
        } else {
            ""
        }

        Text(
            text = "№ ${train.number} $stationStart$stationEnd",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun PassengerSubItem(index: Int, passenger: Passenger) {
    if (passenger.trainNumber.isNullOrBlank()) {
        Text(
            text = "Пассажиром № ${index + 1}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
    } else {
        Text(
            text = "№ ${passenger.trainNumber}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun ItemNotes(
    modifier: Modifier = Modifier,
    notes: String?,
    onNotesChanged: (String) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = Shapes.medium),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextFieldApp(
            modifier = Modifier
                .heightIn(max = 105.dp)
                .fillMaxWidth(),
            value = notes ?: "",
            onValueChange = {
                onNotesChanged(it)
            },
            placeholder = {
                Text(
                    text = "Примечания",
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = Shapes.medium
        )
    }
}

@Composable
fun InfoRestOfHomeOfTime(
    restDuration: Long?,
    timeEndHomeRest: Long?,
    onSettingClick: () -> Unit,
    dateAndTimeConverter: DateAndTimeConverter?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f),
            horizontalAlignment = Alignment.Start
        ) {
            if (restDuration == null || timeEndHomeRest == null) {
                Text(
                    text = "Невозможно рассчитать время отдыха.\nПроверьте начало и окончание работы во всей цепочке маршрутов.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                val timeEndHomeRestText =
                    dateAndTimeConverter?.getDateAndTime(timeEndHomeRest) ?: ""
                val restDuration = ConverterLongToTime.formatDurationFromMillis(restDuration)

                Text(
                    text = "Продлится $restDuration",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "До $timeEndHomeRestText",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "\nформула расчета\n(время рабочее * 2,6) - время отдыха в ПО",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }

        }

        Icon(
            modifier = Modifier
                .clickable {
                    onSettingClick()
                },
            painter = painterResource(R.drawable.settings_24px),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun InfoRestPointOfTurnoverTime(
    minTimeDuration: Long?,
    fullTimeDuration: Long?,
    timeEndMinTimeRest: Long?,
    timeEndFullTimeRest: Long?,
    onSettingClick: () -> Unit,
    dateAndTimeConverter: DateAndTimeConverter?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start
        ) {
            if (minTimeDuration == null || fullTimeDuration == null) {
                Text(
                    text = "Невозможно рассчитать время отдыха.\nПроверьте начало и окончание работы.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                val minTimeDurationText =
                    ConverterLongToTime.formatDurationFromMillis(minTimeDuration)
                val timeEndMinTimeRestText =
                    dateAndTimeConverter?.getDateMiniAndTime(timeEndMinTimeRest) ?: ""

                Text(
                    text = "Короткий $minTimeDurationText",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "до $timeEndMinTimeRestText",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                val fullTimeDurationText =
                    ConverterLongToTime.formatDurationFromMillis(fullTimeDuration)
                val timeEndFullTimeRestText =
                    dateAndTimeConverter?.getDateMiniAndTime(timeEndFullTimeRest) ?: ""
                Text(
                    text = "Полный $fullTimeDurationText",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "до $timeEndFullTimeRestText",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Icon(
            modifier = Modifier
                .clickable {
                    onSettingClick()
                },
            painter = painterResource(R.drawable.settings_24px),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
    }
}