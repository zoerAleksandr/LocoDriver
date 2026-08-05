package com.z_company.route.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.theme.MonoFont
import com.z_company.core.ui.theme.Shapes
import com.z_company.route.R
import com.z_company.route.viewmodel.PartnerEditorViewModel
import kotlinx.coroutines.delay

/**
 * Карточка напарника как под-экран Настроек (аналогично редакторам
 * «Станция»/«Серия»): контент без собственного Scaffold. Автосохранение
 * с debounce — верхняя панель настроек только возвращает назад.
 */
@Composable
fun SettingsPartnerEditorContent(
    viewModel: PartnerEditorViewModel,
    onDone: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.deleted) {
        if (state.deleted) onDone()
    }
    // Автосейв с debounce.
    LaunchedEffect(state.fullName, state.tabNumber, state.notes) {
        if (state.fullName.isNotBlank()) {
            delay(500)
            viewModel.save()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "ДАННЫЕ НАПАРНИКА",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Shapes.medium)
                .background(MaterialTheme.colorScheme.secondary, Shapes.medium)
        ) {
            EditRow(
                label = "ФИО",
                value = state.fullName,
                placeholder = "Фамилия Имя Отчество",
                onValueChange = viewModel::setFullName,
            )
            HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
            EditRow(
                label = "Табельный №",
                value = state.tabNumber,
                placeholder = "1234",
                keyboardType = KeyboardType.Number,
                mono = true,
                onValueChange = viewModel::setTabNumber,
            )
            HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
            EditRow(
                label = "Примечания",
                value = state.notes,
                placeholder = "Плечи, контакты",
                onValueChange = viewModel::setNotes,
            )
        }

        Text(
            text = "Напарник сохранится в справочнике — в следующий раз его можно будет быстро выбрать в маршруте.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 4.dp, top = 8.dp)
        )

        if (state.partnerId != null) {
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Shapes.medium)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), Shapes.medium)
                    .clickable { viewModel.delete() }
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.delete_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Удалить напарника",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun EditRow(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    mono: Boolean = false,
) {
    val valueStyle: TextStyle = MaterialTheme.typography.bodyLarge.copy(
        textAlign = TextAlign.End,
        color = MaterialTheme.colorScheme.primary,
        fontFamily = if (mono) MonoFont else null,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 2.dp)
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
                .heightIn(min = 24.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = valueStyle,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        style = valueStyle.copy(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                inner()
            }
        )
    }
}
