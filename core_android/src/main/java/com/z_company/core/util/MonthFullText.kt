package com.z_company.core.util

import android.content.res.Resources
import com.z_company.core.R
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object MonthFullText: KoinComponent {
    private val res: Resources by inject()

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