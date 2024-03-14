package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavBackStackEntry
import com.z_company.domain.navigation.Router
import com.z_company.route.Const.NULLABLE_ID
import com.z_company.route.ui.CreatePhotoScreen
import com.z_company.route.viewmodel.CreatePhotoViewModel
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun CreatePhotoDestination(
    router: Router,
    backStackEntry: NavBackStackEntry
) {
    val basicId = CreatePhotoRoute.getBasicId(backStackEntry) ?: NULLABLE_ID
    val viewModel = getViewModel<CreatePhotoViewModel>(
        parameters = { parametersOf(basicId) }
    )
    val formUiState by viewModel.uiState.collectAsState()

    CreatePhotoScreen(
        savePhotoState = formUiState.savePhotoState,
        onSelectPhotosInGallery = viewModel::savePhotoFromGallery,
        onPhotoSelected = router::back,
        onCreatePhoto = router::showPreviewPhotoScreen,
        onDismissPermission = router::back,
        basicId = basicId
    )
}