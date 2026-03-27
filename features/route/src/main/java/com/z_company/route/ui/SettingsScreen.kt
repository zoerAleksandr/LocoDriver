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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    NORMA("Норма"),
    ACCOUNTING("Учёт"),
    REST("Отдых"),
    ROUTE("Маршрут"),
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
    deleteServicePhase: (Int) -> Unit,
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

    BackHandler(currentSubScreen != SettingsSubScreen.HUB) {
        currentSubScreen = SettingsSubScreen.HUB
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
                        IconButton(onClick = { currentSubScreen = SettingsSubScreen.HUB }) {
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
                            )
                        }

                        SettingsSubScreen.LOCOMOTIVE -> {
                            SettingsLocoContent(
                                currentSettings = settings,
                                changeShowLocoHeating = changeShowLocoHeating,
                                changeShowLocoAuxiliary = changeShowLocoAuxiliary,
                                changeShowLocoStatistics = changeShowLocoStatistics,
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
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsNavItem(
            title = "Норма",
            onClick = { onNavigate(SettingsSubScreen.NORMA) }
        )
        SettingsNavItem(
            title = "Учёт",
            onClick = { onNavigate(SettingsSubScreen.ACCOUNTING) }
        )
        SettingsNavItem(
            title = "Отдых",
            onClick = { onNavigate(SettingsSubScreen.REST) }
        )
        SettingsNavItem(
            title = "Маршрут",
            onClick = { onNavigate(SettingsSubScreen.ROUTE) }
        )
        SettingsNavItem(
            title = "Плечи",
            onClick = { onNavigate(SettingsSubScreen.SHOULDERS) }
        )
        SettingsNavItem(
            title = "Локомотив",
            onClick = { onNavigate(SettingsSubScreen.LOCOMOTIVE) }
        )
        SettingsNavItem(
            title = "Зарплата",
            onClick = showSettingSalary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // О приложении
        Text(
            modifier = Modifier.padding(start = 16.dp, bottom = 6.dp),
            text = "О приложении",
            style = styleTitle
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 2.dp, shape = Shapes.medium)
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
                    color = primaryColor.copy(alpha = 0.7f)
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
                    color = primaryColor.copy(alpha = 0.7f)
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
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = Shapes.medium)
            .background(
                color = MaterialTheme.colorScheme.secondary,
                shape = Shapes.medium
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Icon(
            painter = painterResource(com.z_company.core.R.drawable.keyboard_arrow_right_24px),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            modifier = Modifier.size(24.dp)
        )
    }
}
