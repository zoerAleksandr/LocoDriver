package com.z_company.route.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.z_company.route.viewmodel.ReferralViewModel

@Composable
fun ReferralScreen(onBack: () -> Unit) {
    val vm: ReferralViewModel = viewModel()
    val status by vm.status.collectAsState()
    val error by vm.error.collectAsState()
    val loading by vm.loading.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Реферальная программа", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack) { Text("Назад") }
        }
        Text("Пригласите друга. Если он ещё ни разу не оплачивал Машинист Про, он сможет ввести ваш код перед первой оплатой. После оплаты каждый из вас получит половину срока оплаченного тарифа дополнительно.")
        Text("Код можно ввести и после регистрации, пока первая оплата ещё не совершена. За повторные покупки бонус не начисляется.")
        if (loading) Text("Загружаем код…")
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        status?.let { data ->
            Text("Ваш код: ${data.code}", style = MaterialTheme.typography.titleLarge)
            Button(onClick = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "Мой код Машинист Про: ${data.code}")
                }
                context.startActivity(Intent.createChooser(send, "Поделиться кодом"))
            }) { Text("Поделиться") }
            Text("Приглашений: ${data.invitedCount}. Начислено бонусов: ${data.rewardedCount}.")
            data.appliedCode?.let { Text("Вы применили код $it: ${if (data.appliedStatus == "rewarded") "бонус начислен" else "ожидает первой оплаты"}") }
        }
        if (error != null) TextButton(onClick = vm::refresh) { Text("Повторить") }
    }
}
