package com.z_company.domain.brake

/**
 * Тексты радиосообщений при вынужденной остановке поезда на перегоне - дословно по технологической карте распоряжения ОАО «РЖД» № ЦТ-162/р от 28.04.2025
 * (операции 4.1 и 7.2): «Внимание все!» об остановке и требование вспомогательного локомотива (не позднее 10 минут после остановки).
 * Незаполненные поля заменяются многоточием «…», как в тексте карты. Спека - docs/BRAKE_FEATURES_PLAN.md.
 */
data class HoldingRadioData(
    val surname: String = "",
    val trainNumber: String = "",
    val km: String = "",
    val picket: String = "",
    /** true - чётный путь, false - нечётный, null - не указан («чётного (нечётного)»). */
    val evenTrack: Boolean? = null,
    val stretch: String = "",
    val cause: String = "",
    val locoSection: String = "",
    val locoSeries: String = "",
    val locoNumber: String = "",
    /** Время сообщения: «ЧЧ:ММ»; пусто - «… час … мин». */
    val time: String = "",
)

object BrakeHoldingRadio {
    /** Время остановки, не позднее которого затребовать вспомогательный локомотив, мин (оп. 7, 7.2 карты). */
    const val HELP_LOCO_DEADLINE_MIN = 10

    private fun v(s: String): String = s.trim().ifEmpty { "…" }

    /** Операция 4.1 карты: объявление об остановке. */
    fun stopMessage(d: HoldingRadioData): String {
        val track = when (d.evenTrack) {
            true -> "чётного"
            false -> "нечётного"
            null -> "чётного (нечётного)"
        }
        return "Внимание все! Я, машинист ${v(d.surname)} поезда № ${v(d.trainNumber)} остановился на ${v(d.km)} километре, ${v(d.picket)} пикете " +
            "$track пути перегона ${v(d.stretch)} вследствие ${v(d.cause)}. Будьте бдительны!"
    }

    /** Операция 7.2 карты: требование вспомогательного локомотива. */
    fun helpLocoMessage(d: HoldingRadioData): String {
        val t = d.time.trim().split(":")
        val hm = if (t.size == 2 && t[0].isNotBlank() && t[1].isNotBlank()) "${t[0].trimStart('0').ifEmpty { "0" }} час ${t[1].trimStart('0').ifEmpty { "0" }} мин" else "… час … мин"
        return "Я машинист ${v(d.surname)} поезда № ${v(d.trainNumber)}, на ${v(d.km)} км ${v(d.picket)} пикете перегона ${v(d.stretch)} " +
            "требую вспомогательный локомотив по причине неисправности локомотива секции ${v(d.locoSection)}, серии ${v(d.locoSeries)}, № ${v(d.locoNumber)}, " +
            "из-за ${v(d.cause)}. Время $hm"
    }
}
