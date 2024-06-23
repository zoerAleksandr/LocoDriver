package com.z_company.settings.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.z_company.core.R as CoreR
import com.z_company.core.ui.theme.custom.AppTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectReleaseDaysScreen(
    onBack: () -> Unit,
    onSaveClick: () -> Unit
) {
    Scaffold(topBar = {
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = CoreR.drawable.ic_arrow_back),
                        contentDescription = stringResource(id = CoreR.string.cd_back)
                    )
                }
            },
            title = {
                Text(text = stringResource(id = CoreR.string.norma_hours))
            },
            actions = {
                TextButton(onClick = onSaveClick) {
                    Text(text = "Готово", style = AppTypography.getType().bodyMedium)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors().copy(
                containerColor = Color.Transparent,
            )
        )
    }) {
        LazyColumn(modifier = Modifier.padding(it)) {

        }
    }
}