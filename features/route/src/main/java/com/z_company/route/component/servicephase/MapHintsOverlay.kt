package com.z_company.route.component.servicephase

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.domain.onboarding.OnboardingHints
import com.z_company.route.component.CoachAnchor

/**
 * Метки поверх карты при первом открытии (ONBOARDING_MATERIALS §2): 1 - значок положения, 2 - линия пути,
 * 3 - точка впереди, 4 - кнопка «?». Цифры 1 и 4 стоят у реальных кнопок ([positionAnchor], [helpAnchor]);
 * 2 и 3 не привязаны к экрану (линия и точки двигаются с картой) - они только в списке. Закрывается кнопкой
 * «Понятно» или нажатием на затемнение.
 */
@Composable
internal fun BoxScope.MapHintsOverlay(
    positionAnchor: CoachAnchor,
    helpAnchor: CoachAnchor,
    onDismiss: () -> Unit,
) {
    val items = listOf(
        OnboardingHints.TEXT_MAP_POSITION,
        OnboardingHints.TEXT_MAP_LINE,
        OnboardingHints.TEXT_MAP_AHEAD,
        OnboardingHints.TEXT_MAP_HELP,
    )
    Box(
        Modifier
            .fillMaxSize()
            .testTag("map_hints_overlay")
            .background(Color(0x99000000))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
    )
    positionAnchor.bounds?.let { NumberBadge(1, it.left, it.top) }
    helpAnchor.bounds?.let { NumberBadge(4, it.left, it.top) }
    Column(
        modifier = Modifier
            .align(Alignment.Center)
            .padding(horizontal = 24.dp)
            .background(Pst.surface, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items.forEachIndexed { i, text ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                HintBadge(i + 1)
                Text(text = text, style = Pst.sans.copy(fontSize = 14.sp), color = Pst.text, modifier = Modifier.weight(1f))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss) { Text(OnboardingHints.BUTTON_OK, color = Pst.accent) }
        }
    }
}

@Composable
private fun HintBadge(n: Int) {
    Box(Modifier.size(24.dp).background(Pst.accent, CircleShape), contentAlignment = Alignment.Center) {
        Text(n.toString(), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BoxScope.NumberBadge(n: Int, leftPx: Float, topPx: Float) {
    // Метка на левом верхнем углу кнопки, слегка выступая за него.
    val half = with(LocalDensity.current) { 12.dp.toPx() }
    Box(Modifier.offset { IntOffset((leftPx - half).toInt(), (topPx - half).toInt()) }) { HintBadge(n) }
}
