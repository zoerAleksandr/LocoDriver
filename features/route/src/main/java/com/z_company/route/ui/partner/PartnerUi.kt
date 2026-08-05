package com.z_company.route.ui.partner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
