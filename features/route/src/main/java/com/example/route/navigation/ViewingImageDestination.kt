package com.example.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavBackStackEntry
import com.example.domain.navigation.Router
import com.example.route.Const.NULLABLE_ID
import com.example.route.ui.ViewingImageScreen
import com.example.route.viewmodel.ViewingImageViewModel
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