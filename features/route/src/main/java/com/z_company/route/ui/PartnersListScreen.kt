package com.z_company.route.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.theme.MonoFont
import com.z_company.core.ui.theme.Shapes
import com.z_company.domain.entities.partner.Partner
import com.z_company.route.component.AppBottomSheet
import com.z_company.route.component.BottomSheetAction
import com.z_company.route.component.SwipeToRevealDelete
import com.z_company.route.ui.partner.PartnerAvatar

enum class PartnerListMode { MANAGE, SELECT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartnersListScreen(
    mode: PartnerListMode,
    partners: List<Partner>,
    selectedIds: Set<String>,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onRowClick: (Partner) -> Unit,
    onEdit: (Partner) -> Unit,
    onDelete: (Partner) -> Unit,
    onAddNew: () -> Unit,
) {
    var partnerForRemove by remember { mutableStateOf<Partner?>(null) }
    var swipeCloseSignal by remember { mutableStateOf(0) }
    val confirmSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    partnerForRemove?.let { partner ->
        AppBottomSheet(
            onDismissRequest = {
                partnerForRemove = null
                swipeCloseSignal++
            },
            sheetState = confirmSheetState,
            headerContent = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Удалить напарника?",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = partner.fullName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            },
            actions = listOf(
                BottomSheetAction(text = "Да, удалить") {
                    onDelete(partner)
                    partnerForRemove = null
                }
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    Text(
                        text = "Напарники",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text(
                            text = "‹",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                actions = {
                    if (mode == PartnerListMode.SELECT) {
                        TextButton(onClick = onDone) {
                            Text(
                                text = "Готово",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            // Нижняя filled-кнопка «Добавить напарника».
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(Shapes.medium)
                        .background(MaterialTheme.colorScheme.primary, Shapes.medium)
                        .clickable { onAddNew() }
                        .padding(vertical = 15.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(com.z_company.core.R.drawable.ic_add),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = "Добавить напарника",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (partners.isEmpty()) {
                PartnersEmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        Text(
                            text = "ВСЕ НАПАРНИКИ · ${partners.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                        )
                    }
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(Shapes.medium)
                                .background(MaterialTheme.colorScheme.secondary, Shapes.medium)
                        ) {
                            partners.forEachIndexed { idx, p ->
                                SwipeToRevealDelete(
                                    itemKey = p.partnerId,
                                    closeSignal = swipeCloseSignal,
                                    compact = true,
                                    onDeleteClick = { partnerForRemove = p },
                                    onContentClick = { onRowClick(p) },
                                ) { _ ->
                                    PartnerRow(
                                        partner = p,
                                        mode = mode,
                                        checked = p.partnerId in selectedIds,
                                        onEdit = { onEdit(p) },
                                    )
                                }
                                if (idx < partners.lastIndex) {
                                    HorizontalDivider(modifier = Modifier.padding(start = 62.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PartnerRow(
    partner: Partner,
    mode: PartnerListMode,
    checked: Boolean,
    onEdit: () -> Unit,
) {
    // Выделение (в режиме выбора) визуализируем лёгким accent-фоном строки.
    val selected = mode == PartnerListMode.SELECT && checked
    val selectionTint = if (selected) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f)
    else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Непрозрачный фон карточки — чтобы красная кнопка «Удалить» не просвечивала.
            .background(MaterialTheme.colorScheme.secondary)
            .background(selectionTint)
            .padding(start = 16.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PartnerAvatar(name = partner.fullName, size = 34.dp)
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = partner.fullName,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            partner.tabNumber?.takeIf { it.isNotBlank() }?.let { tab ->
                Text(
                    text = "таб. $tab",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFont),
                    color = MaterialTheme.colorScheme.tertiary,
                    maxLines = 1,
                )
            }
            // Примечание — отдельной строкой под табельным номером.
            partner.notes?.takeIf { it.isNotBlank() }?.let { note ->
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
        // Карандаш → редактирование карточки этого напарника.
        IconButton(onClick = onEdit) {
            Icon(
                painter = painterResource(com.z_company.core.R.drawable.ic_edit),
                contentDescription = "Редактировать",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun PartnersEmptyState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(com.z_company.route.R.drawable.ic_card_passenger_ref),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(34.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Пока нет напарников",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Добавьте машинистов и помощников, с которыми работаете — их можно будет быстро выбрать в маршруте.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
