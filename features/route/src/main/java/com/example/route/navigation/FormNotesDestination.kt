package com.example.route.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavBackStackEntry
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
){
    val notesId = FormNotes.getNotesId(backStackEntry) ?: NULLABLE_ID
    val basicId = FormNotes.getBasicId(backStackEntry) ?: NULLABLE_ID
    val viewModel = getViewModel<NotesFormViewModel>(
        parameters = { parametersOf(notesId, basicId) }
    )
    val formUiState by viewModel.uiState.collectAsState()

    FormNotesScreen(
        notesDetailState = formUiState.notesDetailState,
        saveNotesState = formUiState.saveNotesState,
        currentNotes = viewModel.currentNotes,
        onBackPressed = router::back,
        onSaveClick = viewModel::saveNotes,
        onTrainSaved = router::back,
        onClearAllField = viewModel::clearAllField,
        resetSaveState = viewModel::resetSaveState,
        photoListState = viewModel.photosListState,
        onTextChanged = viewModel::setNoteText,
        onAddingPhoto = viewModel::addingPhoto,
        onDeletePhoto = viewModel::deletePhoto,
        createPhoto = router::showCameraScreen,
        onViewingPhoto = router::showViewingPhotoScreen
    )
}