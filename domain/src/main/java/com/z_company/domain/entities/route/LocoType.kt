package com.z_company.domain.entities.route

enum class LocoType {
    ELECTRIC, DIESEL
}

object LocoTypeHelper {
    fun converterLocoTypeToString(locoType: LocoType): String {
        return when (locoType) {
            LocoType.ELECTRIC -> "Электровоз"
            LocoType.DIESEL -> "Тепловоз"
        }
    }
}
