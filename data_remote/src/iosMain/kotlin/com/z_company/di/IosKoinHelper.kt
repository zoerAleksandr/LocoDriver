package com.z_company.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module

/**
 * Точка входа Koin для iOS.
 *
 * Вызывается из Swift при старте приложения:
 * ```swift
 * IosKoinHelperKt.doInitKoin(additionalModules: [
 *     SqlDelightRouteDiKt.sqlDelightRouteModule,
 *     SqlDelightSettingDiKt.sqlDelightSettingsModule,
 *     UseCaseDiKt.useCaseModule,
 * ])
 * ```
 *
 * [additionalModules] — модули, которые не могут быть включены здесь напрямую
 * из-за отсутствия зависимости data_remote → data_local.
 * iOS-приложение передаёт их при инициализации.
 */
fun initKoin(additionalModules: List<Module> = emptyList()) {
    startKoin {
        modules(listOf(iosRepositoryModule) + additionalModules)
    }
}
