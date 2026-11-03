package com.z_company.route.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.domain.onboarding.OnboardingHints
import com.z_company.route.component.servicephase.Pst

/** Шаг экскурсии: заголовок и текст карточки + элемент экрана, который подсвечивается. */
class TourStep(
    val title: String,
    val text: String,
    val anchor: CoachAnchor,
    /** false - элемента сейчас не бывает (другой режим экрана): шаг пропускается. */
    val present: Boolean = true,
    /** Элемент в прокручиваемом списке и сейчас вне экрана: прокрутка к нему, когда шаг становится текущим. */
    val reveal: (suspend () -> Unit)? = null,
)

/**
 * Экскурсия по экрану (ONBOARDING_MATERIALS §2): затемнение с «окном» вокруг реального элемента и карточка шага
 * (заголовок, текст, «n из N», «Назад» / «Далее»). Шаг показывается, только если его элемент сейчас есть на экране,
 * поэтому в подсказках нет ничего, чего не видно. Закрывается «Пропустить» или «Понятно» на последнем шаге;
 * нажатие на затемнение ничего не делает. Рисуется поверх всего содержимого родителя ([Box] на весь экран/окно).
 */
@Composable
fun CoachTour(
    steps: List<TourStep>,
    onDismiss: () -> Unit,
) {
    // Окно экрана и локальные координаты оверлея могут не совпадать (статус-бар): сдвиг убираем.
    var origin by remember { mutableStateOf(Offset.Zero) }
    val visible = steps.filter { s ->
        s.present && (s.reveal != null || s.anchor.bounds?.let { it.width > 0f && it.height > 0f } == true)
    }
    // Границы появляются после первой раскладки; пока ни одного элемента нет - ничего не рисуем (и не гасим «показано»).
    if (visible.isEmpty()) return
    var index by rememberSaveable { mutableIntStateOf(0) }
    val i = index.coerceIn(0, visible.lastIndex)
    val step = visible[i]
    val bounds = step.anchor.bounds
    // Элемент вне экрана (ленивый список) - прокручиваем к нему; до тех пор рисуем только затемнение.
    LaunchedEffect(step, bounds == null) { if (bounds == null) step.reveal?.invoke() }
    val target = bounds?.translate(-origin)
    val last = i == visible.lastIndex
    val padPx = with(LocalDensity.current) { 6.dp.toPx() }
    val accent = Pst.accent

    Box(
        Modifier
            .fillMaxSize()
            .testTag("coach_tour")
            .onGloballyPositioned { origin = it.positionInWindow() }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
    ) {
        Canvas(Modifier.fillMaxSize().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
            drawRect(Color(0xB3000000))
            if (target != null) {
                val hole = Rect(target.left - padPx, target.top - padPx, target.right + padPx, target.bottom + padPx)
                val corner = CornerRadius(18.dp.toPx())
                drawRoundRect(Color.Black, hole.topLeft, Size(hole.width, hole.height), corner, blendMode = BlendMode.Clear)
                drawRoundRect(accent, hole.topLeft, Size(hole.width, hole.height), corner, style = Stroke(2.dp.toPx()))
            }
        }
        if (target == null) return@Box
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val hPx = with(LocalDensity.current) { maxHeight.toPx() }
            val gap = 16.dp
            // Цель может оказаться за краем экрана (прокрутка, устаревшие координаты):
            // отрицательный padding роняет Compose (IllegalArgumentException), поэтому зажимаем в 0.
            val above = target.center.y > hPx / 2f
            val cardMod = if (above) {
                Modifier.align(Alignment.BottomCenter).padding(bottom = with(LocalDensity.current) { (hPx - target.top + padPx).coerceAtLeast(0f).toDp() } + gap)
            } else {
                Modifier.align(Alignment.TopCenter).padding(top = with(LocalDensity.current) { (target.bottom + padPx).coerceAtLeast(0f).toDp() } + gap)
            }
            Column(
                modifier = cardMod
                    .padding(horizontal = 16.dp)
                    .shadow(10.dp, RoundedCornerShape(20.dp))
                    .background(Pst.surface, RoundedCornerShape(20.dp))
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    HintBadgeIcon()
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(step.title, style = Pst.sans.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold), color = Pst.text)
                        Text(step.text, style = Pst.sans.copy(fontSize = 14.sp, lineHeight = 20.sp), color = Pst.textMuted)
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${i + 1} из ${visible.size}", style = Pst.mono.copy(fontSize = 12.sp), color = Pst.textMuted)
                        Box(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            visible.forEachIndexed { k, _ ->
                                Box(Modifier.size(6.dp).background(if (k == i) Pst.accent else Pst.border, CircleShape))
                            }
                        }
                    }
                    if (!last) {
                        Text(
                            OnboardingHints.BUTTON_SKIP,
                            style = Pst.sans.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                            color = Pst.textMuted,
                            modifier = Modifier.clickable(onClick = onDismiss).padding(horizontal = 10.dp, vertical = 10.dp),
                        )
                    }
                    if (i > 0) {
                        Text(
                            OnboardingHints.BUTTON_BACK,
                            style = Pst.sans.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                            color = Pst.accent,
                            modifier = Modifier.clickable { index = i - 1 }.padding(horizontal = 10.dp, vertical = 10.dp),
                        )
                    }
                    HintPrimaryButton(
                        text = if (last) OnboardingHints.BUTTON_OK else OnboardingHints.BUTTON_NEXT,
                        onClick = { if (last) onDismiss() else index = i + 1 },
                    )
                }
            }
        }
    }
}
