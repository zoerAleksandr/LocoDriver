package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.z_company.domain.navigation.Router
import com.z_company.route.ui.ScheduleWizardScreen
import com.z_company.route.viewmodel.ScheduleWizardViewModel

@Composable
fun ScheduleWizardDestination(
    router: Router,
) {
    val viewModel: ScheduleWizardViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.prepareScreen() }

    // После успешного «Применить» — вернуться на Календарь.
    LaunchedEffect(state.done) {
        if (state.done) router.back()
    }

    // Не хватает бесплатного лимита — экран показывает диалог, отсюда только
    // переход на покупки (через гейт авторизации).
    val showPurchases = rememberShowPurchasesScreen(router)

    ScheduleWizardScreen(
        state = state,
        onBack = router::back,
        onSelectPattern = viewModel::selectPattern,
        onDeletePattern = viewModel::deletePattern,
        onSetDayStart = viewModel::setDayStart,
        onSetDayEnd = viewModel::setDayEnd,
        onSetNightStart = viewModel::setNightStart,
        onSetNightEnd = viewModel::setNightEnd,
        onSetFirstDay = viewModel::setFirstDay,
        onShiftMonth = viewModel::shiftMonth,
        onContinuePrevious = viewModel::continuePreviousSchedule,
        onDeclineContinuePrevious = viewModel::declineContinuePrevious,
        onGoToStep = viewModel::goToStep,
        onApply = viewModel::apply,
        onOpenTypePicker = viewModel::openTypePicker,
        onCloseTypePicker = viewModel::closeTypePicker,
        onSetCycleDayType = viewModel::setCycleDayType,
        onAddCycleDay = viewModel::addCycleDay,
        onRemoveCycleDay = viewModel::removeCycleDay,
        onDismissSubscriptionLimit = viewModel::dismissSubscriptionLimit,
        onPurchasesClick = showPurchases,
    )
}
