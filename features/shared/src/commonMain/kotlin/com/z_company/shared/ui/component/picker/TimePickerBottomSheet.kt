package com.z_company.shared.ui.component.picker

import androidx.compose.foundation.layout.Column
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
import kotlinx.datetime.LocalTime

/**
 * Time-only picker as a ModalBottomSheet.
 * Accepts and returns millis (hours*3600000 + minutes*60000).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerBottomSheet(
    title: String,
    initialTimeMillis: Long,
    onTimeSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val initHour = ((initialTimeMillis / 3_600_000) % 24).toInt()
    val initMinute = ((initialTimeMillis % 3_600_000) / 60_000).toInt()
    val initTime = remember { LocalTime(initHour, initMinute) }

    var selectedHour by remember { mutableStateOf(initHour) }
    var selectedMinute by remember { mutableStateOf(initMinute) }

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

            MyDefaultWheelTimePicker(
                startTime = initTime,
                modifier = Modifier.padding(vertical = 16.dp),
                height = 160.dp,
                rowCount = 3,
                onSnappedTime = { snappedTime, _ ->
                    when (snappedTime) {
                        is MySnappedTime.Hour -> {
                            selectedHour = snappedTime.localTime.hour
                            selectedMinute = snappedTime.localTime.minute
                        }
                        is MySnappedTime.Minute -> {
                            selectedHour = snappedTime.localTime.hour
                            selectedMinute = snappedTime.localTime.minute
                        }
                    }
                    null
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${selectedHour.toString().padStart(2, '0')}:${selectedMinute.toString().padStart(2, '0')}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val millis = selectedHour.toLong() * 3_600_000L + selectedMinute.toLong() * 60_000L
                    onTimeSelected(millis)
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
