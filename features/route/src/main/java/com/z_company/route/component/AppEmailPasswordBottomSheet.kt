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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.route.R

/**
 * Нижняя шторка «почта + пароль» по стандарту `docs/DIALOGS_STANDARD.md`.
 * Отличается от [AppInputBottomSheet] только вторым полем: всё остальное —
 * те же радиус 28dp, accent-кнопка и серая «Отмена».
 *
 * Нужна аккаунтам, заведённым через VK ID: у них нет ни почты, ни пароля,
 * и почту нельзя привязать, не задав пароль (иначе аккаунт останется без
 * второго способа входа).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppEmailPasswordBottomSheet(
    onDismissRequest: () -> Unit,
    title: String,
    onConfirm: (email: String, password: String) -> Unit,
    confirmText: String = "Сохранить",
    hint: String? = null,
    isEmailValid: (String) -> Boolean = { it.isNotBlank() },
    minPasswordLength: Int = 4,
    errorText: String? = null,
    isLoading: Boolean = false,
) {
    val sheetState = rememberModalBottomSheetState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val accent = MaterialTheme.colorScheme.tertiary
    val canConfirm = isEmailValid(email) && password.length >= minPasswordLength && !isLoading

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
                value = email,
                onValueChange = { email = it.trim() },
                label = { Text("Почта") },
                singleLine = true,
                isError = errorText != null,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent,
                    focusedLabelColor = accent,
                    cursorColor = accent,
                ),
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = password,
                onValueChange = { password = it },
                label = { Text("Пароль") },
                singleLine = true,
                isError = errorText != null,
                supportingText = {
                    Text(
                        text = errorText ?: "Не короче $minPasswordLength символов",
                        color = if (errorText != null) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            painter = painterResource(
                                if (passwordVisible) R.drawable.outline_visibility_24
                                else R.drawable.outline_visibility_off_24
                            ),
                            contentDescription = if (passwordVisible) "Скрыть пароль" else "Показать пароль",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { if (canConfirm) onConfirm(email, password) }
                ),
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
                    .clickable(enabled = canConfirm) { onConfirm(email, password) }
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
