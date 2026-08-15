package com.z_company.route.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.z_company.core.ResultState
import com.z_company.core.ui.component.CustomSnackBar
import com.z_company.core.ui.component.customDatePicker.noRippleEffect
import com.z_company.core.ui.theme.MonoFont
import com.z_company.core.ui.theme.Shapes
import com.z_company.domain.entities.setting.ServicePhase
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.route.ui.settings.SettingsAccountingContent
import com.z_company.route.ui.settings.SettingsLocoContent
import com.z_company.route.ui.settings.SettingsNormaContent
import com.z_company.route.ui.settings.SettingsRestContent
import com.z_company.route.ui.settings.SettingsRouteContent
import com.z_company.route.ui.settings.SettingsRouteFormContent
import com.z_company.route.ui.settings.SettingsShouldersContent
import com.z_company.route.ui.settings.SettingsSeriesListContent
import com.z_company.route.ui.settings.SettingsSeriesEditorContent
import com.z_company.route.ui.settings.SettingsStationListContent
import com.z_company.route.ui.settings.SettingsStationEditorContent
import com.z_company.route.ui.settings.SettingsPartnerListContent
import com.z_company.route.ui.settings.SettingsPartnerEditorContent
import com.z_company.route.viewmodel.SettingsViewModel
import com.z_company.route.component.PullToSyncContainer
import com.z_company.route.viewmodel.SettingsUiState
import com.z_company.route.viewmodel.TimeZoneRussia
import com.z_company.route.viewmodel.SeriesListViewModel
import com.z_company.route.viewmodel.SeriesEditorViewModel
import com.z_company.route.viewmodel.StationNormListViewModel
import com.z_company.route.viewmodel.StationNormEditorViewModel
import com.z_company.route.viewmodel.PartnerListViewModel
import com.z_company.route.viewmodel.PartnerEditorViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlinx.coroutines.launch
import androidx.core.net.toUri

enum class SettingsSubScreen(val title: String, val depth: Int) {
    HUB("Настройки", 0),
    ROUTE("Основные", 1),
    ROUTE_FORM("Маршрут", 1),
    NORMA("Норма/Регион", 1),
    ACCOUNTING("Учёт", 1),
    REST("Отдых", 1),
    SHOULDERS("Плечи", 1),
    LOCOMOTIVE("Локомотив", 1),
    SERIES_LIST("Серии", 1),
    SERIES_EDITOR("Серия", 2),
    STATION_LIST("Станции", 1),
    STATION_EDITOR("Станция", 2),
    PARTNER_LIST("Напарники", 1),
    PARTNER_EDITOR("Напарник", 2),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    settingsUiState: SettingsUiState,
    currentSettings: UserSettings?,
    initialSubScreen: String?,
    workTimeChanged: (Long) -> Unit,
    restTimeChanged: (Long) -> Unit,
    homeRestTimeChanged: (Long) -> Unit,
    logOut: () -> Unit,
    showAbsenceScreen: () -> Unit,
    resetUploadState: () -> Unit,
    resetDownloadState: () -> Unit,
    changeStartNightTime: (Int, Int) -> Unit,
    changeEndNightTime: (Int, Int) -> Unit,
    changeUsingDefaultWorkTime: (Boolean) -> Unit,
    changeConsiderFutureRoute: (Boolean) -> Unit,
    changeShowBreak: (Boolean) -> Unit,
    changeShowOnePersonSwitch: (Boolean) -> Unit,
    changeShowLocomotive: (Boolean) -> Unit,
    changeShowTrain: (Boolean) -> Unit,
    changeShowPassenger: (Boolean) -> Unit,
    changeShowOtherWork: (Boolean) -> Unit,
    changeShowPartner: (Boolean) -> Unit,
    changeShowLocoHeating: (Boolean) -> Unit,
    changeShowLocoAuxiliary: (Boolean) -> Unit,
    changeShowOtherCurrent: (Boolean) -> Unit,
    setTimeZone: (Long) -> Unit,
    timeZoneRussiaList: List<TimeZoneRussia>,
    servicePhases: SnapshotStateList<ServicePhase>?,
    showDialogAddServicePhase: (ServicePhase) -> Unit,
    hideDialogAddServicePhase: () -> Unit,
    addServicePhase: (ServicePhase, Int) -> Unit,
    deleteServicePhase: (ServicePhase) -> Unit,
    updateServicePhase: (ServicePhase, Int) -> Unit,
    showSettingSalary: () -> Unit,
    onBack: () -> Unit,
    seriesListViewModel: SeriesListViewModel? = null,
    stationListViewModel: StationNormListViewModel? = null,
    partnerListViewModel: PartnerListViewModel? = null,
    isPullRefreshing: Boolean = false,
    onPullRefresh: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Parse STATION_EDITOR_<id> / SERIES_EDITOR_<id> to open editors directly (from long-tap in picker)
    data class InitState(
        val screen: SettingsSubScreen,
        val stationId: String?,
        val seriesId: String?,
    )
    val initState = remember(initialSubScreen) {
        when {
            initialSubScreen?.startsWith("STATION_EDITOR_") == true ->
                InitState(SettingsSubScreen.STATION_EDITOR, initialSubScreen.removePrefix("STATION_EDITOR_"), null)
            initialSubScreen?.startsWith("SERIES_EDITOR_") == true ->
                InitState(SettingsSubScreen.SERIES_EDITOR, null, initialSubScreen.removePrefix("SERIES_EDITOR_"))
            else -> when (initialSubScreen) {
                "ROUTE" -> InitState(SettingsSubScreen.ROUTE, null, null)
                "ROUTE_FORM" -> InitState(SettingsSubScreen.ROUTE_FORM, null, null)
                "NORMA" -> InitState(SettingsSubScreen.NORMA, null, null)
                "ACCOUNTING" -> InitState(SettingsSubScreen.ACCOUNTING, null, null)
                "REST" -> InitState(SettingsSubScreen.REST, null, null)
                "SHOULDERS" -> InitState(SettingsSubScreen.SHOULDERS, null, null)
                "LOCOMOTIVE" -> InitState(SettingsSubScreen.LOCOMOTIVE, null, null)
                "SERIES_LIST" -> InitState(SettingsSubScreen.SERIES_LIST, null, null)
                "STATION_LIST" -> InitState(SettingsSubScreen.STATION_LIST, null, null)
                else -> InitState(SettingsSubScreen.HUB, null, null)
            }
        }
    }
    var currentSubScreen by remember { mutableStateOf(initState.screen) }
    var selectedSeriesId by remember { mutableStateOf(initState.seriesId) }
    var selectedStationId by remember { mutableStateOf(initState.stationId) }
    var selectedPartnerId by remember { mutableStateOf<String?>(null) }
    // Название «старой» серии (из locomotiveSeriesList, без норм), открываемой
    // в редакторе для добавления норм приёмки/сдачи. null — обычная новая серия.
    var prefillSeriesName by remember { mutableStateOf<String?>(null) }
    // То же для «старой» станции (из stationList, без норм).
    var prefillStationName by remember { mutableStateOf<String?>(null) }

    // Если пользователь попал сразу на под-экран (через deep link из FormLocoScreen
    // и т.п.) — back должен возвращать по backstack, а не в HUB настроек.
    // Если пользователь открыл настройки с HUB и перешёл во вложенный — back возвращает в HUB.
    val enteredDirectly = remember { initialSubScreen != null }
    // Редактор станции/серии открыт напрямую (deep link из FormLocoScreen),
    // а не из списка внутри настроек. Тогда back/Готово возвращают на форму,
    // а не в список. Сбрасывается в false при заходе в редактор из списка.
    var editorEnteredDirectly by remember {
        mutableStateOf(
            initState.screen == SettingsSubScreen.STATION_EDITOR ||
                initState.screen == SettingsSubScreen.SERIES_EDITOR
        )
    }

    BackHandler(currentSubScreen != SettingsSubScreen.HUB) {
        when (currentSubScreen) {
            SettingsSubScreen.SERIES_EDITOR ->
                if (editorEnteredDirectly) onBack() else currentSubScreen = SettingsSubScreen.SERIES_LIST
            SettingsSubScreen.STATION_EDITOR ->
                if (editorEnteredDirectly) onBack() else currentSubScreen = SettingsSubScreen.STATION_LIST
            SettingsSubScreen.PARTNER_EDITOR ->
                currentSubScreen = SettingsSubScreen.PARTNER_LIST
            else -> {
                if (enteredDirectly) onBack() else currentSubScreen = SettingsSubScreen.HUB
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { snackBarData ->
                CustomSnackBar(snackBarData = snackBarData)
            }
        },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (currentSubScreen != SettingsSubScreen.HUB) {
                        IconButton(onClick = {
                            when (currentSubScreen) {
                                SettingsSubScreen.SERIES_EDITOR ->
                                    if (editorEnteredDirectly) onBack() else currentSubScreen = SettingsSubScreen.SERIES_LIST
                                SettingsSubScreen.STATION_EDITOR ->
                                    if (editorEnteredDirectly) onBack() else currentSubScreen = SettingsSubScreen.STATION_LIST
                                SettingsSubScreen.PARTNER_EDITOR ->
                                    currentSubScreen = SettingsSubScreen.PARTNER_LIST
                                else -> if (enteredDirectly) onBack() else currentSubScreen = SettingsSubScreen.HUB
                            }
                        }) {
                            Icon(
                                painter = painterResource(com.z_company.core.R.drawable.ic_arrow_back),
                                contentDescription = "Назад",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                title = {
                    val titleText = when (currentSubScreen) {
                        SettingsSubScreen.SERIES_EDITOR -> {
                            if (selectedSeriesId != null)
                                seriesListViewModel?.seriesFlow?.collectAsState()?.value
                                    ?.find { it.seriesId == selectedSeriesId }?.name
                                    ?: "Новая серия"
                            else "Новая серия"
                        }
                        else -> currentSubScreen.title
                    }
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors().copy(
                    containerColor = Color.Transparent,
                )
            )
        }
    ) { paddingValues ->

        LaunchedEffect(settingsUiState.uploadState) {
            if (settingsUiState.uploadState is ResultState.Error) {
                scope.launch {
                    snackbarHostState.showSnackbar("Ошибка выгрузки данных. \n${settingsUiState.uploadState.entity.message}.")
                }
                resetUploadState()
            }
            if (settingsUiState.uploadState is ResultState.Success) {
                scope.launch {
                    if (settingsUiState.uploadState.data == 0) {
                        snackbarHostState.showSnackbar("Все маршруты синхронизированы")
                    } else {
                        snackbarHostState.showSnackbar("Маршруты успешно сохранены на сервере. (${settingsUiState.uploadState.data})")
                    }
                }
                resetUploadState()
            }
        }

        LaunchedEffect(settingsUiState.downloadState) {
            if (settingsUiState.downloadState is ResultState.Error) {
                scope.launch {
                    snackbarHostState.showSnackbar("Ошибка загрузки данных. \n${settingsUiState.downloadState.entity.message}.")
                }
                resetDownloadState()
            }
            if (settingsUiState.downloadState is ResultState.Success) {
                scope.launch {
                    snackbarHostState.showSnackbar("Маршруты загружены. (${settingsUiState.downloadState.data})")
                }
                resetUploadState()
            }
        }

        LaunchedEffect(settingsUiState.logOutState) {
            if (settingsUiState.logOutState is ResultState.Success) {
                logOut()
            }
        }

        PullToSyncContainer(
            isRefreshing = isPullRefreshing,
            onRefresh = onPullRefresh,
            modifier = Modifier.padding(paddingValues),
        ) {
            Box(Modifier.fillMaxSize()) {
                currentSettings?.let { settings ->
                AnimatedContent(
                    targetState = currentSubScreen,
                    transitionSpec = {
                        // Направление зависит от «глубины» экранов: уходим глубже
                        // (HUB → список → редактор) — въезд справа; возвращаемся
                        // назад (редактор → список → HUB) — въезд слева.
                        if (targetState.depth < initialState.depth) {
                            slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                        } else {
                            slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                        }
                    },
                    label = "settings_sub_screen"
                ) { screen ->
                    when (screen) {
                        SettingsSubScreen.HUB -> {
                            val seriesRecords =
                                seriesListViewModel?.seriesFlow?.collectAsState()?.value ?: emptyList()
                            val stationRecords =
                                stationListViewModel?.stationsFlow?.collectAsState()?.value ?: emptyList()
                            val partnerRecords =
                                partnerListViewModel?.partnersFlow?.collectAsState()?.value ?: emptyList()
                            SettingsHubContent(
                                currentSettings = settings,
                                seriesTotalCount = seriesUnionCount(settings.locomotiveSeriesList, seriesRecords.map { it.name }),
                                stationTotalCount = seriesUnionCount(settings.stationList, stationRecords.map { it.name }),
                                partnerTotalCount = partnerRecords.size,
                                onNavigate = { currentSubScreen = it },
                                showSettingSalary = showSettingSalary,
                            )
                        }

                        SettingsSubScreen.NORMA -> {
                            SettingsNormaContent(
                                currentSettings = settings,
                                showAbsenceScreen = showAbsenceScreen,
                                timeZoneRussiaList = timeZoneRussiaList,
                                setTimeZone = setTimeZone,
                                setCountry = viewModel::changeCountry,
                                countryLoadingState = settingsUiState.countryLoadingState,
                                onDismissCountryDialog = viewModel::clearCountryLoadingState,
                                setCrossMonthTimezone = viewModel::setCrossMonthTimezone,
                                regionsForCountry = viewModel.regionsForCountry.collectAsState().value,
                                isRegionsLoading = viewModel.isRegionsLoading.collectAsState().value,
                                setRegion = viewModel::changeRegion,
                                regionLoadingState = settingsUiState.regionLoadingState,
                                onDismissRegionDialog = viewModel::clearRegionLoadingState,
                                normaHours = settingsUiState.normaHours,
                            )
                        }

                        SettingsSubScreen.ACCOUNTING -> {
                            SettingsAccountingContent(
                                currentSettings = settings,
                                changeStartNightTime = changeStartNightTime,
                                changeEndNightTime = changeEndNightTime,
                                changeConsiderFutureRoute = changeConsiderFutureRoute,
                            )
                        }

                        SettingsSubScreen.REST -> {
                            SettingsRestContent(
                                currentSettings = settings,
                                restTimeChanged = restTimeChanged,
                                homeRestTimeChanged = homeRestTimeChanged,
                            )
                        }

                        SettingsSubScreen.ROUTE -> {
                            SettingsRouteContent(
                                currentSettings = settings,
                                viewModel = viewModel,
                                changeUsingDefaultWorkTime = changeUsingDefaultWorkTime,
                                workTimeChanged = workTimeChanged,
                                changeShowBreak = changeShowBreak,
                            )
                        }

                        SettingsSubScreen.ROUTE_FORM -> {
                            SettingsRouteFormContent(
                                currentSettings = settings,
                                changeShowBreak = changeShowBreak,
                                changeShowOnePersonSwitch = changeShowOnePersonSwitch,
                                changeShowLocomotive = changeShowLocomotive,
                                changeShowTrain = changeShowTrain,
                                changeShowPassenger = changeShowPassenger,
                                changeShowOtherWork = changeShowOtherWork,
                                changeShowPartner = changeShowPartner,
                            )
                        }

                        SettingsSubScreen.SHOULDERS -> {
                            SettingsShouldersContent(
                                settingsUiState = settingsUiState,
                                servicePhases = servicePhases,
                                stationList = settings.stationList,
                                showDialogAddServicePhase = showDialogAddServicePhase,
                                hideDialogAddServicePhase = hideDialogAddServicePhase,
                                addServicePhase = addServicePhase,
                                deleteServicePhase = deleteServicePhase,
                                updateServicePhase = updateServicePhase,
                                onDeleteStationName = viewModel::removeStationName,
                            )
                        }

                        SettingsSubScreen.LOCOMOTIVE -> {
                            SettingsLocoContent(
                                currentSettings = settings,
                                changeShowLocoHeating = changeShowLocoHeating,
                                changeShowLocoAuxiliary = changeShowLocoAuxiliary,
                                changeShowOtherCurrent = changeShowOtherCurrent,
                                setDefaultLocoType = viewModel::setDefaultLocoType,
                            )
                        }

                        SettingsSubScreen.SERIES_LIST -> {
                            seriesListViewModel?.let { vm ->
                                SettingsSeriesListContent(
                                    viewModel = vm,
                                    legacyNames = settings.locomotiveSeriesList,
                                    onOpenEditor = { id ->
                                        selectedSeriesId = id
                                        prefillSeriesName = null
                                        editorEnteredDirectly = false
                                        currentSubScreen = SettingsSubScreen.SERIES_EDITOR
                                    },
                                    onOpenLegacy = { name ->
                                        selectedSeriesId = null
                                        prefillSeriesName = name
                                        editorEnteredDirectly = false
                                        currentSubScreen = SettingsSubScreen.SERIES_EDITOR
                                    }
                                )
                            }
                        }

                        SettingsSubScreen.SERIES_EDITOR -> {
                            val editorVm = koinViewModel<SeriesEditorViewModel>(
                                key = "series_editor_${selectedSeriesId ?: prefillSeriesName ?: "new"}",
                                parameters = { parametersOf(selectedSeriesId, prefillSeriesName) }
                            )
                            SettingsSeriesEditorContent(
                                viewModel = editorVm,
                                onDone = {
                                    if (editorEnteredDirectly) onBack()
                                    else currentSubScreen = SettingsSubScreen.SERIES_LIST
                                }
                            )
                        }

                        SettingsSubScreen.STATION_LIST -> {
                            stationListViewModel?.let { vm ->
                                SettingsStationListContent(
                                    viewModel = vm,
                                    legacyNames = settings.stationList,
                                    onOpenEditor = { id ->
                                        selectedStationId = id
                                        prefillStationName = null
                                        editorEnteredDirectly = false
                                        currentSubScreen = SettingsSubScreen.STATION_EDITOR
                                    },
                                    onOpenLegacy = { name ->
                                        selectedStationId = null
                                        prefillStationName = name
                                        editorEnteredDirectly = false
                                        currentSubScreen = SettingsSubScreen.STATION_EDITOR
                                    }
                                )
                            }
                        }

                        SettingsSubScreen.STATION_EDITOR -> {
                            val editorVm = koinViewModel<StationNormEditorViewModel>(
                                key = "station_editor_${selectedStationId ?: prefillStationName ?: "new"}",
                                parameters = { parametersOf(selectedStationId, prefillStationName) }
                            )
                            SettingsStationEditorContent(
                                viewModel = editorVm,
                                onDone = {
                                    if (editorEnteredDirectly) onBack()
                                    else currentSubScreen = SettingsSubScreen.STATION_LIST
                                }
                            )
                        }

                        SettingsSubScreen.PARTNER_LIST -> {
                            partnerListViewModel?.let { vm ->
                                SettingsPartnerListContent(
                                    viewModel = vm,
                                    onOpenEditor = { id ->
                                        selectedPartnerId = id
                                        currentSubScreen = SettingsSubScreen.PARTNER_EDITOR
                                    }
                                )
                            }
                        }

                        SettingsSubScreen.PARTNER_EDITOR -> {
                            val editorVm = koinViewModel<PartnerEditorViewModel>(
                                key = "partner_editor_${selectedPartnerId ?: "new"}",
                                parameters = { parametersOf(selectedPartnerId) }
                            )
                            SettingsPartnerEditorContent(
                                viewModel = editorVm,
                                onDone = { currentSubScreen = SettingsSubScreen.PARTNER_LIST }
                            )
                        }

                    }
                }
                }
            }
        }
    }
}

@Composable
private fun SettingsHubContent(
    currentSettings: UserSettings,
    seriesTotalCount: Int,
    stationTotalCount: Int,
    partnerTotalCount: Int,
    onNavigate: (SettingsSubScreen) -> Unit,
    showSettingSalary: () -> Unit,
) {
    val seriesCount = seriesTotalCount
    val stationCount = stationTotalCount
    val partnerCount = partnerTotalCount
    val shouldersCount = currentSettings.servicePhases.size

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp)
            .testTag("settings_scroll_column"),
    ) {
        // ── Справочники норм ──────────────────────────────────────
        SettingsGroupHeader("Справочники норм", top = 4.dp)
        SettingsGroupCard {
            SettingsRow(
                iconRes = com.z_company.route.R.drawable.ic_card_locomotive_ref,
                accent = true,
                title = "Серии локомотивов",
                value = "$seriesCount ${pluralRu(seriesCount, "серия", "серии", "серий")}",
                onClick = { onNavigate(SettingsSubScreen.SERIES_LIST) },
            )
            SettingsRowDivider()
            SettingsRow(
                iconRes = com.z_company.route.R.drawable.location_on_24px,
                accent = true,
                title = "Станции",
                value = "$stationCount ${pluralRu(stationCount, "станция", "станции", "станций")}",
                onClick = { onNavigate(SettingsSubScreen.STATION_LIST) },
            )
            SettingsRowDivider()
            SettingsRow(
                iconRes = com.z_company.route.R.drawable.straighten_24px,
                accent = true,
                title = "Плечи",
                value = "$shouldersCount ${pluralRu(shouldersCount, "плечо", "плечи", "плеч")}",
                onClick = { onNavigate(SettingsSubScreen.SHOULDERS) },
            )
            SettingsRowDivider()
            SettingsRow(
                iconRes = com.z_company.route.R.drawable.ic_card_passenger_ref,
                accent = true,
                title = "Напарники",
                value = "$partnerCount ${pluralRu(partnerCount, "напарник", "напарника", "напарников")}",
                onClick = { onNavigate(SettingsSubScreen.PARTNER_LIST) },
            )
        }

        // ── Расчёт ────────────────────────────────────────────────
        SettingsGroupHeader("Расчёт")
        SettingsGroupCard {
            SettingsRow(
                iconRes = com.z_company.route.R.drawable.wallet_24px,
                title = "Зарплата",
                subtitle = "Тарифная ставка и доплаты",
                onClick = showSettingSalary,
            )
            SettingsRowDivider()
            SettingsRow(
                iconRes = com.z_company.route.R.drawable.calendar_today_24px,
                title = "Норма и регион",
                value = countryLabel(currentSettings.country),
                onClick = { onNavigate(SettingsSubScreen.NORMA) },
            )
            SettingsRowDivider()
            SettingsRow(
                iconRes = com.z_company.route.R.drawable.baseline_tune_24,
                title = "Учёт",
                subtitle = "Ночные часы и будущие маршруты",
                onClick = { onNavigate(SettingsSubScreen.ACCOUNTING) },
            )
            SettingsRowDivider()
            SettingsRow(
                iconRes = com.z_company.route.R.drawable.hotel_24px,
                title = "Отдых",
                subtitle = "Нормы домашнего отдыха и в ПО",
                onClick = { onNavigate(SettingsSubScreen.REST) },
            )
        }

        // ── Поездки ───────────────────────────────────────────────
        SettingsGroupHeader("Поездки")
        SettingsGroupCard {
            SettingsRow(
                iconRes = com.z_company.route.R.drawable.settings_24px,
                title = "Основные",
                subtitle = "Значения по умолчанию для новых маршрутов",
                onClick = { onNavigate(SettingsSubScreen.ROUTE) },
            )
            SettingsRowDivider()
            SettingsRow(
                iconRes = com.z_company.route.R.drawable.description_24px,
                title = "Маршрут",
                subtitle = "Поля формы маршрута",
                onClick = { onNavigate(SettingsSubScreen.ROUTE_FORM) },
            )
            SettingsRowDivider()
            SettingsRow(
                iconRes = com.z_company.route.R.drawable.ic_card_locomotive_ref,
                title = "Локомотив",
                subtitle = "Поля ввода показаний и вид тяги",
                onClick = { onNavigate(SettingsSubScreen.LOCOMOTIVE) },
            )
        }

        SettingsAboutCard()
    }
}

// ── Пуантификатор счётчиков: серия / серии / серий ─────────────────
private fun pluralRu(n: Int, one: String, few: String, many: String): String {
    val n10 = n % 10
    val n100 = n % 100
    return when {
        n10 == 1 && n100 != 11 -> one
        n10 in 2..4 && n100 !in 12..14 -> few
        else -> many
    }
}

private fun countryLabel(code: String): String = when (code) {
    "RU" -> "Россия"
    "BY" -> "Беларусь"
    "KZ" -> "Казахстан"
    else -> code
}

// Всего справочных элементов (серий/станций) = записи + «старые» имена из
// legacy-списка, которым не сопоставлена запись (по имени, без учёта регистра).
private fun seriesUnionCount(
    legacyNames: List<String>,
    recordNames: List<String>,
): Int {
    val records = recordNames.map { it.trim().lowercase() }.toSet()
    val legacyOnly = legacyNames
        .map { it.trim() }
        .filter { it.isNotBlank() && it.lowercase() !in records }
        .distinct()
    return recordNames.size + legacyOnly.size
}

// ── UPPERCASE-mono заголовок группы ────────────────────────────────
@Composable
private fun SettingsGroupHeader(text: String, top: Dp = 22.dp) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = top, bottom = 8.dp)
    )
}

// ── Белая карточка группы: тонкая рамка, 16r, без тени ─────────────
@Composable
private fun SettingsGroupCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsRowDivider() {
    HorizontalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

// ── Строка настройки: icon-аватар · заголовок(+sub) · value · шеврон ─
@Composable
private fun SettingsRow(
    iconRes: Int,
    title: String,
    accent: Boolean = false,
    subtitle: String? = null,
    value: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // При крупном шрифте значение/счётчик не помещается справа от заголовка и
        // «съедает» ширину — заголовок рвётся посреди слова. Тогда переносим значение
        // под заголовок (заголовок получает всю ширину). Фиксированный порог — чтобы
        // все пункты меню выглядели одинаково. Стандартный шрифт — значение справа.
        val valueBelow = value != null && LocalDensity.current.fontScale > 1.15f
        val valueContent: @Composable (Modifier) -> Unit = { mod ->
            Text(
                text = value ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (valueBelow) 2 else 1,
                overflow = TextOverflow.Ellipsis,
                modifier = mod,
            )
        }
        SettingsIconAvatar(iconRes = iconRes, accent = accent)
        Spacer(modifier = Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (valueBelow) {
                valueContent(Modifier.padding(top = 2.dp))
            }
        }
        if (value != null && !valueBelow) {
            valueContent(Modifier.padding(start = 10.dp))
        }
        Icon(
            painter = painterResource(com.z_company.core.R.drawable.keyboard_arrow_right_24px),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
            modifier = Modifier
                .padding(start = 4.dp)
                .size(20.dp)
        )
    }
}

// ── Квадратный icon-аватар 34dp: neutral (bgSubtle) / accent (soft) ─
@Composable
private fun SettingsIconAvatar(iconRes: Int, accent: Boolean) {
    val bg = if (accent) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceBright
    val tint = if (accent) MaterialTheme.colorScheme.tertiary
    else MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .size(34.dp)
            .background(color = bg, shape = Shapes.small),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(19.dp)
        )
    }
}

// ── Карточка «О приложении»: логотип · версия · Поддержка(email) ────
@Composable
private fun SettingsAboutCard() {
    SettingsGroupHeader("О приложении")

    val context = LocalContext.current
    val email = "locodriver.app@yandex.ru"
    val versionName = remember {
        try {
            val packageInfo =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(
                        context.packageName,
                        PackageManager.PackageInfoFlags.of(0L)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(context.packageName, 0)
                }
            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    SettingsGroupCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = Shapes.small
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "М",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Spacer(modifier = Modifier.size(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Машинист",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "версия $versionName",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFont),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        SettingsRowDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val mailIntent = Intent(Intent.ACTION_SENDTO).apply {
                        data = "mailto:$email".toUri()
                    }
                    try {
                        context.startActivity(mailIntent)
                    } catch (e: Exception) {
                        val selector = Intent(Intent.ACTION_SENDTO).apply {
                            data = "mailto:".toUri()
                        }
                        context.startActivity(selector)
                    }
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsIconAvatar(
                iconRes = com.z_company.route.R.drawable.ic_mail_24,
                accent = false,
            )
            Spacer(modifier = Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Поддержка",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}
