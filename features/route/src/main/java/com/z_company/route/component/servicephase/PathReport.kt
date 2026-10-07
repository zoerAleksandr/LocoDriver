package com.z_company.route.component.servicephase

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.net.toUri
import java.util.Locale

/** Адрес поддержки (тот же, что в «Настройках»). */
internal const val SUPPORT_EMAIL = "locodriver.app@yandex.ru"

/** Точка маршрута для письма: название и, если известны, координаты станции (не персональные данные). */
internal data class ReportStop(val name: String, val latitude: Double?, val longitude: Double?)

/**
 * «Сообщить разработчику, что нельзя проложить маршрут»: открывает почтовое приложение с адресом поддержки и
 * готовым текстом (плечо, станции по порядку с координатами) — пользователь дополняет и отправляет сам.
 */
internal fun reportNoPath(context: Context, stops: List<ReportStop>) {
    val route = stops.joinToString(" → ") { it.name }
    val lines = stops.mapIndexed { i, s ->
        val role = when (i) {
            0 -> "отправление"
            stops.lastIndex -> "прибытие"
            else -> "через"
        }
        val coords = if (s.latitude != null && s.longitude != null) {
            " (${String.format(Locale.US, "%.5f, %.5f", s.latitude, s.longitude)})"
        } else ""
        "${i + 1}. $role: ${s.name}$coords"
    }
    val body = buildString {
        appendLine("Здравствуйте!")
        appendLine()
        appendLine("В приложении LocoDriver не получается проложить маршрут по железной дороге: $route.")
        appendLine("Станции по порядку:")
        lines.forEach(::appendLine)
        appendLine()
        appendLine("Как поезд должен идти (по каким станциям/линиям), если знаете:")
        appendLine()
    }
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = "mailto:".toUri()
        putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, "Не удаётся проложить маршрут: $route")
        putExtra(Intent.EXTRA_TEXT, body)
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "Нет почтового приложения. Напишите на $SUPPORT_EMAIL", Toast.LENGTH_LONG).show()
    }
}

internal const val WRITE_TO_SUPPORT = "Написать в поддержку"

/** Сообщение, когда станции без координат нет на карте (SCREEN_SPECS §6.6, «Станция не найдена»). */
internal fun stationNotFoundMessage(name: String) =
    "Станция «$name» не найдена на карте. Напишите в техническую поддержку — мы добавим её."

/**
 * «Написать в поддержку, чтобы добавили станцию»: почта с адресом поддержки, темой «Добавьте станцию: X» и готовым
 * текстом (станция, плечо, регион/дорога если известны) — пользователь дополняет и отправляет сам.
 */
internal fun reportMissingStation(
    context: Context,
    stationName: String,
    departure: String?,
    arrival: String?,
    region: String? = null,
) {
    val body = buildString {
        appendLine("Здравствуйте!")
        appendLine()
        appendLine("В приложении LocoDriver не найдена на карте станция «$stationName».")
        if (!departure.isNullOrBlank() && !arrival.isNullOrBlank()) appendLine("Плечо: $departure → $arrival.")
        if (!region.isNullOrBlank()) appendLine("Регион / дорога: $region.")
        appendLine()
        appendLine("Пожалуйста, добавьте станцию на карту. Где она находится (регион, дорога, рядом с какими станциями), если знаете:")
        appendLine()
    }
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = "mailto:".toUri()
        putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, "Добавьте станцию: $stationName")
        putExtra(Intent.EXTRA_TEXT, body)
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "Нет почтового приложения. Напишите на $SUPPORT_EMAIL", Toast.LENGTH_LONG).show()
    }
}
