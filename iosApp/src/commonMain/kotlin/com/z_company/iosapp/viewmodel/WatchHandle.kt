package com.z_company.iosapp.viewmodel

import kotlinx.coroutines.Job

/**
 * Токен подписки `watchX(callback)`. Каждый `watchX` в IosViewModel /
 * SharedRouteLinkHandler возвращает [WatchHandle], а Swift-Wrapper хранит
 * список этих токенов и вызывает [cancel] в `deinit`, чтобы остановить
 * `flow.collect` корутину в `viewModelScope` Koin-singleton VM.
 *
 * Без этого collect-корутина живёт всё время работы приложения — каждое
 * повторное открытие экрана накапливает новую подписку поверх старой.
 *
 * `internal constructor` — Swift не может конструировать руками; единственный
 * легальный путь получить `WatchHandle` — вернуть его из `watchX`.
 */
class WatchHandle internal constructor(private val job: Job) {
    fun cancel() {
        job.cancel()
    }
}
