package com.z_company.core.util

import android.content.res.Resources
import com.z_company.core.R
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object MonthFullText: KoinComponent {
    private val res: Resources by inject()

    fun getMonthFullText(value: Int?): String {
        return when (value) {
            0 -> {
                MonthFullText.JANUARY
            }

            1 -> {
                MonthFullText.FEBRUARY
            }

            2 -> {
                MonthFullText.MARCH
            }

            3 -> {
                MonthFullText.APRIL
            }

            4 -> {
                MonthFullText.MAY
            }

            5 -> {
                MonthFullText.JUNE
            }

            6 -> {
                MonthFullText.JULY
            }

            7 -> {
                MonthFullText.AUGUST
            }

            8 -> {
                MonthFullText.SEPTEMBER
            }

            9 -> {
                MonthFullText.OCTOBER
            }

            10 -> {
                MonthFullText.NOVEMBER
            }

            11 -> {
                MonthFullText.DECEMBER
            }

            else -> {
                ""
            }
        }
    }

    val JANUARY = res.getString(R.string.january_full)
    val FEBRUARY = res.getString(R.string.february_full)
    val MARCH = res.getString(R.string.march_full)
    val APRIL = res.getString(R.string.april_full)
    val MAY = res.getString(R.string.may_full)
    val JUNE = res.getString(R.string.june_full)
    val JULY = res.getString(R.string.july_full)
    val AUGUST = res.getString(R.string.august_full)
    val SEPTEMBER = res.getString(R.string.september_full)
    val OCTOBER = res.getString(R.string.october_full)
    val NOVEMBER = res.getString(R.string.november_full)
    val DECEMBER = res.getString(R.string.december_full)
}