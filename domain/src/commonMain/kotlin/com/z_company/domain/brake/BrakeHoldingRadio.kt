package com.z_company.domain.brake

/**
 * Радиосообщения при вынужденной остановке из-за неисправности ТПС/ССПС - по распоряжению ОАО «РЖД» №1900/р,
 * раздел 13, пп. 13.1 и 13.4. Незаполненные поля отображаются многоточием, чтобы машинист мог назвать их голосом.
 */
data class HoldingRadioData(
    val surname: String = "",
    val trainNumber: String = "",
    val km: String = "",
    val picket: String = "",
    /** Номер железнодорожного пути; в форме сообщения инструкции требуется назвать именно номер. */
    val trackNumber: String = "",
    val stretch: String = "",
    val cause: String = "",
    val locoSection: String = "",
    val locoSeries: String = "",
    val locoNumber: String = "",
    /** Время сообщения: «ЧЧ:ММ»; пусто - «… час … мин». */
    val time: String = "",
)

object BrakeHoldingRadio {
    /** Базовый срок запроса помощи по п. 13.4 распоряжения №1900/р, мин. */
    const val HELP_LOCO_DEADLINE_MIN = 10

    private fun v(s: String): String = s.trim().ifEmpty { "…" }

    /** Распоряжение №1900/р, п. 13.1: сообщение об остановке. */
    fun stopMessage(d: HoldingRadioData): String {
        return "Внимание, все! Я, машинист ${v(d.surname)} поезда № ${v(d.trainNumber)}, остановился по причине ${v(d.cause)} " +
            "на ${v(d.km)} километре, пикете ${v(d.picket)}, номер пути ${v(d.trackNumber)}, ближайшая станция ${v(d.stretch)}. Будьте бдительны!"
    }

    /** Распоряжение №1900/р, п. 13.4: требование помощи; точное место сообщается после личной проверки. */
    fun helpLocoMessage(d: HoldingRadioData): String {
        val t = d.time.trim().split(":")
        val hm = if (t.size == 2 && t[0].isNotBlank() && t[1].isNotBlank()) "${t[0].trimStart('0').ifEmpty { "0" }} час ${t[1].trimStart('0').ifEmpty { "0" }} мин" else "… час … мин"
        return "Я машинист ${v(d.surname)} поезда № ${v(d.trainNumber)}, на ${v(d.km)} км ${v(d.picket)} пикете, у станции ${v(d.stretch)} " +
            "требую вспомогательный локомотив по причине неисправности ТПС, ССПС секции ${v(d.locoSection)}, серии ${v(d.locoSeries)}, № ${v(d.locoNumber)}, " +
            "из-за ${v(d.cause)}, точное место остановки сообщу после личного убеждения. Время $hm"
    }

    /** Распоряжение №1900/р, п. 13.4: доклад после личной проверки места головы поезда. */
    fun verifiedPlaceMessage(d: HoldingRadioData): String =
        "Я машинист ${v(d.surname)} поезда № ${v(d.trainNumber)}, нахожусь «головой» поезда на км ${v(d.km)} " +
            "пикете ${v(d.picket)}, у станции ${v(d.stretch)}."
}
