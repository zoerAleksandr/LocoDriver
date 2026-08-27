package com.z_company.route.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BasicTooltipBox
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberBasicTooltipState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.flowWithLifecycle
import coil.compose.AsyncImagePainter.State.Empty.painter
import com.z_company.core.ResultState
import com.z_company.core.ui.component.CustomSnackBar
import com.z_company.route.component.AppAlertDialog
import com.z_company.route.component.AppDateTimePicker
import com.z_company.core.ui.component.customDatePicker.noRippleEffect
import com.z_company.core.ui.snackbar.ISnackbarManager
import com.z_company.core.ui.theme.MonoFont
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.core.util.ConverterLongToTime
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.entities.route.OtherWork
import com.z_company.domain.entities.route.RoutePartner
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.route.UtilsForEntities.getBreakDuration
import com.z_company.domain.entities.route.UtilsForEntities.getPassengerTime
import com.z_company.domain.entities.route.UtilsForEntities.getPassengerTimeWithinWork
import com.z_company.domain.entities.route.UtilsForEntities.getPassengerTimeOutsideWork
import com.z_company.domain.entities.route.UtilsForEntities.getPureWorkTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.util.minus
import com.z_company.domain.util.moreThan
import com.z_company.domain.util.str
import com.z_company.domain.util.currencySymbol
import com.z_company.domain.util.toMoneyString
import com.z_company.route.R
import com.z_company.route.component.AppBottomSheet
import com.z_company.route.component.BottomShadow
import com.z_company.route.component.Passenger12hBottomSheet
import com.z_company.route.component.BottomSheetAction
import com.z_company.route.component.OutlinedTextFieldApp
import com.z_company.route.extention.isScrollInInitialState
import com.z_company.route.viewmodel.DialogRestUiState
import com.z_company.route.viewmodel.FormViewModel
import com.z_company.route.viewmodel.NightWarnRow
import com.z_company.route.viewmodel.NightWarnState
import com.z_company.route.viewmodel.RouteFormUiState
import com.z_company.route.viewmodel.SalaryForRouteState
import com.z_company.route.viewmodel.home_view_model.AlertBeforePurchasesEvent
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import com.z_company.core.util.TimeManager

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
    isNewRoute: Boolean,
    exitScreen: () -> Unit,
    onSettingClick: () -> Unit,
    onFormSettingsClick: () -> Unit,
    onRestSettingClick: () -> Unit,
    resetSaveState: () -> Unit,
    onNumberChanged: (String) -> Unit,
    checkedOnePersonOperation: (Boolean) -> Unit,
    onNotesChanged: (String) -> Unit,
    onTimeStartWorkChanged: (Long?) -> Unit,
    onTimeEndWorkChanged: (Long?) -> Unit,
    onTimeStartBreakChanged: (Long?) -> Unit,
    onTimeEndBreakChanged: (Long?) -> Unit,
    isShowBreak: Boolean,
    isShowOnePersonSwitch: Boolean,
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
    isShowLocomotive: Boolean,
    isShowTrain: Boolean,
    isShowPassenger: Boolean,
    isShowOtherWork: Boolean,
    onChangeOtherWorkClick: (otherWork: OtherWork) -> Unit,
    onNewOtherWorkClick: (basicId: String) -> Unit,
    onDeleteOtherWork: (otherWork: OtherWork) -> Unit,
    isShowPartner: Boolean,
    onAddPartners: () -> Unit,
    onOpenPartnerCard: (partner: RoutePartner) -> Unit,
    onDeletePartner: (partner: RoutePartner) -> Unit,
    nightTime: Long?,
    onSalarySettingClick: () -> Unit,
    setFavoriteState: () -> Unit,
    dateAndTimeConverter: DateAndTimeConverter?,
    showPurchasesScreen: () -> Unit,
    onCopyClick: () -> Unit
) {
    val displayTz = dateAndTimeConverter?.timeZoneText ?: "GMT+3"
    // Валюта — по стране из настроек (₽ / ₸ / Br). Касается только денег пользователя.
    val currency = currencySymbol(viewModel.userSetting.collectAsState().value?.country)
    val scope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycle = lifecycleOwner.lifecycle

    // Нижняя контекстная панель прячется при скролле вниз и появляется при скролле вверх.
    var bottomBarVisible by remember { mutableStateOf(true) }
    val bottomBarNestedScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -3f) bottomBarVisible = false
                else if (available.y > 3f) bottomBarVisible = true
                return Offset.Zero
            }
        }
    }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    var isShowNeedSubscribeDialog by remember {
        mutableStateOf(false)
    }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showCopyConfirmDialog by remember { mutableStateOf(false) }

    // Шторка подтверждения создания копии маршрута
    if (showCopyConfirmDialog) {
        AppBottomSheet(
            onDismissRequest = { showCopyConfirmDialog = false },
            sheetState = sheetState,
            headerContent = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Создать копию маршрута?",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "\nОткроется редактирование копии — исходный маршрут не изменится.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            },
            actions = listOf(
                BottomSheetAction(text = "Создать копию") { onCopyClick() }
            )
        )
    }

    // Шторки «Расчёт» и «Отдых» (открываются из плиток)
    var showCalcSheet by remember { mutableStateOf(false) }
    var showRestSheet by remember { mutableStateOf(false) }

    // Диалог подтверждения удаления единицы маршрута (локомотив/поезд/пассажир)
    var pendingDeleteTitle by remember { mutableStateOf("") }
    var pendingDeleteMessage by remember { mutableStateOf("") }
    var pendingDeleteAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Подтверждение удаления единицы маршрута — шторка (как удаление маршрута).
    pendingDeleteAction?.let { action ->
        AppBottomSheet(
            onDismissRequest = { pendingDeleteAction = null },
            sheetState = sheetState,
            headerContent = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = pendingDeleteTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "\n$pendingDeleteMessage",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            },
            actions = listOf(
                BottomSheetAction(text = "Да, удалить") { action() }
            )
        )
    }

    // Шторка подтверждения удаления (такая же, как в HomeScreen)
    if (showDeleteConfirmDialog) {
        val deleteTitle = buildString {
            append("Удалить маршрут?")
            currentRoute?.basicData?.timeStartWork?.let { ms ->
                val dateText = dateAndTimeConverter?.getDateMiniAndTime(value = ms)
                if (!dateText.isNullOrBlank()) append("\nот $dateText")
            }
        }
        AppBottomSheet(
            onDismissRequest = { showDeleteConfirmDialog = false },
            sheetState = sheetState,
            title = deleteTitle,
            actions = listOf(
                BottomSheetAction(text = "Да, удалить") {
                    viewModel.onDeleteRoute()
                }
            )
        )
    }

    // Шторка «Расчёт за смену» — разбивка заработка
    if (showCalcSheet) {
        val holidayTimeValue by viewModel.holidayTime.collectAsState()
        CalcBottomSheet(
            onDismissRequest = { showCalcSheet = false },
            sheetState = sheetState,
            routeNumber = currentRoute?.basicData?.number,
            salaryForRouteState = salaryForRouteState,
            nightTime = nightTime,
            passengerTime = currentRoute?.getPassengerTimeWithinWork(),
            passengerOutsideTime = currentRoute?.getPassengerTimeOutsideWork(),
            holidayTime = holidayTimeValue,
            currency = currency,
            onSalarySettingClick = {
                showCalcSheet = false
                onSalarySettingClick()
            }
        )
    }

    // Шторка «Отдых» — тип отдыха + расчёт длительности
    if (showRestSheet) {
        RestBottomSheet(
            onDismissRequest = { showRestSheet = false },
            sheetState = sheetState,
            isRestPointOfTurnover = currentRoute?.basicData?.restPointOfTurnover == true,
            onRestChanged = onRestChanged,
            dialogRestUiState = dialogRestUiState,
            onRestSettingClick = {
                showRestSheet = false
                onRestSettingClick()
            },
            dateAndTimeConverter = dateAndTimeConverter
        )
    }

    val nightWarnState by viewModel.nightWarnState.collectAsState()

    // Шторка о дублирующем маршруте по явке
    val duplicateRouteState by viewModel.duplicateRouteSheet.collectAsState()
    duplicateRouteState?.let { capturedDupState ->
        // Захватываем state в локальную переменную, потому что AppBottomSheet
        // авто-вызывает onDismissRequest до нашего action callback — к этому
        // моменту viewModel.duplicateRouteSheet.value уже null.
        AppBottomSheet(
            onDismissRequest = { viewModel.dismissDuplicateSheet() },
            sheetState = sheetState,
            title = "Маршрут с такой явкой уже сохранён.",
            actions = listOf(
                BottomSheetAction(text = "Заменить") {
                    viewModel.confirmReplaceDuplicate(capturedDupState)
                },
                BottomSheetAction(text = "Оставить оба") {
                    viewModel.confirmKeepBothDuplicates(capturedDupState)
                }
            )
        )
    }

    val isSharedPreview by viewModel.isSharedPreview.collectAsState()

    // Шторка «Вы получили новый маршрут» для маршрутов по публичной ссылке
    if (isSharedPreview) {
        AppBottomSheet(
            onDismissRequest = { viewModel.dismissSharedPreviewSheet() },
            sheetState = sheetState,
            title = "Получен новый маршрут",
            actions = listOf(
                BottomSheetAction(text = "Просмотр") {
                    viewModel.dismissSharedPreviewSheet()
                },
                BottomSheetAction(text = "Сохранить") {
                    viewModel.saveSharedRouteAndExit()
                }
            ),
            cancelText = "Отмена",
            onCancel = { exitScreen() }
        )
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
                    viewModel.saveRoute()
                },
                BottomSheetAction(text = "Оформить подписку за 69 руб/мес") {
                    showPurchasesScreen()
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
                BottomSheetAction(text = "Оформить подписку за 69 руб/мес") {
                    showPurchasesScreen()
                },
                BottomSheetAction(text = "Восстановить покупки") {
                    viewModel.restorePurchases()
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
    val sharedPrefs: com.z_company.domain.repositories.SharedPreferencesRepositories = koinInject()

    LaunchedEffect(Unit) {
        snackbarManager.events
            .flowWithLifecycle(lifecycle)
            .collect { event ->
                val result = snackbarHostState.showSnackbar(
                    message = event.message,
                    actionLabel = event.actionLabel,
                    duration = event.duration
                )
                if (result == SnackbarResult.ActionPerformed) {
                    event.onAction?.let { onAction ->
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
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                title = {
                    val routeNumber = currentRoute?.basicData?.number
                    val titleText = when {
                        // Маршрут создаётся (новый или копия) — «Новый маршрут»
                        isNewRoute || isCopy -> "Новый маршрут"
                        // Открыт повторно без номера — «Маршрут б/н»
                        routeNumber.isNullOrBlank() -> "Маршрут б/н"
                        // Открыт повторно с номером — «Маршрут · №N»
                        else -> "Маршрут · №$routeNumber"
                    }
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                navigationIcon = {
                    TextButton(onClick = viewModel::onSaveClick) {
                        Text(
                            text = "Готово",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                },
                actions = {
                    // Шестерёнка ведёт на экран настроек формы маршрута
                    // (Настройки → Маршрут).
                    IconButton(onClick = onFormSettingsClick) {
                        Icon(
                            painter = painterResource(R.drawable.settings_24px),
                            contentDescription = "Настройки маршрута",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
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

        // Навигация при успешном сохранении обрабатывается через FormScreenEvent.RouteSaved
        // в FormDestination.kt — там же показывается toast «Маршрут сохранен».

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

        Box(
            Modifier
                .padding(it)
                .clipToBounds()
                .nestedScroll(bottomBarNestedScroll)
        ) {
            currentRoute?.let { route ->
                var showStartDatePickerCopyRoute by remember {
                    mutableStateOf(false)
                }

                var showStartDatePicker by remember {
                    mutableStateOf(false)
                }

                // Подтверждение ручного изменения явки при включённой «явке по прибытию».
                var showChangeStartConfirm by remember {
                    mutableStateOf(false)
                }

                var showEndDatePicker by remember {
                    mutableStateOf(false)
                }

                var showStartBreakDatePicker by remember {
                    mutableStateOf(false)
                }

                var showEndBreakDatePicker by remember {
                    mutableStateOf(false)
                }

                var showBottomSheetRemoveTimeStartBreak by remember {
                    mutableStateOf(false)
                }

                var showBottomSheetRemoveTimeEndBreak by remember {
                    mutableStateOf(false)
                }

                if (showStartBreakDatePicker) {
                    AppDateTimePicker(
                        title = "Начало перерыва",
                        onDateTimeSelected = { timestamp ->
                            onTimeStartBreakChanged(timestamp)
                        },
                        onDismiss = { showStartBreakDatePicker = false },
                        startDateTime = route.basicData.timeStartBreak
                            ?: route.basicData.timeStartWork
                            ?: TimeManager().now(),
                        timeZoneStr = displayTz,
                        recentTimes = sharedPrefs.getRecentTimes("time_start_break"),
                        onRecentTimeSaved = { sharedPrefs.addRecentTime("time_start_break", it) }
                    )
                }

                if (showEndBreakDatePicker) {
                    AppDateTimePicker(
                        title = "Окончание перерыва",
                        onDateTimeSelected = { timestamp ->
                            onTimeEndBreakChanged(timestamp)
                        },
                        onDismiss = { showEndBreakDatePicker = false },
                        startDateTime = route.basicData.timeEndBreak
                            ?: route.basicData.timeStartBreak
                            ?: route.basicData.timeStartWork
                            ?: TimeManager().now(),
                        timeZoneStr = displayTz,
                        recentTimes = sharedPrefs.getRecentTimes("time_end_break"),
                        onRecentTimeSaved = { sharedPrefs.addRecentTime("time_end_break", it) }
                    )
                }

                if (showBottomSheetRemoveTimeStartBreak) {
                    AppBottomSheet(
                        onDismissRequest = { showBottomSheetRemoveTimeStartBreak = false },
                        sheetState = sheetState,
                        headerContent = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = "Начало перерыва",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        actions = listOf(
                            BottomSheetAction(text = "Удалить значение") {
                                onTimeStartBreakChanged(null)
                            }
                        )
                    )
                }

                if (showBottomSheetRemoveTimeEndBreak) {
                    AppBottomSheet(
                        onDismissRequest = { showBottomSheetRemoveTimeEndBreak = false },
                        sheetState = sheetState,
                        headerContent = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = "Окончание перерыва",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        actions = listOf(
                            BottomSheetAction(text = "Удалить значение") {
                                onTimeEndBreakChanged(null)
                            }
                        )
                    )
                }

                if (showStartDatePicker) {
                    AppDateTimePicker(
                        title = "Явка",
                        onDateTimeSelected = { timestamp ->
                            onTimeStartWorkChanged(timestamp)
                        },
                        onDismiss = { showStartDatePicker = false },
                        startDateTime = route.basicData.timeStartWork
                            ?: TimeManager().now(),
                        timeZoneStr = displayTz,
                        recentTimes = sharedPrefs.getRecentTimes("time_start_work"),
                        onRecentTimeSaved = { sharedPrefs.addRecentTime("time_start_work", it) }
                    )
                }

                // Явка определяется прибытием пассажиром — подтверждаем ручное изменение.
                if (showChangeStartConfirm) {
                    RouteConfirmDialog(
                        title = "Изменить время явки?",
                        message = "Сейчас явка определяется прибытием пассажиром. Если " +
                                "задать время вручную, режим «явка по прибытию» отключится.",
                        confirmText = "Изменить",
                        onDismiss = { showChangeStartConfirm = false },
                        onConfirm = {
                            showChangeStartConfirm = false
                            viewModel.disableWorkStartByArrival()
                            showStartDatePicker = true
                        }
                    )
                }

                if (showEndDatePicker) {
                    AppDateTimePicker(
                        title = "Сдача",
                        onDateTimeSelected = { timestamp ->
                            onTimeEndWorkChanged(timestamp)
                        },
                        onDismiss = { showEndDatePicker = false },
                        startDateTime = route.basicData.timeEndWork
                            ?: route.basicData.timeStartWork
                            ?: TimeManager().now(),
                        timeZoneStr = displayTz,
                        recentTimes = sharedPrefs.getRecentTimes("time_end_work"),
                        onRecentTimeSaved = { sharedPrefs.addRecentTime("time_end_work", it) }
                    )
                }

                val showPassenger12hSheet by viewModel.showPassenger12hSheet.collectAsState()
                if (showPassenger12hSheet) {
                    val (prefilledDep, prefilledArr) = viewModel.getPrefilledPassengerTimes()
                    val prefilledStation = viewModel.getPrefilledDepartureStation()
                    val userSettingValue = viewModel.userSetting.collectAsState().value
                    Passenger12hBottomSheet(
                        prefilledTimeDeparture = prefilledDep,
                        prefilledTimeArrival = prefilledArr,
                        prefilledStationDeparture = prefilledStation,
                        workTimeStart = route.basicData.timeStartWork!!,
                        workTimeEnd = route.basicData.timeEndWork!!,
                        stationList = userSettingValue?.stationList ?: emptyList(),
                        dateAndTimeConverter = dateAndTimeConverter,
                        onSave = { stDep, stArr, timeDep, timeArr ->
                            viewModel.savePassengerFromSheet(stDep, stArr, timeDep, timeArr)
                            viewModel.dismissPassenger12hSheet()
                        },
                        onDismissNo = {
                            viewModel.dismissPassenger12hSheet()
                        },
                        onNavigateToSettings = {
                            viewModel.dismissPassenger12hSheet()
                            onSettingClick()
                        },
                        onDismiss = { viewModel.dismissPassenger12hSheet() }
                    )
                }

                LaunchedEffect(isCopy) {
                    if (isCopy) {
                        showStartDatePickerCopyRoute = true
                    }
                }

                // Диалог при копировании маршрута
                if (showStartDatePickerCopyRoute) {
                    AppDateTimePicker(
                        title = "Явка",
                        onDateTimeSelected = { timestamp ->
                            showStartDatePickerCopyRoute = false
                            onTimeStartWorkChanged(timestamp)
                            val workTimeInMillis = route.getPureWorkTime()
                            workTimeInMillis?.let { workTime ->
                                onTimeEndWorkChanged(timestamp + workTime)
                            }
                        },
                        onDismiss = { showStartDatePickerCopyRoute = false },
                        startDateTime = route.basicData.timeStartWork
                            ?: TimeManager().now(),
                        timeZoneStr = displayTz
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

                LazyColumn(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .testTag("form_lazy_column"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    state = scrollState,
                    // Нижний отступ учитывает высоту оверлей-панели действий.
                    contentPadding = PaddingValues(top = 8.dp, bottom = 84.dp)
                ) {
                    val startTimeInLong = route.basicData.timeStartWork
                    val endTimeInLong = route.basicData.timeEndWork
                    // «Чистая» работа (сдача − явка): проезд пассажиром до явки показываем
                    // отдельной строкой, поэтому здесь getPureWorkTime, а не getWorkTime.
                    val workTimeInLong = route.getPureWorkTime()
                    val workTimeInFormatted =
                        viewModel.convertTimeToStringFormat(workTimeInLong)

                    // ─── Основные данные + плитки «Расчёт» / «Отдых» ───
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            formUiState.errorMessage?.let { message ->
                                val widthScreen =
                                    LocalConfiguration.current.screenWidthDp.toFloat()
                                val errorGradient = Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
                                        MaterialTheme.colorScheme.error.copy(alpha = 0.72f),
                                    ),
                                    center = Offset(Float.POSITIVE_INFINITY, 0f),
                                    radius = widthScreen * 2
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(Shapes.medium)
                                        .background(brush = errorGradient, shape = Shapes.medium)
                                        .padding(vertical = 14.dp, horizontal = 16.dp)
                                ) {
                                    Text(
                                        text = message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onError
                                    )
                                }
                            }

                            FormGroupHeader(
                                text = "Основные данные",
                                badge = if (salaryForRouteState.isBusinessTrip) "КОМ" else null,
                            )
                            FormMCard {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Номер",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val number = route.basicData.number ?: ""
                                    BasicTextField(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(start = 12.dp),
                                        value = number,
                                        onValueChange = onNumberChanged,
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = MonoFont,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            textAlign = TextAlign.End
                                        ),
                                        keyboardOptions = KeyboardOptions.Default.copy(
                                            keyboardType = KeyboardType.Number
                                        ),
                                        cursorBrush = SolidColor(MaterialTheme.colorScheme.tertiary),
                                        decorationBox = { inner ->
                                            Box(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.CenterEnd
                                            ) {
                                                if (number.isEmpty()) {
                                                    Text(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        text = "Введите номер",
                                                        textAlign = TextAlign.End,
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                                    )
                                                }
                                                inner()
                                            }
                                        }
                                    )
                                }
                            }

                            // Работа в одно лицо — отдельный блок
                            // (показ управляется настройкой в разделе Настройки → Маршрут)
                            if (isShowOnePersonSwitch) {
                                val isOnePerson = route.basicData.isOnePersonOperation
                                FormMCard {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { checkedOnePersonOperation(!isOnePerson) }
                                            .padding(start = 20.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Работа в одно лицо",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Switch(
                                            checked = isOnePerson,
                                            onCheckedChange = { checkedOnePersonOperation(it) }
                                        )
                                    }
                                }
                            }

                            val calcText = if (salaryForRouteState.isCalculated) {
                                salaryForRouteState.totalPayment.toMoneyString(currency)
                            } else {
                                null.toMoneyString(currency)
                            }
                            val restText = if (route.basicData.restPointOfTurnover) {
                                "В пункте оборота"
                            } else {
                                "Домашний"
                            }
                            val calcTile: @Composable (Modifier) -> Unit = { m ->
                                FormTile(
                                    modifier = m,
                                    label = "Расчёт",
                                    value = calcText,
                                    valueMono = true,
                                    valueFilled = salaryForRouteState.isCalculated,
                                    icon = {
                                        Text(
                                            text = currency,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    },
                                    onClick = { showCalcSheet = true }
                                )
                            }
                            val restTile: @Composable (Modifier) -> Unit = { m ->
                                FormTile(
                                    modifier = m,
                                    label = "Отдых",
                                    value = restText,
                                    valueMono = false,
                                    valueFilled = true,
                                    icon = {
                                        Icon(
                                            modifier = Modifier.size(16.dp),
                                            painter = painterResource(
                                                if (route.basicData.restPointOfTurnover) R.drawable.hotel_24px
                                                else R.drawable.home_24px
                                            ),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.tertiary
                                        )
                                    },
                                    onClick = { showRestSheet = true }
                                )
                            }
                            // При крупном шрифте суммы «Расчёт»/«Отдых» не помещаются в
                            // половину ширины и обрезаются — раскладываем плитки в столбец.
                            if (LocalDensity.current.fontScale > 1.15f) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    calcTile(Modifier.fillMaxWidth())
                                    restTile(Modifier.fillMaxWidth())
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    calcTile(Modifier.weight(1f))
                                    restTile(Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    // ─── Напарники (лёгкий блок, выше «Время работы») ───
                    if (isShowPartner) {
                        item {
                            Column(modifier = Modifier.animateItem()) {
                                RoutePartnersBlock(
                                    partners = route.partners,
                                    onAdd = onAddPartners,
                                    onOpenCard = onOpenPartnerCard,
                                    onDelete = onDeletePartner,
                                )
                            }
                        }
                    }

                    // ─── Время работы ───
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FormGroupHeader(text = "Время работы")

                            // Голубая шапка (рабочее + ночное время) + белый блок Явка/Сдача —
                            // единый скруглённый контейнер.
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(elevation = 1.dp, shape = Shapes.medium)
                                    .clip(Shapes.medium)
                                    .background(MaterialTheme.colorScheme.secondary)
                            ) {
                                // Синяя шапка: отработанное время, под ним ночные часы
                                if (workTimeInLong != null) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                            .padding(horizontal = 20.dp, vertical = 14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Следование пассажиром ВНЕ рабочего времени («явка по прибытию»):
                                        // хронологически проезд до явки идёт раньше работы, поэтому — СВЕРХУ.
                                        // Отдельный самостоятельный итог, равнозначный «Отработано».
                                        val passengerOutsideWork = route.getPassengerTimeOutsideWork()
                                        if (passengerOutsideWork > 0L) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Пассажиром до явки",
                                                    style = MaterialTheme.typography.bodyLarge.copy(
                                                        fontWeight = FontWeight.SemiBold
                                                    ),
                                                    color = MaterialTheme.colorScheme.tertiary
                                                )
                                                Text(
                                                    text = ConverterLongToTime.getTimeInStringFormat(passengerOutsideWork),
                                                    style = MaterialTheme.typography.titleLarge.copy(
                                                        fontFamily = MonoFont,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = MaterialTheme.colorScheme.tertiary
                                                )
                                            }
                                            HorizontalDivider(
                                                thickness = 1.dp,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Отработано",
                                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                            Text(
                                                text = workTimeInFormatted,
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    fontFamily = MonoFont,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                        if (nightTime != null && nightTime > 0L) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(
                                                        modifier = Modifier.size(16.dp),
                                                        painter = painterResource(R.drawable.dark_mode_24px),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        text = "В ночное время",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                Text(
                                                    text = ConverterLongToTime.getTimeInStringFormat(nightTime),
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontFamily = MonoFont,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        // Следование пассажиром, входящее в рабочее время
                                        val passengerWithinWork = route.getPassengerTimeWithinWork()
                                        if (passengerWithinWork > 0L) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(
                                                        modifier = Modifier.size(16.dp),
                                                        painter = painterResource(R.drawable.passenger_24px),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        text = "Пассажиром",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                Text(
                                                    text = ConverterLongToTime.getTimeInStringFormat(passengerWithinWork),
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontFamily = MonoFont,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }

                                val isWorkStartByArrival = route.passengers.any { it.isWorkStartByArrival }
                                FormTimeRow(
                                    label = "Явка",
                                    valueText = startTimeInLong?.let {
                                        dateAndTimeConverter?.getDateAndTime(it)
                                    },
                                    onClick = {
                                        // При «явке по прибытию» — сперва подтверждение ручного изменения.
                                        if (isWorkStartByArrival) showChangeStartConfirm = true
                                        else showStartDatePicker = true
                                    },
                                    onLongClick = {
                                        startTimeInLong?.let { showBottomSheetRemoveTimeStartWork = true }
                                    },
                                    // Если явка определяется прибытием пассажиром — показываем это.
                                    subtitle = if (isWorkStartByArrival)
                                        "по прибытию пассажиром" else null
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                                FormTimeRow(
                                    label = "Сдача",
                                    valueText = endTimeInLong?.let {
                                        dateAndTimeConverter?.getDateAndTime(it)
                                    },
                                    onClick = { showEndDatePicker = true },
                                    onLongClick = {
                                        endTimeInLong?.let { showBottomSheetRemoveTimeEndWork = true }
                                    }
                                )
                                // Перерыв — в том же блоке, сразу под Явкой/Сдачей
                                if (isShowBreak) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 20.dp),
                                        thickness = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                    FormTimeRow(
                                        label = "Начало перерыва",
                                        valueText = route.basicData.timeStartBreak
                                            ?.takeIf { it != 0L }
                                            ?.let { dateAndTimeConverter?.getDateAndTime(it) },
                                        onClick = { showStartBreakDatePicker = true },
                                        onLongClick = {
                                            route.basicData.timeStartBreak?.let {
                                                showBottomSheetRemoveTimeStartBreak = true
                                            }
                                        }
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 20.dp),
                                        thickness = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                    FormTimeRow(
                                        label = "Конец перерыва",
                                        valueText = route.basicData.timeEndBreak
                                            ?.let { dateAndTimeConverter?.getDateAndTime(it) },
                                        onClick = { showEndBreakDatePicker = true },
                                        onLongClick = {
                                            route.basicData.timeEndBreak?.let {
                                                showBottomSheetRemoveTimeEndBreak = true
                                            }
                                        }
                                    )
                                }
                            }

                            // Предупреждение «вторая ночь подряд»
                            nightWarnState?.let { warn ->
                                RouteNightWarn(
                                    state = warn,
                                    dateAndTimeConverter = dateAndTimeConverter
                                )
                            }

                        }
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()
                                .padding(bottom = 32.dp, top = 8.dp),
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val basicId = route.basicData.id
                            if (isShowLocomotive) {
                                ItemAddingScreen(
                                    title = stringResource(id = R.string.locomotive),
                                    iconRes = R.drawable.ic_card_locomotive_ref,
                                    contentList = route.locomotives,
                                    onChangeElementClick = onChangedLocoClick,
                                    onNewElementClick = onNewLocoClick,
                                    basicId = basicId,
                                    onDeleteClick = { loco ->
                                        pendingDeleteTitle = "Удалить локомотив?"
                                        pendingDeleteMessage =
                                            "Локомотив будет убран из маршрута. Это действие нельзя отменить."
                                        pendingDeleteAction = { onDeleteLoco(loco) }
                                    }
                                ) { index, locomotive ->
                                    LocomotiveSubItem(locomotive, index)
                                }
                            }
                            if (isShowTrain) {
                                ItemAddingScreen(
                                    title = stringResource(id = R.string.train),
                                    iconRes = R.drawable.ic_card_train_ref,
                                    // Сортировка: поезда с временем отправления первой станции — по убыванию
                                    // (последний отправившийся сверху), поезда без времени — в порядке добавления.
                                    contentList = route.trains
                                        .filter { it.stations.firstOrNull()?.timeDeparture != null }
                                        .sortedByDescending { it.stations.firstOrNull()?.timeDeparture } +
                                        route.trains.filter { it.stations.firstOrNull()?.timeDeparture == null },
                                    onChangeElementClick = onChangeTrainClick,
                                    onNewElementClick = onNewTrainClick,
                                    basicId = basicId,
                                    onDeleteClick = { train ->
                                        pendingDeleteTitle = "Удалить поезд?"
                                        pendingDeleteMessage =
                                            "Поезд будет убран из маршрута. Это действие нельзя отменить."
                                        pendingDeleteAction = { onDeleteTrain(train) }
                                    }
                                ) { index, train ->
                                    TrainSubItem(index, train)
                                }
                            }
                            if (isShowOtherWork) {
                                ItemAddingScreen(
                                    title = stringResource(id = R.string.other_work),
                                    iconRes = R.drawable.ic_card_other_work_ref,
                                    contentList = route.otherWorks,
                                    onChangeElementClick = onChangeOtherWorkClick,
                                    onNewElementClick = onNewOtherWorkClick,
                                    basicId = basicId,
                                    onDeleteClick = { otherWork ->
                                        pendingDeleteTitle = "Удалить запись?"
                                        pendingDeleteMessage =
                                            "Запись прочей работы будет убрана из маршрута. Это действие нельзя отменить."
                                        pendingDeleteAction = { onDeleteOtherWork(otherWork) }
                                    }
                                ) { index, otherWork ->
                                    OtherWorkSubItem(index, otherWork)
                                }
                            }
                            if (isShowPassenger) {
                                ItemAddingScreen(
                                    title = stringResource(id = R.string.passenger),
                                    iconRes = R.drawable.ic_card_passenger_ref,
                                    contentList = route.passengers,
                                    onChangeElementClick = onChangePassengerClick,
                                    onNewElementClick = onNewPassengerClick,
                                    basicId = basicId,
                                    onDeleteClick = { passenger ->
                                        pendingDeleteTitle = "Удалить поездку пассажиром?"
                                        pendingDeleteMessage =
                                            "Запись будет убрана из маршрута. Это действие нельзя отменить."
                                        pendingDeleteAction = { onDeletePassenger(passenger) }
                                    }
                                ) { index, passenger ->
                                    PassengerSubItem(index, passenger)
                                }
                            }
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FormGroupHeader(text = "Заметки")
                                ItemNotes(
                                    notes = route.basicData.notes,
                                    onNotesChanged = onNotesChanged,
                                )
                            }
                        }
                    }
                }
            }

            // Нижняя контекстная панель — оверлей: плавно съезжает вниз на свою
            // высоту при скролле вниз и приезжает обратно при скролле вверх.
            // Панель всегда в композиции, анимируется только смещение — надёжно.
            var barHeightPx by remember { mutableStateOf(0) }
            val barOffsetY by animateIntAsState(
                targetValue = if (bottomBarVisible) 0 else barHeightPx,
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                label = "bottomBarOffset"
            )
            FormBottomAppBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { barHeightPx = it.height }
                    .offset { IntOffset(0, barOffsetY) },
                isFavorite = currentRoute?.basicData?.isFavorite == true,
                onFavoriteClick = {
                    val isFavorite = currentRoute?.basicData?.isFavorite == true
                    setFavoriteState()
                    val textSnackbar =
                        if (isFavorite) "Убрали из избранного" else "Маршрут добавлен в избранное"
                    scope.launch { snackbarManager.show(textSnackbar) }
                },
                onShareClick = { viewModel.onShareClick() },
                onCopyClick = { showCopyConfirmDialog = true },
                onDeleteClick = { showDeleteConfirmDialog = true }
            )
        }
    }
}

/** Иконка единицы маршрута в тональном контейнере 40dp — единый вид для
 *  строк локомотива/поезда/пассажира и кнопки «Добавить …». */
@Composable
private fun UnitIconBox(iconRes: Int) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(Shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size(22.dp),
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary
        )
    }
}

@Composable
fun <T> ItemAddingScreen(
    title: String,
    iconRes: Int,
    contentList: List<T>?,
    onChangeElementClick: (element: T) -> Unit,
    onNewElementClick: (basicId: String) -> Unit,
    basicId: String,
    onDeleteClick: (element: T) -> Unit,
    subItem: @Composable RowScope.(index: Int, element: T) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FormGroupHeader(text = title)

        FormMCard {
            val elements = contentList.orEmpty()
            elements.forEachIndexed { index, element ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChangeElementClick(element) }
                        .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UnitIconBox(iconRes)
                    Spacer(modifier = Modifier.width(14.dp))
                    Row(modifier = Modifier.weight(1f)) {
                        subItem(index, element)
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.10f))
                            .clickable { onDeleteClick(element) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            painter = painterResource(com.z_company.core.R.drawable.ic_clear),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNewElementClick(basicId) }
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UnitIconBox(iconRes)
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    modifier = Modifier.weight(1f),
                    text = "Добавить ${title.lowercase()}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "›",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                )
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
        // Серия и номер локомотива — идентификатор → Mono.
        Text(
            text = "$series $numberText",
            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = MonoFont),
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

        // Номер поезда — Mono (идентификатор), названия станций — Inter (имена собственные).
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontFamily = MonoFont)) { append("№ ${train.number}") }
                append(" $stationStart$stationEnd")
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun PassengerSubItem(index: Int, passenger: Passenger) {
    if (passenger.trainNumber.isNullOrBlank() && passenger.stationDeparture.isNullOrBlank() && passenger.stationArrival.isNullOrBlank()) {
        Text(
            text = "Пассажиром № ${index + 1}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
    } else {
        val textNumber = passenger.trainNumber?.let {
            "№ $it"
        } ?: ""

        val textStationDeparture = passenger.stationDeparture ?: ""
        val textStationArrival = passenger.stationArrival?.let {
            " - ${it}"
        } ?: ""

        // Номер поезда — Mono, названия станций — Inter.
        Text(
            text = buildAnnotatedString {
                if (textNumber.isNotEmpty()) {
                    withStyle(SpanStyle(fontFamily = MonoFont)) { append(textNumber) }
                    append(" ")
                }
                append("$textStationDeparture $textStationArrival")
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun OtherWorkSubItem(index: Int, otherWork: OtherWork) {
    val type = otherWork.workType?.takeIf { it.isNotBlank() }
    val station = otherWork.station?.takeIf { it.isNotBlank() }
    if (type == null && station == null) {
        Text(
            text = "Прочая работа № ${index + 1}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
    } else {
        val stationSuffix = station?.let { " · $it" } ?: ""
        Text(
            text = "${type ?: "Прочая работа"}$stationSuffix",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Лёгкий блок «Напарники» в форме маршрута (вариант D): карточка со строками
 * (аватар · короткое ФИО · таб. справа) + accent-строка «Добавить напарника».
 * Тап по строке → карточка напарника ([onOpenCard]). Свайп влево → «Удалить» с
 * подтверждением-шторкой ([onDelete]). Добавление — мультивыбор из справочника ([onAdd]).
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun RoutePartnersBlock(
    partners: List<RoutePartner>,
    onAdd: () -> Unit,
    onOpenCard: (RoutePartner) -> Unit,
    onDelete: (RoutePartner) -> Unit,
) {
    var partnerForRemove by remember { mutableStateOf<RoutePartner?>(null) }
    var swipeCloseSignal by remember { mutableStateOf(0) }
    val confirmSheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)

    partnerForRemove?.let { partner ->
        AppBottomSheet(
            onDismissRequest = {
                partnerForRemove = null
                swipeCloseSignal++
            },
            sheetState = confirmSheetState,
            headerContent = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Удалить напарника?",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    val name = com.z_company.route.ui.partner.partnerShortName(partner.fullName)
                        .ifBlank { "Напарник" }
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            },
            actions = listOf(
                BottomSheetAction(text = "Да, удалить") {
                    onDelete(partner)
                    partnerForRemove = null
                }
            )
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FormGroupHeader(text = "Напарники")
        FormMCard {
            partners.forEach { partner ->
                com.z_company.route.component.SwipeToRevealDelete(
                    itemKey = partner.routePartnerId,
                    closeSignal = swipeCloseSignal,
                    compact = true,
                    onDeleteClick = { partnerForRemove = partner },
                    onContentClick = { onOpenCard(partner) },
                ) { _ ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.secondary)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        com.z_company.route.ui.partner.PartnerAvatar(name = partner.fullName, size = 32.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            modifier = Modifier.weight(1f),
                            text = com.z_company.route.ui.partner.partnerShortName(partner.fullName)
                                .ifBlank { "Напарник" },
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        partner.tabNumber?.takeIf { it.isNotBlank() }?.let { tab ->
                            Text(
                                text = "таб. $tab",
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFont),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAdd() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(com.z_company.core.R.drawable.ic_add),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Добавить напарника",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
    }
}

@Composable
fun ItemNotes(
    modifier: Modifier = Modifier,
    notes: String?,
    onNotesChanged: (String) -> Unit,
) {
    FormMCard(modifier = modifier) {
        val value = notes ?: ""
        BasicTextField(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp, max = 160.dp)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            value = value,
            onValueChange = onNotesChanged,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.primary
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.tertiary),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        text = "Опиши смену, если нужно…",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
                }
                inner()
            }
        )
    }
}

@Composable
fun InfoRestOfHomeOfTime(
    restDuration: Long?,
    timeEndHomeRest: Long?,
    timeEndMinHomeRest: Long? = null,
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
                timeEndMinHomeRest?.let {
                    Text(
                        text = "Минимальный отдых до ${dateAndTimeConverter?.getDateAndTime(it) ?: ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

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

// ─────────────────────────────────────────────────────────────
// Компоненты редизайна FormScreen
// ─────────────────────────────────────────────────────────────

/** Нейтральный диалог подтверждения (не удаление) — с настраиваемой кнопкой подтверждения. */
@Composable
private fun RouteConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = title,
        text = message,
        confirmText = confirmText,
        onConfirm = onConfirm,
        dismissText = "Отмена",
        onDismiss = onDismiss,
    )
}

/** UPPERCASE mono-заголовок группы. */
@Composable
private fun FormGroupHeader(
    text: String,
    modifier: Modifier = Modifier,
    badge: String? = null,
) {
    val headerStyle = MaterialTheme.typography.labelMedium.copy(
        fontFamily = MonoFont,
        fontSize = 11.sp,
        letterSpacing = TextUnit(1.2f, TextUnitType.Sp)
    )
    if (badge == null) {
        Text(
            modifier = modifier.padding(start = 4.dp),
            text = text.uppercase(),
            style = headerStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = text.uppercase(),
                style = headerStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Box(
                modifier = Modifier
                    .background(
                        color = BusinessTripColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(6.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    text = badge,
                    style = headerStyle,
                    color = BusinessTripColor,
                )
            }
        }
    }
}

// Цвет метки «Командировка» — согласован с календарём (CalendarScreen.BusinessTripColor).
private val BusinessTripColor = Color(0xFF30B0C7)

/** Белая карточка-контейнер (surface, скругление 16, мягкая тень). */
@Composable
private fun FormMCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 1.dp, shape = Shapes.medium)
            .clip(Shapes.medium)
            .background(MaterialTheme.colorScheme.secondary),
        content = content
    )
}

/** Плитка «Расчёт» / «Отдых» — иконка+подпись сверху, значение снизу. */
@Composable
private fun FormTile(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueMono: Boolean,
    valueFilled: Boolean,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .shadow(elevation = 1.dp, shape = Shapes.medium)
            .clip(Shapes.medium)
            .background(MaterialTheme.colorScheme.secondary)
            .clickable { onClick() }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            icon()
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = if (valueMono) {
                MaterialTheme.typography.titleLarge.copy(
                    fontFamily = MonoFont,
                    fontWeight = FontWeight.Bold
                )
            } else {
                MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            },
            color = if (valueFilled) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )
    }
}

/** Строка «Явка» / «Сдача» внутри карточки. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FormTimeRow(
    label: String,
    valueText: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    subtitle: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    modifier = Modifier.padding(top = 2.dp),
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
        Text(
            text = if (valueText.isNullOrBlank()) "Выбрать" else valueText,
            // Дата/время — Mono (значение), плейсхолдер «Выбрать» — Inter (язык).
            style = if (valueText.isNullOrBlank()) MaterialTheme.typography.bodyLarge
            else MaterialTheme.typography.bodyLarge.copy(fontFamily = MonoFont),
            color = if (valueText.isNullOrBlank()) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.primary
        )
    }
}

/** Нижняя панель действий над маршрутом (избранное / поделиться / копия / удалить). */
@Composable
private fun FormBottomAppBar(
    modifier: Modifier = Modifier,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onShareClick: () -> Unit,
    onCopyClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    painter = painterResource(
                        if (isFavorite) R.drawable.favorite_fill_24px else R.drawable.favorite_24px
                    ),
                    contentDescription = "В избранное",
                    tint = if (isFavorite) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onShareClick) {
                Icon(
                    painter = painterResource(R.drawable.share_24px),
                    contentDescription = "Поделиться",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onCopyClick) {
                Icon(
                    painter = painterResource(R.drawable.outline_content_copy_24),
                    contentDescription = "Сделать копию",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onDeleteClick) {
                Icon(
                    painter = painterResource(R.drawable.delete_24px),
                    contentDescription = "Удалить маршрут",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/** Общая оболочка Material-шторки с заголовком/подзаголовком. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RouteSheetShell(
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    title: String,
    subtitle: String?,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outline)
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp)
                .navigationBarsPadding()
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 8.dp),
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

/** Кнопка-переход в настройки внутри шторки. */
@Composable
private fun SheetSettingsRow(
    text: String,
    onClick: () -> Unit,
) {
    FormMCard(modifier = Modifier.padding(top = 12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.settings_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                modifier = Modifier.weight(1f),
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "›",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            )
        }
    }
}

/** Процент без лишнего «.0»: 25.0 → «25», 12.5 → «12,5». */
private fun formatPercent(p: Double): String {
    return if (p % 1.0 == 0.0) {
        p.toInt().toString()
    } else {
        p.toString().trimEnd('0').trimEnd('.').replace('.', ',')
    }
}

/** Шторка «Расчёт за смену» — разбивка заработка. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalcBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    routeNumber: String?,
    salaryForRouteState: SalaryForRouteState,
    nightTime: Long?,
    passengerTime: Long?,
    passengerOutsideTime: Long?,
    holidayTime: Long?,
    currency: String,
    onSalarySettingClick: () -> Unit,
) {
    RouteSheetShell(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        title = "Расчёт за смену",
        subtitle = routeNumber?.takeIf { it.isNotBlank() }?.let { "Маршрут №$it" }
    ) {
        if (!salaryForRouteState.isCalculated) {
            FormMCard {
                Text(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                    text = "Укажите начало и окончание рабочего времени для расчёта заработной платы за поездку",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Итоговая сумма — filled tonal
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Shapes.medium)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Заработано",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = salaryForRouteState.totalPayment.toMoneyString(currency),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = MonoFont,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Маршрут в командировке оплачивается только по среднему часу. Если
            // средний час не задан — сумма 0, поясняем это пользователю и даём
            // быстрый переход в настройки зарплаты.
            val averageHourMissing = salaryForRouteState.isBusinessTrip &&
                    (salaryForRouteState.businessTripMoney == null ||
                            salaryForRouteState.businessTripMoney == 0.0)
            if (averageHourMissing) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(Shapes.medium)
                        .background(BusinessTripColor.copy(alpha = 0.14f))
                        .clickable { onSalarySettingClick() }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "Маршрут в командировке",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = BusinessTripColor,
                    )
                    Text(
                        text = "Оплачивается только по среднему часу, без надбавок. " +
                                "Средний час не указан — поэтому сумма 0. " +
                                "Нажмите, чтобы задать его в настройках зарплаты.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Формулы-пояснения из реальных исходников расчёта
            val rateStr = salaryForRouteState.tariffRate
                ?.takeIf { it != 0.0 }
                ?.let { it.toMoneyString(currency) }
            val hourlyHint = if (rateStr != null && salaryForRouteState.workTimeForPay != null) {
                "${ConverterLongToTime.getTimeInStringFormat(salaryForRouteState.workTimeForPay)} × $rateStr/ч"
            } else {
                "оплата рабочего времени по тарифу"
            }
            // «HH:MM × rate/ч × pct%» — если есть все исходники, иначе описание
            fun timeRatePercentHint(
                time: Long?,
                percent: Double?,
                fallback: String,
                percentText: String? = null,
            ): String {
                return if (rateStr != null && time != null && time > 0L && (percent != null || percentText != null)) {
                    val pct = percentText ?: "${formatPercent(percent!!)} %"
                    "${ConverterLongToTime.getTimeInStringFormat(time)} × $rateStr/ч × $pct"
                } else {
                    fallback
                }
            }
            // «HH:MM × rate/ч» — оплата по полному тарифу (без процента)
            fun timeRateHint(time: Long?, fallback: String): String {
                return if (rateStr != null && time != null && time > 0L) {
                    "${ConverterLongToTime.getTimeInStringFormat(time)} × $rateStr/ч"
                } else {
                    fallback
                }
            }
            val zonalHint = timeRatePercentHint(
                salaryForRouteState.zonalTime, salaryForRouteState.zonalPercent,
                "надбавка за разъездной характер работы"
            )
            val nightHint = timeRatePercentHint(
                nightTime, salaryForRouteState.nightPercent, "доплата за работу в ночное время"
            )
            val onePersonHint = timeRatePercentHint(
                salaryForRouteState.onePersonTime, salaryForRouteState.onePersonPercent,
                "надбавка за работу в одно лицо"
            )
            val overRestHint = timeRatePercentHint(
                salaryForRouteState.overRestTime, null, "компенсация за переотдых",
                percentText = "2/3"
            )
            val passengerHint = timeRateHint(
                passengerTime, "оплата времени следования пассажиром"
            )
            val passengerOutsideHint = timeRateHint(
                passengerOutsideTime, "следование пассажиром до явки"
            )
            val holidayHint = holidayTime
                ?.takeIf { it > 0L }
                ?.let { "${ConverterLongToTime.getTimeInStringFormat(it)} в праздничные дни" }
                ?: "доплата за работу в праздничные дни"
            val rows = buildList {
                fun add(label: String, hint: String, value: Double?) {
                    if (value != null && value != 0.0) add(Triple(label, hint, value))
                }
                add("Почасовая оплата", hourlyHint, salaryForRouteState.paymentAtTariffRate)
                add("Праздничные", holidayHint, salaryForRouteState.paymentHolidayMoney)
                add("Зональная надбавка", zonalHint, salaryForRouteState.zonalSurchargeMoney)
                add("Ночные", nightHint, salaryForRouteState.paymentAtNightTime)
                add("Пассажиром", passengerHint, salaryForRouteState.paymentAtPassengerTime)
                add("Пассажиром до явки", passengerOutsideHint, salaryForRouteState.paymentAtPassengerOutsideTime)
                salaryForRouteState.linearMileageAccruals.forEach { accrual ->
                    val distance = if (accrual.distance % 1.0 == 0.0) {
                        accrual.distance.toLong().toString()
                    } else {
                        "%.1f".format(accrual.distance).replace('.', ',')
                    }
                    val rate = accrual.rate.toMoneyString(currency)
                    add(
                        "Доплата за пробег",
                        "${accrual.phaseName}: $distance км × $rate/км",
                        accrual.money,
                    )
                }
                add("Одно лицо", onePersonHint, salaryForRouteState.paymentAtOnePerson)
                val trainSurchargeHint = salaryForRouteState.trainSurchargeTypes
                    .takeIf { it.isNotEmpty() }
                    ?.joinToString(", ")
                    ?: "доплаты за категорию поезда"
                add("Доплаты за поезд", trainSurchargeHint, salaryForRouteState.surchargesAtTrain)
                add("Прочие доплаты", "дополнительные начисления", salaryForRouteState.otherSurcharge)
                add("Переотдых", overRestHint, salaryForRouteState.overRestMoney)
                add("Командировка (по среднему)", "оплата только по среднему часу", salaryForRouteState.businessTripMoney)
            }

            if (rows.isNotEmpty()) {
                FormMCard {
                    rows.forEachIndexed { index, (label, hint, value) ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 20.dp),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = hint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = value.toMoneyString(currency),
                                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = MonoFont),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            if (!salaryForRouteState.isSetTariffRate) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 10.dp)
                ) {
                    Text(
                        modifier = Modifier.noRippleEffect { onSalarySettingClick() },
                        text = "Установите значение тарифной ставки в настройках.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontStyle = FontStyle.Italic,
                            textDecoration = TextDecoration.Underline
                        ),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }

        SheetSettingsRow(text = "Настройки зарплаты", onClick = onSalarySettingClick)
    }
}

/** Шторка «Отдых» — тип отдыха + расчёт длительности. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RestBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    isRestPointOfTurnover: Boolean,
    onRestChanged: (Boolean) -> Unit,
    dialogRestUiState: DialogRestUiState,
    onRestSettingClick: () -> Unit,
    dateAndTimeConverter: DateAndTimeConverter?,
) {
    RouteSheetShell(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        title = "Отдых",
        subtitle = "Тип и длительность отдыха"
    ) {
        // Segmented control: Домашний / В пункте оборота.
        // Выделение заполняет свою половину: внешние углы скруглены под
        // капсулу-контейнер, внутренние (у центра) — прямые.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clip(CircleShape)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = CircleShape
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RestSegButton(
                modifier = Modifier.weight(1f),
                label = "Домашний",
                active = !isRestPointOfTurnover,
                shape = RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50),
                onClick = { onRestChanged(false) }
            )
            RestSegButton(
                modifier = Modifier.weight(1f),
                label = "В пункте оборота",
                active = isRestPointOfTurnover,
                shape = RoundedCornerShape(topEndPercent = 50, bottomEndPercent = 50),
                onClick = { onRestChanged(true) }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (isRestPointOfTurnover) {
            val min = dialogRestUiState.minTimeDuration
            val full = dialogRestUiState.fullTimeDuration
            val endMin = dialogRestUiState.timeEndMinTimeRestPointOfTurnover
            val endFull = dialogRestUiState.timeEndFullTimeRestPointOfTurnover
            if (min == null || full == null || endMin == null || endFull == null) {
                RestUnavailableCard(
                    "Невозможно рассчитать время отдыха.\nПроверьте начало и окончание работы."
                )
            } else {
                FormMCard {
                    RestItem(
                        title = "Короткий отдых",
                        duration = ConverterLongToTime.formatDurationFromMillis(min),
                        until = dateAndTimeConverter?.getDateMiniAndTime(endMin) ?: ""
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    RestItem(
                        title = "Полный отдых",
                        duration = ConverterLongToTime.formatDurationFromMillis(full),
                        until = dateAndTimeConverter?.getDateMiniAndTime(endFull) ?: ""
                    )
                    ActualRestFormItem(dialogRestUiState, dateAndTimeConverter)
                }
            }
        } else {
            val dur = dialogRestUiState.homeRestDuration
            val end = dialogRestUiState.timeEndHomeRest
            val minEnd = dialogRestUiState.timeEndMinHomeRest
            if (dur == null || end == null) {
                RestUnavailableCard(
                    "Невозможно рассчитать время отдыха.\nПроверьте начало и окончание работы во всей цепочке маршрутов."
                )
            } else {
                FormMCard {
                    minEnd?.let {
                        RestItem(
                            title = "Минимальный отдых",
                            duration = ConverterLongToTime.formatDurationFromMillis(it - (end - dur)),
                            until = dateAndTimeConverter?.getDateMiniAndTime(it) ?: ""
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                    RestItem(
                        title = "Полный отдых",
                        duration = ConverterLongToTime.formatDurationFromMillis(dur),
                        until = dateAndTimeConverter?.getDateMiniAndTime(end) ?: ""
                    )
                    ActualRestFormItem(dialogRestUiState, dateAndTimeConverter)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        modifier = Modifier.size(16.dp),
                        painter = painterResource(R.drawable.info_24px),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Полный = время работы × 2,6 минус отдых в пункте оборота.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        SheetSettingsRow(text = "Настройки времени отдыха", onClick = onRestSettingClick)
    }
}

/** Строка расчёта отдыха: «Заголовок · длительность» + «до дата·время» моно. */
@Composable
private fun RestItem(
    title: String,
    duration: String,
    until: String,
) {
    val titleText: @Composable () -> Unit = {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    val untilText: @Composable () -> Unit = {
        Text(
            text = "до $until",
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = MonoFont,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            softWrap = false,
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        // При крупном шрифте «название · длительность» в одну строку не помещается —
        // название и длительность каждое на своей строке. Стандартный шрифт — прежний ряд.
        if (LocalDensity.current.fontScale > 1.15f) {
            titleText()
            Text(
                text = duration,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFont),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                titleText()
                Text(
                    text = " · $duration",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFont),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        untilText()
    }
}

/**
 * Строка «Фактический отдых» — реальный отдых до следующей явки по расписанию.
 * Показывается только если следующая явка есть (`actualRestDuration != null`).
 */
@Composable
private fun ActualRestFormItem(
    dialogRestUiState: DialogRestUiState,
    dateAndTimeConverter: DateAndTimeConverter?,
) {
    val dur = dialogRestUiState.actualRestDuration
    val end = dialogRestUiState.timeEndActualRest
    if (dur == null || dur <= 0 || end == null) return
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
    RestItem(
        title = "Фактический отдых",
        duration = ConverterLongToTime.formatDurationFromMillis(dur),
        until = dateAndTimeConverter?.getDateMiniAndTime(end) ?: ""
    )
}

/** Карточка «отдых рассчитать невозможно». */
@Composable
private fun RestUnavailableCard(message: String) {
    FormMCard {
        Text(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RestSegButton(
    modifier: Modifier = Modifier,
    label: String,
    active: Boolean,
    shape: Shape,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(shape)
            .background(if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        // Сегмент фиксированной ширины — «В пункте оборота» при крупном шрифте
        // обрезается; ограничиваем масштаб подписи, чтобы помещалась.
        val d = LocalDensity.current
        val labelDensity = if (d.fontScale > 1.15f) Density(d.density, 1.15f) else d
        CompositionLocalProvider(LocalDensity provides labelDensity) {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (active) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


/** Предупреждение «вторая ночь подряд» — предыдущий и текущий маршрут
 *  оба захватывают ночное окно (00:00–05:00). Только информирование. */
@Composable
private fun RouteNightWarn(
    state: NightWarnState,
    dateAndTimeConverter: DateAndTimeConverter?,
) {
    var visible by remember(state) { mutableStateOf(true) }
    if (!visible) return
    val warning = MaterialTheme.colorScheme.surfaceContainerHigh
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Shapes.medium)
            .background(warning.copy(alpha = 0.10f))
            .border(width = 1.dp, color = warning.copy(alpha = 0.55f), shape = Shapes.medium)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(warning.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(14.dp),
                    painter = painterResource(R.drawable.dark_mode_24px),
                    contentDescription = null,
                    tint = warning
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Вторая ночь подряд",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Третья ночь подряд не допускается.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable { visible = false },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(15.dp),
                    painter = painterResource(com.z_company.core.R.drawable.ic_clear),
                    contentDescription = "Скрыть",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        state.rows.forEachIndexed { index, row ->
            if (index > 0) {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = warning.copy(alpha = 0.25f)
                )
            }
            NightWarnRowItem(
                row = row,
                dateAndTimeConverter = dateAndTimeConverter
            )
        }

        // Легенда
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NightWarnLegendItem(color = MaterialTheme.colorScheme.surfaceVariant, text = "Маршрут")
            NightWarnLegendItem(color = MaterialTheme.colorScheme.tertiary, text = "Ночные часы · 00:00–05:00")
        }
    }
}

@Composable
private fun NightWarnLegendItem(color: Color, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 14.dp, height = 8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NightWarnRowItem(
    row: NightWarnRow,
    dateAndTimeConverter: DateAndTimeConverter?,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
                val range = "${dateAndTimeConverter?.getDateMiniAndTime(row.start) ?: ""} – " +
                    (dateAndTimeConverter?.getDateMiniAndTime(row.end) ?: "")
                Text(
                    text = range,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFont),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    modifier = Modifier.size(14.dp),
                    painter = painterResource(R.drawable.dark_mode_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = ConverterLongToTime.getTimeInStringFormat(row.nightMillis),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = MonoFont,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
        NightTimelineBar(
            start = row.start,
            end = row.end,
            nightIntervals = row.nightIntervals
        )
    }
}

/**
 * Графическая линия маршрута: серая полоса всего времени в пути + синие
 * сегменты ночных окон (как в референсе «вторая ночь подряд»).
 */
@Composable
private fun NightTimelineBar(
    start: Long,
    end: Long,
    nightIntervals: List<Pair<Long, Long>>,
) {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val border = MaterialTheme.colorScheme.outlineVariant
    val night = MaterialTheme.colorScheme.tertiary
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
    ) {
        val total = (end - start).coerceAtLeast(1L).toFloat()
        val w = size.width
        val h = size.height
        val radius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
        // Базовая полоса всего маршрута
        drawRoundRect(color = base, size = Size(w, h), cornerRadius = radius)
        // Ночные сегменты поверх
        nightIntervals.forEach { (ns, ne) ->
            val x1 = (((ns - start).toFloat()) / total * w).coerceIn(0f, w)
            val x2 = (((ne - start).toFloat()) / total * w).coerceIn(0f, w)
            if (x2 > x1) {
                drawRect(
                    color = night,
                    topLeft = Offset(x1, 0f),
                    size = Size(x2 - x1, h)
                )
            }
        }
        // Контур
        drawRoundRect(
            color = border,
            size = Size(w, h),
            cornerRadius = radius,
            style = Stroke(1.dp.toPx())
        )
    }
}
