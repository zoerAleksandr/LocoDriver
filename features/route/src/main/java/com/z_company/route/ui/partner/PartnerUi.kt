package com.z_company.route.ui.partner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.domain.entities.partner.Partner
import com.z_company.core.ui.theme.Shapes
import com.z_company.route.component.AppAlertDialog
import com.z_company.route.ui.settings.SettingsSwitchRow

/** Инициалы для аватара: первые буквы первых двух слов ФИО, uppercase. */
fun partnerInitials(fullName: String?): String {
    val words = fullName.orEmpty().trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        words.isEmpty() -> "?"
        words.size == 1 -> words[0].take(1).uppercase()
        else -> (words[0].take(1) + words[1].take(1)).uppercase()
    }
}

/**
 * Короткое ФИО для строки маршрута: «Иванов Иван Иванович» → «Иванов И. И.».
 * Фамилия целиком, остальные слова — инициалами с точкой.
 */
fun partnerShortName(fullName: String?): String {
    val words = fullName.orEmpty().trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (words.isEmpty()) return ""
    val surname = words[0]
    val initials = words.drop(1).joinToString(" ") { "${it.take(1).uppercase()}." }
    return if (initials.isBlank()) surname else "$surname $initials"
}

/** Круглый аватар с инициалами (accentSoft фон = primaryContainer, accent текст = tertiary). */
@Composable
fun PartnerAvatar(
    name: String?,
    size: Dp = 34.dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = partnerInitials(name),
            color = MaterialTheme.colorScheme.tertiary,
            fontWeight = FontWeight.SemiBold,
            fontSize = (size.value * 0.4f).sp,
        )
    }
}

private val PermanentChipShape = RoundedCornerShape(50)

/** Отметка «постоянный напарник» в строке справочника / пикера. */
@Composable
fun PermanentPartnerChip(modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.tertiary
    Row(
        modifier = modifier
            .clip(PermanentChipShape)
            .background(MaterialTheme.colorScheme.primaryContainer, PermanentChipShape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(com.z_company.core.R.drawable.ic_star),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(13.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "Постоянный",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = accent,
        )
    }
}

/** Точка в конце фразы — без дубля, если короткое ФИО уже кончается инициалом «И.». */
private fun withPeriod(text: String): String = if (text.endsWith(".")) text else "$text."

/**
 * Предупреждение о замене постоянного напарника: в справочнике может быть
 * только один постоянный, прежний ([current]) перестанет им быть.
 */
@Composable
fun PermanentPartnerReplaceDialog(
    current: Partner,
    newName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val newShort = partnerShortName(newName).ifBlank { "Новый напарник" }
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = "Заменить постоянного напарника?",
        text = "Постоянный напарник сейчас: ${withPeriod(partnerShortName(current.fullName))}\n" +
            "После замены в новые маршруты будет автоматически добавляться ${withPeriod(newShort)}",
        confirmText = "Заменить",
        onConfirm = onConfirm,
        dismissText = "Отмена",
    )
}

/**
 * Блок карточки напарника «Постоянный напарник»: переключатель + диалог
 * подтверждения замены прежнего постоянного ([replacePermanent] != null).
 */
@Composable
fun PermanentPartnerSection(
    isPermanent: Boolean,
    replacePermanent: Partner?,
    fullName: String,
    onCheckedChange: (Boolean) -> Unit,
    onConfirmReplace: () -> Unit,
    onDismissReplace: () -> Unit,
) {
    replacePermanent?.let { current ->
        PermanentPartnerReplaceDialog(
            current = current,
            newName = fullName,
            onConfirm = onConfirmReplace,
            onDismiss = onDismissReplace,
        )
    }
    Column(
        modifier = Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .clip(Shapes.medium)
            .background(MaterialTheme.colorScheme.secondary, Shapes.medium)
    ) {
        SettingsSwitchRow(
            label = "Постоянный напарник",
            sub = "Автоматически добавляется в каждый новый маршрут. Постоянным может быть только один напарник.",
            checked = isPermanent,
            onCheckedChange = onCheckedChange,
        )
    }
}

/**
 * Широкая кнопка под списком пикера: «Сделать постоянным напарником» +
 * короткое ФИО кандидата второй строкой (ФИО не склоняем — род не определить).
 */
@Composable
fun MakePermanentButton(
    partner: Partner,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
            .clip(Shapes.medium)
            .background(MaterialTheme.colorScheme.primaryContainer, Shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(com.z_company.core.R.drawable.ic_star_border),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = "Сделать постоянным напарником",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.tertiary,
            )
            Text(
                text = partnerShortName(partner.fullName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f),
            )
        }
    }
}
