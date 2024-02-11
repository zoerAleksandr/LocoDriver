package com.example.domain.entities

data class UserSettings(
    var key: String = "User_Settings_Key",
    var minTimeRest: Long = 10_800_000L,
    var lastEnteredDieselCoefficient: Double = 0.83
)
