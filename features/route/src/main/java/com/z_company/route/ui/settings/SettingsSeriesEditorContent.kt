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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.theme.MonoFont
import com.z_company.core.ui.theme.Shapes
import com.z_company.domain.brake.SeriesPassportField
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.entities.norma_time.SectionNumberingType
import com.z_company.route.R
import com.z_company.route.component.AppInputBottomSheet
import com.z_company.route.component.coachAnchor
import com.z_company.route.viewmodel.SeriesEditorViewModel
import com.z_company.route.viewmodel.SeriesNormField
import kotlinx.coroutines.delay

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsSeriesEditorContent(
    viewModel: SeriesEditorViewModel,
    onDone: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    // Navigate only when deleted
    LaunchedEffect(state.deleted) {
        if (state.deleted) onDone()
    }

    // Шторка подтверждения удаления — как при удалении маршрута.
    var showConfirmDelete by remember { mutableStateOf(false) }
    val deleteSheetState = androidx.compose.material3.rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    if (showConfirmDelete) {
        com.z_company.route.component.AppBottomSheet(
            onDismissRequest = { showConfirmDelete = false },
            sheetState = deleteSheetState,
            headerContent = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Удалить серию?",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    if (state.name.isNotBlank()) {
                        Text(
                            text = state.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            actions = listOf(
                com.z_company.route.component.BottomSheetAction(text = "Да, удалить") {
                    viewModel.delete()
                }
            )
        )
    }

    // Autosave with 500ms debounce on any field change
    LaunchedEffect(
        state.name,
        state.type,
        state.sectionNumberingType,
        state.acceptanceDurationMin,
        state.deliveryDurationMin,
        state.acceptanceHandToHandMin,
        state.deliveryHandToHandMin,
        state.sectionCount,
        state.weightT,
        state.axleCount,
        state.axleLoadTf,
        state.brakeVolumeL,
    ) {
        if (state.name.isNotBlank()) {
            delay(500)
            viewModel.save()
        }
    }

    // Единый диалог ввода значения нормы для любого из четырёх полей
    var dialogField by remember { mutableStateOf<SeriesNormField?>(null) }
    var dialogText by remember { mutableStateOf("") }
    dialogField?.let { field ->
        val title = when (field) {
            SeriesNormField.ACCEPTANCE_PARKING -> "Приёмка · из депо"
            SeriesNormField.ACCEPTANCE_HAND -> "Приёмка · из рук в руки"
            SeriesNormField.DELIVERY_PARKING -> "Сдача · в депо"
            SeriesNormField.DELIVERY_HAND -> "Сдача · из рук в руки"
        }
        AppInputBottomSheet(
            onDismissRequest = { dialogField = null },
            title = title,
            initialValue = dialogText,
            onConfirm = { v ->
                viewModel.setField(field, v.toIntOrNull() ?: 0)
                dialogField = null
            },
            label = "Значение",
            suffix = "мин",
            keyboardType = KeyboardType.Number,
            transform = { it.filter { c -> c.isDigit() }.take(3) },
            // Пусто — «не задано» (как 0).
            isValid = { it.isEmpty() || it.toIntOrNull() != null },
            cursorAtEnd = true,
        )
    }

    // Ввод значения данных локомотива (секции, вес, оси, нажатие, объём ГР)
    var brakeDialog by remember { mutableStateOf<SeriesPassportField?>(null) }
    brakeDialog?.let { field ->
        val initial = when (field) {
            SeriesPassportField.SECTIONS -> state.sectionCount?.toString()
            SeriesPassportField.WEIGHT -> formatSeriesNumber(state.weightT)
            SeriesPassportField.AXLES -> state.axleCount?.toString()
            SeriesPassportField.AXLE_LOAD -> formatSeriesNumber(state.axleLoadTf)
            SeriesPassportField.VOLUME -> state.brakeVolumeL?.toString()
        }.orEmpty()
        val decimal = field == SeriesPassportField.WEIGHT || field == SeriesPassportField.AXLE_LOAD
        AppInputBottomSheet(
            onDismissRequest = { brakeDialog = null },
            title = brakeFieldTitle(field),
            initialValue = initial,
            onConfirm = { v ->
                viewModel.setBrakeField(field, v)
                brakeDialog = null
            },
            label = "Значение",
            suffix = brakeFieldUnit(field),
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
            transform = { raw ->
                if (decimal) {
                    var dot = false
                    raw.replace(',', '.').filter { c ->
                        if (c == '.') (!dot).also { dot = true } else c.isDigit()
                    }.take(6)
                } else raw.filter { it.isDigit() }.take(4)
            },
            // Пусто — «не задано».
            isValid = { it.isEmpty() || (it.toDoubleOrNull() != null) },
            cursorAtEnd = true,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Hint
        Text(
            text = "Для каждой серии задаются нормы приёмки и сдачи. " +
                "Нормы применяются автоматически при заполнении времени в шторке локомотива.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
        )

        // ОСНОВНОЕ
        Text(
            text = "ОСНОВНОЕ",
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
            // Name — BasicTextField без рамки
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "Серия",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 2.dp)
                )
                BasicTextField(
                    value = state.name,
                    onValueChange = viewModel::setName,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp),
                    // Серия — идентификатор (напр. «ВЛ10у») → моноширинный шрифт.
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = MonoFont,
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.primary,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { inner ->
                        if (state.name.isEmpty()) {
                            Text(
                                "Серия",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = MonoFont,
                                    textAlign = TextAlign.End,
                                ),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        inner()
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(start = 16.dp))

            // Type — inline radio (как переключатель «Вид тяги» в «Основных»)
            SettingsRadioRow(
                label = "Электровоз",
                selected = state.type == LocoType.ELECTRIC,
                onClick = { viewModel.setType(LocoType.ELECTRIC) },
            )
            HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
            SettingsRadioRow(
                label = "Тепловоз",
                selected = state.type == LocoType.DIESEL,
                onClick = { viewModel.setType(LocoType.DIESEL) },
            )
        }

        Text(
            text = "НОМЕР СЕКЦИИ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Shapes.medium)
                .background(MaterialTheme.colorScheme.secondary, Shapes.medium)
        ) {
            SectionNumberingChoice(
                label = "1 / 2 / 3",
                selected = state.sectionNumberingType == SectionNumberingType.NUMERIC,
                onClick = { viewModel.setSectionNumberingType(SectionNumberingType.NUMERIC) },
                modifier = Modifier.weight(1f),
            )
            SectionNumberingChoice(
                label = "А / Б / В",
                selected = state.sectionNumberingType == SectionNumberingType.LETTERS,
                onClick = { viewModel.setSectionNumberingType(SectionNumberingType.LETTERS) },
                modifier = Modifier.weight(1f),
            )
        }

        // ПРИЁМКА
        Text(
            text = "ПРИЁМКА",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, Shapes.medium)
                .background(MaterialTheme.colorScheme.secondary, Shapes.medium)
        ) {
            StepperRow(
                label = "Из депо",
                sub = "без бригады",
                value = state.acceptanceDurationMin,
                onIncrement = { viewModel.increment(SeriesNormField.ACCEPTANCE_PARKING) },
                onDecrement = { viewModel.decrement(SeriesNormField.ACCEPTANCE_PARKING) },
                onValueClick = {
                    dialogText = state.acceptanceDurationMin?.takeIf { it > 0 }?.toString() ?: ""
                    dialogField = SeriesNormField.ACCEPTANCE_PARKING
                }
            )
            HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
            StepperRow(
                label = "Из рук в руки",
                value = state.acceptanceHandToHandMin,
                onIncrement = { viewModel.increment(SeriesNormField.ACCEPTANCE_HAND) },
                onDecrement = { viewModel.decrement(SeriesNormField.ACCEPTANCE_HAND) },
                onValueClick = {
                    dialogText = state.acceptanceHandToHandMin?.takeIf { it > 0 }?.toString() ?: ""
                    dialogField = SeriesNormField.ACCEPTANCE_HAND
                }
            )
        }

        // СДАЧА
        Text(
            text = "СДАЧА",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, Shapes.medium)
                .background(MaterialTheme.colorScheme.secondary, Shapes.medium)
        ) {
            StepperRow(
                label = "В депо",
                sub = "без бригады",
                value = state.deliveryDurationMin,
                onIncrement = { viewModel.increment(SeriesNormField.DELIVERY_PARKING) },
                onDecrement = { viewModel.decrement(SeriesNormField.DELIVERY_PARKING) },
                onValueClick = {
                    dialogText = state.deliveryDurationMin?.takeIf { it > 0 }?.toString() ?: ""
                    dialogField = SeriesNormField.DELIVERY_PARKING
                }
            )
            HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
            StepperRow(
                label = "Из рук в руки",
                value = state.deliveryHandToHandMin,
                onIncrement = { viewModel.increment(SeriesNormField.DELIVERY_HAND) },
                onDecrement = { viewModel.decrement(SeriesNormField.DELIVERY_HAND) },
                onValueClick = {
                    dialogText = state.deliveryHandToHandMin?.takeIf { it > 0 }?.toString() ?: ""
                    dialogField = SeriesNormField.DELIVERY_HAND
                }
            )
        }

        // ДАННЫЕ ЛОКОМОТИВА
        Text(
            text = "ДАННЫЕ ЛОКОМОТИВА",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)
        )
        val brakeDataAnchor = com.z_company.route.component.rememberCoachAnchor()
        com.z_company.route.component.OnceCoachMark(
            id = com.z_company.domain.onboarding.HintId.SERIES_BRAKE_DATA,
            anchor = brakeDataAnchor,
            title = com.z_company.domain.onboarding.OnboardingHints.TITLE_SERIES_BRAKE_DATA,
            text = com.z_company.domain.onboarding.OnboardingHints.TEXT_SERIES_BRAKE_DATA,
            blocked = dialogField != null || brakeDialog != null,
        )
        Column(
            modifier = Modifier
                .coachAnchor(brakeDataAnchor)
                .fillMaxWidth()
                .shadow(2.dp, Shapes.medium)
                .background(MaterialTheme.colorScheme.secondary, Shapes.medium)
        ) {
            val rows = listOf(
                SeriesPassportField.SECTIONS to state.sectionCount?.toString(),
                SeriesPassportField.WEIGHT to formatSeriesNumber(state.weightT),
                SeriesPassportField.AXLES to state.axleCount?.toString(),
                SeriesPassportField.AXLE_LOAD to formatSeriesNumber(state.axleLoadTf),
                SeriesPassportField.VOLUME to state.brakeVolumeL?.toString(),
            )
            rows.forEachIndexed { i, (field, text) ->
                if (i > 0) HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                BrakeValueRow(
                    label = brakeFieldTitle(field),
                    value = text,
                    unit = brakeFieldUnit(field),
                    onClick = { brakeDialog = field },
                )
            }
        }
        Text(
            text = "Подставляются в форму локомотива (число секций) и в справку ВУ-45. " +
                "Подсказки заполнены по документам; пусто — данных нет, введите свои.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 4.dp, top = 6.dp)
        )

        // Delete — always visible
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Shapes.medium)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), Shapes.medium)
                .clickable { showConfirmDelete = true }
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
                text = "Удалить серию",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun SectionNumberingChoice(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        color = if (selected) MaterialTheme.colorScheme.tertiary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp),
    )
}

private fun brakeFieldTitle(field: SeriesPassportField): String = when (field) {
    SeriesPassportField.SECTIONS -> "Количество секций"
    SeriesPassportField.WEIGHT -> "Вес"
    SeriesPassportField.AXLES -> "Число осей"
    SeriesPassportField.AXLE_LOAD -> "Нажатие на ось"
    SeriesPassportField.VOLUME -> "Объём ГР"
}

private fun brakeFieldUnit(field: SeriesPassportField): String? = when (field) {
    SeriesPassportField.SECTIONS, SeriesPassportField.AXLES -> null
    SeriesPassportField.WEIGHT -> "т"
    SeriesPassportField.AXLE_LOAD -> "тс"
    SeriesPassportField.VOLUME -> "л"
}

/** 192,0 -> «192», 14,3 -> «14,3»; null -> null. */
private fun formatSeriesNumber(v: Double?): String? = v?.let {
    if (it % 1.0 == 0.0) it.toLong().toString() else it.toString().replace('.', ',')
}

@Composable
private fun BrakeValueRow(label: String, value: String?, unit: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = when {
                value == null -> "—"
                unit != null -> "$value $unit"
                else -> value
            },
            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = MonoFont),
            color = if (value == null) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.primary,
        )
    }
}
