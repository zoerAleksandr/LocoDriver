package com.z_company.repository.remote_rest

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

actual fun createHttpEngine(): HttpClientEngine = Darwin.create {
    configureSession {
        timeoutIntervalForRequest = 30.0
        timeoutIntervalForResource = 60.0
        // httpShouldUsePipelining = false + ограничение пула — защита от
        // -1005 NetworkConnectionLost. NSURLSession переиспользует "плохие"
        // соединения (сервер ответил Connection: close, но сокет ещё не
        // закрыт), и следующий запрос на нём падает. Мы и сервер шлём
        // Connection: close — этими настройками подталкиваем NSURLSession
        // к более частому пересозданию соединений.
        HTTPShouldUsePipelining = false
        HTTPMaximumConnectionsPerHost = 4L
    }
}
