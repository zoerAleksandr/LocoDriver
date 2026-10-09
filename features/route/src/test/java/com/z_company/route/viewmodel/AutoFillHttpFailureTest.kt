package com.z_company.route.viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class AutoFillHttpFailureTest {
    @Test fun busyOn503() {
        assertEquals(AutoFillFailure.BUSY, httpFailureOf("ServerResponseException Server error(POST ...): 503 Service Unavailable."))
    }

    @Test fun rateLimitOn429() {
        assertEquals(AutoFillFailure.RATE_LIMITED, httpFailureOf("ClientRequestException 429 Too Many Requests"))
    }

    @Test fun otherIsNull() {
        assertNull(httpFailureOf("SerializationException Field 'osmId' is required"))
    }

    @Test fun busyTextDiffersFromNoConnection() {
        assertNotEquals(AutoFillFailure.NO_CONNECTION.message(), AutoFillFailure.BUSY.message())
    }
}
