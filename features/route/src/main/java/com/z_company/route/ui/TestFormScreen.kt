package com.z_company.route.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.z_company.core.ui.component.CustomSnackBar
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.route.component.BottomShadow
import com.z_company.route.extention.isScrollInInitialState
import com.z_company.route.viewmodel.TestFormScreenUIState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestFormScreen(
    uiState: TestFormScreenUIState,
    onBack: () -> Unit,
    setFavoriteState: () -> Unit,
    onNumberChanged: (String) -> Unit,
){
    val snackbarHostState = remember { SnackbarHostState() }
    val route = uiState.route
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Маршрут",
                    )
                }, navigationIcon = {
                    IconButton(onClick = {
                        onBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                }, actions = {
                    route.let { route ->
                        IconButton(onClick = setFavoriteState) {
                            AnimatedContent(targetState = route.basicData.isFavorite, label = "") {
                                Icon(
                                    imageVector = if (it) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null
                                )
                            }
                        }
                    }

                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { snackBarData ->
                CustomSnackBar(snackBarData = snackBarData)
            }
        }
    ) {
        Box(Modifier.padding(it)){
            val scrollState = rememberLazyListState()
            val dataTextStyle = AppTypography.getType().titleLarge

            AnimatedVisibility(
                modifier = Modifier.zIndex(1f),
                visible = !scrollState.isScrollInInitialState(),
                enter = fadeIn(animationSpec = tween(durationMillis = 300)),
                exit = fadeOut(animationSpec = tween(durationMillis = 300))
            ) {
                BottomShadow()
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                state = scrollState,
            ){
                item {
                    OutlinedTextField(
                        modifier = Modifier
                            .padding(vertical = 16.dp)
                            .fillMaxWidth(),
                        value = route.basicData.number ?: "",
                        onValueChange = onNumberChanged,
                        placeholder = {
                            Text(text = "маршрута", style = dataTextStyle)
                        },
                        prefix = {
                            Text(text = "№ ", style = dataTextStyle)
                        },
                        singleLine = true,
                        textStyle = dataTextStyle.copy(
                            color = MaterialTheme.colorScheme.primary,
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        shape = Shapes.medium,
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Number
                        )
                    )
                }
            }
        }
    }
}