package com.z_company.route.viewmodel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoPathHttpErrorTest {
    private val head = "Client request(POST https://api.locodriver.ru/v1/service_phase/route/length) invalid: 404 Not Found. Text: "

    @Test
    fun serverNoPathAnswerIsNoPath() {
        val msg = head + """"{"detail":"Путь по железной дороге между станциями не найден"}""""
        assertTrue(isNoPathHttpError(msg))
    }

    @Test
    fun missingEndpointIsNotNoPath() {
        val msg = head + """"{"detail":"Not Found"}""""
        assertFalse(isNoPathHttpError(msg))
    }

    @Test
    fun otherErrorsAreNotNoPath() {
        assertFalse(isNoPathHttpError("Server error(POST url) 500 Internal Server Error."))
        assertFalse(isNoPathHttpError("Unable to resolve host"))
    }
}
