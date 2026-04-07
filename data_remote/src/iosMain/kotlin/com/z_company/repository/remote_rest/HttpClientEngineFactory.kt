package com.z_company.repository.remote_rest

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

actual fun createHttpEngine(): HttpClientEngine = Darwin.create {
    configureSession {
        // Отключаем pipeline и ограничиваем соединения — избегаем NSURLErrorDomain -1005
        timeoutIntervalForRequest = 30.0
        timeoutIntervalForResource = 60.0
    }
}
