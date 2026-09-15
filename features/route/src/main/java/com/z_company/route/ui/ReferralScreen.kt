package com.z_company.route.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.z_company.core.ui.theme.MonoFont
import com.z_company.core.ui.theme.Shapes
import com.z_company.repository.remote_rest.response.ReferralStatusResponse
import com.z_company.route.R
import com.z_company.route.viewmodel.ReferralViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferralScreen(onBack: () -> Unit) {
    val vm: ReferralViewModel = viewModel()
    val status by vm.status.collectAsState()
    val error by vm.error.collectAsState()
    val loading by vm.loading.collectAsState()
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Реферальная программа", fontWeight = FontWeight.SemiBold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(painterResource(R.drawable.keyboard_arrow_left_24px), "Назад") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        )
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ReferralHero()
            when {
                loading && status == null -> ReferralLoadingCard()
                error != null && status == null -> ReferralErrorCard(error.orEmpty(), vm::refresh)
                status != null -> ReferralContent(status!!)
            }
            ReferralRulesCard()
            val context = LocalContext.current
            TextButton(
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://locodriver.ru/referral.html")))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Подробные правила программы")
                Icon(painterResource(R.drawable.keyboard_arrow_right_24px), null, Modifier.padding(start = 4.dp).size(18.dp))
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ReferralHero() {
    AndroidView(
        modifier = Modifier.fillMaxWidth().aspectRatio(360f / 320f).clip(Shapes.medium),
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                overScrollMode = android.view.View.OVER_SCROLL_NEVER
                contentDescription = "Пример: друг впервые оплачивает год Про, каждому добавляется половина оплаченного периода — примерно шесть месяцев"
                settings.apply {
                    javaScriptEnabled = false
                    blockNetworkLoads = true
                    allowFileAccess = false
                    allowContentAccess = false
                }
                val svg = context.assets.open("referral_v3_animated.svg").bufferedReader().use { it.readText() }
                val html = """<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"><style>html,body{margin:0;padding:0;width:100%;height:100%;overflow:hidden;background:transparent}svg{display:block;width:100%;height:100%}</style></head><body>$svg</body></html>"""
                loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            }
        },
        onRelease = { it.stopLoading(); it.destroy() },
    )
}

@Composable
private fun ReferralContent(data: ReferralStatusResponse) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, Shapes.medium), shape = Shapes.medium) {
            Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ВАШ КОД", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(data.code, fontFamily = MonoFont, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Друг вводит его перед своей первой оплатой", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Реферальный код", data.code))
                    }) {
                        Icon(painterResource(R.drawable.outline_content_copy_24), null, Modifier.size(18.dp)); Text("Копировать", Modifier.padding(start = 6.dp))
                    }
                    Button(onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Мой код Машинист Про: ${data.code}")
                        }
                        context.startActivity(Intent.createChooser(send, "Поделиться кодом"))
                    }) {
                        Icon(painterResource(R.drawable.share_24px), null, Modifier.size(18.dp)); Text("Поделиться", Modifier.padding(start = 6.dp))
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ReferralMetric(R.drawable.group_24px, "Приглашено", data.invitedCount.toString(), Modifier.weight(1f))
            ReferralMetric(R.drawable.check_circle_24px, "Начислено", data.rewardedCount.toString(), Modifier.weight(1f))
        }
        data.appliedCode?.let { ReferralAppliedCard(it, data.appliedStatus) }
    }
}

@Composable
private fun ReferralMetric(icon: Int, label: String, value: String, modifier: Modifier) {
    Row(modifier.clip(Shapes.medium).background(MaterialTheme.colorScheme.surfaceVariant).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(icon), null, Modifier.size(24.dp), MaterialTheme.colorScheme.tertiary)
        Column(Modifier.padding(start = 10.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ReferralAppliedCard(code: String, status: String?) {
    val (title, body, tone) = when (status) {
        "rewarded" -> Triple("Бонус начислен", "Код $code успешно сработал", MaterialTheme.colorScheme.surfaceTint)
        "reversed" -> Triple("Бонус отменён", "Оплата по коду $code была возвращена", MaterialTheme.colorScheme.error)
        else -> Triple("Код применён", "$code · ожидает первой оплаты", MaterialTheme.colorScheme.tertiary)
    }
    Row(Modifier.fillMaxWidth().clip(Shapes.medium).background(tone.copy(alpha = 0.08f)).border(1.dp, tone.copy(alpha = 0.28f), Shapes.medium).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(R.drawable.check_circle_24px), null, tint = tone)
        Column(Modifier.padding(start = 12.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = tone)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ReferralRulesCard() {
    Column(Modifier.fillMaxWidth().clip(Shapes.medium).background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
        Text("Как это работает", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        RuleRow("1", "Поделитесь своим кодом с другом")
        RuleRow("2", "Друг вводит код перед своей первой оплатой Про")
        RuleRow("3", "После оплаты половина периода добавится каждому")
        Text("Код можно применить после регистрации, если подписка ещё ни разу не оплачивалась. За повторные покупки бонус не начисляется.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RuleRow(number: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(24.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Text(number, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
        Text(text, Modifier.padding(start = 11.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ReferralLoadingCard() {
    Box(Modifier.fillMaxWidth().height(150.dp).clip(Shapes.medium).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
private fun ReferralErrorCard(message: String, retry: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(Shapes.medium).background(MaterialTheme.colorScheme.errorContainer).padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message, color = MaterialTheme.colorScheme.onErrorContainer, textAlign = TextAlign.Center)
        Button(onClick = retry, modifier = Modifier.padding(top = 12.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
            Icon(painterResource(R.drawable.sync_24px), null); Text("Повторить", Modifier.padding(start = 8.dp))
        }
    }
}
