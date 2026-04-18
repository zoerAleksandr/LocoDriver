package com.z_company.route.util

import android.content.Context
import android.content.Intent
import com.z_company.domain.entities.route.Route

/**
 * Данные для share-Intent при отправке маршрута другому пользователю.
 *
 * @property text — основной текст сообщения (заголовок + станции + ссылка).
 * @property subject — тема сообщения вида "Маршрут от 17.04.2026 12:30".
 *   Используется email-клиентами (Gmail, Outlook) как Subject.
 *   Мессенджеры (WhatsApp, Telegram) игнорируют — у них нет понятия темы.
 */
data class ShareLinkData(
    val text: String,
    val subject: String,
) {
    companion object {
        /**
         * Собирает [ShareLinkData] из маршрута и публичной ссылки.
         * Единая точка формирования текста и темы — раньше эта логика
         * была дублирована в HomeViewModel/FormViewModel/AllRouteViewModel.
         */
        fun fromRoute(route: Route, url: String): ShareLinkData {
            val text = buildString {
                append("Маршрут из приложения «Машинист» \uD83D\uDE82")
                append("\n")
                route.basicData.timeStartWork?.let { ms ->
                    val sdf = java.text.SimpleDateFormat(
                        "dd.MM.yyyy HH:mm",
                        java.util.Locale.getDefault()
                    )
                    append(" от ${sdf.format(java.util.Date(ms))}")
                }
                append("\n")
                // Маршрут следования: первая и последняя станции
                val stations = route.trains
                    .flatMap { it.stations }
                    .sortedBy { it.orderIndex }
                val firstStation = stations.firstOrNull()?.stationName?.takeIf { it.isNotBlank() }
                val lastStation = stations.lastOrNull()?.stationName?.takeIf { it.isNotBlank() }
                if (firstStation != null && lastStation != null && firstStation != lastStation) {
                    append(", $firstStation — $lastStation")
                }
                append("\n\n")
                append("Чтобы открыть маршрут, нажмите на ссылку внизу")
                append("\n\n")
                append(url)
            }

            val subject = route.basicData.timeStartWork?.let { ms ->
                val sdf = java.text.SimpleDateFormat(
                    "dd.MM.yyyy HH:mm",
                    java.util.Locale.getDefault()
                )
                "Маршрут от ${sdf.format(java.util.Date(ms))}"
            } ?: "Маршрут"

            return ShareLinkData(text = text, subject = subject)
        }
    }
}

/**
 * Создаёт chooser-Intent для share-sheet с темой и текстом.
 * Email-клиенты увидят [ShareLinkData.subject] как тему письма.
 *
 * @param fromActivity true если intent запускается из Activity (контекст Activity),
 *   false — если из Application context (нужен FLAG_ACTIVITY_NEW_TASK).
 */
fun ShareLinkData.toShareIntent(fromActivity: Boolean = false): Intent {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    val chooser = Intent.createChooser(sendIntent, "Поделиться маршрутом")
    if (!fromActivity) {
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return chooser
}

/** Удобный helper — сразу запустить share-sheet из Context. */
fun Context.startShare(data: ShareLinkData) {
    startActivity(data.toShareIntent(fromActivity = this is android.app.Activity))
}
