package com.z_company.domain.brake

/** Ввод даты «ДД.ММ.ГГГГ» на бланке справки ВУ-45: только цифры, точки подставляются сами. */
object DateInput {
    /**
     * Жёсткая маска. Из [raw] берутся только цифры по порядку (не более 8); цифра, делающая дату заведомо недопустимой, и всё после неё
     * отбрасываются: день 01-31 (первая цифра 0-3, при 3 вторая 0-1), месяц 01-12 (первая 0-1, при 1 вторая 0-2). Точка ставится после дня и месяца
     * при наборе вперёд, при стирании - нет (чтобы не мешать удалению). [prev] - предыдущее значение поля.
     */
    fun mask(prev: String, raw: String): String {
        val d = StringBuilder()
        for (c in raw) {
            if (c !in '0'..'9') continue
            val v = c - '0'
            val ok = when (d.length) {
                0 -> v <= 3
                1 -> d[0] != '3' || v <= 1
                2 -> v <= 1
                3 -> d[2] != '1' || v <= 2
                in 4..7 -> true
                else -> false
            }
            if (!ok) break
            d.append(c)
        }
        val grow = raw.length > prev.length
        return when {
            d.length < 2 -> d.toString()
            d.length == 2 -> d.toString() + if (grow || raw.contains('.')) "." else ""
            d.length < 4 -> d.substring(0, 2) + "." + d.substring(2)
            d.length == 4 -> d.substring(0, 2) + "." + d.substring(2) + if (grow || raw.endsWith(".")) "." else ""
            else -> d.substring(0, 2) + "." + d.substring(2, 4) + "." + d.substring(4)
        }
    }
}
