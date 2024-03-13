package com.example.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavBackStackEntry
import com.example.domain.navigation.Router
import com.example.route.Const.NULLABLE_ID
import com.example.route.ui.CreatePhotoScreen
import com.example.route.viewmodel.CreatePhotoViewModel
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
        onSelectPhotosInGallery = viewModel::savePhotoInNotes,
        onPhotoSelected = router::back,
        onCreatePhoto = router::showViewingPhotoScreen,
        onDismissPermission = router::back,
        basicId = basicId
    )
}