package com.z_company.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavBackStackEntry
import com.z_company.domain.navigation.Router
import com.z_company.route.Const.NULLABLE_ID
import com.z_company.route.ui.ViewingImageScreen
import com.z_company.route.viewmodel.ViewingImageViewModel
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ViewingImageDestination(
    router: Router,
    navBackStackEntry: NavBackStackEntry
) {
    val imageId = ViewingImageRoute.getImageId(navBackStackEntry) ?: NULLABLE_ID
    val viewModel = getViewModel<ViewingImageViewModel>(
        parameters = { parametersOf(imageId) }
    )
    val uiState = viewModel.uiState.collectAsState()
    ViewingImageScreen(
        imageState = uiState.value.imageState,
        removeRouteState = uiState.value.removeImageState,
        deletePhoto = viewModel::removePhoto,
        onPhotoDeleting = router::back,
        onBack = router::back,
    )
}