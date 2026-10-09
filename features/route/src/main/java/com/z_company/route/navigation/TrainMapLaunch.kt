package com.z_company.route.navigation

/**
 * Одноразовая просьба «открыть экран Поезд сразу с картой» (плитка «Карта» на Главном, SCREEN_SPECS §4/§6.9).
 * Ставится перед переходом на форму поезда и забирается ровно один раз при её создании.
 */
object TrainMapLaunch {
    @Volatile
    private var pending = false

    fun request() {
        pending = true
    }

    fun consume(): Boolean = pending.also { pending = false }
}
