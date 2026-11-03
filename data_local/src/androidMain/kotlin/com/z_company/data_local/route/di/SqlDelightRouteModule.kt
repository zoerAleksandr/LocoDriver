package com.z_company.data_local.route.di

import com.z_company.data_local.DatabaseDriverFactory
import com.z_company.data_local.brake.SqlDelightBrakeCertificateRepository
import com.z_company.data_local.brake.db.BrakeCertificateDatabase
import com.z_company.domain.brake.storage.BrakeCertificateRepository
import com.z_company.domain.brake.storage.BrakeCertificateUseCase
import com.z_company.data_local.route.SqlDelightHistoryResponseRepository
import com.z_company.data_local.route.SqlDelightRouteRepository
import com.z_company.data_local.route.db.RouteDatabase
import com.z_company.data_local.route.searchdb.SearchResponseDatabase
import com.z_company.data_local.track.SqlDelightTrackRepository
import com.z_company.data_local.track.db.TrackDatabase
import com.z_company.domain.track.TrackRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val sqlDelightRouteModule = module {
    single { DatabaseDriverFactory(androidContext()) }

    single {
        RouteDatabase(get<DatabaseDriverFactory>().createRouteDriver())
    }

    single {
        SearchResponseDatabase(get<DatabaseDriverFactory>().createSearchResponseDriver())
    }

    // Локальные GPS-треки «АВТО» (отдельный файл Track.db).
    single { TrackDatabase(get<DatabaseDriverFactory>().createTrackDriver()) }
    single<TrackRepository> { SqlDelightTrackRepository(get()) }
    // Журнал проследования ограничений (тот же Track.db, миграция 3.sqm).
    single<com.z_company.domain.track.RestrictionJournalStore> {
        com.z_company.data_local.track.SqlDelightRestrictionJournalStore(get()) { System.currentTimeMillis() }
    }

    // Справки ВУ-45 (отдельный файл BrakeCertificate.db).
    single { BrakeCertificateDatabase(get<DatabaseDriverFactory>().createBrakeCertificateDriver()) }
    single<BrakeCertificateRepository> { SqlDelightBrakeCertificateRepository(get()) }
    single { BrakeCertificateUseCase(get()) }
}
