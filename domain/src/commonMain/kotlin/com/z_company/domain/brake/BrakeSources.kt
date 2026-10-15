package com.z_company.domain.brake

/** Ссылки на источник норм. Везде в UI и спеках пункт/таблица указываются вместе с названием Инструкции. */
object InstructionRef {
    /** Полное название (утв. МПС России 16.05.1994 N ЦТ-ЦВ-ЦЛ-ВНИИЖТ/277). */
    const val FULL = "Инструкция по эксплуатации тормозов подвижного состава железных дорог ЦТ-ЦВ-ЦЛ-ВНИИЖТ/277"
    const val SHORT = "Инструкция ЦТ-ЦВ-ЦЛ-ВНИИЖТ/277"
    const val GENITIVE = "Инструкции ЦТ-ЦВ-ЦЛ-ВНИИЖТ/277"

    /** «п. 4.4 Инструкции ЦТ-ЦВ-ЦЛ-ВНИИЖТ/277» */
    fun p(clause: String) = "п. $clause $GENITIVE"

    /** «табл. 1 прил. 2 Инструкции ЦТ-ЦВ-ЦЛ-ВНИИЖТ/277» */
    fun table(num: String, appendix: String? = "прил. 2") = "табл. $num" + (appendix?.let { " $it" } ?: "") + " $GENITIVE"

    private val refRegex = Regex("""(?:п\.|табл\.) ?\d+(?:\.\d+)*(?:\s*[-–,]\s*\d+(?:\.\d+)*)*""")

    /**
     * Дописывает название Инструкции после ПЕРВОЙ ссылки на пункт/таблицу в строке («п. 4.4» → «п. 4.4 Инструкции ЦТ-ЦВ-ЦЛ-ВНИИЖТ/277»).
     * Строка, уже называющая Инструкцию (или другой документ), не меняется.
     */
    fun annotate(text: String): String {
        if (text.contains("Инструкци") || text.contains("808-2017")) return text
        val m = refRegex.find(text) ?: return text
        return text.substring(0, m.range.last + 1) + " $GENITIVE" + text.substring(m.range.last + 1)
    }
}
