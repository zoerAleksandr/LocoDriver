package com.z_company.domain.util

/**
 * Хранит id маршрута, только что импортированного по публичной ссылке.
 *
 * Маршрут уже сохранён в локальную БД с флагом [isDeleted = true]
 * (tentative preview — не отображается в списках, но доступен через
 * [RouteRepository.loadRoute] по id, и его дочерние сущности
 * грузятся нормально).
 *
 * FormViewModel при загрузке проверяет [consume] — если id совпадает,
 * помечает `isSharedPreview = true`, чтобы показать шторку.
 */
object SharedRouteHolder {
    @kotlin.concurrent.Volatile
    private var _sharedRouteId: String? = null

    /** Пометить маршрут как импортированный по публичной ссылке. */
    fun markSharedPreview(routeId: String) {
        _sharedRouteId = routeId
    }

    /**
     * Если переданный id помечен как shared preview — очистить метку и вернуть true.
     * Вызывается из FormViewModel.loadData ровно один раз для экземпляра FormViewModel.
     */
    fun consume(routeId: String): Boolean {
        if (_sharedRouteId == routeId) {
            _sharedRouteId = null
            return true
        }
        return false
    }
}
