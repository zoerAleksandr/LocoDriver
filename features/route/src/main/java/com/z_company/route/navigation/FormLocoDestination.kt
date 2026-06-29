package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavBackStackEntry
import com.z_company.domain.navigation.Router
import com.z_company.route.Const.NULLABLE_ID
import com.z_company.route.ui.FormLocoScreen
import com.z_company.route.viewmodel.LocoFormViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun FormLocoDestination(
    router: Router,
    backStackEntry: NavBackStackEntry
) {
    val locoId = FormLoco.getLocoId(backStackEntry) ?: NULLABLE_ID
    val basicId = FormLoco.getBasicId(backStackEntry) ?: NULLABLE_ID
    val viewModel = koinViewModel<LocoFormViewModel>(
        parameters = { parametersOf(locoId, basicId) }
    )

    // При возврате из SettingsScreen (LOCOMOTIVE subscreen) перечитываем
    // per-section флаги видимости из SharedPreferences — чтобы изменения,
    // сделанные в SettingsViewModel, мгновенно отразились на экране.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.reloadSettingsFromPrefs()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val formUiState by viewModel.uiState.collectAsState()
    val currentLocomotive by viewModel.currentLoco.collectAsState()
    val locoSeriesList by viewModel.seriesList.collectAsState()
    val electricSectionList by viewModel.electricSectionListState.collectAsState()
    val dieselSectionList by viewModel.dieselSectionListState.collectAsState()
    val userSettings by viewModel.settings.collectAsState()

    FormLocoScreen(
        viewModel = viewModel,
        currentLoco = currentLocomotive,
        dieselSectionListState = dieselSectionList,
        electricSectionListState = electricSectionList,
        onLocoSaved = router::back,
        formUiState = formUiState,
        resetSaveState = viewModel::resetSaveState,
        onNumberChanged = viewModel::setNumber,
        onSeriesChanged = viewModel::setSeries,
        onChangedTypeLoco = viewModel::changeLocoType,
        onStartAcceptedTimeChanged = viewModel::setStartAcceptanceTime,
        onEndAcceptedTimeChanged = viewModel::setEndAcceptanceTime,
        onStartDeliveryTimeChanged = viewModel::setStartDeliveryTime,
        onEndDeliveryTimeChanged = viewModel::setEndDeliveryTime,
        onFuelAcceptedChanged = viewModel::setFuelAccepted,
        onFuelDeliveredChanged = viewModel::setFuelDelivery,
        // Свайп → запрос подтверждения (bottom-sheet "Удалить секцию?"),
        // фактическое удаление в LocoFormViewModel.confirmDelete*Section()
        onDeleteSectionDiesel = viewModel::requestDeleteDieselSection,
        addingSectionDiesel = viewModel::addingSectionDiesel,
        focusChangedDieselSection = viewModel::focusChangedDieselSection,
        onDeleteSectionElectric = viewModel::requestDeleteElectricSection,
        addingSectionElectric = viewModel::addingSectionElectric,
        focusChangedElectricSection = viewModel::focusChangedElectricSection,
        onExpandStateElectricSection = viewModel::isExpandElectricItem,
        onRefuelValueChanged = viewModel::setRefuelDiesel,
        onRefuelInKiloValueChanged = viewModel::setRefuelInKiloDiesel,
        onRefuelCoefficientValueChanged = viewModel::setRefuelCoefficientDiesel,
        onCoefficientValueChanged = viewModel::setCoefficient,
        exitScreen = router::back,
        dropDownSeriesMenuList = viewModel.dropDownSeriesList,
        isExpandedMenu = formUiState.isExpandedDropDownMenuSeries,
        onExpandedMenuChange = viewModel::changeExpandedMenu,
        onChangedContentMenu = viewModel::onChangedDropDownContent,
        onDeleteSeries = viewModel::removeSeries,
        dateAndTimeConverter = formUiState.dateAndTimeConverter,
        userSettings = userSettings,
        onSettingsClick = router::showSettingsLoco,
        onNavigateToSeriesSettings = router::showSettingsSeriesList,
        onNavigateToStationSettings = router::showSettingsStationList,
        onEditStation = router::showSettingsStationEditor,
        onEditSeries = router::showSettingsSeriesEditor,
    )
}