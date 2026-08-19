package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.z_company.domain.navigation.Router
import com.z_company.route.ui.AbsenceScreen
import com.z_company.route.viewmodel.AbsenceViewModel

@Composable
fun AbsenceDestination(
    router: Router,
) {
    val viewModel: AbsenceViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.prepareScreen() }

    LaunchedEffect(state.done) {
        if (state.done) router.back()
    }

    AbsenceScreen(
        state = state,
        types = viewModel.types,
        onBack = router::back,
        onDayTap = viewModel::onDayTap,
        onSetType = viewModel::setType,
        onToggleTypePicker = viewModel::toggleTypePicker,
        onShiftMonth = viewModel::shiftMonth,
        onSave = viewModel::save,
    )
}
