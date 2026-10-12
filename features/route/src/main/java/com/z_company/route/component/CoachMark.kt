package com.z_company.route.component

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.z_company.domain.onboarding.HintId
import com.z_company.domain.onboarding.OnboardingHints

/**
 * Хранилище показанных подсказок (SharedPreferences, только на устройстве) и флага «пользователь уже видел Главную».
 * Состояние наблюдаемое: показанная подсказка сразу исчезает с экрана.
 */
@Stable
class OnboardingHintsStore(private val prefs: SharedPreferences) {
    var shown: Set<String> by mutableStateOf(prefs.getStringSet(KEY_SHOWN, emptySet()).orEmpty().toSet())
        private set
    var homeReached: Boolean by mutableStateOf(prefs.getBoolean(KEY_HOME, false))
        private set

    fun markShown(id: HintId) {
        if (id.key in shown) return
        shown = shown + id.key
        prefs.edit().putStringSet(KEY_SHOWN, shown).apply()
    }

    /** Повтор подсказок карты по кнопке «?» не стирает «показано» - просто открывает метки вручную. */
    fun markHomeReached() {
        if (homeReached) return
        homeReached = true
        prefs.edit().putBoolean(KEY_HOME, true).apply()
    }

    companion object {
        const val PREFS_NAME = "onboarding_hints"
        private const val KEY_SHOWN = "shown"
        private const val KEY_HOME = "home_reached"
    }
}

@Composable
fun rememberOnboardingHints(): OnboardingHintsStore {
    val context = LocalContext.current
    return remember {
        OnboardingHintsStore(context.getSharedPreferences(OnboardingHintsStore.PREFS_NAME, Context.MODE_PRIVATE))
    }
}

/** Запоминает границы элемента в окне - к ним привязывается [CoachMarkPopup]. */
@Stable
class CoachAnchor {
    var bounds: Rect? by mutableStateOf(null)
}

@Composable
fun rememberCoachAnchor(): CoachAnchor = remember { CoachAnchor() }

fun Modifier.coachAnchor(anchor: CoachAnchor): Modifier =
    onGloballyPositioned { anchor.bounds = it.boundsInWindow() }

private val BUBBLE_WIDTH: Dp = 280.dp
private val ARROW_HEIGHT: Dp = 8.dp
private val ARROW_HALF: Dp = 9.dp
private val EDGE: Dp = 12.dp

/**
 * Облачко подсказки со стрелкой к элементу [anchor] и кнопкой «Понятно» (ONBOARDING_MATERIALS §2).
 * Не блокирует экран (popup без фокуса и без закрытия по нажатию снаружи); закрывается только кнопкой.
 */
@Composable
fun CoachMarkPopup(
    anchor: Rect?,
    text: String,
    onDismiss: () -> Unit,
    tag: String = "coach_mark",
) {
    val target = anchor ?: return
    // Элемент прокручен за пределы экрана - не показываем (облачко повисло бы в углу).
    val heightPx = with(LocalDensity.current) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    if (target.top < 0f || target.bottom > heightPx) return
    val density = LocalDensity.current
    val screenW = LocalConfiguration.current.screenWidthDp.dp
    val screenH = LocalConfiguration.current.screenHeightDp.dp
    val bubbleW = minOf(BUBBLE_WIDTH, screenW - EDGE * 2)
    val (bubbleWpx, edgePx, arrowHalfPx, gapPx) = with(density) {
        listOf(bubbleW.toPx(), EDGE.toPx(), ARROW_HALF.toPx(), ARROW_HEIGHT.toPx())
    }
    val centerX = (target.left + target.right) / 2f
    val bubbleLeft = centerX.minus(bubbleWpx / 2f)
        .coerceIn(edgePx, (with(density) { screenW.toPx() } - bubbleWpx - edgePx).coerceAtLeast(edgePx))
    val arrowX = (centerX - bubbleLeft).coerceIn(arrowHalfPx + 8f, bubbleWpx - arrowHalfPx - 8f)
    // Элемент в верхней половине экрана - облачко под ним, иначе над ним.
    val below = target.center.y < with(density) { screenH.toPx() } / 2f

    val provider = remember(target, bubbleLeft, below) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val y = if (below) target.bottom.toInt() + 4 else target.top.toInt() - popupContentSize.height - 4
                return IntOffset(bubbleLeft.toInt(), y.coerceAtLeast(0))
            }
        }
    }
    val bubbleColor = MaterialTheme.colorScheme.inverseSurface
    val textColor = MaterialTheme.colorScheme.inverseOnSurface
    Popup(
        popupPositionProvider = provider,
        properties = PopupProperties(focusable = false, dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Column(modifier = Modifier.width(bubbleW).testTag(tag)) {
            if (below) Arrow(arrowX, up = true, color = bubbleColor)
            Surface(shape = RoundedCornerShape(14.dp), color = bubbleColor, shadowElevation = 6.dp) {
                Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)) {
                    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = textColor)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) {
                            Text(OnboardingHints.BUTTON_OK, color = MaterialTheme.colorScheme.inversePrimary)
                        }
                    }
                }
            }
            if (!below) Arrow(arrowX, up = false, color = bubbleColor)
        }
    }
}

@Composable
private fun Arrow(centerPx: Float, up: Boolean, color: androidx.compose.ui.graphics.Color) {
    Canvas(Modifier.fillMaxWidth().height(ARROW_HEIGHT)) {
        val half = ARROW_HALF.toPx()
        val h = size.height
        val path = Path().apply {
            if (up) {
                moveTo(centerPx - half, h); lineTo(centerPx, 0f); lineTo(centerPx + half, h)
            } else {
                moveTo(centerPx - half, 0f); lineTo(centerPx, h); lineTo(centerPx + half, 0f)
            }
            close()
        }
        drawPath(path, color)
    }
}
