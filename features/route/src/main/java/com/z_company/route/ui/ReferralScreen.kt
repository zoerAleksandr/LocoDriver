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
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ReferralHero() {
    Column(
        Modifier.fillMaxWidth().clip(Shapes.medium).background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.group_24px), null, Modifier.size(20.dp), MaterialTheme.colorScheme.primary)
            }
            Text("Про для вас и друга", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Text("Друг оплачивает Про впервые — половина периода добавляется каждому", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        ReferralBenefitExample()
    }
}

@Composable
private fun ReferralBenefitExample() {
    Column(
        Modifier.fillMaxWidth().padding(top = 3.dp).clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(painterResource(R.drawable.ic_pro_crown), null, Modifier.size(18.dp), MaterialTheme.colorScheme.tertiary)
            Text("Друг покупает 3 месяца Про", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BenefitParticipant("Вам", Modifier.weight(1f))
            BenefitParticipant("Другу", Modifier.weight(1f))
        }
    }
}

@Composable
private fun BenefitParticipant(label: String, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(11.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)).padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(28.dp).clip(RoundedCornerShape(9.dp)).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.person_24px), null, Modifier.size(17.dp), MaterialTheme.colorScheme.onPrimary)
        }
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("+ 1,5 месяца", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
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
