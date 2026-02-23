package com.z_company.repository.remote_rest

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.android.Android

actual fun createHttpEngine(): HttpClientEngine = Android.create()
