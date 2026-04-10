package com.z_company.domain.util

import com.z_company.domain.entities.route.Route

/**
 * Временное хранилище Route для передачи между ViewModel-ами
 * при импорте маршрута по публичной ссылке.
 *
 * MainViewModel загружает Route с сервера, помещает его сюда через [set],
 * затем навигирует на FormScreen. FormViewModel в loadData() проверяет
 * [consume] — если Route с подходящим id есть, использует его напрямую,
 * минуя загрузку из БД (маршрут ещё не сохранён).
 */
object SharedRouteHolder {
    @Volatile
    private var _route: Route? = null

    fun set(route: Route) { _route = route }

    /** Возвращает Route и очищает хранилище (one-shot). */
    fun consume(): Route? {
        val r = _route
        _route = null
        return r
    }

    fun peek(): Route? = _route
}
