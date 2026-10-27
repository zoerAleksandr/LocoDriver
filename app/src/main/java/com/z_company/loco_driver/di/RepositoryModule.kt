package com.z_company.loco_driver.di

import com.z_company.data_local.SharedPreferenceStorage
import com.z_company.data_local.calendar.CalendarStorageLocalImpl
import com.z_company.data_local.calendar.SqlDelightProductionCalendarRepository
import com.z_company.data_local.calendar.SqlDelightReleaseDayRepository
import com.z_company.data_local.norma_time.SqlDelightLocomotiveSeriesRepository
import com.z_company.data_local.norma_time.SqlDelightPartnerRepository
import com.z_company.data_local.norma_time.SqlDelightStationNormRepository
import com.z_company.data_local.setting.DataStoreRepository
import com.z_company.data_local.calendar.SqlDelightCalendarRepository
import com.z_company.data_local.route.SqlDelightHistoryResponseRepository
import com.z_company.data_local.route.SqlDelightRouteRepository
import com.z_company.data_local.route.SqlDelightDiagnosticRepository
import com.z_company.data_local.setting.SqlDelightSalarySettingRepository
import com.z_company.data_local.setting.SqlDelightSettingRepository
import com.z_company.domain.repositories.CalendarStorage
import com.z_company.domain.repositories.LocomotiveSeriesRepository
import com.z_company.domain.repositories.ProductionCalendarRepository
import com.z_company.domain.repositories.ReleaseDayRepository
import com.z_company.domain.repositories.RouteRepository
import com.z_company.domain.repositories.DiagnosticRepository
import com.z_company.domain.repositories.CalendarRepositories
import com.z_company.domain.repositories.HistoryResponseRepository
import com.z_company.domain.repositories.SalarySettingRepository
import com.z_company.domain.repositories.SettingsRepository
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.repositories.PartnerRepository
import com.z_company.domain.repositories.StationNormRepository
import com.z_company.core.ui.snackbar.ISnackbarManager
import com.z_company.core.ui.snackbar.SnackbarManagerImpl
import com.z_company.core.theme.ThemeManager
import com.z_company.loco_driver.ui.theme.ThemeManagerImpl
import com.z_company.loco_driver.BuildConfig
import com.z_company.repository.SecureTokenStorage
import com.z_company.route.session.SessionExpiredHandler
import com.z_company.route.subscription.SubscriptionPeriodTracker
import com.z_company.route.subscription.PaymentReturnChecker
import com.z_company.route.subscription.asPendingPaymentStore
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.core.ResultState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.z_company.route.subscription.asSubscriptionSeenStore
import com.z_company.repository.ShareManager
import com.z_company.repository.remote_rest.ApiForSendEmail
import com.z_company.repository.remote_rest.AuthManager
import com.z_company.repository.remote_rest.RemoteAnnouncementRepository
import com.z_company.repository.remote_rest.RemoteRestApi
import com.z_company.repository.remote_rest.RemoteRestClient
import com.z_company.domain.repositories.AnnouncementRepository
import com.z_company.repository.remote_rest.RoutesManager
import com.z_company.repository.remote_rest.SettingManager
import com.z_company.repository.remote_rest.ShareRouteManager
import com.z_company.repository.remote_rest.SessionRefresher
import com.z_company.repository.remote_rest.SyncManager
import com.z_company.core.widget.WidgetUpdater
import com.z_company.loco_driver.widget.GlanceWidgetUpdater
import com.z_company.route.viewmodel.RouteActionsHelper
import com.z_company.use_case.SubscriptionHelper
import kotlinx.coroutines.flow.first
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** Участки советника отдаются только вошедшему: токен берётся из защищённого хранилища на каждый запрос (смена аккаунта подхватывается). */
private fun bindAdvisorToken(tokens: com.z_company.repository.SecureTokenStorage, appContext: android.content.Context) {
    RemoteRestClient.advisorBearerToken = { tokens.getAuthBearerTokenFlow().first() }
    RemoteRestClient.advisorAccountId = { tokens.getUserIdFlow().first() }
    if (RemoteRestClient.advisorDeviceKeys == null) {
        RemoteRestClient.advisorDeviceKeys = com.z_company.repository.remote_rest.tracks.AndroidDeviceKeys(appContext)
    }
}

val repositoryModule = module {
    single<ISnackbarManager> { SnackbarManagerImpl() }

    single<RouteRepository> { SqlDelightRouteRepository() }
    single<DiagnosticRepository> { SqlDelightDiagnosticRepository() }

    single<CalendarRepositories> { SqlDelightCalendarRepository() }

    single<ReleaseDayRepository> { SqlDelightReleaseDayRepository() }

    single<LocomotiveSeriesRepository> { SqlDelightLocomotiveSeriesRepository() }

    single<StationNormRepository> { SqlDelightStationNormRepository() }

    single<PartnerRepository> { SqlDelightPartnerRepository() }

    single<ProductionCalendarRepository> { SqlDelightProductionCalendarRepository() }

    single<CalendarStorage> { CalendarStorageLocalImpl() }

    single { DataStoreRepository(androidContext()) }

    single<SettingsRepository> { SqlDelightSettingRepository() }

    single<HistoryResponseRepository> { SqlDelightHistoryResponseRepository() }

    single<SharedPreferencesRepositories> { SharedPreferenceStorage(application = androidApplication()) }

    single<ThemeManager> { ThemeManagerImpl(prefs = get()) }

    single<SalarySettingRepository> { SqlDelightSalarySettingRepository() }

    single<ShareManager> { ShareManager(androidContext()) }

    // Шаг 3 KMP-миграции: абстракция Context (SecureDataStore → SecureTokenStorage)
    single { SecureTokenStorage(androidContext()) }

    // Разлогин по 401 с любого запроса; подписку запускает StartApp.
    single { SessionExpiredHandler(secureTokenStorage = get(), sharedPrefs = get()) }
    // Связь экрана Подписки с глобальным диалогом «Подписка продлена».
    single {
        val secureTokenStorage: SecureTokenStorage = get()
        SubscriptionPeriodTracker(
            currentUserId = { secureTokenStorage.getUserIdFlow().first() },
            store = get<SharedPreferencesRepositories>().asSubscriptionSeenStore(),
        )
    }
    // Подтверждение оплаты при любом возврате в приложение → «Платёж принят!».
    single {
        val secureTokenStorage: SecureTokenStorage = get()
        val subscriptionHelper: SubscriptionHelper = get()
        val settingsUseCase: SettingsUseCase = get()
        PaymentReturnChecker(
            store = get<SharedPreferencesRepositories>().asPendingPaymentStore(),
            currentUserId = { secureTokenStorage.getUserIdFlow().first() },
            refreshPeriodFromServer = {
                val token = secureTokenStorage.getAuthBearerTokenFlow().first()
                val result = subscriptionHelper.restorePurchases(null, token)
                if (result is ResultState.Success) {
                    settingsUseCase.getUserSettingFlow().first().subscriptionPeriod
                } else {
                    null
                }
            },
            tracker = get(),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        )
    }
    // «Рядом с путями ли я»: для профиля пути (сообщение «Вы находитесь вне путей»); тот же запрос ближайшей станции по рельсам.
    single<com.z_company.domain.util.RailStationLookup> {
        val tokens: SecureTokenStorage = get()
        com.z_company.repository.remote_rest.RemoteRailStationLookup(
            api = get(),
            bearerToken = { tokens.getAuthBearerTokenFlow().first() },
        )
    }
    // Единое определение названия станции по месту (GO, «Добавить станцию», АВТО, карта, трек; SCREEN_SPECS §6.2 п.6а).
    single {
        val tokens: SecureTokenStorage = get()
        com.z_company.domain.util.StopStationNamer(
            lookup = com.z_company.repository.remote_rest.RemoteRailStationLookup(
                api = get(),
                bearerToken = { tokens.getAuthBearerTokenFlow().first() },
            ),
            nowMs = { System.currentTimeMillis() },
            phaseLine = com.z_company.route.component.servicephase.RailLineCache(androidContext()),
        )
    }
    // Кнопка «АВТО»: запись стоянок/отправлений по GPS (SCREEN_SPECS §6.2 п.6а).
    single {
        com.z_company.route.autorecord.AutoRecordManager(
            context = androidContext(),
            routeUseCase = get(),
            trainUseCase = get(),
            stationDirectory = get(),
            settingsUseCase = get(),
            stopNamer = get(),
            onWatchdog = { on ->
                val wm = androidx.work.WorkManager.getInstance(androidContext())
                if (on) {
                    wm.enqueueUniquePeriodicWork(
                        "auto_record_watchdog",
                        androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                        androidx.work.PeriodicWorkRequestBuilder<com.z_company.loco_driver.widget.AutoRecordWatchdogWorker>(
                            15, java.util.concurrent.TimeUnit.MINUTES,
                        ).build(),
                    )
                } else {
                    wm.cancelUniqueWork("auto_record_watchdog")
                }
            },
            prefs = androidContext().getSharedPreferences(
                com.z_company.route.autorecord.AutoRecordManager.PREFS_NAME,
                android.content.Context.MODE_PRIVATE,
            ),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
            onChanged = {
                runCatching {
                    com.z_company.loco_driver.widget.WidgetDataLoader.loadAndPush(androidContext())
                }
            },
            trackRepository = get(),
            trackConsent = get(),
            onTrackUpload = { com.z_company.loco_driver.track.TrackUploadScheduler.enqueue(androidContext()) },
            altitudeStore = get(),
            onAltitudeContrib = { com.z_company.loco_driver.track.AltitudeContribScheduler.enqueue(androidContext()) },
            gnssStore = get(),
            onGnssContrib = { com.z_company.loco_driver.track.GnssZoneContribScheduler.enqueue(androidContext()) },
        )
    }
    // Согласие №1 «Сохранять мои треки и историю поездок» (SCREEN_SPECS §6.9, §17.7).
    single {
        com.z_company.route.autorecord.TrackConsentStore(
            prefs = androidContext().getSharedPreferences(
                com.z_company.route.autorecord.TrackConsentStore.PREFS_NAME,
                android.content.Context.MODE_PRIVATE,
            ),
            trackRepository = get(),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
            onConsentChanged = { com.z_company.loco_driver.track.TrackUploadScheduler.enqueue(androidContext()) },
            onAltitudeConsentChanged = { on ->
                if (on) {
                    com.z_company.loco_driver.track.AltitudeContribScheduler.enqueue(androidContext())
                    com.z_company.loco_driver.track.GnssZoneContribScheduler.enqueue(androidContext())
                } else {
                    // Места без GPS: то же согласие. Накопленное удаляем, отправку отменяем, tag забываем.
                    com.z_company.loco_driver.track.GnssZoneContribScheduler.cancel(androidContext())
                    get<com.z_company.domain.gnss.GnssContribStore>().clear()
                    get<com.z_company.domain.gnss.GnssContribTag>().reset()
                    // Отзыв №2: накопленные, но не отправленные точки удаляем, отправку отменяем.
                    com.z_company.loco_driver.track.AltitudeContribScheduler.cancel(androidContext())
                    get<com.z_company.domain.track.AltitudeContribStore>().clear()
                }
            },
        )
    }
    // Вклад высот (согласие №2): временные точки сеанса (не Track.db), кэш осей участков, отправка.
    single<com.z_company.domain.track.AltitudeContribStore> {
        com.z_company.route.autorecord.FileAltitudeContribStore(java.io.File(androidContext().filesDir, "altitude_contrib"))
    }
    single<com.z_company.domain.track.AxisCache> {
        com.z_company.route.autorecord.FileAxisCache(java.io.File(androidContext().cacheDir, "advisor_axes"))
    }
    // Профили участков для экрана «Профиль пути» (общие данные, без входа): список и участки с кэшем на диске.
    single<com.z_company.repository.remote_rest.tracks.SectionProfileCache> {
        com.z_company.route.autorecord.FileSectionProfileCache(java.io.File(androidContext().filesDir, "advisor_profiles"))
    }
    single {
        // Участки отдаются только вошедшему пользователю: токен берём из защищённого хранилища на каждый запрос.
        bindAdvisorToken(get(), androidContext())
        com.z_company.repository.remote_rest.tracks.SectionProfileRepository(
            api = RemoteRestClient.advisorApi,
            cache = get(),
        )
    }
    single {
        bindAdvisorToken(get(), androidContext())
        com.z_company.repository.remote_rest.tracks.AltitudeContributor(
            api = RemoteRestClient.advisorApi,
            store = get(),
            axes = get(),
            settings = get<com.z_company.route.autorecord.TrackConsentStore>(),
            tools = get(),
        )
    }
    // Вклад «места без GPS» (согласие №2 в редакции с GPS): временные ячейки сеанса, случайный tag, отправка.
    single<com.z_company.domain.gnss.GnssContribStore> {
        com.z_company.route.autorecord.FileGnssContribStore(java.io.File(androidContext().filesDir, "gnss_contrib"))
    }
    single {
        com.z_company.domain.gnss.GnssContribTag(
            com.z_company.route.autorecord.PrefsGnssTagStorage(
                androidContext().getSharedPreferences(
                    com.z_company.route.autorecord.PrefsGnssTagStorage.PREFS_NAME,
                    android.content.Context.MODE_PRIVATE,
                ),
            ),
        )
    }
    single {
        com.z_company.repository.remote_rest.tracks.GnssZoneContributor(
            api = RemoteRestClient.gnssZonesApi,
            store = get(),
            settings = get<com.z_company.route.autorecord.TrackConsentStore>().gnssContribSettings,
            tag = get(),
        )
    }
    // Общая карта зон без GPS: кэш плиток в cacheDir; серверные ячейки видят только вошедшие.
    single {
        com.z_company.repository.remote_rest.tracks.GnssZonesRepository(
            api = RemoteRestClient.gnssZonesApi,
            store = com.z_company.route.autorecord.FileGnssTileStore(java.io.File(androidContext().cacheDir, "gnss_zones")),
            token = {
                get<com.z_company.repository.SecureTokenStorage>().getAuthBearerTokenFlow().first()
                    ?.takeIf { it.isNotBlank() }?.let { "Bearer $it" }
            },
        )
    }
    // История треков на сервере (этап 3б): кодек, связь поезд -> рейс, отправка, чтение.
    single<com.z_company.domain.track.TrackBlobTools> {
        com.z_company.repository.remote_rest.tracks.AndroidTrackBlobTools()
    }
    single<com.z_company.route.autorecord.TrackUploadTrigger> {
        com.z_company.route.autorecord.TrackUploadTrigger {
            com.z_company.loco_driver.track.TrackUploadScheduler.enqueue(androidContext())
        }
    }
    single { com.z_company.route.autorecord.TrackRouteResolver(routeUseCase = get(), trackRepository = get()) }
    single {
        com.z_company.repository.remote_rest.tracks.TrackUploader(
            api = RemoteRestClient.tracksApi,
            repository = get(),
            tools = get(),
            resolver = get<com.z_company.route.autorecord.TrackRouteResolver>(),
            settings = get<com.z_company.route.autorecord.TrackConsentStore>(),
            isSubscribed = { get<com.z_company.route.viewmodel.RouteActionsHelper>().hasActiveSubscription() },
        )
    }
    single { com.z_company.repository.remote_rest.tracks.TrackHistoryLoader(api = RemoteRestClient.tracksApi, tools = get()) }
    // Продление токена при старте приложения (POST /v1/auth/refresh); зовёт StartApp.
    single { SessionRefresher(remoteRestApi = get(), secureTokenStorage = get()) }

    // Ktor-based API (заменяет Retrofit RemoteRestClient)
    single<RemoteRestApi> { RemoteRestClient.remoteRestApi }
    single<ApiForSendEmail> { RemoteRestClient.apiForSendEmail }

    // Managers теперь классы с DI (Шаг 2: object → class)
    single<AnnouncementRepository> { RemoteAnnouncementRepository(api = get()) }

    single {
        AuthManager(
            remoteRestApi = get(),
            apiForSendEmail = get(),
            // client_id из secret.properties (VKIDClientID) — с ним сервер
            // проверяет VK access token у нужного приложения.
            vkClientId = BuildConfig.VKID_CLIENT_ID.takeIf { it.isNotBlank() }
        )
    }
    single { RoutesManager(remoteRestApi = get(), brakeCertificates = get()) }
    single { SettingManager(remoteRestApi = get()) }
    single { ShareRouteManager(remoteRestApi = get()) }

    single { RouteActionsHelper() }
    single { SubscriptionHelper() }

    // Widget updater (Glance)
    single<WidgetUpdater> { GlanceWidgetUpdater(androidContext()) }
    // Шаг 10: SyncManager — конструкторная инжекция (KoinComponent → DI)
    single {
        SyncManager(
            settingsUseCase = get(),
            salarySettingUseCase = get(),
            calendarUseCase = get(),
            productionCalendarUseCase = get(),
            releaseDayUseCase = get(),
            routeUseCase = get(),
            routesManager = get(),
            settingManager = get(),
            sharedPrefs = get(),
            locomotiveSeriesRepository = get(),
            stationNormRepository = get(),
            partnerRepository = get(),
            remoteRestApi = get(),
            brakeCertificates = get(),
            sectionProfiles = get(),
            clientPlatform = "android",
            clientVersion = BuildConfig.VERSION_NAME,
        )
    }
}
