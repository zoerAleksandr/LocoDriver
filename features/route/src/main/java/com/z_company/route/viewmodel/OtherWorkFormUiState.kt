package com.z_company.route.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.z_company.core.ResultState
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.entities.route.OtherWork

data class OtherWorkFormUiState(
    val otherWorkDetailState: ResultState<OtherWork?> = ResultState.Loading(),
    val saveOtherWorkState: ResultState<Unit>? = null,
    val errorMessage: String? = null,
    val resultTime: Long? = null,
    val exitFromScreen: Boolean = false,
    val changesHaveState: Boolean = false,
    val confirmExitDialogShow: Boolean = false,
    /** Список станций для автодополнения. */
    val stationList: SnapshotStateList<String> = mutableStateListOf(),
    /** Доступные типы работ: предопределённые + пользовательские. */
    val workTypeList: List<String> = emptyList(),
    /** Список локомотивов для шторки выбора (при нескольких локо); null — шторка скрыта. */
    val locoPicker: List<Locomotive>? = null,
    var dateAndTimeConverter: DateAndTimeConverter? = null
)
