package com.z_company.route.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────
// Иконки раздела «Статистика».
// Построены из SVG-путей дизайна (design/src/stats-ui.jsx, icons.jsx),
// viewBox 0 0 24 24, обводка 1.5, круглые концы — 1-в-1 с макетом.
// Цвет задаётся через tint у Icon(), базовый цвет тут не важен.
// ─────────────────────────────────────────────────────────────

private fun ImageVector.Builder.stroke(pathData: String): ImageVector.Builder =
    addPath(
        pathData = PathParser().parsePathString(pathData).toNodes(),
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.5f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    )

private fun ImageVector.Builder.fill(pathData: String): ImageVector.Builder =
    addPath(
        pathData = PathParser().parsePathString(pathData).toNodes(),
        fill = SolidColor(Color.Black),
    )

// Окружность как path (PathParser не умеет <circle>).
private fun circle(cx: Float, cy: Float, r: Float): String =
    "M ${cx - r} $cy a $r $r 0 1 0 ${r * 2} 0 a $r $r 0 1 0 ${-r * 2} 0"

private fun statIcon(block: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = "stat",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply(block).build()

object StatIcons {
    // Отработанное время
    val Clock: ImageVector = statIcon {
        stroke(circle(12f, 12f, 9f))
        stroke("M12 7v5l3.5 2")
    }

    // Переработка (спидометр со стрелкой)
    val Gauge: ImageVector = statIcon {
        stroke("M12 13l4-3")
        stroke("M4.5 17a9 9 0 1 1 15 0")
        fill(circle(12f, 13f, 1.4f))
    }

    // Расстояние (маршрут)
    val Route: ImageVector = statIcon {
        stroke(circle(6f, 18f, 2.4f))
        stroke(circle(18f, 6f, 2.4f))
        stroke("M8.4 18H14a3.5 3.5 0 0 0 0-7H10a3.5 3.5 0 0 1 0-7h5.6")
    }

    // Тонно-км брутто (гиря)
    val Weight: ImageVector = statIcon {
        stroke("M8.5 9.5a3.5 3.5 0 1 1 7 0")
        stroke("M7.4 9.5h9.2c.5 0 .9.3 1 .8l1.4 8.2a1 1 0 0 1-1 1.2H6c-.62 0-1.1-.55-1-1.2l1.4-8.2c.1-.5.5-.8 1-.8z")
    }

    // Ночные часы (луна)
    val Moon: ImageVector = statIcon {
        stroke("M20 14.5A8 8 0 1 1 9.5 4a6.3 6.3 0 0 0 10.5 10.5z")
    }

    // Маршрутов / смен (документ)
    val Document: ImageVector = statIcon {
        stroke("M6 3h8l5 5v11a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z")
        stroke("M13 3v6h6")
    }

    // Заработано (рубль)
    val Ruble: ImageVector = statIcon {
        stroke("M8 20V5h5.5a4 4 0 0 1 0 8H6M6 17h9")
    }

    // Средняя скорость (спидометр)
    val Speed: ImageVector = statIcon {
        stroke("M5 19a9 9 0 1 1 14 0")
        stroke("M12 15l4.5-5.5")
        fill(circle(12f, 15f, 1.3f))
    }

    // Время в пути (таймер)
    val Timer: ImageVector = statIcon {
        stroke(circle(12f, 13f, 8f))
        stroke("M12 9v4l2.5 2")
        stroke("M9 2h6")
    }

    // Следование пассажиром (кресло)
    val Seat: ImageVector = statIcon {
        stroke("M6 4v8a2 2 0 0 0 2 2h6")
        stroke("M6 12l-1 7")
        stroke("M14 14l1.5 5")
        stroke("M9 8h4")
    }

    // Навигация
    val ChevronLeft: ImageVector = statIcon { stroke("M15 5l-7 7 7 7") }
    val ChevronRight: ImageVector = statIcon { stroke("M9 5l7 7-7 7") }
    val ChevronDown: ImageVector = statIcon { stroke("M6 9l6 6 6-6") }

    // Стрелка «откуда → куда» в топе направлений
    val ArrowRight: ImageVector = statIcon { stroke("M4 12h14M13 7l5 5-5 5") }

    // Документ/PDF в топбаре
    val Pdf: ImageVector = statIcon {
        stroke("M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z")
        stroke("M14 3v5h5")
    }

    // Стрелка дельты (вверх). Вниз — поворотом на 180°, «flat» — отдельный путь.
    val DeltaUp: ImageVector = statIcon { stroke("M6 10V2M3 5l3-3 3 3") }
    val DeltaFlat: ImageVector = statIcon { stroke("M2 6h8") }

    fun forKey(key: String): ImageVector = when (key) {
        "worked" -> Clock
        "overtime" -> Gauge
        "distance" -> Route
        "tkm" -> Weight
        "night" -> Moon
        "routes" -> Document
        "earnings" -> Ruble
        "speed" -> Speed
        "transit" -> Timer
        "rider" -> Seat
        else -> Clock
    }
}
