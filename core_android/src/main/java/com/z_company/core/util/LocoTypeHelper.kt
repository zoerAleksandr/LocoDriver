package com.z_company.core.util

import android.content.res.Resources
import com.z_company.core.R
import com.z_company.domain.entities.route.LocoType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object LocoTypeHelper: KoinComponent {
    val res: Resources by inject()
    fun converterLocoTypeToString(locoType: LocoType): String {
        return when (locoType) {
            LocoType.ELECTRIC -> res.getString(R.string.electricType)
            LocoType.DIESEL -> res.getString(R.string.dieselType)
        }
    }
}