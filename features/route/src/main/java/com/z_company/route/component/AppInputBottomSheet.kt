package com.z_company.route.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Единая нижняя шторка ввода по стандарту `docs/DIALOGS_STANDARD.md`.
 * Всё, что требует ввода/выбора, — это шторка, а не диалог: верхний радиус 28dp,
 * grabber-полоска (dragHandle по умолчанию), то же затемнение, фон `surface`.
 *
 * Одно текстовое поле + первичная кнопка (accent) + «Отмена» (серая).
 * Значение поля хранится внутри; наружу отдаётся через [onConfirm]/[onValueChange].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppInputBottomSheet(
    onDismissRequest: () -> Unit,
    title: String,
    initialValue: String,
    onConfirm: (String) -> Unit,
    confirmText: String = "Сохранить",
    hint: String? = null,
    label: String? = null,
    suffix: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    transform: (String) -> String = { it },
    isValid: (String) -> Boolean = { it.isNotBlank() },
    errorText: String? = null,
    isLoading: Boolean = false,
    onValueChange: ((String) -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState()
    var value by remember { mutableStateOf(initialValue) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val accent = MaterialTheme.colorScheme.tertiary
    val canConfirm = isValid(value) && !isLoading

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 4.dp, bottom = 24.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title, fontSize = 20.sp, fontWeight = FontWeight.W600,
                color = MaterialTheme.colorScheme.primary
            )
            if (hint != null) {
                Text(
                    text = hint, fontSize = 14.sp, lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                value = value,
                onValueChange = {
                    value = transform(it)
                    onValueChange?.invoke(value)
                },
                label = label?.let { { Text(it) } },
                suffix = suffix?.let { { Text(it) } },
                singleLine = singleLine,
                isError = errorText != null,
                supportingText = errorText?.let {
                    {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (canConfirm) onConfirm(value) }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent,
                    focusedLabelColor = accent,
                    cursorColor = accent,
                ),
            )
            Spacer(Modifier.height(2.dp))
            // Первичное действие — accent.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (canConfirm) accent else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(enabled = canConfirm) { onConfirm(value) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = confirmText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        color = if (canConfirm) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // «Отмена» — серая.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onDismissRequest() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Отмена", fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
