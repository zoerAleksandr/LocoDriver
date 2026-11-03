package com.z_company.route.viewmodel

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Шаблон отмены, используемый в ProfileViewModel/HomeViewModel: отмена job прерывает сбор потока, CancellationException пробрасывается. */
class SyncCancelTest {
    @Test fun cancelJob_stopsCollect_andRethrowsCancellation() = runBlocking {
        var collected = 0
        var swallowed = false
        var cancelled = false
        val job = launch(Dispatchers.Default) {
            try {
                flow { var i = 0; while (true) { emit(i++); delay(10) } }.collect { collected++ }
            } catch (e: CancellationException) {
                cancelled = true; throw e
            } catch (e: Exception) {
                swallowed = true
            }
        }
        delay(80)
        job.cancel()
        job.join()
        val atCancel = collected
        delay(60)
        assertTrue(cancelled)
        assertEquals(false, swallowed)
        assertEquals(atCancel, collected) // после отмены сбор остановлен
    }
}
