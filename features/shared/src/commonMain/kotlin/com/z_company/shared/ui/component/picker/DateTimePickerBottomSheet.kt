package com.z_company.shared.ui.component.picker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * KMP-compatible DateTimePicker as a ModalBottomSheet.
 *
 * Uses the wheel picker components for cross-platform date/time selection.
 * Accepts and returns epoch milliseconds (Long) for compatibility with domain entities.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerBottomSheet(
    title: String = "Дата и время",
    onDateTimeSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
    startDateTime: Long,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val initialLdt = remember(startDateTime) {
        Instant.fromEpochMilliseconds(startDateTime).toLocalDateTime(timeZone)
    }

    var selectedDateTime by remember { mutableStateOf(initialLdt) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Title
            if (title.isNotBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                )
            }

            // Wheel DateTime Picker
            MyDefaultWheelDateTimePicker(
                startDateTime = initialLdt,
                modifier = Modifier.padding(vertical = 16.dp),
                height = 160.dp,
                rowCount = 3,
                onSnappedDateTime = { snapped ->
                    selectedDateTime = snapped.snappedLocalDateTime
                    snapped.snappedIndex
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Selected date/time display
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val d = selectedDateTime.dayOfMonth.pad2()
                val m = selectedDateTime.monthNumber.pad2()
                val y = selectedDateTime.year
                val h = selectedDateTime.hour.pad2()
                val min = selectedDateTime.minute.pad2()

                Text(
                    text = "$d.$m.$y",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "$h:$min",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Select button
            Button(
                onClick = {
                    val instant = selectedDateTime.toInstant(timeZone)
                    onDateTimeSelected(instant.toEpochMilliseconds())
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            ) {
                Text(text = "Выбрать")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun Int.pad2(): String = if (this < 10) "0$this" else this.toString()
