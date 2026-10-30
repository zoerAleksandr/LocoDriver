package com.z_company.route.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
internal fun HomeTrainMap(
    trainId: String,
    basicId: String,
    /** Поезда текущего маршрута: из них строятся чипы-переключатель (меньше двух - чипов нет). */
    routeTrains: List<com.z_company.domain.entities.route.Train> = emptyList(),
    onSelectTrain: (com.z_company.domain.entities.route.Train) -> Unit = {},
    onConsentSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    com.z_company.core.ui.perf.ScreenOpenTrace("train_map")
    // Хранилище на каждый поезд: смена поезда создаёт новое, старое очищается. Окно карты при этом не закрывается:
    // пока новый поезд грузится, остаётся прежний (иначе Главный мигал бы между закрытием и открытием карты).
    val owner = remember(trainId) {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
    val loadingVm = koinViewModel<TrainFormViewModel>(
        viewModelStoreOwner = owner,
        parameters = { parametersOf(trainId, basicId) },
    )
    val loadingState by loadingVm.uiState.collectAsState()
    val loaded = loadingState.trainDetailState is ResultState.Success && loadingVm.currentTrain != null
    var shownVm by remember { mutableStateOf<TrainFormViewModel?>(null) }
    if (loaded && shownVm !== loadingVm) shownVm = loadingVm
    val viewModel = (if (loaded) loadingVm else shownVm) ?: return
    val formUiState by viewModel.uiState.collectAsState()
    if (formUiState.trainDetailState !is ResultState.Success || viewModel.currentTrain == null) return

    val autoRecordManager: AutoRecordManager = koinInject()
    val autoRecordState = com.z_company.route.autorecord.rememberAutoStateFor(autoRecordManager, viewModel.currentTrain?.trainId)
    val onAutoToggle = rememberAutoRecordToggle(
        autoRecordManager, viewModel.currentTrain?.trainId,
    )
    val managerState by autoRecordManager.state.collectAsState()
    val recordingId = if (managerState.enabled) autoRecordManager.targetTrainId() else null
    // Порядок чипов фиксируется при первом показе: открытие поезда перекладывает его в списке маршрута (у поездов без времён
    // порядок берётся из входного списка), и выбранный чип иначе «уезжал» бы на первое место.
    val chipOrder = remember { mutableListOf<String>() }
    val chips = remember(routeTrains, recordingId) {
        val sorted = com.z_company.domain.autorecord.TrainOrdering.sortForDisplay(routeTrains)
        sorted.forEach { if (it.trainId !in chipOrder) chipOrder.add(it.trainId) }
        chipOrder.retainAll(sorted.map { it.trainId }.toSet())
        chipOrder.mapNotNull { id -> sorted.firstOrNull { it.trainId == id } }.mapIndexed { i, t ->
            com.z_company.route.component.servicephase.MapTrainChip(
                id = t.trainId,
                label = t.number?.takeIf { it.isNotBlank() } ?: "Поезд ${i + 1}",
                recording = t.trainId == recordingId,
            )
        }
    }
    val railVm: ServicePhaseAutoFillViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    var showLegPicker by remember { mutableStateOf(false) }
    if (showLegPicker) {
        com.z_company.route.component.servicephase.LegPickerDialog(
            phases = viewModel.sortedServicePhases(formUiState.servicePhaseList),
            selectedId = formUiState.selectedServicePhase?.id,
            onSelect = { viewModel.setSelectedServicePhase(it) },
            onDismiss = { showLegPicker = false },
        )
    }
    TrainMapScreen(
        missingStation = { viewModel.trainMapMissingStation() },
        stops = remember(formUiState.selectedServicePhase, viewModel.fullSelectedPhaseOrNull()) { viewModel.trainMapStops() },
        loadRailLine = { railVm.railLine(it) },
        currentFix = { viewModel.trainMapCurrentFix() },
        loadTrack = { viewModel.trainMapTrack() },
        loadRecordingState = { viewModel.trainMapRecordingState() },
        makeTarget = { viewModel.trainMapMakeTarget() },
        loadConflictText = { viewModel.trainMapConflictText() },
        findStation = { q, near -> railVm.lookup(q, viewModel.userCountry, near) },
        onSetCoordinates = { n, lat, lon -> viewModel.saveStationCoordinates(n, lat, lon) },
        loadZone = { viewModel.trainMapZone() },
        planKey = remember { viewModel.trainMapPlanKey() },
        // Карта остаётся «открытой» (mapTrainId сохраняется): после «Готово» в настройках согласия возвращаемся на неё.
        onOpenConsentSettings = { onConsentSettings() },
        onEnableAuto = { if (!autoRecordState.enabled) onAutoToggle() },
        autoRecording = autoRecordState.enabled,
        onToggleRecording = onAutoToggle,
        onDismiss = onDismiss,
        profileTrains = viewModel.trainMapTrains(),
        profileTrainId = viewModel.currentTrain?.trainId,
        leg = com.z_company.route.component.servicephase.LegContext(
            title = formUiState.selectedServicePhase?.let { "${it.departureStation} — ${it.arrivalStation}" },
            phase = viewModel.fullSelectedPhaseOrNull(),
            country = viewModel.userCountry,
            onSelect = { showLegPicker = true },
            onUpdate = { viewModel.updateServicePhase(it) },
            onAddNames = { viewModel.addStationNames(it) },
            // Автоопределение не справилось (неоднозначные названия): справочник открывается в поезде, здесь выбор плеча.
            onOpenEditor = {},
            editorAvailable = false,
        ),
        trainChips = chips,
        onSelectTrain = { id -> routeTrains.firstOrNull { it.trainId == id }?.let(onSelectTrain) },
    )
}
