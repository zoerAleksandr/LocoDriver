package com.z_company.route.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.z_company.core.ResultState
import com.z_company.core.ui.component.CustomDivider
import com.z_company.core.ui.component.CustomSnackBar
import com.z_company.core.ui.component.customDatePicker.noRippleEffect
import com.z_company.core.ui.theme.Shapes
import com.z_company.domain.entities.setting.ServicePhase
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.route.ui.settings.SettingsAccountingContent
import com.z_company.route.ui.settings.SettingsLocoContent
import com.z_company.route.ui.settings.SettingsNormaContent
import com.z_company.route.ui.settings.SettingsRestContent
import com.z_company.route.ui.settings.SettingsRouteContent
import com.z_company.route.ui.settings.SettingsShouldersContent
import com.z_company.route.viewmodel.SettingsViewModel
import com.z_company.route.viewmodel.SettingsUiState
import com.z_company.route.viewmodel.TimeZoneRussia
import kotlinx.coroutines.launch
import androidx.core.net.toUri

enum class SettingsSubScreen(val title: String) {
    HUB("Настройки"),
    ROUTE("Основные"),
    NORMA("Норма/Регион"),
    ACCOUNTING("Учёт"),
    REST("Отдых"),
    SHOULDERS("Плечи"),
    LOCOMOTIVE("Локомотив")
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
    showReleaseDaySelectScreen: () -> Unit,
    resetUploadState: () -> Unit,
    resetDownloadState: () -> Unit,
    changeStartNightTime: (Int, Int) -> Unit,
    changeEndNightTime: (Int, Int) -> Unit,
    changeUsingDefaultWorkTime: (Boolean) -> Unit,
    changeConsiderFutureRoute: (Boolean) -> Unit,
    changeShowBreak: (Boolean) -> Unit,
    changeShowLocoHeating: (Boolean) -> Unit,
    changeShowLocoAuxiliary: (Boolean) -> Unit,
    changeShowLocoStatistics: (Boolean) -> Unit,
    changeShowLocoNorma: (Boolean) -> Unit,
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
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentSubScreen by remember {
        val initial = when (initialSubScreen) {
            "ROUTE" -> SettingsSubScreen.ROUTE
            "NORMA" -> SettingsSubScreen.NORMA
            "ACCOUNTING" -> SettingsSubScreen.ACCOUNTING
            "REST" -> SettingsSubScreen.REST
            "SHOULDERS" -> SettingsSubScreen.SHOULDERS
            "LOCOMOTIVE" -> SettingsSubScreen.LOCOMOTIVE
            else -> SettingsSubScreen.HUB
        }
        mutableStateOf(initial)
    }

    // Если пользователь попал сразу на под-экран (через deep link из FormLocoScreen
    // и т.п.) — back должен возвращать по backstack, а не в HUB настроек.
    // Если пользователь открыл настройки с HUB и перешёл во вложенный — back возвращает в HUB.
    val enteredDirectly = remember { initialSubScreen != null }

    BackHandler(currentSubScreen != SettingsSubScreen.HUB) {
        if (enteredDirectly) {
            onBack()
        } else {
            currentSubScreen = SettingsSubScreen.HUB
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
                            if (enteredDirectly) {
                                onBack()
                            } else {
                                currentSubScreen = SettingsSubScreen.HUB
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
                    Text(
                        text = currentSubScreen.title,
                        style = if (currentSubScreen == SettingsSubScreen.HUB)
                            MaterialTheme.typography.headlineLarge
                        else MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
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

        Box(Modifier.padding(paddingValues)) {
            currentSettings?.let { settings ->
                AnimatedContent(
                    targetState = currentSubScreen,
                    transitionSpec = {
                        if (targetState == SettingsSubScreen.HUB) {
                            slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                        } else {
                            slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                        }
                    },
                    label = "settings_sub_screen"
                ) { screen ->
                    when (screen) {
                        SettingsSubScreen.HUB -> {
                            SettingsHubContent(
                                onNavigate = { currentSubScreen = it },
                                showSettingSalary = showSettingSalary,
                            )
                        }

                        SettingsSubScreen.NORMA -> {
                            SettingsNormaContent(
                                currentSettings = settings,
                                showReleaseDaySelectScreen = showReleaseDaySelectScreen,
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
                                changeShowLocoStatistics = changeShowLocoStatistics,
                                changeDefaultLocoType = viewModel::changeDefaultLocoType,
                                changeShowLocoNorma = changeShowLocoNorma,
                                changeShowOtherCurrent = changeShowOtherCurrent,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsHubContent(
    onNavigate: (SettingsSubScreen) -> Unit,
    showSettingSalary: () -> Unit,
) {
    val styleHint = MaterialTheme.typography.bodyMedium
    val styleTitle = MaterialTheme.typography.titleSmall
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp)
            .padding(bottom = 24.dp)
            .testTag("settings_scroll_column"),
    ) {
        // СПРАВОЧНИКИ НОРМ
        Text(
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp),
            text = "СПРАВОЧНИКИ НОРМ",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SettingsNavItem(
            title = "Серии локомотивов",
            onClick = { /* TODO: navigate to NormsRoute */ }
        )
        Spacer(modifier = Modifier.height(8.dp))
        SettingsNavItem(
            title = "Станции",
            onClick = { /* TODO: navigate to StationsNormsRoute */ }
        )
        Spacer(modifier = Modifier.height(8.dp))
        SettingsNavItem(
            title = "Плечи",
            onClick = { onNavigate(SettingsSubScreen.SHOULDERS) }
        )

        // РАСЧЁТ
        Text(
            modifier = Modifier.padding(start = 16.dp, top = 22.dp, bottom = 8.dp),
            text = "РАСЧЁТ",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SettingsNavItem(
            title = "Зарплата",
            subtitle = "Тарифы и надбавки",
            onClick = showSettingSalary
        )
        Spacer(modifier = Modifier.height(8.dp))
        SettingsNavItem(
            title = "Норма и регион",
            subtitle = "Часовой пояс, регион и нормы",
            onClick = { onNavigate(SettingsSubScreen.NORMA) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        SettingsNavItem(
            title = "Учёт",
            subtitle = "Ночные часы и будущие маршруты",
            onClick = { onNavigate(SettingsSubScreen.ACCOUNTING) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        SettingsNavItem(
            title = "Отдых",
            onClick = { onNavigate(SettingsSubScreen.REST) }
        )

        // ВНЕШНИЙ ВИД
        Text(
            modifier = Modifier.padding(start = 16.dp, top = 22.dp, bottom = 8.dp),
            text = "ВНЕШНИЙ ВИД",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SettingsNavItem(
            title = "Основные",
            subtitle = "Переключатели и настройки для маршрутов",
            onClick = { onNavigate(SettingsSubScreen.ROUTE) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        SettingsNavItem(
            title = "Локомотив",
            subtitle = "Тяга, расчёты, итоги",
            onClick = { onNavigate(SettingsSubScreen.LOCOMOTIVE) }
        )

        // ПРИЛОЖЕНИЕ
        Text(
            modifier = Modifier.padding(start = 16.dp, top = 22.dp, bottom = 8.dp),
            text = "ПРИЛОЖЕНИЕ",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SettingsNavItem(
            title = "Тема",
            value = "Системная",
            onClick = { /* TODO */ }
        )
        Spacer(modifier = Modifier.height(8.dp))
        SettingsNavItem(
            title = "Уведомления",
            value = "Включены",
            onClick = { /* TODO */ }
        )
        Spacer(modifier = Modifier.height(8.dp))
        SettingsNavItem(
            title = "Резервные копии",
            subtitle = "Каждые 30 мин, очищ. в 22 ч.",
            value = "Вкл",
            onClick = { /* TODO */ }
        )
        Spacer(modifier = Modifier.height(8.dp))
        SettingsNavItem(
            title = "Экспорт данных",
            onClick = { /* TODO */ }
        )

        // ПОДСВЕТКА
        Text(
            modifier = Modifier.padding(start = 16.dp, top = 22.dp, bottom = 8.dp),
            text = "ПОДСВЕТКА",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SettingsNavItem(
            title = "Помощь и FAQ",
            onClick = { /* TODO */ }
        )
        Spacer(modifier = Modifier.height(8.dp))
        SettingsNavItem(
            title = "Написать в поддержку",
            onClick = { /* TODO: email intent */ }
        )
        Spacer(modifier = Modifier.height(8.dp))
        SettingsNavItem(
            title = "Оценить в Google Play",
            onClick = { /* TODO: play store intent */ }
        )

        // О ПРИЛОЖЕНИИ
        Text(
            modifier = Modifier.padding(start = 16.dp, top = 22.dp, bottom = 8.dp),
            text = "О ПРИЛОЖЕНИИ",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 1.dp, shape = Shapes.medium)
                .background(
                    color = MaterialTheme.colorScheme.secondary,
                    shape = Shapes.medium
                )
                .padding(16.dp)
        ) {
            val context = LocalContext.current

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
                            context.packageManager.getPackageInfo(
                                context.packageName,
                                0
                            )
                        }
                    packageInfo.versionName ?: "Unknown"
                } catch (e: Exception) {
                    "Unknown"
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val email = "locodriver.app@yandex.ru"
                Text(
                    text = "Версия приложения",
                    style = styleHint,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = versionName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = primaryColor
                )

                CustomDivider(orientation = Orientation.Horizontal)

                Text(
                    text = "Поддержка",
                    style = styleHint,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                Text(
                    modifier = Modifier.noRippleEffect {
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
                    },
                    text = email,
                    style = MaterialTheme.typography.bodyLarge,
                    color = primaryColor,
                    maxLines = 2,
                    overflow = TextOverflow.Visible
                )
            }
        }
    }
}

@Composable
private fun SettingsNavItem(
    title: String,
    subtitle: String? = null,
    value: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 1.dp, shape = Shapes.medium)
            .background(
                color = MaterialTheme.colorScheme.secondary,
                shape = Shapes.medium
            )
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (!value.isNullOrBlank()) {
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            painter = painterResource(com.z_company.core.R.drawable.keyboard_arrow_right_24px),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}
