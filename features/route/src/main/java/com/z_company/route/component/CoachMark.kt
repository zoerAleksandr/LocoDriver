package com.z_company.route.component

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.composed
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.z_company.route.component.servicephase.Pst
import com.z_company.route.component.servicephase.PstIcon
import com.z_company.route.component.servicephase.PstIcons
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
        // Хранилищ на устройстве несколько (экран «Поезд», вложенные экраны): подмешиваем записанное другими, чтобы не затереть.
        shown = shown + prefs.getStringSet(KEY_SHOWN, emptySet()).orEmpty() + id.key
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

fun Modifier.coachAnchor(anchor: CoachAnchor): Modifier = composed {
    // Элемент ушёл с экрана - границы сбрасываются, иначе подсказка осталась бы висеть на старом месте.
    DisposableEffect(anchor) { onDispose { anchor.bounds = null } }
    onGloballyPositioned { anchor.bounds = it.boundsInWindow() }
}

private val BUBBLE_WIDTH: Dp = 300.dp
private val ARROW_HEIGHT: Dp = 8.dp
private val ARROW_HALF: Dp = 9.dp
private val EDGE: Dp = 12.dp
private const val ANCHOR_SETTLE_MS = 600L

/**
 * Облачко подсказки (заголовок + текст) со стрелкой к элементу [anchor] и кнопкой «Понятно» (ONBOARDING_MATERIALS §2).
 * Не блокирует экран (popup без фокуса и без закрытия по нажатию снаружи); закрывается только кнопкой.
 */
@Composable
fun CoachMarkPopup(
    anchor: Rect?,
    title: String,
    text: String,
    onDismiss: () -> Unit,
    tag: String = "coach_mark",
) {
    val target = anchor ?: return
    // Показываем, только когда положение якоря не менялось ~0,6 с: при входе на экран (анимация перехода, раскладка списка)
    // границы сначала «плывут», и облачко мигало бы на промежуточном месте.
    var stable by remember { mutableStateOf(false) }
    LaunchedEffect(target) {
        stable = false
        kotlinx.coroutines.delay(ANCHOR_SETTLE_MS)
        stable = true
    }
    if (!stable) return
    // Элемент прокручен за пределы экрана - не показываем (облачко повисло бы в углу).
    val heightPx = with(LocalDensity.current) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val widthPx = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    if (target.top < 0f || target.bottom > heightPx || target.right < 0f || target.left > widthPx) return
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
    val bubbleColor = Pst.surface
    Popup(
        popupPositionProvider = provider,
        properties = PopupProperties(focusable = false, dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Column(modifier = Modifier.width(bubbleW).testTag(tag)) {
            if (below) Arrow(arrowX, up = true, color = bubbleColor)
            Column(
                modifier = Modifier
                    .shadow(8.dp, RoundedCornerShape(18.dp))
                    .background(bubbleColor, RoundedCornerShape(18.dp))
                    .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    HintBadgeIcon()
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(title, style = Pst.sans.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold), color = Pst.text)
                        Text(text, style = Pst.sans.copy(fontSize = 14.sp, lineHeight = 20.sp), color = Pst.textMuted)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    HintPrimaryButton(OnboardingHints.BUTTON_OK, onDismiss)
                }
            }
            if (!below) Arrow(arrowX, up = false, color = bubbleColor)
        }
    }
}

/** Круглая акцентная метка с «i» - общий знак подсказок (облачко и экскурсия по карте). */
@Composable
internal fun HintBadgeIcon() {
    Box(Modifier.size(32.dp).background(Pst.accentSoft, CircleShape), contentAlignment = Alignment.Center) {
        PstIcon(PstIcons.Info, 18.dp, Pst.accent)
    }
}

/** Основная кнопка подсказок: заливка акцентным цветом приложения. */
@Composable
internal fun HintPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Pst.accent)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Pst.sans.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = Pst.onAccent, maxLines = 1)
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

/**
 * Разовая подсказка на месте для экрана с одной подсказкой: показывается, пока [id] не помечена и пользователь уже видел «Главную».
 * Закрывается «Понятно»; [onShown] можно вызвать из самого элемента (нажали - тоже показ), поэтому хранилище отдаётся наружу.
 */
@Composable
fun OnceCoachMark(
    id: HintId,
    anchor: CoachAnchor,
    title: String,
    text: String,
    store: OnboardingHintsStore = rememberOnboardingHints(),
    blocked: Boolean = false,
    tag: String = "coach_" + id.key,
) {
    if (!OnboardingHints.canShow(id, store.shown, store.homeReached, recording = false, blocked = blocked)) return
    CoachMarkPopup(anchor = anchor.bounds, title = title, text = text, onDismiss = { store.markShown(id) }, tag = tag)
}
