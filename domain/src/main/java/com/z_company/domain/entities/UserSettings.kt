package com.z_company.domain.entities

import com.z_company.domain.entities.route.LocoType

const val SETTINGS_KEY = "User_Settings_Key"
data class UserSettings(
    var key: String = SETTINGS_KEY,
    var minTimeRest: Long? = 10_800_000L,
    var minTimeHomeRest: Long? = 57_600_000L,
    var lastEnteredDieselCoefficient: Double = 0.83,
    var nightTime: NightTime = NightTime(),
    var defaultLocoType: LocoType = LocoType.ELECTRIC,
    var defaultWorkTime: Long = 43_200_000L,
    var updateAt: Long = 0L
)

data class NightTime(
    val startNightHour: Int = 22,
    val startNightMinute: Int = 0,
    val endNightHour: Int = 5,
    val endNightMinute: Int = 30
)
