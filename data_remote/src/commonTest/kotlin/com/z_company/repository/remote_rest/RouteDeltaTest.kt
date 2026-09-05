package com.z_company.repository.remote_rest

import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Route
import com.z_company.repository.remote_rest.response.RouteDeltaResponse
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Обход страниц дельты. Каждый тест закрывает свой способ потерять маршрут.
 *
 * Корутина запускается вручную (`startCoroutine`), а не через `runTest`:
 * kotlinx-coroutines-test в зависимостях проекта нет, а фейковый источник
 * страниц никогда не уходит в ожидание — обход завершается синхронно.
 */
class RouteDeltaTest {

    private fun <T> runSync(block: suspend () -> T): T {
        var outcome: Result<T>? = null
        block.startCoroutine(Continuation(EmptyCoroutineContext) { outcome = it })
        return (outcome ?: error("Обход приостановился: фейковые страницы не должны ждать ввода-вывода"))
            .getOrThrow()
    }

    private fun route(id: String) = Route(basicData = BasicData(id = id))

    private fun pages(vararg pages: RouteDeltaResponse): suspend (String?) -> RouteDeltaResponse {
        var index = 0
        return { pages[index++] }
    }

    // --- 13: курсор без данных не отправляется ---

    @Test
    fun doesNotSendCursorWhenThereAreNoLocalRoutes() {
        assertNull(cursorForRequest(stored = "cursor-from-previous-install", hasLocalRoutes = false))
    }

    @Test
    fun sendsStoredCursorWhenLocalRoutesExist() {
        assertEquals("stored", cursorForRequest(stored = "stored", hasLocalRoutes = true))
    }

    @Test
    fun treatsBlankStoredCursorAsAbsent() {
        assertNull(cursorForRequest(stored = "   ", hasLocalRoutes = true))
    }

    // --- 10: обрыв на середине обхода ---

    @Test
    fun brokenTraversalYieldsNothingToApply() {
        var fetched = 0
        val failing: suspend (String?) -> RouteDeltaResponse = { cursor ->
            fetched++
            if (fetched == 1) {
                RouteDeltaResponse(routes = listOf(route("a")), cursor = "c1", hasMore = true)
            } else {
                throw IllegalStateException("сеть отвалилась на второй странице")
            }
        }

        assertFailsWith<IllegalStateException> { runSync { collectRouteDelta(null, failing) } }
        // Первая страница уже была получена, но наружу не вышла: применять
        // нечего, курсор сохранять нечего — следующая синхронизация начнёт
        // с прежней границы.
        assertEquals(2, fetched)
    }

    @Test
    fun refusesToLoopForeverWhenServerKeepsAskingForMore() {
        val endless: suspend (String?) -> RouteDeltaResponse = { cursor ->
            RouteDeltaResponse(cursor = "always-more", hasMore = true)
        }

        assertFailsWith<IllegalStateException> { runSync { collectRouteDelta(null, endless) } }
    }

    @Test
    fun refusesToContinueWithoutACursor() {
        val cursorless = pages(RouteDeltaResponse(routes = listOf(route("a")), cursor = null, hasMore = true))

        assertFailsWith<IllegalStateException> { runSync { collectRouteDelta(null, cursorless) } }
    }

    // --- 11: пустая дельта ---

    @Test
    fun emptyDeltaCarriesNoChangesAndNoDeletions() {
        val snapshot = runSync {
            collectRouteDelta("c0", pages(RouteDeltaResponse(cursor = "c0")))
        }

        assertTrue(snapshot.routes.isEmpty())
        assertTrue(snapshot.deletedIds.isEmpty())
        assertFalse(snapshot.fullResync)
        assertEquals("c0", snapshot.cursor)
    }

    // --- обход страниц ---

    @Test
    fun collectsEveryPageBeforeReturning() {
        val snapshot = runSync {
            collectRouteDelta(
                null,
                pages(
                    RouteDeltaResponse(routes = listOf(route("a")), cursor = "c1", hasMore = true),
                    RouteDeltaResponse(routes = listOf(route("b")), deleted = listOf("c"), cursor = "c2", hasMore = true),
                    RouteDeltaResponse(routes = listOf(route("d")), cursor = "c3", hasMore = false),
                ),
            )
        }

        assertEquals(listOf("a", "b", "d"), snapshot.routes.map { it.basicData.id })
        assertEquals(setOf("c"), snapshot.deletedIds)
        assertEquals("c3", snapshot.cursor)
    }

    @Test
    fun laterPageWinsWhenRouteIsRecreatedDuringTraversal() {
        val snapshot = runSync {
            collectRouteDelta(
                null,
                pages(
                    RouteDeltaResponse(deleted = listOf("a"), cursor = "c1", hasMore = true),
                    RouteDeltaResponse(routes = listOf(route("a")), cursor = "c2", hasMore = false),
                ),
            )
        }

        assertEquals(listOf("a"), snapshot.routes.map { it.basicData.id })
        assertTrue(snapshot.deletedIds.isEmpty())
    }

    @Test
    fun laterPageWinsWhenRouteIsDeletedDuringTraversal() {
        val snapshot = runSync {
            collectRouteDelta(
                null,
                pages(
                    RouteDeltaResponse(routes = listOf(route("a")), cursor = "c1", hasMore = true),
                    RouteDeltaResponse(deleted = listOf("a"), cursor = "c2", hasMore = false),
                ),
            )
        }

        assertTrue(snapshot.routes.isEmpty())
        assertEquals(setOf("a"), snapshot.deletedIds)
    }

    // --- 9: признак полной выгрузки ---

    @Test
    fun fullResyncOnAnyPageMarksTheWholeTraversal() {
        val snapshot = runSync {
            collectRouteDelta(
                "stale",
                pages(
                    RouteDeltaResponse(routes = listOf(route("a")), cursor = "c1", hasMore = true, fullResync = true),
                    RouteDeltaResponse(routes = listOf(route("b")), cursor = "c2", hasMore = false, fullResync = true),
                ),
            )
        }

        // Локальный набор заменяется целиком, а не домерживается — решение
        // принимается по итогу всего обхода, а не по первой странице.
        assertTrue(snapshot.fullResync)
        assertEquals(listOf("a", "b"), snapshot.routes.map { it.basicData.id })
    }
}
