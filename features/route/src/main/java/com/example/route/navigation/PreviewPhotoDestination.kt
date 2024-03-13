package com.example.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavBackStackEntry
import com.example.domain.navigation.Router
import com.example.route.Const.NULLABLE_ID
import com.example.route.ui.PreviewPhotoScreen
import com.example.route.viewmodel.PreviewPhotoViewModel
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun PreviewPhotoDestination(
    router: Router,
    backStackEntry: NavBackStackEntry
) {
    val photoUrl = PreviewPhotoRoute.getPhotoUrl(backStackEntry) ?: NULLABLE_ID
    val basicId = PreviewPhotoRoute.getBasicId(backStackEntry) ?: NULLABLE_ID

    val viewModel = getViewModel<PreviewPhotoViewModel>(
        parameters = { parametersOf(photoUrl, basicId) }
    )
    val uiState = viewModel.uiState.collectAsState()

    PreviewPhotoScreen(
        photoUrl = photoUrl,
        onSavePhoto = viewModel::savePhoto,
        reshoot = router::back,
        onPhotoSaved = router::showRouteForm,
        resetSaveState = viewModel::resetSaveState,
        photoSaveState = uiState.value.savePhotoState,
        basicId = basicId
    )
}