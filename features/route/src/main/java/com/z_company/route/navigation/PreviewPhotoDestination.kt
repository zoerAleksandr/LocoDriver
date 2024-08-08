package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavBackStackEntry
import com.z_company.domain.navigation.Router
import com.z_company.route.Const.NULLABLE_ID
import com.z_company.route.ui.PreviewPhotoScreen
import com.z_company.route.viewmodel.PreviewPhotoViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun PreviewPhotoDestination(
    router: Router,
    backStackEntry: NavBackStackEntry
) {
    val photoUrl = PreviewPhotoRoute.getPhotoUrl(backStackEntry) ?: NULLABLE_ID
    val basicId = PreviewPhotoRoute.getBasicId(backStackEntry) ?: NULLABLE_ID

    val viewModel = koinViewModel<PreviewPhotoViewModel>(
        parameters = { parametersOf(basicId) }
    )
    val uiState = viewModel.uiState.collectAsState()

    PreviewPhotoScreen(
        uri = photoUrl,
        onSavePhoto = viewModel::savePhoto,
        reshoot = router::back,
        onPhotoSaved = router::showRouteForm,
        resetSaveState = viewModel::resetSaveState,
        photoSaveState = uiState.value.savePhotoState,
        basicId = basicId
    )
}