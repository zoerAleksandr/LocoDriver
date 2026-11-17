package com.z_company.loco_driver.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.z_company.core.util.ConverterLongToTime
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.core.ui.theme.Shapes
import com.z_company.route.component.ItemHomeScreen
import com.z_company.loco_driver.viewmodel.SharedRouteImportState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedRoutesImportBottomSheet(
    state: SharedRouteImportState,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val dateAndTimeConverter = remember(state.userSettings) {
        DateAndTimeConverter(state.userSettings)
    }
    val convertTimeToString: (Long?) -> String = if (state.userSettings.isDecimalTime) {
        { value -> ConverterLongToTime.getTimeInStringDecimalFormat(value) }
    } else {
        { value -> ConverterLongToTime.getTimeInStringFormat(value) }
    }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { target -> !state.isSaving || target != SheetValue.Hidden },
    )
    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 720.dp)
                .padding(horizontal = 20.dp),
        ) {
            Text(
                text = if (state.routes.size == 1) "Получен маршрут" else "Получены маршруты",
                style = MaterialTheme.typography.titleLarge,
            )
            if (state.isLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Text(
                    text = "К импорту: ${state.routes.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 500.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(state.routes, key = { _, route -> route.basicData.id }) { index, route ->
                        ItemHomeScreen(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            route = route,
                            convertTimeToString = convertTimeToString,
                            onRequestDelete = {},
                            containerColor = MaterialTheme.colorScheme.secondary,
                            onClick = {},
                            dateAndTimeConverter = dateAndTimeConverter,
                            number = state.routes.size - index,
                            isInteractive = false,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    enabled = !state.isSaving,
                    modifier = Modifier.weight(1f),
                    shape = Shapes.medium,
                ) {
                    Text("Отмена")
                }
                Button(
                    onClick = onSave,
                    enabled = !state.isLoading && !state.isSaving && state.routes.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                    shape = Shapes.medium,
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.size(8.dp))
                    }
                    Text("Сохранить")
                }
            }
        }
    }
}
