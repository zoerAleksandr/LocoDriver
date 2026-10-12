package com.z_company.route.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.z_company.core.ResultState
import com.z_company.route.autorecord.AutoRecordManager
import com.z_company.route.autorecord.rememberAutoRecordToggle
import com.z_company.route.component.servicephase.TrainMapScreen
import com.z_company.route.viewmodel.ServicePhaseAutoFillViewModel
import com.z_company.route.viewmodel.TrainFormViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

/**
 * Карта поезда, открытая НАПРЯМУЮ поверх Главного (плитка «Карта», SCREEN_SPECS §4/§6.9): без перехода на форму
 * «Поезд». Данные - тот же [TrainFormViewModel] с id поезда, но в собственном хранилище, которое очищается
 * при закрытии карты (форма не остаётся «редактором» поезда у «АВТО» после закрытия). Карту показываем, когда
 * поезд загружен (доли секунды), всё остальное - как в [FormTrainScreen].
 */
@Composable
internal fun HomeTrainMap(trainId: String, basicId: String, onConsentSettings: () -> Unit, onDismiss: () -> Unit) {
    com.z_company.core.ui.perf.ScreenOpenTrace("train_map")
    val owner = remember {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
    val viewModel = koinViewModel<TrainFormViewModel>(
        viewModelStoreOwner = owner,
        parameters = { parametersOf(trainId, basicId) },
    )
    val formUiState by viewModel.uiState.collectAsState()
    if (formUiState.trainDetailState !is ResultState.Success || viewModel.currentTrain == null) return

    val autoRecordManager: AutoRecordManager = koinInject()
    val autoRecordState = com.z_company.route.autorecord.rememberAutoStateFor(autoRecordManager, viewModel.currentTrain?.trainId)
    val onAutoToggle = rememberAutoRecordToggle(
        autoRecordManager, viewModel.currentTrain?.trainId, onOpenPrivacy = { onDismiss(); onConsentSettings() },
    )
    val railVm: ServicePhaseAutoFillViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    TrainMapScreen(
        missingStation = { viewModel.trainMapMissingStation() },
        stops = remember(formUiState.selectedServicePhase, formUiState.servicePhaseList.size) { viewModel.trainMapStops() },
        loadRailLine = { railVm.railLine(it) },
        currentFix = { viewModel.trainMapCurrentFix() },
        loadTrack = { viewModel.trainMapTrack() },
        loadRecordingState = { viewModel.trainMapRecordingState() },
        makeTarget = { viewModel.trainMapMakeTarget() },
        loadConflictText = { viewModel.trainMapConflictText() },
        onDetermine = { viewModel.saveStationCoordinatesFromGps(it) },
        loadZone = { viewModel.trainMapZone() },
        planKey = remember { viewModel.trainMapPlanKey() },
        onOpenConsentSettings = { onDismiss(); onConsentSettings() },
        onEnableAuto = { if (!autoRecordState.enabled) onAutoToggle() },
        autoRecording = autoRecordState.enabled,
        onToggleRecording = onAutoToggle,
        onDismiss = onDismiss,
    )
}
