package com.z_company.domain.track

/** Проверки высот и давления перед записью в трек (SCREEN_SPECS §6.9, этап 3в). Чистые функции без платформы. */
object AltitudeMath {
    /** Правдоподобные высоты для железной дороги и формата `ld-track-v2` (alt: -500…9000 м). */
    const val MIN_ALT_M = -500.0
    const val MAX_ALT_M = 9000.0

    /** Давление, при котором высоту считать не имеет смысла (глюк датчика), гПа. */
    const val MIN_PRESSURE_HPA = 300f
    const val MAX_PRESSURE_HPA = 1100f

    /** Высота, годная для записи: конечное число в допустимых пределах, иначе null. */
    fun sanitize(altitudeM: Double?): Double? =
        altitudeM?.takeIf { it.isFinite() && it >= MIN_ALT_M && it <= MAX_ALT_M }

    /** Показание барометра правдоподобно (для расчёта высоты по стандартной атмосфере). */
    fun isPlausiblePressureHpa(hpa: Float?): Boolean =
        hpa != null && hpa.isFinite() && hpa >= MIN_PRESSURE_HPA && hpa <= MAX_PRESSURE_HPA
}
