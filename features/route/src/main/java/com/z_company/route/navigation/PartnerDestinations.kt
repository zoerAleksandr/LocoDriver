package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavBackStackEntry
import com.z_company.domain.navigation.Router
import com.z_company.route.Const
import com.z_company.route.ui.PartnerEditScreen
import com.z_company.route.ui.PartnerListMode
import com.z_company.route.ui.PartnersListScreen
import com.z_company.route.viewmodel.PartnerEditorViewModel
import com.z_company.route.viewmodel.PartnerListViewModel
import com.z_company.route.viewmodel.PartnerPickerViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/** Справочник напарников (управление): открывается из Настроек. */
@Composable
fun PartnersManageDestination(router: Router) {
    val viewModel = koinViewModel<PartnerListViewModel>()
    val partners by viewModel.partnersFlow.collectAsState()

    PartnersListScreen(
        mode = PartnerListMode.MANAGE,
        partners = partners,
        selectedIds = emptySet(),
        onBack = router::back,
        onDone = {},
        onRowClick = { partner -> router.showEditPartnerEditor(partner.partnerId) },
        onEdit = { partner -> router.showEditPartnerEditor(partner.partnerId) },
        onDelete = { partner -> viewModel.delete(partner) },
        onAddNew = { router.showNewPartnerEditor() },
    )
}

/** Выбор напарников в маршрут (мультивыбор): открывается из формы маршрута. */
@Composable
fun PartnerPickerDestination(router: Router, backStackEntry: NavBackStackEntry) {
    val basicId = PartnerPickerRoute.getBasicId(backStackEntry) ?: Const.NULLABLE_ID
    val viewModel = koinViewModel<PartnerPickerViewModel>(
        parameters = { parametersOf(basicId) }
    )
    val partners by viewModel.partnersFlow.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()

    PartnersListScreen(
        mode = PartnerListMode.SELECT,
        partners = partners,
        selectedIds = selectedIds,
        onBack = router::back,
        onDone = { viewModel.confirm { router.back() } },
        onRowClick = { partner -> viewModel.toggle(partner.partnerId) },
        onEdit = { partner -> router.showEditPartnerEditor(partner.partnerId) },
        onDelete = { partner -> viewModel.deletePartner(partner) },
        onAddNew = { router.showNewPartnerEditor() },
    )
}

/** Создание/редактирование записи справочника напарников. */
@Composable
fun PartnerEditDestination(router: Router, backStackEntry: NavBackStackEntry) {
    val partnerId = PartnerEditRoute.getPartnerId(backStackEntry)
    val viewModel = koinViewModel<PartnerEditorViewModel>(
        key = "partner_editor_${partnerId ?: "new"}",
        parameters = { parametersOf(partnerId) }
    )
    PartnerEditScreen(
        viewModel = viewModel,
        onDone = router::back,
    )
}
