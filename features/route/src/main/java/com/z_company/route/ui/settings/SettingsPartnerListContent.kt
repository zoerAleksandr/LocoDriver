package com.z_company.route.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.z_company.core.ui.theme.MonoFont
import com.z_company.core.ui.theme.Shapes
import com.z_company.domain.entities.partner.Partner
import com.z_company.route.component.AppBottomSheet
import com.z_company.route.component.BottomSheetAction
import com.z_company.route.component.SwipeToRevealDelete
import com.z_company.route.ui.partner.PartnerAvatar
import com.z_company.route.viewmodel.PartnerListViewModel

/**
 * Список напарников как под-экран Настроек (аналогично «Станциям»/«Сериям»):
 * контент без собственного Scaffold — верхнюю панель и нижнее меню даёт
 * общий каркас настроек. Тап по строке или карандашу открывает карточку,
 * свайп влево — удаление с подтверждением.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPartnerListContent(
    viewModel: PartnerListViewModel,
    onOpenEditor: (String?) -> Unit,
) {
    val partners by viewModel.partnersFlow.collectAsState()

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
                    viewModel.delete(partner)
                    partnerForRemove = null
                }
            )
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
        if (partners.isEmpty()) {
            PartnersEmptyState()
        } else {
            Text(
                text = "ВСЕ НАПАРНИКИ · ${partners.size}",
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
                partners.forEachIndexed { idx, p ->
                    SwipeToRevealDelete(
                        itemKey = p.partnerId,
                        closeSignal = swipeCloseSignal,
                        compact = true,
                        onDeleteClick = { partnerForRemove = p },
                        onContentClick = { onOpenEditor(p.partnerId) },
                    ) { _ ->
                        PartnerRow(
                            partner = p,
                            onEdit = { onOpenEditor(p.partnerId) },
                        )
                    }
                    if (idx < partners.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(start = 62.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // «Добавить напарника» — inline-кнопка в стиле списков Станций/Серий.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 2.dp, shape = Shapes.medium)
                .background(color = MaterialTheme.colorScheme.secondary, shape = Shapes.medium)
                .clickable { onOpenEditor(null) }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(com.z_company.core.R.drawable.ic_add),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "  Добавить напарника",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
private fun PartnerRow(
    partner: Partner,
    onEdit: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Непрозрачный фон карточки — чтобы красная кнопка «Удалить» не просвечивала.
            .background(MaterialTheme.colorScheme.secondary)
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
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
