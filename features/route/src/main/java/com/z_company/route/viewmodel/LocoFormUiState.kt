package com.z_company.route.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.z_company.core.ResultState
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.entities.route.Locomotive

data class LocoFormUiState(
    val locoDetailState: ResultState<Locomotive?> = ResultState.Loading(),
    val dieselSectionList: SnapshotStateList<DieselSectionFormState>? = mutableStateListOf(),
    val electricSectionList: SnapshotStateList<ElectricSectionFormState>? = mutableStateListOf(),
    val saveLocoState: ResultState<Unit>? = null,
    val errorMessage: String? = null,
    val refuelDialogShow: Pair<Boolean, Int> = Pair(false, 0),
    val coefficientDialogShow: Pair<Boolean, Int> = Pair(false, 0),
    val settingsState: ResultState<UserSettings?> = ResultState.Loading(),
    val exitFromScreen: Boolean = false,
    val changesHaveState: Boolean = false,
    val confirmExitDialogShow: Boolean = false,
    /** sectionId секции, для которой показываем bottom-sheet подтверждения удаления.
     *  Null — sheet скрыт. */
    val confirmDeleteDieselSectionId: String? = null,
    val confirmDeleteElectricSectionId: String? = null,
    val isExpandedDropDownMenuSeries: Boolean = false,
    var dateAndTimeConverter: DateAndTimeConverter? = null,
    var isShowHeatingCounter: Boolean = false,
    var isShowAuxiliaryCounter: Boolean = false,
    var isShowOtherCurrent: Boolean = false,
    var isShowTime: Boolean = false,
    var isShowResults: Boolean = false,
    var isShowNorma: Boolean = false,
    val isKiloMode: Boolean = false,
    val isShowUpdateHint: Boolean = false,
    val heatingAcceptedText: String = "",
    val heatingDeliveryText: String = "",
    val auxiliaryAcceptedText: String = "",
    val auxiliaryDeliveryText: String = "",
    val norma1Text: String = "",
    val norma2Text: String = "",
)