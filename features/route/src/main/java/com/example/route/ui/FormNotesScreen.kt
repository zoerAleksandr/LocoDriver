package com.example.route.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.rememberAsyncImagePainter
import com.example.core.ResultState
import com.example.core.ui.theme.Shapes
import com.example.core.ui.theme.custom.AppTypography
import com.example.route.R
import com.example.route.component.BottomShadow
import com.example.route.component.camera.CameraCapturePreview
import com.example.route.extention.EMPTY_IMAGE_URI
import com.example.route.extention.isScrollInInitialState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.maxkeppeker.sheets.core.views.Grid
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormNotesScreen(
    saveNotesState: ResultState<Unit>?,
    currentNotes: String?,
    onBackPressed: () -> Unit,
    onSaveClick: () -> Unit,
    onTrainSaved: () -> Unit,
    onClearAllField: () -> Unit,
    resetSaveState: () -> Unit,
    photoListState: SnapshotStateList<String>,
    onTextChanged: (String) -> Unit,
    onAddingPhoto: (String) -> Unit,
    onDeletePhoto: (String) -> Unit,
    createPhoto: (notesId: String) -> Unit,
    onViewingPhoto: (String) -> Unit
) {
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            confirmValueChange = {
                it != SheetValue.Hidden
            }
        )
    )

    Scaffold(
        modifier = Modifier
            .fillMaxWidth(),
        snackbarHost = {
            SnackbarHost(hostState = scaffoldState.snackbarHostState) { snackBarData ->
                Snackbar(snackBarData)
            }
        },
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        text = "Заметки",
                        style = AppTypography.getType().headlineSmall
                            .copy(color = MaterialTheme.colorScheme.primary)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onBackPressed() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                actions = {
                    ClickableText(
                        text = AnnotatedString(text = "Сохранить"),
                        style = AppTypography.getType().titleMedium,
                        onClick = { onSaveClick() }
                    )
                    var dropDownExpanded by remember { mutableStateOf(false) }

                    IconButton(
                        onClick = {
                            dropDownExpanded = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Меню"
                        )
                        DropdownMenu(
                            expanded = dropDownExpanded,
                            onDismissRequest = { dropDownExpanded = false },
                            offset = DpOffset(x = 4.dp, y = 8.dp)
                        ) {
                            DropdownMenuItem(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                onClick = {
                                    onClearAllField()
                                    dropDownExpanded = false
                                },
                                text = {
                                    Text(
                                        text = "Очистить",
                                        style = AppTypography.getType().bodyLarge
                                    )
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            FormNotesScreenContent(
                notes = currentNotes,
                photoListState = photoListState,
                onTextChanged = onTextChanged,
                onAddingPhoto = onAddingPhoto,
                onDeletePhoto = onDeletePhoto,
                createPhoto = createPhoto,
                onViewingPhoto = onViewingPhoto
            )
        }
    }
}

@Composable
fun FormNotesScreenContent(
    notes: String?,
    photoListState: SnapshotStateList<String>,
    onTextChanged: (String) -> Unit,
    onAddingPhoto: (String) -> Unit,
    onDeletePhoto: (String) -> Unit,
    createPhoto: (notesId: String) -> Unit,
    onViewingPhoto: (String) -> Unit
) {
    val scrollState = rememberLazyListState()

    AnimatedVisibility(
        modifier = Modifier
            .zIndex(1f),
        visible = !scrollState.isScrollInInitialState(),
        enter = fadeIn(animationSpec = tween(durationMillis = 300)),
        exit = fadeOut(animationSpec = tween(durationMillis = 300))
    ) {
        BottomShadow()
    }

    LazyColumn(
        state = scrollState
    ) {
        item {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                value = notes ?: "",
                onValueChange = {
                    onTextChanged(it)
                },
                placeholder = {
                    Text(text = "Введите текст")
                }
            )
        }
        item {
            Column {
                Text(
                    modifier = Modifier.padding(start = 24.dp, bottom = 12.dp),
                    text = "Фотографии"
                )
                val listPhotos = photoListState.toList()
                Grid(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    items = listPhotos,
                    columns = 2,
                    rowSpacing = 12.dp,
                    columnSpacing = 12.dp,
                ) { item ->
                    if (listPhotos.isEmpty() || item == EMPTY_IMAGE_URI) {
                        ItemCameraPreview(
                            createPhoto = { createPhoto("") }
                        )
                    } else {
                        ItemPhoto(
                            photo = item,
                            onDeletePhoto = onDeletePhoto,
                            onViewingPhoto = onViewingPhoto
                        )
                    }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ItemPhoto(
    photo: String,
    onDeletePhoto: (String) -> Unit,
    onViewingPhoto: (String) -> Unit
) {
    val widthScreen = LocalConfiguration.current.screenWidthDp
    val scope = rememberCoroutineScope()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height((widthScreen / 2).dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = Shapes.extraSmall
            ),
        shape = Shapes.extraSmall,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        content = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    painter = rememberAsyncImagePainter(photo),
                    contentScale = ContentScale.Crop,
                    contentDescription = null
                )
                IconButton(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.White.copy(alpha = 0.7f),
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    onClick = {
                        onDeletePhoto(photo.toString())
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Удалить"
                    )
                }
            }
        },
        onClick = { onViewingPhoto(photo) }
    )
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalCoroutinesApi::class)
@Composable
fun ItemCameraPreview(
    createPhoto: () -> Unit
) {
    val widthScreen = LocalConfiguration.current.screenWidthDp
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height((widthScreen / 2).dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = Shapes.extraSmall
            ),
        shape = Shapes.extraSmall,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        onClick = {
            createPhoto()
        }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CameraCapturePreview(
                modifier = Modifier
                    .fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.7f))
            )
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    modifier = Modifier
                        .size(64.dp)
                        .padding(12.dp),
                    painter = painterResource(id = R.drawable.add_a_photo_24px),
                    contentDescription = "Добавить фото"
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Добавить фото",
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}