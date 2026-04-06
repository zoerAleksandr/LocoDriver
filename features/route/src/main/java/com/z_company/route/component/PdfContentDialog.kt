package com.z_company.route.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.z_company.route.util.PdfSections

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfContentDialog(
    onDismiss: () -> Unit,
    onGenerate: (PdfSections) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var includeDetails by rememberSaveable { mutableStateOf(true) }
    var includeSchedule by rememberSaveable { mutableStateOf(true) }
    var includeSalary by rememberSaveable { mutableStateOf(true) }

    AppBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        title = "Содержание PDF",
        contentAfterHeader = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                CheckboxRow(
                    label = "Поездки детально",
                    checked = includeDetails,
                    onCheckedChange = { includeDetails = it }
                )
                CheckboxRow(
                    label = "График",
                    checked = includeSchedule,
                    onCheckedChange = { includeSchedule = it }
                )
                CheckboxRow(
                    label = "Расчётный листок",
                    checked = includeSalary,
                    onCheckedChange = { includeSalary = it }
                )
            }
        },
        actions = listOf(
            BottomSheetAction(text = "Сформировать") {
                onGenerate(
                    PdfSections(
                        includeRouteDetails = includeDetails,
                        includeSchedule = includeSchedule,
                        includeSalary = includeSalary
                    )
                )
            },
        )
    )
}

@Composable
private fun CheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.surfaceContainerLow)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
