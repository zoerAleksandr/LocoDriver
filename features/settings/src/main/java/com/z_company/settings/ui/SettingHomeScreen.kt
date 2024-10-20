package com.z_company.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.core.R
import com.z_company.core.ResultState
import com.z_company.core.ui.component.AsyncDataValue
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.settings.viewmodel.SettingHomeScreenUIState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingHomeScreen(
    uiState: SettingHomeScreenUIState,
    onBack: () -> Unit,
    onSaveClick: () -> Unit,
    onSettingSaved: () -> Unit,
    changeIsVisibleNightTime: (Boolean) -> Unit
) {
    val titleStyle = AppTypography.getType().headlineMedium.copy(fontWeight = FontWeight.Light)
    val styleData = AppTypography.getType().titleLarge.copy(fontWeight = FontWeight.Light)
    val styleHint = AppTypography.getType().titleLarge
        .copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Light
        )

    if (uiState.saveSettingState is ResultState.Success) {
        LaunchedEffect(uiState.saveSettingState) {
            onSettingSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_arrow_back),
                        contentDescription = stringResource(id = R.string.cd_back)
                    )
                }
            }, title = {
                Text(
                    text = stringResource(id = R.string.settings_home_screen),
                    style = titleStyle
                )
            }, actions = {
                AsyncDataValue(resultState = uiState.saveSettingState) {
                    TextButton(onClick = onSaveClick) {
                        Text(
                            text = "Сохранить",
                            style = AppTypography.getType().titleLarge
                                .copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Light,
                                    color = MaterialTheme.colorScheme.tertiary
                                ),
                        )
                    }
                }
            },
                colors = TopAppBarDefaults.topAppBarColors().copy(
                    containerColor = Color.Transparent,
                )
            )
        }) {
        Column(
            modifier = Modifier
                .padding(it)
                .padding(start = 12.dp, end = 12.dp, top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = Shapes.medium
                    )
                    .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Работа в ночное время", style = styleData)
                    Switch(
                        checked = uiState.isVisibleNightTime,
                        onCheckedChange = changeIsVisibleNightTime
                    )
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Следование пассажиром", style = styleData)
                    Switch(
                        checked = uiState.isVisibleNightTime,
                        onCheckedChange = changeIsVisibleNightTime
                    )
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Время отвлечения", style = styleData)
                    Switch(
                        checked = uiState.isVisibleNightTime,
                        onCheckedChange = changeIsVisibleNightTime
                    )
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        modifier = Modifier.weight(0.8f),
                        overflow = TextOverflow.Visible,
                        text = "Праздничные часы",
                        style = styleData
                    )
                    Switch(
                        checked = uiState.isVisibleNightTime,
                        onCheckedChange = changeIsVisibleNightTime
                    )
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        modifier = Modifier.weight(0.8f),
                        overflow = TextOverflow.Visible,
                        text = "Время на удлинненных плечах обслуживания",
                        style = styleData
                    )
                    Switch(
                        checked = uiState.isVisibleNightTime,
                        onCheckedChange = changeIsVisibleNightTime
                    )
                }

            }

            Text(
                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                text = "Выбоанные параметры будут отображаться на главном экране если их значение больше 0.",
                style = styleHint,
            )
        }
    }
}