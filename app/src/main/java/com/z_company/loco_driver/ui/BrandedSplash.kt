package com.z_company.loco_driver.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.loco_driver.R

/**
 * Брендовый сплэш по референсу (design/screenshots/01-splash.png):
 * лок-ап «M» + линия + МАШИНИСТ по центру, слоган внизу.
 * Показывается overlay'ем поверх приложения на старте.
 */
@Composable
fun BrandedSplash() {
    val dark = isSystemInDarkTheme()
    val bg = if (dark) Color(0xFF0F1011) else Color(0xFFFFFFFF)
    val ink = if (dark) Color(0xFFF5F5F5) else Color(0xFF0A0E14)

    Box(modifier = Modifier.fillMaxSize().background(bg)) {
        // Justified-лок-ап по центру: глиф «M», линия и слово МАШИНИСТ — ОДНОЙ ширины.
        // width(IntrinsicSize.Max) → ширина колонки = ширина самого широкого «своего»
        // ребёнка (текст МАШИНИСТ). M (Image) и линия — fillMaxWidth → та же ширина.
        Column(
            modifier = Modifier.align(Alignment.Center).width(IntrinsicSize.Max),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_logo_m),
                contentDescription = null,
                colorFilter = ColorFilter.tint(ink),
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(4.dp).background(ink))
            Spacer(Modifier.height(10.dp))
            Text(
                text = "МАШИНИСТ",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                color = ink,
            )
        }

        // Слоган прижат к низу.
        Text(
            text = "Для тех, у кого всё под контролем.",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = ink.copy(alpha = 0.55f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 48.dp),
        )
    }
}
