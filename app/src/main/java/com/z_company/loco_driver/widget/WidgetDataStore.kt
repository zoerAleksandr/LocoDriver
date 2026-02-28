package com.z_company.loco_driver.widget

/**
 * Helper for formatting month names (Russian locale) for the widget.
 */
object WidgetUtils {

    private val MONTH_NAMES = arrayOf(
        "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
    )

    fun getMonthName(month: Int): String {
        return MONTH_NAMES.getOrElse(month - 1) { "" }
    }

    fun formatMonthYear(month: Int, year: Int): String {
        return "${getMonthName(month)} $year"
    }
}
