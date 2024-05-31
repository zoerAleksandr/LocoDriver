package com.z_company.domain.entities

const val SETTINGS_KEY = "User_Settings_Key"
data class UserSettings(
    var key: String = SETTINGS_KEY,
    var minTimeRest: Long? = 10_800_000L,
    var lastEnteredDieselCoefficient: Double = 0.83,
    var nightTime: NightTime = NightTime()
)

data class NightTime(
    val startNightHour: Int = 0,
    val startNightMinute: Int = 0,
    val endNightHour: Int = 6,
    val endNightMinute: Int = 0
)
