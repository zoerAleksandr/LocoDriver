package com.example.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.navigation.NavBackStackEntry
import com.example.core.ResultState
import com.example.domain.navigation.Router
import com.example.route.Const.NULLABLE_ID
import com.example.route.ui.FormNotesScreen
import com.example.route.viewmodel.NotesFormViewModel
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun FormNotesDestination(
    router: Router,
    backStackEntry: NavBackStackEntry
) {
    val basicId = FormNotes.getBasicId(backStackEntry) ?: NULLABLE_ID
    val viewModel = getViewModel<NotesFormViewModel>(
        parameters = { parametersOf(basicId) }
    )
    val formUiState by viewModel.uiState.collectAsState()

//    FormNotesScreen(
//        saveNotesState = ResultState.Loading,
//        currentNotes = viewModel.currentNotes,
//        onBackPressed = router::back,
//        onSaveClick = {},
//        onTrainSaved = router::back,
//        onClearAllField = viewModel::clearAllField,
//        resetSaveState = {},
//        photoListState = mutableStateListOf(),
//        onTextChanged = viewModel::setNotesText,
//        onAddingPhoto = {},
//        onDeletePhoto = {},
//        createPhoto = {
//            router.showCameraScreen(it)
//        },
//        onViewingPhoto = router::showViewingPhotoScreen
//    )
}