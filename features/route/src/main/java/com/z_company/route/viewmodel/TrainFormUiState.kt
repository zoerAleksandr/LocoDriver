package com.z_company.route.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.z_company.core.ResultState
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.setting.ServicePhase
import com.z_company.domain.entities.route.Train

data class TrainFormUiState(
    val trainDetailState: ResultState<Train?> = ResultState.Loading(),
    val saveTrainState: ResultState<Unit>? = null,
    val stationsListState: SnapshotStateList<StationFormState>? = mutableStateListOf(),
    val errorMessage: String? = null,
    val exitFromScreen: Boolean = false,
    val changesHaveState: Boolean = false,
    val confirmExitDialogShow: Boolean = false,
    val isExpandedDropDownMenuStation: Pair<Int, Boolean>? = null,
    val isShowDialogSelectServicePhase: Boolean = false,
    val servicePhaseList: SnapshotStateList<ServicePhase> = mutableStateListOf(),
    val selectedServicePhase: ServicePhase? = null,
    var dateAndTimeConverter: DateAndTimeConverter? = null,
    var isStationsReversed: Boolean = false,
    val reorderingStationId: String? = null,
    val editingStationIndex: Int? = null,
    // Индекс станции ПЕРЕД перегоном, который сейчас редактируется в SegmentEditBottomSheet
    // (данные перегона хранятся на station[index + 1]).
    val editingSegmentAfterIndex: Int? = null,
    // Показывать карточки перегонов в списке станций (кнопка над блоком «Маршрут»).
    val showSegments: Boolean = true,
    val showCreateServicePhaseSheet: Boolean = false,
    val suggestedDepartureStation: String = "",
    val suggestedArrivalStation: String = "",
    val confirmDeleteStationIndex: Int? = null,
    val expandedSeriesSectionId: String? = null
)