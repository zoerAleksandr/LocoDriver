package com.z_company.repository.remote_rest

import com.z_company.domain.entities.route.Route
import com.z_company.repository.remote_rest.response.RouteDeltaResponse

/**
 * Итог полного обхода страниц дельты — то, что можно применять к локальной базе.
 *
 * Собирается целиком до применения: если обход оборвался на середине, ни данные,
 * ни курсор не сохраняются, и следующая синхронизация начнёт с прежней границы.
 * Иначе получилось бы, что часть данных применена, курсор сдвинут, а пропущенный
 * кусок не вернётся уже никогда.
 */
data class RouteDeltaSnapshot(
    val routes: List<Route>,
    val deletedIds: Set<String>,
    val cursor: String?,
    val fullResync: Boolean,
)

/** Предохранитель от бесконечного обхода, если сервер всё время просит «ещё». */
internal const val MAX_DELTA_PAGES = 500

/**
 * Курсор, который допустимо отправить серверу.
 *
 * Пустая локальная база с сохранённым курсором означает, что данные и курсор
 * разъехались: чистка данных приложения, переустановка, смена аккаунта. Дельта
 * по такому курсору пришла бы почти пустой, и устройство осталось бы без
 * маршрутов, считая себя синхронизированным. Курсор без данных не отправляем —
 * пусть сервер отдаст полный набор.
 */
internal fun cursorForRequest(stored: String?, hasLocalRoutes: Boolean): String? =
    if (hasLocalRoutes) stored?.takeIf { it.isNotBlank() } else null

/**
 * Обойти все страницы дельты и собрать их в один снимок.
 *
 * Исключение из [fetchPage] пробрасывается наружу: незавершённый обход не должен
 * ни применяться, ни двигать курсор.
 */
internal suspend fun collectRouteDelta(
    startCursor: String?,
    fetchPage: suspend (cursor: String?) -> RouteDeltaResponse,
): RouteDeltaSnapshot {
    val routes = LinkedHashMap<String, Route>()
    val deleted = LinkedHashSet<String>()
    var cursor = startCursor
    var fullResync = false
    var pages = 0

    while (true) {
        val page = fetchPage(cursor)
        fullResync = fullResync || page.fullResync

        // Маршруты и удаления идут одним упорядоченным потоком, поэтому внутри
        // обхода побеждает то, что встретилось позже: маршрут, удалённый и
        // созданный заново между страницами, не должен приехать сразу и в
        // «изменённые», и в «удалённые».
        for (route in page.routes) {
            routes[route.basicData.id] = route
            deleted.remove(route.basicData.id)
        }
        for (id in page.deleted) {
            deleted.add(id)
            routes.remove(id)
        }

        cursor = page.cursor
        pages++

        if (!page.hasMore) break
        if (page.cursor.isNullOrBlank()) {
            throw IllegalStateException("Сервер просит следующую страницу дельты, но не выдал курсор")
        }
        if (pages >= MAX_DELTA_PAGES) {
            throw IllegalStateException("Обход дельты не заканчивается: $pages страниц")
        }
    }

    return RouteDeltaSnapshot(
        routes = routes.values.toList(),
        deletedIds = deleted,
        cursor = cursor,
        fullResync = fullResync,
    )
}

/**
 * Локальные маршруты, которые сервер считает удалёнными.
 *
 * Удаление принимается только по явному серверному tombstone. Даже ответ,
 * помеченный full_resync, может оказаться пустым или неполным из-за ошибки
 * пагинации/прокси/серверной реализации. Отсутствие записи не является
 * доказательством удаления и никогда не должно менять локальные данные.
 */
internal fun deletionSuspects(
    localRoutes: List<Route>,
    snapshot: RouteDeltaSnapshot,
): List<Route> {
    return localRoutes.filter { it.basicData.id in snapshot.deletedIds }
}

/** Серверная запись отменяет только удаление, инициированное серверной синхронизацией. */
internal fun serverRouteCancelsPendingDeletion(local: Route): Boolean =
    local.basicData.isDeleted && local.basicData.deletionReason == "REMOTE_SYNC_DELETE"
