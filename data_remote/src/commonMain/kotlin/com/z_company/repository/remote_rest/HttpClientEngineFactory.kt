package com.z_company.repository.remote_rest

import io.ktor.client.engine.HttpClientEngine

/**
 * expect-декларация фабрики HTTP-движка.
 * Android → ktor-client-android
 * iOS    → ktor-client-darwin (NSURLSession)
 */
expect fun createHttpEngine(): HttpClientEngine
