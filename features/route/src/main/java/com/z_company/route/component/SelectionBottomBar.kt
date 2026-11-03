package com.z_company.route.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.z_company.route.R

/**
 * Нижняя панель массовых действий режима «Выбрать» («Все маршруты» и «Мои справки ВУ-45»): слева дополнительные действия [leading]
 * (избранное, поделиться), справа красная корзина.
 */
@Composable
fun SelectionBottomBar(
    isEnabled: Boolean,
    onDeleteClick: () -> Unit,
    deleteContentDescription: String,
    modifier: Modifier = Modifier,
    leading: @Composable RowScope.() -> Unit = {},
) {
    val disabledTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading()
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onDeleteClick, enabled = isEnabled) {
                Icon(
                    painter = painterResource(R.drawable.delete_24px),
                    contentDescription = deleteContentDescription,
                    tint = if (isEnabled) MaterialTheme.colorScheme.error else disabledTint,
                )
            }
        }
    }
}
