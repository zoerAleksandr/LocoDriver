package com.z_company.core.util

import android.content.res.Resources
import com.z_company.core.R
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
object MonthShortenedText: KoinComponent {
    private val res: Resources by inject()

    fun getMonthShortText(value: Int?): String {
        return when (value) {
            0 -> {
                MonthShortenedText.JANUARY
            }

            1 -> {
                MonthShortenedText.FEBRUARY
            }

            2 -> {
                MonthShortenedText.MARCH
            }

            3 -> {
                MonthShortenedText.APRIL
            }

            4 -> {
                MonthShortenedText.MAY
            }

            5 -> {
                MonthShortenedText.JUNE
            }

            6 -> {
                MonthShortenedText.JULY
            }

            7 -> {
                MonthShortenedText.AUGUST
            }

            8 -> {
                MonthShortenedText.SEPTEMBER
            }

            9 -> {
                MonthShortenedText.OCTOBER
            }

            10 -> {
                MonthShortenedText.NOVEMBER
            }

            11 -> {
                MonthShortenedText.DECEMBER
            }

            else -> {
                ""
            }
        }
    }

    val JANUARY = res.getString(R.string.january_short)
    val FEBRUARY = res.getString(R.string.february_short)
    val MARCH = res.getString(R.string.march_short)
    val APRIL = res.getString(R.string.april_short)
    val MAY = res.getString(R.string.may_short)
    val JUNE = res.getString(R.string.june_short)
    val JULY = res.getString(R.string.july_short)
    val AUGUST = res.getString(R.string.august_short)
    val SEPTEMBER = res.getString(R.string.september_short)
    val OCTOBER = res.getString(R.string.october_short)
    val NOVEMBER = res.getString(R.string.november_short)
    val DECEMBER = res.getString(R.string.december_short)
}